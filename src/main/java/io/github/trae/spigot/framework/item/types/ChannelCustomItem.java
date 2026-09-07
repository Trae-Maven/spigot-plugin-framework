package io.github.trae.spigot.framework.item.types;

import io.github.trae.spigot.framework.item.enums.ActivateType;
import io.github.trae.spigot.framework.item.listeners.ItemActivateListener;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A {@link SingleActivatableCustomItem} that does something continuously while a player holds right
 * click, rather than once when they press it.
 * <p>
 * The right click starts a channel and {@link ItemActivateListener} ticks it from there, calling
 * {@link #onChannel(Player, ItemStack)} every tick until the player lets go, swaps items, logs out,
 * a {@link io.github.trae.spigot.framework.item.events.ItemChannelEvent} is cancelled, or
 * {@link #canChannel(Player, ItemStack)} stops returning {@code true}. Whichever ends it,
 * {@link #onStop(Player, ItemStack)} fires exactly once.
 * <p>
 * Holding right click requires the item to have a use action. Many materials have none, a sword
 * among them, and the hold never registers for those. Attaching the {@code blocks_attacks} data
 * component gives a sword one, so a channelling sword needs that component to work at all.
 * <p>
 * The click type is fixed to {@link ActivateType#RIGHT_CLICK} by the superclass, and
 * {@link #onActivate(Player, ItemStack)} is final here, since starting a channel is the only thing
 * an activation may do. Every way a channel ends belongs to the tick instead. A subclass implements
 * {@link #onChannel(Player, ItemStack)} and overrides the start, stop, and gate hooks as needed.
 */
public abstract class ChannelCustomItem extends SingleActivatableCustomItem {

    /**
     * The players currently channelling this item, by identifier.
     * <p>
     * Entries are added by the activation and removed only by the channel tick, which is what keeps
     * the set bounded: a player whose conditions lapse is dropped on the next tick rather than left
     * behind.
     */
    @Getter
    private final Set<UUID> activeChannelSet = new HashSet<>();

    /**
     * Creates a channelling item of the given material.
     *
     * @param material   the material every stack is created with
     * @param identifier the opaque, permanent identity to stamp onto every stack
     * @param namespace  the readable key this item is named by
     */
    public ChannelCustomItem(final Material material, final String identifier, final String namespace) {
        super(material, identifier, namespace, ActivateType.RIGHT_CLICK);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Starts a channel and fires {@link #onStart(Player, ItemStack)}, once. Holding right click
     * re-fires the interaction repeatedly, so this only acts on the first, and the tick owns every
     * way the channel ends.</p>
     */
    @Override
    public final void onActivate(final Player player, final ItemStack itemStack) {
        if (this.activeChannelSet.add(player.getUniqueId())) {
            this.onStart(player, itemStack);
        }
    }

    /**
     * Returns whether the channel may continue for the given player and stack (e.g. gated behind a
     * resource or a durability threshold). Defaults to {@code true}.
     * <p>
     * Checked every tick, not just at the start, so returning {@code false} mid-channel ends it and
     * fires {@link #onStop(Player, ItemStack)}. This is separate from
     * {@link #canActivate(Player, ItemStack)}, which decides only whether a channel may begin.
     * <p>
     * This is the item-level check, evaluated after the
     * {@link io.github.trae.spigot.framework.item.events.ItemChannelEvent}, for conditions the item
     * itself owns. A condition external to it, such as a region restriction, belongs on that event
     * instead, which ends every channel rather than only this item's.
     *
     * @param player    the player channelling
     * @param itemStack the stack being channelled with
     * @return {@code true} if the channel may continue
     */
    public boolean canChannel(final Player player, final ItemStack itemStack) {
        return true;
    }

    /**
     * Called once when a channel starts. Does nothing by default.
     *
     * @param player    the player who started channelling
     * @param itemStack the stack being channelled with
     */
    public void onStart(final Player player, final ItemStack itemStack) {
    }

    /**
     * Called once when a channel ends, however it ended. Does nothing by default.
     * <p>
     * Not called for a player who went offline mid-channel, since there is nothing left to act on.
     *
     * @param player    the player who stopped channelling
     * @param itemStack the stack that was being channelled with
     */
    public void onStop(final Player player, final ItemStack itemStack) {
    }

    /**
     * Performs this item's per-tick action while the channel runs.
     *
     * @param player    the player channelling
     * @param itemStack the stack being channelled with
     */
    public abstract void onChannel(final Player player, final ItemStack itemStack);

    /**
     * Returns every online player currently channelling this item.
     * <p>
     * Resolved fresh on each call, so a player who went offline between the last tick and this call
     * is skipped rather than returned as a stale reference.
     *
     * @return the channelling players, in no meaningful order
     */
    public final List<Player> getChannelingPlayers() {
        return this.activeChannelSet.stream().map(Bukkit.getServer()::getPlayer).filter(Objects::nonNull).toList();
    }

    /**
     * Returns whether the given player is currently channelling this item.
     *
     * @param player the player to check
     * @return {@code true} if a channel is active for them
     */
    public final boolean isChanneling(final Player player) {
        return this.activeChannelSet.contains(player.getUniqueId());
    }
}