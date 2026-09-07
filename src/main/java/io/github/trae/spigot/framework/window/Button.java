package io.github.trae.spigot.framework.window;

import io.github.trae.spigot.framework.utility.UtilMessage;
import io.github.trae.utilities.UtilJava;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A single clickable slot within a {@link io.github.trae.spigot.framework.window.Window}.
 * <p>
 * Buttons are constructed by their window during {@link io.github.trae.spigot.framework.window.Window#populate(Player)} and are therefore
 * as short-lived as the window that holds them, so a button is free to hold whatever state that
 * render needs. The stack it renders is built by {@link #toItemStack()} on every refresh, so a
 * button whose appearance depends on changing state shows the change the next time the window
 * redraws.
 * <p>
 * The owning window is held and typed, so a button can reach the state its window carries without
 * casting, and act on it directly: paging, toggling a filter, or triggering a re-render.
 *
 * @param <Window> the window type this button belongs to
 */
@AllArgsConstructor
@Getter
public abstract class Button<Window extends io.github.trae.spigot.framework.window.Window> {

    /**
     * The window this button belongs to, typed so its own state and methods are reachable without a
     * cast.
     */
    private final Window window;

    /**
     * The inventory slot this button occupies. Two buttons in the same window must not share a slot,
     * as the later registration replaces the earlier.
     */
    private final int slot;

    /**
     * The base stack this button renders, before its own display name and lore are applied.
     */
    private final ItemStack itemStack;

    /**
     * Returns the display name to apply over the base stack's own, or {@code null} to leave it
     * alone. Defaults to {@code null}.
     *
     * @return the display name, or {@code null}
     */
    protected String getDisplayName() {
        return null;
    }

    /**
     * Returns lore to append beneath the base stack's own, or {@code null} for none. Defaults to
     * {@code null}.
     * <p>
     * Appended rather than replacing, so a button can annotate an item with what clicking it does
     * while leaving the item's own description intact.
     *
     * @return the lore lines, or {@code null}
     */
    protected List<String> getLore() {
        return null;
    }

    /**
     * Returns whether the given player may click this button with the given click type (e.g. gated
     * behind a permission, or restricted to left clicks). Defaults to {@code true}.
     * <p>
     * This is the button-level check, evaluated after the system-level
     * {@link io.github.trae.spigot.framework.window.events.ButtonPreClickEvent} and used for
     * conditions the button itself owns.
     *
     * @param player    the player clicking
     * @param clickType the type of click
     * @return {@code true} if the click should proceed to {@link #onClick(Player, ClickType)}
     */
    public boolean canClick(final Player player, final ClickType clickType) {
        return true;
    }

    /**
     * Performs this button's action. Called only once the click has passed the
     * {@link io.github.trae.spigot.framework.window.events.ButtonPreClickEvent} and
     * {@link #canClick(Player, ClickType)}.
     * <p>
     * The underlying inventory click is always cancelled before this runs, so the stack itself never
     * moves.
     *
     * @param player    the player who clicked
     * @param clickType the type of click
     */
    public abstract void onClick(final Player player, final ClickType clickType);

    /**
     * Builds the stack this button renders, applying its display name and lore over the base stack.
     * <p>
     * The base is copied first, so a button handed a shared stack does not mutate it. A display name
     * the button declares overrides whatever the base carried; lore is appended beneath the base's
     * own, separated by a blank line.
     *
     * @return the stack to place in this button's slot
     */
    protected final ItemStack toItemStack() {
        final ItemStack itemStack = this.itemStack.clone();

        itemStack.editMeta(itemMeta -> {
            if (this.getDisplayName() != null) {
                itemMeta.displayName(UtilMessage.deserialize(this.getDisplayName()).decoration(TextDecoration.ITALIC, false));
            }

            if (this.getLore() != null && !this.getLore().isEmpty()) {
                final List<Component> lore = UtilJava.createCollection(new ArrayList<>(), list -> {
                    if (itemMeta.lore() != null) {
                        list.addAll(itemMeta.lore());
                        list.add(Component.empty());
                    }

                    this.getLore().forEach(line -> list.add(UtilMessage.deserialize(line).decoration(TextDecoration.ITALIC, false)));
                });

                itemMeta.lore(lore);
            }
        });

        return itemStack;
    }
}