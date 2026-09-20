package io.github.trae.spigot.framework.damage.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.events.damage.CustomPreDamageEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * The entry point of the damage pipeline.
 *
 * <p>Takes over vanilla damage: the vanilla event is cancelled and the pre stage is dispatched in
 * its place. Nothing vanilla would have done happens on its own after this, which is why the
 * manager reproduces it by hand.</p>
 *
 * <p>This class does no more than the handover. Advancing through the remaining stages and applying
 * the result is {@link CustomDamageListener}'s job, which keeps the vanilla boundary in one place
 * and the pipeline's own flow in another.</p>
 *
 * @see CustomPreDamageEvent
 * @see CustomDamageListener
 */
@Singleton
public final class DamageListener implements Listener {

    /**
     * Cancels the vanilla event and dispatches the pre stage in its place.
     *
     * <p>The cancellation check on the returned event is what stops the handover when the pre stage
     * was refused, since nothing further happens here either way.</p>
     *
     * @param entityDamageEvent the vanilla event being taken over
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(final EntityDamageEvent entityDamageEvent) {
        if (entityDamageEvent.isCancelled()) {
            return;
        }

        entityDamageEvent.setCancelled(true);

        final CustomPreDamageEvent customPreDamageEvent = UtilEvent.supply(this.getCustomPreDamageEvent(entityDamageEvent));
        if (customPreDamageEvent.isCancelled()) {
            return;
        }
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