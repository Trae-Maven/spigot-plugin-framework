package io.github.trae.spigot.framework.damage.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.DamageManager;
import io.github.trae.spigot.framework.damage.configs.DamageConfig;
import io.github.trae.spigot.framework.damage.events.damage.CustomPreDamageEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * Applies the critical hit multiplier.
 *
 * <p>Whether an attack qualifies is decided once at the pre stage, taken from vanilla's own decision,
 * and carried on the event, so this only reads that flag rather than re-testing the attacker's
 * state.</p>
 *
 * <p>Sweep attacks are excluded: a sweep only happens on a grounded swing, which is the opposite of
 * what a critical requires.</p>
 *
 * <p>Whether criticals count, and what they multiply by, are read from {@link DamageConfig.Critical}
 * on each hit, so a reload takes effect on the next one.</p>
 */
@RequiredArgsConstructor
@Singleton
public final class DamageCriticalListener implements Listener {

    private final DamageManager damageManager;

    /**
     * Files the critical multiplier under {@link DamageModifier#CRITICAL}.
     *
     * <p>Its own key rather than sharing with the weapon, so both compose instead of one overwriting
     * the other.</p>
     *
     * @param event the pre stage
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCustomPreDamage(final CustomPreDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (event.getCause() != DamageCause.ENTITY_ATTACK || !event.isCritical()) {
            return;
        }

        final DamageConfig.Critical critical = this.damageManager.getDamageConfig().getCritical();
        if (!critical.isEnabled()) {
            return;
        }

        event.setMultiplier(DamageModifier.CRITICAL, critical.getMultiplier());
    }
}