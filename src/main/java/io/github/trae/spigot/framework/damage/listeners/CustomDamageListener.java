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

/**
 * Advances a damage pass through its stages and applies the result.
 *
 * <p>Each handler runs at {@code MONITOR} on one stage and dispatches the next, so a stage that was
 * cancelled by anything at a lower priority never produces a successor. That is what makes
 * cancelling at any point stop the chain without every listener having to check.</p>
 *
 * <p>Splitting this from {@link DamageListener} keeps the vanilla handover separate from the
 * pipeline's own flow: one class knows about {@code EntityDamageEvent}, this one does not.</p>
 *
 * @see CustomPreDamageEvent
 * @see DamageManager
 */
@AllArgsConstructor
@Singleton
public class CustomDamageListener implements Listener {

    private final DamageManager damageManager;

    /**
     * Advances the pre stage to the damage stage.
     *
     * @param event the pre stage
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onCustomPreDamage(final CustomPreDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        UtilEvent.dispatch(new CustomDamageEvent(event));
    }

    /**
     * Advances the damage stage to the post stage.
     *
     * @param event the damage stage
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onCustomDamage(final CustomDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        UtilEvent.dispatch(new CustomPostDamageEvent(event));
    }

    /**
     * Applies the resolved damage once the post stage has settled.
     *
     * <p>Runs at {@code MONITOR}, so the reductions and side effects that sit at lower priorities on
     * this same stage have all had their turn by the time the figure is read.</p>
     *
     * @param event the completed post stage
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onCustomPostDamage(final CustomPostDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        this.damageManager.apply(event);
    }
}