package io.github.trae.spigot.framework.window;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * A single clickable slot within a {@link Window}.
 * <p>
 * Buttons are constructed by their window during {@link Window#populate(Player)} and are therefore
 * as short-lived as the window that holds them, so a button is free to hold whatever state that
 * render needs. The stack returned by {@link #getItemStack()} is resolved on every refresh, so a
 * button whose appearance depends on changing state simply returns a different stack the next time
 * the window redraws.
 */
@AllArgsConstructor
@Getter
public abstract class Button {

    /**
     * The inventory slot this button occupies. Two buttons in the same window must not share a slot,
     * as the later registration replaces the earlier.
     */
    private final int slot;

    /**
     * Returns the stack rendered in this button's slot. Called on every window refresh, so the
     * result may vary between renders.
     *
     * @return the stack to display
     */
    protected abstract ItemStack getItemStack();

    /**
     * Returns whether the given player may click this button with the given click type (e.g. gated
     * behind a permission, or restricted to left clicks). Defaults to {@code true}.
     * <p>
     * This is the window-level check, evaluated after the system-level
     * {@link io.github.trae.spigot.framework.window.events.ButtonClickEvent} and used for conditions
     * the button itself owns.
     *
     * @param player    the player clicking
     * @param clickType the type of click
     * @return {@code true} if the click should proceed to {@link #onClick(Player, ClickType)}
     */
    protected boolean canClick(final Player player, final ClickType clickType) {
        return true;
    }

    /**
     * Performs this button's action. Called only once the click has passed both the
     * {@link io.github.trae.spigot.framework.window.events.ButtonClickEvent} and
     * {@link #canClick(Player, ClickType)}.
     * <p>
     * The underlying inventory click is always cancelled before this runs, so the stack itself never
     * moves.
     *
     * @param player    the player who clicked
     * @param clickType the type of click
     */
    protected abstract void onClick(final Player player, final ClickType clickType);
}