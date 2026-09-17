package io.github.trae.spigot.framework.damage.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.DamageManager;
import io.github.trae.spigot.framework.damage.configs.DamageConfig;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.damage.events.damage.CustomPreDamageEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.tag.DamageTypeTags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies the potion effects that change how much damage a hit deals.
 *
 * <p>Strength and weakness change a player's melee damage. Vanilla folds them into the attack damage
 * attribute, but a player's melee hit starts from a base of zero and resolves the held item itself,
 * so both are added here under {@link DamageModifier#POTION}. A mob's attack already carries them in
 * vanilla's figure, so only players are resolved.</p>
 *
 * <p>With old combat disabled, the bonus is scaled by how charged the swing was, as vanilla scales
 * the whole attribute: the configured minimum plus the configured range times the charge squared.
 * Vanilla resets the charge before the damage event fires, so it is captured when the attack
 * begins.</p>
 *
 * <p>Resistance reduces damage by the configured share per level, floored at zero. Whether it applies
 * is decided by the damage type's tags, as vanilla decides it, rather than by the cause.</p>
 *
 * <p>Every figure is read from {@link DamageConfig.PotionEffect} on each hit, so a reload takes effect
 * on the next one. Effects vanilla already accounts for are left alone: poison and wither are dealt by
 * {@link DamageIntervalListener}, absorption is drained by the damage manager, and jump boost and slow
 * falling reduce fall damage before the event fires.</p>
 */
@RequiredArgsConstructor
@Singleton
public class DamagePotionEffectListener implements Listener {

    private final DamageManager damageManager;

    /**
     * The attack charge each player had when their current attack began, keyed by their identifier
     * and consumed by the damage pass it belongs to.
     */
    private final Map<UUID, Float> attackChargeMap = new HashMap<>();

    /**
     * Records the attacker's charge before vanilla resets it.
     *
     * <p>Runs at {@code MONITOR} so only an attack that is actually going ahead is recorded. A newer
     * attack overwrites an older record, so a record left behind by a refused hit never reaches a
     * later one.</p>
     *
     * @param event the pre attack event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onPrePlayerAttackEntity(final PrePlayerAttackEntityEvent event) {
        if (event.isCancelled() || !event.willAttack()) {
            return;
        }

        this.attackChargeMap.put(event.getPlayer().getUniqueId(), event.getPlayer().getAttackCooldown());
    }

    /**
     * Files the attacker's strength and weakness under {@link DamageModifier#POTION}.
     *
     * <p>Runs at {@code HIGH}, after the weapon's contribution is in place, so weakness can be held
     * to what the weapon deals: vanilla floors the attack damage attribute at zero, so weakness never
     * reaches into damage from anywhere else. The recorded charge is consumed before the cancellation
     * check, so a hit refused earlier in the stage does not leave it behind.</p>
     *
     * @param event the pre stage
     */
    @EventHandler(priority = EventPriority.HIGH)
    public final void onCustomPreDamage(final CustomPreDamageEvent event) {
        if (!(event.getDamager() instanceof final Player player) || event.getProjectile() != null || !this.usesAttackDamage(event.getCause())) {
            return;
        }

        final Float attackCharge = event.getCause() == DamageCause.ENTITY_ATTACK ? this.attackChargeMap.remove(player.getUniqueId()) : null;

        if (event.isCancelled()) {
            return;
        }

        final DamageConfig damageConfig = this.damageManager.getDamageConfig();

        final double bonus = this.getAttackDamageBonus(player, damageConfig);
        if (bonus == 0.0D) {
            return;
        }

        final double scale = attackCharge == null || damageConfig.isOldCombatEnabled() ? 1.0D : damageConfig.getPotionEffect().getChargeMinimum() + Math.pow(Math.clamp(attackCharge, 0.0D, 1.0D), 2.0D) * damageConfig.getPotionEffect().getChargeRange();

        event.setModifier(DamageModifier.POTION, Math.max(bonus * scale, -event.getModifier(DamageModifier.WEAPON)));
    }

    /**
     * Reduces the damage by the damagee's resistance effect, the configured share per level.
     *
     * <p>Runs early in the post stage, alongside the armour reduction. Floored at zero, so enough
     * levels grant total immunity.</p>
     *
     * @param event the post stage
     */
    @EventHandler(priority = EventPriority.LOW)
    public final void onCustomPostDamage(final CustomPostDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getDamagee() instanceof final LivingEntity damagee) || this.bypassesResistance(event.getSource())) {
            return;
        }

        final PotionEffect potionEffect = damagee.getPotionEffect(PotionEffectType.RESISTANCE);
        if (potionEffect == null) {
            return;
        }

        event.setMultiplier(DamageModifier.RESISTANCE, Math.max(0.0D, 1.0D - (potionEffect.getAmplifier() + 1) * this.damageManager.getDamageConfig().getPotionEffect().getResistancePerLevel()));
    }

    /**
     * Drops the leaving player's recorded charge, so the map holds only players who are online.
     *
     * @param event the quit event
     */
    @EventHandler
    public final void onPlayerQuit(final PlayerQuitEvent event) {
        this.attackChargeMap.remove(event.getPlayer().getUniqueId());
    }

    /**
     * The flat attack damage the player's effects add: the configured amount per level of strength,
     * minus the configured amount per level of weakness.
     *
     * @param player       the attacking player
     * @param damageConfig the damage settings
     * @return the bonus, negative when weakness outweighs strength
     */
    private double getAttackDamageBonus(final Player player, final DamageConfig damageConfig) {
        double bonus = 0.0D;

        final PotionEffect strength = player.getPotionEffect(PotionEffectType.STRENGTH);
        if (strength != null) {
            bonus += damageConfig.getPotionEffect().getStrengthPerLevel() * (strength.getAmplifier() + 1);
        }

        final PotionEffect weakness = player.getPotionEffect(PotionEffectType.WEAKNESS);
        if (weakness != null) {
            bonus -= damageConfig.getPotionEffect().getWeaknessPerLevel() * (weakness.getAmplifier() + 1);
        }

        return bonus;
    }

    /**
     * Whether the attacker's attack damage applies for this cause.
     *
     * <p>Kept in step with the causes the weapon listener resolves.</p>
     *
     * @param damageCause the cause of the damage
     * @return whether strength and weakness apply
     */
    private boolean usesAttackDamage(final DamageCause damageCause) {
        return damageCause == DamageCause.ENTITY_ATTACK || damageCause == DamageCause.ENTITY_SWEEP_ATTACK;
    }

    /**
     * Whether resistance is skipped for this damage source.
     *
     * <p>Mirrors vanilla's check: a type tagged as bypassing effects, such as starvation, skips every
     * effect, and a type tagged as bypassing resistance, such as {@code /kill} or the world border,
     * skips resistance alone. A pass with no source is not reduced.</p>
     *
     * @param damageSource the damage source
     * @return whether resistance is skipped
     */
    @SuppressWarnings("UnstableApiUsage")
    private boolean bypassesResistance(final DamageSource damageSource) {
        if (damageSource == null) {
            return true;
        }

        return DamageTypeTags.BYPASSES_EFFECTS.isTagged(damageSource.getDamageType()) || DamageTypeTags.BYPASSES_RESISTANCE.isTagged(damageSource.getDamageType());
    }
}