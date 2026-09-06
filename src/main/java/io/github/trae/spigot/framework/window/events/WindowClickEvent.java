package io.github.trae.spigot.framework.window.events;

import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.window.Window;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired when a player clicks anywhere in a window's inventory, before the click is resolved to a
 * button.
 * <p>
 * Dispatched by {@link io.github.trae.spigot.framework.window.WindowListener} for every click on a
 * window, including empty slots and clicks on the player's own inventory while a window is open.
 * Cancelling stops the click being dispatched to a button at all, so it is the coarsest of the
 * three gates: it suppresses every button in the window at once, where
 * {@link ButtonPreClickEvent} suppresses one.
 * <p>
 * The underlying inventory click is cancelled regardless of this event, so nothing can be moved
 * into or out of a window either way.
 */
@AllArgsConstructor
@Getter
public class WindowClickEvent extends CustomCancellableEvent {

    /**
     * The window that was clicked.
     */
    private final Window window;

    /**
     * The player who clicked.
     */
    private final Player player;
}