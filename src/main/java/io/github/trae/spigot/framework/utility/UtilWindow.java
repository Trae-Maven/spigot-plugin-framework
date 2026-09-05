package io.github.trae.spigot.framework.utility;

import io.github.trae.spigot.framework.window.Window;
import io.github.trae.spigot.framework.window.events.WindowOpenEvent;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;

/**
 * Entry point for opening a {@link Window}.
 * <p>
 * Exists as a utility rather than a method on the window manager so that windows and buttons can
 * open one another without holding a manager reference. Tracking is not performed here: the manager
 * hangs it off {@code InventoryOpenEvent}, so a window opened through this method is registered the
 * same as one opened by any other route.
 */
@UtilityClass
public class UtilWindow {

    /**
     * Renders the window and opens it for the player.
     * <p>
     * A cancelled {@link WindowOpenEvent} or a {@link Window#canOpen(Player)} returning {@code false}
     * aborts the open, leaving whatever the player currently has open in place. Otherwise
     * {@link Window#render(Player)} rebuilds the buttons for that player before the inventory is
     * shown, so the contents reflect the player being opened for.
     *
     * @param player the player to open the window for
     * @param window the window to open
     */
    public static void open(final Player player, final Window window) {
        if (UtilEvent.supply(new WindowOpenEvent(window, player)).isCancelled() || !window.canOpen(player)) {
            return;
        }

        window.render(player);

        player.openInventory(window.getInventory());
    }
}