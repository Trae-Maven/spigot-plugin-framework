package io.github.trae.spigot.framework.window;

import io.github.trae.di.annotations.type.component.Singleton;
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
 * <p>
 * The cooldown hooks are extension points: they do nothing here, so button clicks are unthrottled
 * unless a plugin subclasses this manager and implements them.
 */
@Getter
@Singleton
public class WindowManager {

    /**
     * The cooldown name button clicks are gated under.
     */
    public static final String BUTTON_CLICK_COOLDOWN_NAME = "Button Click";

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

    /**
     * Records a cooldown of the given name against the player. Does nothing by default; override to
     * gate repeated button clicks.
     *
     * @param player the player to record against
     * @param name   the cooldown name
     */
    protected void addCooldown(final Player player, final String name) {
    }

    /**
     * Returns whether the player is currently under a cooldown of the given name. Always
     * {@code false} by default, so clicks are unthrottled until overridden.
     *
     * @param player the player to check
     * @param name   the cooldown name
     * @return {@code true} if the player is on cooldown
     */
    protected boolean hasCooldown(final Player player, final String name) {
        return false;
    }
}