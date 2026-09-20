package io.github.trae.spigot.framework.damage.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.DamageManager;
import io.github.trae.spigot.framework.damage.configs.DamageConfig;
import io.github.trae.spigot.framework.damage.events.CustomKnockbackEvent;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

/**
 * Knocks the damagee away from its attacker.
 *
 * <p>Vanilla's knockback is skipped along with the rest of its damage handling, so this reproduces
 * it: half the existing velocity plus a push away from the attacker, with the vertical component
 * capped only while grounded so an airborne target is not launched further.</p>
 *
 * <p>Split across two handlers so the calculation and the application are separately overridable: a
 * listener adjusts the vector on the knockback event without having to recompute it. The strength
 * and vertical cap are read from {@link DamageConfig.Knockback} on each hit, so a reload takes effect
 * on the next one.</p>
 *
 * @see CustomKnockbackEvent
 */
@RequiredArgsConstructor
@Singleton
public final class DamageKnockbackListener implements Listener {

    private static final double DEFAULT_RESISTANCE = 0.0D;
    private static final double MINIMUM_DISTANCE_SQUARED = 1.0E-5D;

    private final DamageManager damageManager;

    /**
     * Calculates the knockback and dispatches it for review.
     *
     * <p>Environmental damage produces none, since there is nothing to be pushed away from. Full
     * knockback resistance returns early rather than dispatching a no-op.</p>
     *
     * @param event the completed post stage
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCustomPostDamage(final CustomPostDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Entity damager = event.getDamager();
        if (damager == null) {
            return;
        }

        if (!(event.getDamagee() instanceof final LivingEntity damagee)) {
            return;
        }

        final DamageConfig.Knockback knockbackConfig = this.damageManager.getDamageConfig().getKnockback();

        final double knockback = knockbackConfig.getStrength() * (1.0D - this.getResistance(damagee));
        if (knockback <= 0.0D) {
            return;
        }

        final Vector direction = this.getDirection(damager, damagee);

        final Vector currentVelocity = damagee.getVelocity();

        final Vector velocity = new Vector(
                currentVelocity.getX() / 2.0D + direction.getX() * knockback,
                damagee.isOnGround() ? Math.min(knockbackConfig.getVerticalLimit(), currentVelocity.getY() / 2.0D + knockback) : currentVelocity.getY(),
                currentVelocity.getZ() / 2.0D + direction.getZ() * knockback
        );

        UtilEvent.dispatch(new CustomKnockbackEvent(event, velocity.clone(), velocity));
    }

    /**
     * Applies whatever velocity survived the knockback event.
     *
     * @param event the knockback event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCustomKnockback(final CustomKnockbackEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getDamageEvent().getDamagee() instanceof final LivingEntity damagee)) {
            return;
        }

        final Vector velocity = event.getVelocity();
        if (velocity == null) {
            return;
        }

        damagee.setVelocity(velocity);
    }

    /**
     * A normalised horizontal vector pointing away from the attacker.
     *
     * <p>Two entities standing on exactly the same spot give no direction at all, so the loop
     * substitutes a small random offset until there is one to normalise. Vanilla does the same.</p>
     *
     * @param damager the attacking entity
     * @param damagee the entity being pushed
     * @return the direction to push in
     */
    private Vector getDirection(final Entity damager, final LivingEntity damagee) {
        double x = damagee.getX() - damager.getX();
        double z = damagee.getZ() - damager.getZ();

        while (x * x + z * z < MINIMUM_DISTANCE_SQUARED) {
            x = (Math.random() - Math.random()) * 0.01D;
            z = (Math.random() - Math.random()) * 0.01D;
        }

        return new Vector(x, 0.0D, z).normalize();
    }

    /**
     * The damagee's knockback resistance, where one is complete immunity.
     *
     * @param damagee the entity being pushed
     * @return the resistance
     */
    private double getResistance(final LivingEntity damagee) {
        final AttributeInstance attributeInstance = damagee.getAttribute(Attribute.KNOCKBACK_RESISTANCE);

        return attributeInstance == null ? DEFAULT_RESISTANCE : attributeInstance.getValue();
    }
}