package io.github.trae.spigot.framework.window;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.UtilTask;
import io.github.trae.spigot.framework.window.events.ButtonClickEvent;
import io.github.trae.spigot.framework.window.events.WindowCloseEvent;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.time.temporal.ChronoUnit;

/**
 * Drives every {@link Window} interaction: keeps the {@link WindowManager} tracking maps in step
 * with inventory open, close, and quit, dispatches clicks to buttons, and blocks any attempt to move
 * items into or out of a window.
 * <p>
 * Every handler resolves the window from the inventory's holder rather than from the manager, so an
 * inventory that is not a window's is left entirely alone.
 */
@AllArgsConstructor
@Singleton
public class WindowListener implements Listener {

    private final WindowManager windowManager;

    /**
     * Records the window against the player and its inventory, then fires
     * {@link Window#onOpen(Player)}.
     * <p>
     * Tracking hangs off the event rather than the open call, so a window opened by any route is
     * registered.
     *
     * @param event the inventory open event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onInventoryOpen(final InventoryOpenEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getPlayer() instanceof final Player player)) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof final Window window)) {
            return;
        }

        this.windowManager.getWindowByPlayerMap().put(player.getUniqueId(), window);
        this.windowManager.getWindowByInventoryMap().put(window.getInventory(), window);

        window.onOpen(player);
    }

    /**
     * Drops the window's tracking entries and fires {@link Window#onClose(Player)}.
     * <p>
     * A cancelled {@link WindowCloseEvent} or a {@link Window#canClose(Player)} returning
     * {@code false} instead re-opens the inventory a tick later, holding the player in the window.
     * The delay is required: an open issued during close processing is discarded by the client.
     * <p>
     * Both removals match on the window as well as the key, so a button that opens a second window
     * cannot have this close wipe the entry the new window just registered.
     *
     * @param event the inventory close event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onInventoryClose(final InventoryCloseEvent event) {
        final Inventory inventory = event.getInventory();

        if (!(event.getPlayer() instanceof final Player player)) {
            return;
        }

        if (!(inventory.getHolder() instanceof final Window window)) {
            return;
        }

        if (UtilEvent.supply(new WindowCloseEvent(window, player)).isCancelled() || !window.canClose(player)) {
            UtilTask.executeLaterSynchronous(() -> player.openInventory(inventory), 50, ChronoUnit.MILLIS);
            return;
        }

        this.windowManager.getWindowByPlayerMap().remove(event.getPlayer().getUniqueId(), window);
        this.windowManager.getWindowByInventoryMap().remove(inventory, window);

        window.onClose(player);
    }

    /**
     * Drops both tracking entries for a quitting player, since no close event is guaranteed.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onPlayerQuit(final PlayerQuitEvent event) {
        final Window window = this.windowManager.getWindowByPlayerMap().remove(event.getPlayer().getUniqueId());
        if (window != null) {
            this.windowManager.getWindowByInventoryMap().remove(window.getInventory(), window);
        }
    }

    /**
     * Cancels the click and, when it landed on the window itself, dispatches it to the button in
     * that slot.
     * <p>
     * The cancel comes first and applies to every click while a window is open, including
     * shift-clicks from the player's own inventory, so nothing can be moved into or out of a window
     * regardless of which half was clicked.
     * <p>
     * A click reaches {@link Button#onClick(Player, ClickType)} only after passing the click
     * cooldown, the {@link ButtonClickEvent}, and {@link Button#canClick(Player, ClickType)}. The
     * cooldown is recorded only for a click that actually ran, so a refused click does not throttle
     * the next attempt.
     *
     * @param event the inventory click event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onInventoryClick(final InventoryClickEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof final Player player)) {
            return;
        }

        final Inventory inventory = event.getInventory();

        if (!(inventory.getHolder() instanceof final Window window)) {
            return;
        }

        event.setCancelled(true);

        if (!inventory.equals(event.getClickedInventory())) {
            return;
        }

        if (this.windowManager.hasCooldown(player, WindowManager.BUTTON_CLICK_COOLDOWN_NAME)) {
            return;
        }

        window.getButtonBySlot(event.getSlot()).ifPresent(button -> {
            final ClickType clickType = event.getClick();

            if (UtilEvent.supply(new ButtonClickEvent(window, button, player)).isCancelled() || !button.canClick(player, clickType)) {
                return;
            }

            button.onClick(player, clickType);

            this.windowManager.addCooldown(player, WindowManager.BUTTON_CLICK_COOLDOWN_NAME);
        });
    }

    /**
     * Cancels any drag touching a window's inventory, the other route by which items could be moved
     * into one.
     *
     * @param event the inventory drag event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onInventoryDrag(final InventoryDragEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof Window)) {
            return;
        }

        event.setCancelled(true);
    }
}