package io.github.trae.spigot.framework.tablist.events;

import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired for a single player when their tab list header and footer should be re-resolved.
 * <p>
 * Dispatched on a fixed interval by the tablist manager. Cancelling the event clears the player's
 * tablist instead of applying one, allowing other systems to suppress the display for that player.
 */
@AllArgsConstructor
@Getter
public class TablistUpdateEvent extends CustomCancellableEvent {

    private final Player player;
}