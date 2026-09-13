package io.github.trae.spigot.framework.damage.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.events.damage.CustomPreDamageEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Applies the critical hit multiplier.
 *
 * <p>Whether an attack qualifies is decided once at the pre stage and carried on the event, so this
 * only reads that flag rather than re-testing the attacker's state.</p>
 *
 * <p>Sweep attacks are excluded: a sweep only happens on a grounded swing, which is the opposite of
 * what a critical requires.</p>
 *
 * @see io.github.trae.spigot.framework.utility.UtilDamage
 */
@Singleton
public class DamageCriticalListener implements Listener {

    private static final double CRITICAL_MULTIPLIER = 1.5D;

    /**
     * Files the critical multiplier under {@link DamageModifier#CRITICAL}.
     *
     * <p>Its own key rather than sharing with the weapon, so both compose instead of one overwriting
     * the other.</p>
     *
     * @param event the pre stage
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public final void onCustomPreDamage(final CustomPreDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }

        if (!event.isCritical()) {
            return;
        }

        event.setMultiplier(DamageModifier.CRITICAL, CRITICAL_MULTIPLIER);
    }
}