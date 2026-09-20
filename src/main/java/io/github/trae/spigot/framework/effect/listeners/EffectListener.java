package io.github.trae.spigot.framework.effect.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.effect.Effect;
import io.github.trae.spigot.framework.effect.EffectManager;
import lombok.AllArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Clears effects from their holders on death and disconnect, for the effects that opted into it via
 * {@link Effect#removeOnDeath()} and {@link Effect#removeOnQuit()}.
 * <p>
 * Both handlers run at {@link EventPriority#MONITOR}, so the outcome of the death or the quit is
 * already settled by the time effects are torn down.
 */
@AllArgsConstructor
@Singleton
public final class EffectListener implements Listener {

    private final EffectManager effectManager;

    /**
     * Removes every effect that opted out of surviving death from the entity that died.
     *
     * @param event the death event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(final EntityDeathEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final LivingEntity entity = event.getEntity();

        for (final Effect effect : this.effectManager.getEffectList()) {
            if (!effect.removeOnDeath()) {
                continue;
            }

            if (effect.getUserByLivingEntity(entity).isEmpty()) {
                continue;
            }

            effect.removeUser(entity, Effect.RemoveReason.DEATH);
        }
    }

    /**
     * Removes every effect that opted out of surviving a disconnect from the departing player.
     *
     * @param event the quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        for (final Effect effect : this.effectManager.getEffectList()) {
            if (!effect.removeOnQuit()) {
                continue;
            }

            if (effect.getUserByLivingEntity(player).isEmpty()) {
                continue;
            }

            effect.removeUser(player, Effect.RemoveReason.QUIT);
        }
    }
}