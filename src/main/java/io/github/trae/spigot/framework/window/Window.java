package io.github.trae.spigot.framework.window;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A menu backed by its own {@link Inventory}, composed of {@link Button}s placed into slots.
 * <p>
 * The window is its own {@link InventoryHolder}, so a click on its inventory resolves straight back
 * to the window without any registry lookup. That is what {@link WindowListener} dispatches on; the
 * maps in {@link WindowManager} exist only for querying which window a player currently has open.
 * <p>
 * The inventory is created once in the constructor and reused for the window's whole lifetime, so
 * rendering into it updates what a viewer is already looking at rather than reopening. Scope
 * therefore follows instance lifetime: a window constructed per open is private to that player,
 * while one held as a singleton is shared by everyone who opens it.
 * <p>
 * Opening goes through {@link io.github.trae.spigot.framework.utility.UtilWindow}, which fires the
 * open event and renders before showing the inventory.
 */
public abstract class Window implements InventoryHolder {

    /**
     * The buttons currently placed in this window, keyed by slot. Rebuilt on every
     * {@link #render(Player)}.
     */
    private final Map<Integer, Button> buttonMap = new HashMap<>();

    /**
     * The inventory this window renders into, created once and reused for the window's lifetime.
     */
    @Getter
    private final Inventory inventory;

    /**
     * Creates a window with the given title and height.
     *
     * @param title the inventory title
     * @param rows  the number of rows, each nine slots wide
     */
    protected Window(final Component title, final int rows) {
        this.inventory = Bukkit.getServer().createInventory(this, rows * 9, title);
    }

    /**
     * Places a button into this window at its declared slot, replacing whatever occupied that slot.
     * Called from {@link #populate(Player)}.
     *
     * @param button the button to place
     */
    protected final void addButton(final Button button) {
        this.buttonMap.put(button.getSlot(), button);
    }

    /**
     * Returns the button occupying the given slot.
     *
     * @param slot the slot to look up
     * @return an {@link Optional} containing the button, or empty if the slot holds none
     */
    public final Optional<Button> getButtonBySlot(final int slot) {
        return Optional.ofNullable(this.buttonMap.get(slot));
    }

    /**
     * Rebuilds the window from scratch: discards the current buttons, re-runs
     * {@link #populate(Player)}, and redraws the inventory.
     * <p>
     * This is the full rebuild, used for the initial open and whenever the underlying data changed
     * and the button set itself may differ. Because the inventory is reused, calling this on a
     * window someone is currently viewing updates it in place without closing it.
     *
     * @param player the player the window is being rendered for
     */
    public final void render(final Player player) {
        this.buttonMap.clear();

        this.populate(player);

        this.refresh();
    }

    /**
     * Redraws the existing buttons into the inventory without re-running {@link #populate(Player)}.
     * <p>
     * Use this when the buttons themselves are unchanged and only their rendered stacks need
     * refreshing; use {@link #render(Player)} when the set of buttons may have changed.
     */
    public final void refresh() {
        this.inventory.clear();

        for (final Button button : this.buttonMap.values()) {
            this.inventory.setItem(button.getSlot(), button.getItemStack());
        }
    }

    /**
     * Called after the window has been shown to a player. Does nothing by default.
     *
     * @param player the player the window was opened for
     */
    public void onOpen(final Player player) {
    }

    /**
     * Called after a player has closed the window and its tracking entries have been dropped. Does
     * nothing by default.
     *
     * @param player the player who closed the window
     */
    public void onClose(final Player player) {
    }

    /**
     * Returns whether this window may be opened for the given player. Returning {@code false}
     * aborts the open, leaving whatever they currently have open in place. Defaults to {@code true}.
     *
     * @param player the player the window would be opened for
     * @return {@code true} if the open may proceed
     */
    public boolean canOpen(final Player player) {
        return true;
    }

    /**
     * Returns whether this window may be closed by the given player. Returning {@code false}
     * re-opens the inventory a tick later, holding the player in the window. Defaults to
     * {@code true}.
     *
     * @param player the player attempting to close
     * @return {@code true} if the close may proceed
     */
    public boolean canClose(final Player player) {
        return true;
    }

    /**
     * Registers this window's buttons via {@link #addButton(Button)}. Called on every
     * {@link #render(Player)} with the button map already cleared, so an implementation always
     * builds the full set rather than adding to what was there before.
     *
     * @param player the player the window is being rendered for
     */
    protected abstract void populate(final Player player);
}