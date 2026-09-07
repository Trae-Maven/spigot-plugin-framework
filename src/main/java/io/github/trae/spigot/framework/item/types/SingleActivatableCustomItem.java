package io.github.trae.spigot.framework.item.types;

import io.github.trae.spigot.framework.item.enums.ActivateType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

/**
 * An {@link ActivatableCustomItem} that responds to exactly one kind of click.
 * <p>
 * The click type is fixed at construction and checked before anything else, so every hook loses the
 * {@link ActivateType} parameter it would otherwise have to branch on. An item that only ever
 * responds to a right click writes no click-type check at all.
 * <p>
 * Every parameterised hook is final here and forwards to a parameterless one, so a subclass cannot
 * accidentally override the wrong overload and find its item responding to clicks it never meant to
 * handle. Extend {@link ActivatableCustomItem} directly for an item that does different things on
 * different clicks.
 */
public abstract class SingleActivatableCustomItem extends ActivatableCustomItem {

    /**
     * The only click type this item responds to. Any other is refused before the item's own checks
     * run.
     */
    private final ActivateType activateType;

    /**
     * Creates an item that responds to a single kind of click.
     *
     * @param material     the material every stack is created with
     * @param identifier   the opaque, permanent identity to stamp onto every stack
     * @param namespace    the readable key this item is named by
     * @param activateType the only click type this item responds to
     */
    public SingleActivatableCustomItem(final Material material, final String identifier, final String namespace, final ActivateType activateType) {
        super(material, identifier, namespace);

        this.activateType = activateType;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Refuses any click type other than this item's own before consulting
     * {@link #canActivate(Player, ItemStack)}, so a subclass never sees a click it does not
     * handle.</p>
     */
    @Override
    public final boolean canActivate(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        return this.activateType == activateType && this.canActivate(player, itemStack);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Forwards to {@link #onActivate(Player, ItemStack)}, dropping the click type, which is
     * already known to be this item's own.</p>
     */
    @Override
    public final void onActivate(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        this.onActivate(player, itemStack);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Forwards to {@link #useItemInHand(Player, ItemStack)}, dropping the click type.</p>
     */
    @Override
    public final Event.Result useItemInHand(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        return this.useItemInHand(player, itemStack);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Forwards to {@link #useInteractedBlock(Player, ItemStack, Block)}, dropping the click
     * type.</p>
     */
    @Override
    public final Event.Result useInteractedBlock(final Player player, final ItemStack itemStack, final Block block, final ActivateType activateType) {
        return this.useInteractedBlock(player, itemStack, block);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For this item's click type, forwards to {@link #getCooldownName()}. Other click types retain
     * the superclass behaviour.</p>
     */
    @Override
    public final String getCooldownName(final ActivateType activateType) {
        return activateType == this.activateType ? this.getCooldownName() : super.getCooldownName(activateType);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For this item's click type, forwards to {@link #getCooldownDuration()}. Other click types
     * retain the superclass behaviour.</p>
     */
    @Override
    public final long getCooldownDuration(final ActivateType activateType) {
        return activateType == this.activateType ? this.getCooldownDuration() : super.getCooldownDuration(activateType);
    }

    /**
     * Returns whether the vanilla use of the item in hand should still run alongside this
     * activation. Defaults to {@link Event.Result#DEFAULT}, leaving vanilla behaviour untouched.
     *
     * @param player    the player clicking
     * @param itemStack the specific stack being clicked with
     * @return the result applied to the interaction's item use
     */
    public Event.Result useItemInHand(final Player player, final ItemStack itemStack) {
        return Event.Result.DEFAULT;
    }

    /**
     * Returns whether the interacted block should still respond alongside this activation. Defaults
     * to {@link Event.Result#DEFAULT}, leaving vanilla behaviour untouched.
     *
     * @param player    the player clicking
     * @param itemStack the specific stack being clicked with
     * @param block     the block that was clicked, or {@code null} for an air click
     * @return the result applied to the interacted block
     */
    public Event.Result useInteractedBlock(final Player player, final ItemStack itemStack, final Block block) {
        return Event.Result.DEFAULT;
    }

    /**
     * Returns the name under which this item's activation cooldown is recorded.
     * <p>
     * Defaults to the cooldown name associated with this item's click type.
     *
     * @return the cooldown name
     */
    public String getCooldownName() {
        return super.getCooldownName(this.activateType);
    }

    /**
     * Returns how long this item's activation cooldown lasts.
     * <p>
     * Defaults to the cooldown duration associated with this item's click type.
     *
     * @return the cooldown duration
     */
    public long getCooldownDuration() {
        return super.getCooldownDuration(this.activateType);
    }

    /**
     * Returns whether this item may activate for the given player and stack (e.g. gated behind a
     * resource or a durability threshold). The click type is not passed, since it is already known
     * to be this item's own.
     * <p>
     * This is the item-level check, evaluated after the pre-activate event, for conditions the item
     * itself owns.
     *
     * @param player    the player clicking
     * @param itemStack the specific stack being clicked with
     * @return {@code true} if the activation should proceed
     */
    public boolean canActivate(final Player player, final ItemStack itemStack) {
        return true;
    }

    /**
     * Performs this item's action.
     *
     * @param player    the player who clicked
     * @param itemStack the specific stack that was clicked with
     */
    public abstract void onActivate(final Player player, final ItemStack itemStack);
}