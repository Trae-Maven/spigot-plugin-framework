package io.github.trae.spigot.framework.item;

import io.github.trae.spigot.framework.item.enums.ActivateType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

/**
 * Implemented by a {@link CustomItem} that does something when a player clicks with it.
 * <p>
 * {@link ItemActivateListener} resolves the item behind the clicked stack and calls
 * {@link #onActivate(Player, ItemStack, ActivateType)} once the click has passed
 * {@link #canActivate(Player, ItemStack, ActivateType)} and the cancellable
 * {@link io.github.trae.spigot.framework.item.events.ItemPreActivateEvent}. An item not implementing
 * this interface is never invoked, so the capability is opt-in per item rather than a hook every
 * custom item has to override.
 * <p>
 * The stack is passed alongside the player because it is the specific stack that was clicked with,
 * carrying its own amount, durability, and persistent data, which the item definition itself does
 * not know about.
 */
public interface Activatable {

    /**
     * Performs this item's action. Called only after the click has passed
     * {@link #canActivate(Player, ItemStack, ActivateType)} and the pre-activate event.
     *
     * @param player       the player who clicked
     * @param itemStack    the specific stack that was clicked with
     * @param activateType the kind of click
     */
    void onActivate(final Player player, final ItemStack itemStack, final ActivateType activateType);

    /**
     * Returns whether this item may activate for the given player, stack, and click type (e.g. gated
     * behind a cooldown, a permission, or a durability threshold). Defaults to {@code true}.
     * <p>
     * This is the item-level check, evaluated before the pre-activate event, for conditions the item
     * itself owns.
     *
     * @param player       the player clicking
     * @param itemStack    the specific stack being clicked with
     * @param activateType the kind of click
     * @return {@code true} if the activation should proceed
     */
    default boolean canActivate(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        return true;
    }

    /**
     * Returns whether the vanilla use of the item in hand should still run alongside this activation.
     * Defaults to {@link Event.Result#DEFAULT}, leaving vanilla behaviour untouched.
     * <p>
     * Return {@link Event.Result#DENY} for an item whose custom action replaces what the material
     * would normally do, such as a right-clickable food item that should not be eaten.
     *
     * @param player       the player clicking
     * @param itemStack    the specific stack being clicked with
     * @param activateType the kind of click
     * @return the result applied to the interaction's item use
     */
    default Event.Result useItemInHand(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        return Event.Result.DEFAULT;
    }

    /**
     * Returns whether the interacted block should still respond alongside this activation. Defaults
     * to {@link Event.Result#DEFAULT}, leaving vanilla behaviour untouched.
     * <p>
     * Return {@link Event.Result#DENY} to stop the click opening a chest or toggling a lever while
     * the item's own action runs.
     *
     * @param player       the player clicking
     * @param itemStack    the specific stack being clicked with
     * @param activateType the kind of click
     * @return the result applied to the interacted block
     */
    default Event.Result useInteractedBlock(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        return Event.Result.DEFAULT;
    }
}