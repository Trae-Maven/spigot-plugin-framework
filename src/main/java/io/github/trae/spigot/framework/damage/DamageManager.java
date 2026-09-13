package io.github.trae.spigot.framework.damage;

import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.data.CustomReason;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.utilities.UtilJava;
import io.github.trae.utilities.mixins.ExpiredMixin;
import lombok.Getter;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.damage.CraftDamageSource;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Applies resolved damage to an entity, reproducing vanilla's side effects.
 *
 * <p>The framework cancels vanilla's own damage handling, so everything vanilla would have done has
 * to be done here: health, absorption, the hurt animation and flash, mob aggro, the combat tracker
 * that names a killer in death messages, statistics, advancements and death itself.</p>
 *
 * <p>Nothing here re-enters {@code EntityDamageEvent}, which is why no reentrancy guard is needed.
 * The tradeoff is that armour, enchantment and resistance reduction are not applied here; those are
 * the pipeline's job, and by this point they are already folded into the resolved figure.</p>
 *
 * <p>The last damage pass that landed on each entity is retained, since cancelling vanilla's
 * handling also means {@code getLastDamageCause} is never populated. That record is what the death
 * system reads to find out who killed whom and with what. Alongside it, any lingering
 * {@link CustomReason} an attacker has on that entity is retained separately, so a death can be
 * attributed to an ability whose effect outlasted the hit that applied it.</p>
 *
 * <h2>Known gaps</h2>
 * <p>Three pieces of vanilla behaviour are unreachable from a plugin because the methods behind them
 * are not visible: totem of undying, the entity-specific hurt sound, and the damagee's
 * {@code getLastDamageSource}. Everything else is covered.</p>
 *
 * @see CustomPostDamageEvent
 */
@Getter
@Singleton
public class DamageManager {

    private static final long RETENTION = TimeUnit.MINUTES.toMillis(1L);

    /**
     * The last damage pass that landed on each entity, by UUID.
     *
     * <p>Entries are written for every hit, dropped by the death system once a death has been read,
     * and swept on a timer for entities that take damage and never die.</p>
     */
    private final ConcurrentHashMap<UUID, CustomPostDamageEvent> lastDamageMap = new ConcurrentHashMap<>();

    /**
     * The lingering reason each attacker has on each damagee, by damagee then attacker UUID.
     *
     * <p>Only a {@link CustomReason} is retained here, since a plain reason describes one hit and
     * has nothing to outlive it. Keyed per attacker so two players applying different effects to
     * the same target are each credited for their own.</p>
     *
     * <p>The death system reads this before falling back to the killing pass's own reason, which is
     * what attributes a death to the ability that caused it rather than to whatever landed last.</p>
     */
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, CustomReason>> lastCustomReasonMap = new ConcurrentHashMap<>();

    /**
     * Drops retained records older than the retention window.
     *
     * <p>Nothing else clears these for a mob, since one that takes damage and never dies leaves its
     * entry behind, and mobs die or unload without notice. The window only needs to outlive the gap
     * between a killing blow and the death being read, which is the same tick, so a minute is
     * generous.</p>
     */
    @Scheduler(period = 1, unit = TimeUnit.MINUTES)
    public final void onScheduler() {
        final long now = System.currentTimeMillis();

        this.lastDamageMap.values().removeIf(event -> now - event.getSystemTime() >= RETENTION);

        this.lastCustomReasonMap.values().forEach(map -> map.values().removeIf(ExpiredMixin::hasExpired));
        this.lastCustomReasonMap.values().removeIf(ConcurrentHashMap::isEmpty);
    }

    /**
     * Applies the resolved damage from a completed damage pass.
     *
     * <p>Runs the same sequence vanilla does, in vanilla's order: state flags, absorption, health,
     * aggro, the hurt broadcast, then statistics, then death. Returns without doing anything if the
     * damagee is not living, is already dead or removed, or is invulnerable to the source, which
     * covers creative players, fire-immune mobs and fall-immune types in one call.</p>
     *
     * @param event the completed post stage
     */
    public final void apply(final CustomPostDamageEvent event) {
        if (!(event.getDamagee() instanceof final LivingEntity bukkitDamagee)) {
            return;
        }

        final net.minecraft.world.entity.LivingEntity damagee = UtilJava.cast(CraftLivingEntity.class, bukkitDamagee).getHandle();
        if (damagee.isRemoved() || damagee.isDeadOrDying()) {
            return;
        }

        final ServerLevel serverLevel = UtilJava.cast(CraftWorld.class, bukkitDamagee.getWorld()).getHandle();
        final DamageSource damageSource = this.getDamageSource(event, serverLevel);

        if (damagee.isInvulnerableTo(serverLevel, damageSource)) {
            return;
        }

        this.lastDamageMap.put(bukkitDamagee.getUniqueId(), event);

        if (event.getDamager() != null && event.getReason() instanceof final CustomReason customReason) {
            this.lastCustomReasonMap.computeIfAbsent(bukkitDamagee.getUniqueId(), __ -> new ConcurrentHashMap<>()).put(event.getDamager().getUniqueId(), customReason);
        }

        final float damage = (float) event.getFinalDamage();

        damagee.setNoActionTime(0);
        damagee.walkAnimation.setSpeed(1.5F);

        damagee.hurtDuration = 10;
        damagee.hurtTime = damagee.hurtDuration;
        damagee.lastHurt = damage;

        this.applyAbsorption(damagee, damageSource, damage);

        this.applyAggro(event, damagee);

        damagee.level().broadcastDamageEvent(damagee, damageSource);
        damagee.hurtMarked = true;
        damagee.gameEvent(GameEvent.ENTITY_DAMAGE);

        this.applyDamageeStatistics(damagee, damage);
        this.applyDamagerStatistics(event, damagee, damageSource, damage);

        if (damagee.isDeadOrDying()) {
            damagee.die(damageSource);
        }
    }

    /**
     * Drains absorption first, then applies what is left to health.
     *
     * <p>Only the portion that reaches health is recorded against the combat tracker, matching
     * vanilla, so damage fully absorbed does not name a killer.</p>
     *
     * @param damagee      the entity taking the damage
     * @param damageSource the source being recorded
     * @param damage       the resolved damage
     */
    private void applyAbsorption(final net.minecraft.world.entity.LivingEntity damagee, final DamageSource damageSource, final float damage) {
        final float absorption = damagee.getAbsorptionAmount();
        final float remaining = Math.max(damage - absorption, 0.0F);

        damagee.setAbsorptionAmount(absorption - (damage - remaining));

        if (remaining <= 0.0F) {
            return;
        }

        damagee.getCombatTracker().recordDamage(damageSource, remaining);
        damagee.setHealth(damagee.getHealth() - remaining);
    }

    /**
     * Marks the damager as the damagee's last attacker.
     *
     * <p>This one field is what makes mobs retaliate: hurt-by-target behaviour reads it on the next
     * tick and acquires a target from it, which is how a spider turns on a player who hits it in
     * daylight, and how neutral mobs become angry.</p>
     *
     * @param event   the completed post stage
     * @param damagee the entity taking the damage
     */
    private void applyAggro(final CustomPostDamageEvent event, final net.minecraft.world.entity.LivingEntity damagee) {
        if (!(event.getDamager() instanceof final LivingEntity bukkitDamager)) {
            return;
        }

        damagee.setLastHurtByMob(UtilJava.cast(CraftLivingEntity.class, bukkitDamager).getHandle());
    }

    /**
     * Records damage taken against the damagee's statistics, when it is a player.
     *
     * @param damagee the entity taking the damage
     * @param damage  the resolved damage
     */
    private void applyDamageeStatistics(final net.minecraft.world.entity.LivingEntity damagee, final float damage) {
        if (!(damagee instanceof final ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.awardStat(Stats.DAMAGE_TAKEN, Math.round(damage * 10.0F));
    }

    /**
     * Records damage dealt and fires the advancement trigger, when the damager is a player.
     *
     * @param event        the completed post stage
     * @param damagee      the entity taking the damage
     * @param damageSource the source being attributed
     * @param damage       the resolved damage
     */
    private void applyDamagerStatistics(final CustomPostDamageEvent event, final net.minecraft.world.entity.LivingEntity damagee, final DamageSource damageSource, final float damage) {
        if (!(event.getDamager() instanceof final LivingEntity bukkitDamager)) {
            return;
        }

        if (!(UtilJava.cast(CraftLivingEntity.class, bukkitDamager).getHandle() instanceof final ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.awardStat(Stats.DAMAGE_DEALT, Math.round(damage * 10.0F));

        CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(serverPlayer, damagee, damageSource, damage, damage, false);
    }

    /**
     * Unwraps the event's damage source, falling back to a generic one.
     *
     * <p>The fallback exists for paths that never carried a source. It costs the death message its
     * specificity, so it is a last resort rather than a default.</p>
     *
     * @param event       the completed post stage
     * @param serverLevel the level the damagee is in
     * @return the vanilla damage source
     */
    private DamageSource getDamageSource(final CustomPostDamageEvent event, final ServerLevel serverLevel) {
        if (event.getSource() != null) {
            return UtilJava.cast(CraftDamageSource.class, event.getSource()).getHandle();
        }

        return serverLevel.damageSources().generic();
    }
}