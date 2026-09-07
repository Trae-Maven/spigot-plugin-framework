package io.github.trae.spigot.framework.window;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.window.listeners.WindowListener;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks which {@link Window} each player currently has open, and which window owns each open
 * inventory.
 * <p>
 * The maps are for querying only. Click dispatch never consults them, since a window is its own
 * {@link org.bukkit.inventory.InventoryHolder} and resolves straight off the event, so a momentarily
 * stale map can never misroute a click. {@link WindowListener} keeps them in step with the inventory
 * open, close, and quit events.
 */
@Getter
@Singleton
public class WindowManager {

    /**
     * The window each player currently has open, keyed by their identifier.
     */
    private final Map<UUID, Window> windowByPlayerMap = new HashMap<>();

    /**
     * The window owning each currently open inventory.
     */
    private final Map<Inventory, Window> windowByInventoryMap = new HashMap<>();

    /**
     * Returns the window the given player currently has open.
     *
     * @param player the player to look up
     * @return an {@link Optional} containing the window, or empty if they have none open
     */
    public final Optional<Window> getWindowByPlayer(final Player player) {
        return Optional.ofNullable(this.windowByPlayerMap.get(player.getUniqueId()));
    }

    /**
     * Returns the window owning the given inventory.
     *
     * @param inventory the inventory to look up
     * @return an {@link Optional} containing the window, or empty if the inventory is not a window's
     */
    public final Optional<Window> getWindowByInventory(final Inventory inventory) {
        return Optional.ofNullable(this.windowByInventoryMap.get(inventory));
    }
}