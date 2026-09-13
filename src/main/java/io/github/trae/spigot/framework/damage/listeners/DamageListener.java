package io.github.trae.spigot.framework.damage.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.DamageManager;
import io.github.trae.spigot.framework.damage.events.damage.CustomDamageEvent;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.damage.events.damage.CustomPreDamageEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * The entry point of the damage pipeline.
 *
 * <p>Takes over vanilla damage entirely: the vanilla event is cancelled, a three-stage chain runs in
 * its place, and the resolved figure is applied by the manager. Nothing vanilla would have done
 * happens on its own after this, which is why the manager reproduces it by hand.</p>
 *
 * <p>Each stage short-circuits the chain if cancelled, so refusing damage at the pre stage means no
 * reduction, durability, knockback or delay work is done for it.</p>
 *
 * @see CustomPreDamageEvent
 * @see DamageManager
 */
@AllArgsConstructor
@Singleton
public class DamageListener implements Listener {

    private final DamageManager damageManager;

    /**
     * Cancels the vanilla event and runs the pipeline in its place.
     *
     * @param entityDamageEvent the vanilla event being taken over
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onEntityDamage(final EntityDamageEvent entityDamageEvent) {
        if (entityDamageEvent.isCancelled()) {
            return;
        }

        entityDamageEvent.setCancelled(true);

        final CustomPreDamageEvent customPreDamageEvent = UtilEvent.supply(this.getCustomPreDamageEvent(entityDamageEvent));
        if (customPreDamageEvent.isCancelled()) {
            return;
        }

        final CustomDamageEvent customDamageEvent = UtilEvent.supply(new CustomDamageEvent(customPreDamageEvent));
        if (customDamageEvent.isCancelled()) {
            return;
        }

        final CustomPostDamageEvent customPostDamageEvent = UtilEvent.supply(new CustomPostDamageEvent(customDamageEvent));
        if (customPostDamageEvent.isCancelled()) {
            return;
        }

        this.damageManager.apply(customPostDamageEvent);
    }

    /**
     * Builds the pre stage from whichever kind of vanilla event arrived.
     *
     * <p>An attack carries a damager, a held item and critical state; an environmental cause carries
     * none of those, so the two have separate factories rather than one with null checks.</p>
     *
     * @param entityDamageEvent the vanilla event being taken over
     * @return the pre stage event
     */
    private CustomPreDamageEvent getCustomPreDamageEvent(final EntityDamageEvent entityDamageEvent) {
        if (entityDamageEvent instanceof final EntityDamageByEntityEvent entityDamageByEntityEvent) {
            return CustomPreDamageEvent.of(entityDamageByEntityEvent);
        }

        return CustomPreDamageEvent.of(entityDamageEvent);
    }
}