package io.github.trae.spigot.framework.window.events;

import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.window.Window;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired before a window is rendered and shown to a player.
 * <p>
 * Dispatched by {@link io.github.trae.spigot.framework.utility.UtilWindow}. Cancelling aborts the
 * open, leaving whatever the player currently has open in place.
 * <p>
 * This is the system-level gate, for conditions external to the window itself. A condition the
 * window owns belongs in {@link Window#canOpen(Player)} instead.
 */
@AllArgsConstructor
@Getter
public class WindowOpenEvent extends CustomCancellableEvent {

    /**
     * The window being opened.
     */
    private final Window window;

    /**
     * The player the window is being opened for.
     */
    private final Player player;
}