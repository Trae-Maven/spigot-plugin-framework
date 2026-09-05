package io.github.trae.spigot.framework.window.events;

import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.window.Window;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired when a player closes a window, before its tracking entries are dropped.
 * <p>
 * Dispatched by {@link io.github.trae.spigot.framework.window.WindowListener}. Cancelling re-opens
 * the inventory a tick later, holding the player in the window.
 * <p>
 * This is the system-level gate, for conditions external to the window itself. A condition the
 * window owns belongs in {@link Window#canClose(Player)} instead.
 */
@AllArgsConstructor
@Getter
public class WindowCloseEvent extends CustomCancellableEvent {

    /**
     * The window being closed.
     */
    private final Window window;

    /**
     * The player closing the window.
     */
    private final Player player;
}