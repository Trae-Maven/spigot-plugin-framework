package io.github.trae.spigot.framework.item.types;

import io.github.trae.spigot.framework.item.CustomItem;
import io.github.trae.spigot.framework.item.enums.ActivateType;
import io.github.trae.spigot.framework.item.listeners.ItemActivateListener;
import io.github.trae.spigot.framework.utility.UtilMaterial;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

/**
 * A {@link CustomItem} that does something when a player clicks with it.
 * <p>
 * {@link ItemActivateListener} resolves the item behind the clicked stack and calls
 * {@link #onActivate(Player, ItemStack, ActivateType)} once the click has survived the cancellable
 * {@link io.github.trae.spigot.framework.item.events.ItemPreActivateEvent} and passed
 * {@link #canActivate(Player, ItemStack, ActivateType)}. An item extending {@link CustomItem}
 * directly is never invoked, so the capability is opt-in per item rather than a hook every custom
 * item has to override.
 * <p>
 * The stack is passed alongside the player because it is the specific stack that was clicked with,
 * carrying its own amount, durability, and persistent data, which the item definition itself does
 * not know about.
 */
public abstract class ActivatableCustomItem extends CustomItem {

    /**
     * Creates an activatable item of the given material.
     *
     * @param material   the material every stack is created with
     * @param identifier the opaque, permanent identity to stamp onto every stack
     * @param namespace  the readable key this item is named by
     */
    public ActivatableCustomItem(final Material material, final String identifier, final String namespace) {
        super(material, identifier, namespace);
    }

    /**
     * Returns whether the vanilla use of the item in hand should still run alongside this activation.
     * Defaults to {@link Event.Result#DENY} when a right click would consume the material, and to
     * {@link Event.Result#DEFAULT} otherwise.
     * <p>
     * That default keeps the stack intact: a custom item built on an ender pearl or a golden apple
     * activates without being thrown or eaten. Where the material has no such use, vanilla behaviour
     * is left untouched. Override to return {@link Event.Result#DEFAULT} for an item that should do
     * both, or {@link Event.Result#DENY} for one whose material is not consumable but whose vanilla
     * use should still be suppressed.
     *
     * @param player       the player clicking
     * @param itemStack    the specific stack being clicked with
     * @param activateType the kind of click
     * @return the result applied to the interaction's item use
     */
    public Event.Result useItemInHand(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        return activateType == ActivateType.RIGHT_CLICK && UtilMaterial.isUsable(itemStack.getType()) ? Event.Result.DENY : Event.Result.DEFAULT;
    }

    /**
     * Returns whether the interacted block should still respond alongside this activation. Defaults
     * to {@link Event.Result#DEFAULT}, leaving vanilla behaviour untouched.
     * <p>
     * Return {@link Event.Result#DENY} to stop the click opening a chest or toggling a lever while
     * the item's own action runs. The block is passed so that decision can depend on what was
     * clicked, and is {@code null} when the player clicked air.
     *
     * @param player       the player clicking
     * @param itemStack    the specific stack being clicked with
     * @param block        the block that was clicked, or {@code null} for an air click
     * @param activateType the kind of click
     * @return the result applied to the interacted block
     */
    public Event.Result useInteractedBlock(final Player player, final ItemStack itemStack, final Block block, final ActivateType activateType) {
        return Event.Result.DEFAULT;
    }

    /**
     * Returns whether this item still activates when the material would be consumed by the click.
     * Defaults to {@code true}, since {@link #useItemInHand(Player, ItemStack, ActivateType)} already
     * denies that use by default, leaving the activation as the only thing the click does.
     * <p>
     * Return {@code false} for an item that should defer to its material entirely, so a custom item
     * built on a golden apple is eaten rather than activated.
     *
     * @param player       the player clicking
     * @param itemStack    the specific stack being clicked with
     * @param activateType the kind of click
     * @return {@code true} if the activation should run despite the material's own use
     */
    public boolean activateOnItemUse(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        return true;
    }

    /**
     * Returns whether this item still activates when the clicked block would respond on its own.
     * Defaults to {@code false}, so the world wins: clicking a chest opens it, clicking a lever
     * toggles it, and clicking dirt with a hoe tills it, with nothing activating.
     * <p>
     * A sneaking player is the exception, since vanilla skips the block's own response entirely when
     * the hand is not empty, leaving nothing for the activation to compete with. Sneak clicking any
     * block therefore activates regardless of what this returns.
     * <p>
     * Return {@code true} for an item whose action should run anyway, in which case
     * {@link #useInteractedBlock(Player, ItemStack, Block, ActivateType)} decides whether the block
     * responds alongside it.
     *
     * @param player       the player clicking
     * @param itemStack    the specific stack being clicked with
     * @param block        the block that was clicked
     * @param activateType the kind of click
     * @return {@code true} if the activation should run despite the block's own response
     */
    public boolean activateOnBlockUse(final Player player, final ItemStack itemStack, final Block block, final ActivateType activateType) {
        return false;
    }

    /**
     * Returns whether the vanilla material cooldown should be cleared after activation.
     *
     * @param player       the player activating the item
     * @param itemStack    the specific stack being activated
     * @param activateType the kind of click
     * @return {@code true} to clear the material's client-visible cooldown
     */
    public boolean clearMaterialCooldown(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        return true;
    }

    /**
     * Returns whether this item may activate for the given player, stack, and click type (e.g. gated
     * behind a cooldown, a permission, or a durability threshold). Defaults to {@code true}.
     * <p>
     * This is the item-level check, evaluated after the pre-activate event, for conditions the item
     * itself owns.
     *
     * @param player       the player clicking
     * @param itemStack    the specific stack being clicked with
     * @param activateType the kind of click
     * @return {@code true} if the activation should proceed
     */
    public boolean canActivate(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        return true;
    }

    /**
     * Performs this item's action. Called only after the click has survived the pre-activate event
     * and passed {@link #canActivate(Player, ItemStack, ActivateType)}.
     *
     * @param player       the player who clicked
     * @param itemStack    the specific stack that was clicked with
     * @param activateType the kind of click
     */
    public abstract void onActivate(final Player player, final ItemStack itemStack, final ActivateType activateType);

    /**
     * Returns the name this item's activation cooldown is recorded under for the given click type.
     * Defaults to the item's display name, so a cooldown message reads as the item the player
     * recognises rather than an internal key.
     * <p>
     * The name is the key rather than the item, so two items sharing a display name share a
     * cooldown, and one item returning different names per click type gates each independently.
     *
     * @param activateType the kind of click
     * @return the cooldown name
     */
    public String getCooldownName(final ActivateType activateType) {
        return this.getDisplayName();
    }

    /**
     * Returns how long the activation cooldown lasts for the given click type, in milliseconds.
     * Defaults to no cooldown.
     * <p>
     * Only consulted when {@link #getCooldownName(ActivateType)} returns a name, so an item gating
     * one click type and not another need only vary that.
     *
     * @param activateType the kind of click
     * @return the cooldown duration in milliseconds
     */
    public long getCooldownDuration(final ActivateType activateType) {
        return 0L;
    }
}