package io.github.trae.spigot.framework.damage.listeners;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.DamageManager;
import io.github.trae.spigot.framework.damage.configs.DamageConfig;
import io.github.trae.utilities.UtilJava;
import io.papermc.paper.event.entity.EntityEffectTickEvent;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Offers every timed damage cause a hit on every server tick, so the damage delay sets its rate.
 *
 * <p>Vanilla fires poison, wither, burning, drowning, freezing and starvation on intervals of its
 * own, hardcoded into the game, so the damage delay could only ever slow them down. Each of them is
 * offered here on every tick for as long as the entity is still in the state that causes it, and the
 * configured delay for that cause decides which of those hits land. The delay is therefore the rate,
 * down to one tick.</p>
 *
 * <p>Every hit goes through vanilla's own damage call, with the damage source and cause vanilla uses
 * and the amount set in {@link DamageConfig.Interval}, so it enters the damage pipeline like any
 * other and keeps vanilla's own checks, such as fire resistance and fire immunity. Poison and wither
 * are applied through the effect itself, which keeps their own damage and poison's floor of one
 * health, and fire {@link EntityEffectTickEvent} first, as vanilla does.</p>
 *
 * <p>Vanilla's own interval hits still run alongside, and the delay refuses them like any other hit
 * inside the window, so nothing is dealt twice.</p>
 *
 * <p>An entity is tracked once poison or wither is applied to it, once it catches fire, or once
 * vanilla deals any timed cause to it, and dropped once it is gone or no timed cause applies any
 * more. Drowning, freezing and starvation therefore begin after their first vanilla hit.</p>
 */
@RequiredArgsConstructor
@Singleton
public final class DamageIntervalListener implements Listener {

    private static final Set<DamageCause> DAMAGE_CAUSE_SET = EnumSet.of(DamageCause.POISON, DamageCause.WITHER, DamageCause.FIRE_TICK, DamageCause.DROWNING, DamageCause.FREEZE, DamageCause.STARVATION);

    private final DamageManager damageManager;

    /**
     * Every entity currently known to be under a timed cause.
     */
    private final Set<LivingEntity> livingEntitySet = new HashSet<>();

    /**
     * Tracks an entity once vanilla deals it any timed cause.
     *
     * <p>Runs at {@code HIGHEST}, before the damage pipeline cancels the event at {@code MONITOR}.</p>
     *
     * @param event the vanilla damage event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(final EntityDamageEvent event) {
        if (event.isCancelled() || event instanceof EntityDamageByEntityEvent) {
            return;
        }

        if (!(event.getEntity() instanceof final LivingEntity livingEntity) || !DAMAGE_CAUSE_SET.contains(event.getCause())) {
            return;
        }

        this.livingEntitySet.add(livingEntity);
    }

    /**
     * Tracks an entity as soon as poison or wither is applied to it, rather than after its first hit.
     *
     * @param event the potion effect event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityPotionEffect(final EntityPotionEffectEvent event) {
        if (event.isCancelled() || event.getNewEffect() == null) {
            return;
        }

        if (!(event.getEntity() instanceof final LivingEntity livingEntity)) {
            return;
        }

        if (!event.getModifiedType().equals(PotionEffectType.POISON) && !event.getModifiedType().equals(PotionEffectType.WITHER)) {
            return;
        }

        this.livingEntitySet.add(livingEntity);
    }

    /**
     * Tracks an entity as soon as it catches fire, rather than after its first burn.
     *
     * @param event the combust event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityCombust(final EntityCombustEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getEntity() instanceof final LivingEntity livingEntity)) {
            return;
        }

        this.livingEntitySet.add(livingEntity);
    }

    /**
     * Offers every tracked entity a hit for each timed cause it is still under.
     *
     * <p>Runs at the end of each tick, on the main thread. Entities are visited through a copy, so a
     * hit that starts a timed cause on another entity cannot disturb the loop. An entity that is gone,
     * or under no timed cause, is dropped. The settings are read once per pass.</p>
     *
     * @param event the tick end event
     */
    @EventHandler
    public void onServerTickEnd(final ServerTickEndEvent event) {
        final DamageConfig.Interval interval = this.damageManager.getDamageConfig().getInterval();

        for (final LivingEntity livingEntity : List.copyOf(this.livingEntitySet)) {
            if (!livingEntity.isValid()) {
                this.livingEntitySet.remove(livingEntity);
                continue;
            }

            final net.minecraft.world.entity.LivingEntity handle = UtilJava.cast(CraftLivingEntity.class, livingEntity).getHandle();
            final ServerLevel serverLevel = UtilJava.cast(CraftWorld.class, livingEntity.getWorld()).getHandle();

            boolean active = false;

            for (final DamageCause damageCause : DAMAGE_CAUSE_SET) {
                if (!this.isActive(livingEntity, handle, damageCause, interval)) {
                    continue;
                }

                active = true;

                if (handle.isDeadOrDying()) {
                    break;
                }

                this.hurt(livingEntity, handle, serverLevel, damageCause, interval);
            }

            if (!active) {
                this.livingEntitySet.remove(livingEntity);
            }
        }
    }

    /**
     * Whether the entity is still in the state that causes the given timed damage.
     *
     * <p>Starvation keeps vanilla's own limits, which live in the hunger timer rather than the damage
     * call: it stops at the configured easy floor on peaceful and easy, at the configured normal floor
     * on normal, and never stops on hard.</p>
     *
     * @param livingEntity the Bukkit view of the entity
     * @param handle       the entity's handle
     * @param damageCause  the timed cause
     * @param interval     the timed damage settings
     * @return whether the cause still applies
     */
    private boolean isActive(final LivingEntity livingEntity, final net.minecraft.world.entity.LivingEntity handle, final DamageCause damageCause, final DamageConfig.Interval interval) {
        return switch (damageCause) {
            case POISON -> handle.hasEffect(MobEffects.POISON);
            case WITHER -> handle.hasEffect(MobEffects.WITHER);
            case FIRE_TICK -> livingEntity.getFireTicks() > 0 && !handle.fireImmune();
            case DROWNING -> livingEntity.getRemainingAir() <= 0;
            case FREEZE -> livingEntity.isFrozen() && handle.canFreeze();
            case STARVATION -> livingEntity instanceof final Player player && player.getFoodLevel() <= 0 && switch (player.getWorld().getDifficulty()) {
                case PEACEFUL, EASY -> player.getHealth() > interval.getStarvationEasyMinimumHealth();
                case NORMAL -> player.getHealth() > interval.getStarvationNormalMinimumHealth();
                case HARD -> true;
            };
            default -> false;
        };
    }

    /**
     * Deals one hit of the given timed cause, the way vanilla deals it, with the configured amount.
     *
     * @param livingEntity the Bukkit view of the entity
     * @param handle       the entity's handle
     * @param serverLevel  the level the entity is in
     * @param damageCause  the timed cause
     * @param interval     the timed damage settings
     */
    private void hurt(final LivingEntity livingEntity, final net.minecraft.world.entity.LivingEntity handle, final ServerLevel serverLevel, final DamageCause damageCause, final DamageConfig.Interval interval) {
        switch (damageCause) {
            case POISON -> this.applyEffect(livingEntity, handle, serverLevel, PotionEffectType.POISON, MobEffects.POISON);
            case WITHER -> this.applyEffect(livingEntity, handle, serverLevel, PotionEffectType.WITHER, MobEffects.WITHER);
            case FIRE_TICK -> handle.hurtServer(serverLevel, handle.damageSources().onFire().knownCause(DamageCause.FIRE_TICK), interval.getFireTickDamage());
            case DROWNING -> handle.hurtServer(serverLevel, handle.damageSources().drown().knownCause(DamageCause.DROWNING), interval.getDrowningDamage());
            case FREEZE -> handle.hurtServer(serverLevel, handle.damageSources().freeze().knownCause(DamageCause.FREEZE), handle.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES) ? interval.getFreezeExtraDamage() : interval.getFreezeDamage());
            case STARVATION -> handle.hurtServer(serverLevel, handle.damageSources().starve().knownCause(DamageCause.STARVATION), interval.getStarvationDamage());
        }
    }

    /**
     * Applies one tick of a damaging effect through the effect's own code.
     *
     * <p>Fires {@link EntityEffectTickEvent} first, as vanilla does, so a plugin that cancels effect
     * ticks is still respected.</p>
     *
     * @param livingEntity     the Bukkit view of the entity
     * @param handle           the entity's handle
     * @param serverLevel      the level the entity is in
     * @param potionEffectType the effect's Bukkit type, for the event
     * @param mobEffect        the effect to apply
     */
    @SuppressWarnings("UnstableApiUsage")
    private void applyEffect(final LivingEntity livingEntity, final net.minecraft.world.entity.LivingEntity handle, final ServerLevel serverLevel, final PotionEffectType potionEffectType, final Holder<MobEffect> mobEffect) {
        final MobEffectInstance mobEffectInstance = handle.getEffect(mobEffect);
        if (mobEffectInstance == null) {
            return;
        }

        if (!new EntityEffectTickEvent(livingEntity, potionEffectType, mobEffectInstance.getAmplifier()).callEvent()) {
            return;
        }

        mobEffect.value().applyEffectTick(serverLevel, handle, mobEffectInstance.getAmplifier());
    }
}