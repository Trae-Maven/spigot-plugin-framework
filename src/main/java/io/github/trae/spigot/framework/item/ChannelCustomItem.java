package io.github.trae.spigot.framework.item;

import io.github.trae.spigot.framework.item.enums.ActivateType;
import lombok.AccessLevel;
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
 * An {@link ActivatableCustomItem} that does something continuously while a player holds right
 * click, rather than once when they press it.
 * <p>
 * The right click starts a channel and {@link ItemActivateListener} ticks it from there, calling
 * {@link #onChannel(Player, ItemStack)} every tick until the player lets go, swaps items, logs out,
 * or {@link #canChannel(Player, ItemStack)} stops returning {@code true}. Whichever ends it,
 * {@link #onStop(Player, ItemStack)} fires exactly once.
 * <p>
 * Holding right click requires the item to have a use action, which a sword does not have on its own.
 * Attaching the {@code blocks_attacks} data component gives it one, so a channelling sword needs
 * that component for the hold to register at all.
 * <p>
 * {@link #onActivate} and {@link #canActivate} are final here, since the channel owns both: the
 * activation only ever starts a channel, and the tick owns every way one ends. Subclasses implement
 * {@link #onChannel(Player, ItemStack)} and override the start, stop, and gate hooks as needed.
 */
public abstract class ChannelCustomItem extends ActivatableCustomItem {

    /**
     * The players currently channelling this item, by identifier.
     * <p>
     * Entries are added by the activation and removed only by the channel tick, which is what keeps
     * the set bounded: a player whose conditions lapse is dropped on the next tick rather than left
     * behind.
     */
    @Getter(AccessLevel.PROTECTED)
    private final Set<UUID> activeChannelSet = new HashSet<>();

    /**
     * Creates a channelling item of the given material under the given identifier.
     *
     * @param material   the material every stack is created with
     * @param identifier the unique identifier to register and stamp under
     */
    protected ChannelCustomItem(final Material material, final String identifier) {
        super(material, identifier);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Starts a channel and fires {@link #onStart(Player, ItemStack)}, once. Holding right click
     * re-fires the interaction repeatedly, so this only acts on the first, and the tick owns every
     * way the channel ends.</p>
     */
    @Override
    protected final void onActivate(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        if (this.activeChannelSet.add(player.getUniqueId())) {
            this.onStart(player, itemStack);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Restricted to right clicks, since a channel is a hold rather than a press. Conditions a
     * subclass owns belong in {@link #canChannel(Player, ItemStack)}, which is checked every tick
     * rather than only at the start.</p>
     */
    @Override
    protected final boolean canActivate(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        return activateType == ActivateType.RIGHT_CLICK;
    }

    /**
     * Returns whether the channel may continue for the given player and stack (e.g. gated behind a
     * resource, a durability threshold, or a region). Defaults to {@code true}.
     * <p>
     * Checked every tick, not just at the start, so returning {@code false} mid-channel ends it and
     * fires {@link #onStop(Player, ItemStack)}.
     *
     * @param player    the player channelling
     * @param itemStack the stack being channelled with
     * @return {@code true} if the channel may continue
     */
    protected boolean canChannel(final Player player, final ItemStack itemStack) {
        return true;
    }

    /**
     * Called once when a channel starts. Does nothing by default.
     *
     * @param player    the player who started channelling
     * @param itemStack the stack being channelled with
     */
    protected void onStart(final Player player, final ItemStack itemStack) {
    }

    /**
     * Called once when a channel ends, however it ended. Does nothing by default.
     * <p>
     * Not called for a player who went offline mid-channel, since there is nothing left to act on.
     *
     * @param player    the player who stopped channelling
     * @param itemStack the stack that was being channelled with
     */
    protected void onStop(final Player player, final ItemStack itemStack) {
    }

    /**
     * Performs this item's per-tick action while the channel runs.
     *
     * @param player    the player channelling
     * @param itemStack the stack being channelled with
     */
    protected abstract void onChannel(final Player player, final ItemStack itemStack);

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