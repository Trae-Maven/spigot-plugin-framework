package io.github.trae.spigot.framework.damage.events;

import io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.util.Vector;

/**
 * Fired before knockback is applied to the damagee.
 *
 * <p>Carries the resolved velocity as one value rather than a direction and magnitudes, so a
 * listener changes knockback by changing that vector. Scale it, clamp its vertical component, or
 * replace it outright. Cancel for no knockback at all.</p>
 *
 * <p>The vector is mutable, so a listener scaling it in place is fine; the original is kept
 * separately as a clone for comparison.</p>
 *
 * @see io.github.trae.spigot.framework.damage.listeners.DamageKnockbackListener
 */
@AllArgsConstructor
@Getter
@Setter
public class CustomKnockbackEvent extends CustomCancellableEvent {

    /**
     * The damage pass this belongs to.
     */
    private final AbstractCustomDamageEvent damageEvent;

    /**
     * What the framework calculated before any listener changed it.
     */
    private final Vector originalVelocity;

    /**
     * The velocity to apply.
     */
    private Vector velocity;
}