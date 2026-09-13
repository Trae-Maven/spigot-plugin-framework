package io.github.trae.spigot.framework.death.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.death.events.CustomDeathEvent;
import io.github.trae.spigot.framework.death.events.CustomDeathMessageEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.UtilMessage;
import io.github.trae.spigot.framework.utility.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Replaces vanilla death messages with the framework's own.
 *
 * <p>Vanilla's message is suppressed rather than edited, since the framework's damage pipeline knows
 * things vanilla does not: what the attack was attributed to, what the killer was holding, and
 * whatever a plugin renamed either side to.</p>
 *
 * <p>Messages are only produced for player deaths. Mob deaths still fire the death events, so a
 * plugin wanting to announce those listens to {@link CustomDeathEvent} itself.</p>
 *
 * @see CustomDeathMessageEvent
 */
@Singleton
public class DeathMessageListener implements Listener {

    /**
     * Suppresses vanilla's death message.
     *
     * <p>Runs at {@code LOWEST} so anything reading the message at a higher priority sees it already
     * gone, rather than acting on a message that will never be sent.</p>
     *
     * @param event the vanilla death event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPlayerDeath(final PlayerDeathEvent event) {
        event.deathMessage(null);
    }

    /**
     * Dispatches a message event per online player.
     *
     * <p>One per recipient rather than one broadcast, so each can be cancelled or reworded
     * independently.</p>
     *
     * @param event the death
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onCustomDeath(final CustomDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        for (final Player recipient : UtilServer.getOnlinePlayers()) {
            UtilEvent.dispatch(new CustomDeathMessageEvent(event, recipient));
        }
    }

    /**
     * Sends the message this recipient is getting.
     *
     * <p>Three shapes, in order of specificity: a self-inflicted death names nobody, a death with a
     * killer names them and their weapon when there was one, and anything else names the cause.</p>
     *
     * @param event the message event for one recipient
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onCustomDeathMessage(final CustomDeathMessageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final CustomDeathEvent deathEvent = event.getDeathEvent();

        final CustomPostDamageEvent damageEvent = deathEvent.getDamageEvent();

        final Player recipient = event.getRecipient();

        final Component entityName = event.getEntityName().getFullName();

        final Entity killer = deathEvent.getKiller();

        if (damageEvent.getCause() == EntityDamageEvent.DamageCause.SUICIDE || damageEvent.getCause() == EntityDamageEvent.DamageCause.KILL) {
            UtilMessage.message(recipient, "Death", "%s was killed.".formatted(UtilMessage.serializeWithReset(entityName)));
            return;
        }

        if (killer != null) {
            if (deathEvent.getReason() == null || deathEvent.getReason().getName() == null) {
                UtilMessage.message(recipient, "Death", "%s was killed by %s.".formatted(
                        UtilMessage.serializeWithReset(entityName),
                        UtilMessage.serializeWithReset(event.getFormattedKillerName())
                ));
            } else {
                UtilMessage.message(recipient, "Death", "%s was killed by %s with %s.".formatted(
                        UtilMessage.serializeWithReset(entityName),
                        UtilMessage.serializeWithReset(event.getFormattedKillerName()),
                        UtilMessage.serializeWithReset(deathEvent.getFormattedReason())
                ));
            }
            return;
        }

        UtilMessage.message(recipient, "Death", "%s was killed by %s.".formatted(
                UtilMessage.serializeWithReset(entityName),
                UtilMessage.serializeWithReset(damageEvent.getCauseName())
        ));
    }
}