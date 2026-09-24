package io.github.trae.spigot.framework.item.listeners;

import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.item.CustomItem;
import io.github.trae.spigot.framework.item.ItemManager;
import io.github.trae.spigot.framework.item.enums.ActivateType;
import io.github.trae.spigot.framework.item.events.ItemChannelEvent;
import io.github.trae.spigot.framework.item.events.ItemPostActivateEvent;
import io.github.trae.spigot.framework.item.events.ItemPreActivateEvent;
import io.github.trae.spigot.framework.item.types.ActivatableCustomItem;
import io.github.trae.spigot.framework.item.types.ChannelCustomItem;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.UtilMaterial;
import io.github.trae.spigot.framework.utility.UtilServer;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Routes player clicks, drops and hand swaps to the {@link ActivatableCustomItem} behind the stack,
 * and ticks any channel those activations started.
 * <p>
 * An interaction reaches the item's own {@code onActivate} only after resolving to a registered item
 * of that type, being one of its supported activate types, surviving a cancellable
 * {@link ItemPreActivateEvent}, and passing the item's own {@code canActivate}. Anything else is left
 * entirely alone, so vanilla items and custom items without the capability behave normally.
 */
@AllArgsConstructor
@Singleton
public final class ItemActivateListener implements Listener {

    /**
     * The registry a stack is resolved against, and the source of the items whose channels are
     * ticked.
     */
    private final ItemManager itemManager;

    /**
     * The tick each player last performed an action that provokes a phantom left click, keyed by
     * their identifier and cleared when they leave.
     */
    private final Map<UUID, Integer> blockedClickTickMap = new HashMap<>();

    /**
     * The tick each player last performed a genuine left or right click, one that survived the phantom
     * click check, keyed by their identifier and cleared when they leave.
     */
    private final Map<UUID, Integer> clickTickMap = new HashMap<>();

    /**
     * Resolves the clicked stack to its item and activates it when eligible.
     * <p>
     * Only the main hand is handled, since the interaction event fires once per hand and an item
     * held in one hand would otherwise activate again on the other hand's pass.
     * <p>
     * A denied right click desyncs the client, which answers with an arm swing the server reads as a
     * left click, so a left click landing within a tick of a right click is discarded before anything
     * else runs. Without it an item that suppresses its material's use would activate twice per
     * click. Every click that survives is recorded, so a drop in the same or the next tick is not
     * treated as a drop activation.
     * <p>
     * The handler deliberately does not ignore cancelled events. An air click carries no block to
     * interact with, so the event arrives already reporting itself as cancelled, and skipping those
     * would leave an item that only responds to blocks.
     * <p>
     * A click type the item does not support is ignored before anything else runs. A right click that
     * competes with vanilla is skipped entirely rather than fought over: the material's own use wins
     * unless the item declares otherwise through
     * {@link ActivatableCustomItem#activateOnItemUse(Player, ItemStack, ActivateType)}, and so does
     * the clicked block through
     * {@link ActivatableCustomItem#activateOnBlockUse(Player, ItemStack, Block, ActivateType)}. A
     * sneaking player is exempt from the block check, since vanilla skips the block's response with a
     * full hand. Left clicks compete with nothing and are never gated.
     * <p>
     * Past those, the item's declared interaction results are applied before the action runs, so an
     * item can suppress the vanilla use of its material or of the block it was aimed at.
     *
     * @param event the interaction event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final Player player = event.getPlayer();
        final Action action = event.getAction();
        final int tick = Bukkit.getCurrentTick();

        if (action.isRightClick()) {
            this.blockedClickTickMap.put(player.getUniqueId(), tick);
        } else if (this.isRecent(this.blockedClickTickMap, player.getUniqueId(), tick)) {
            return;
        }

        this.clickTickMap.put(player.getUniqueId(), tick);

        final ItemStack itemStack = event.getItem();
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        ActivateType.getByAction(action).ifPresent(activateType -> {
            this.itemManager.getItemByItemStack(itemStack).ifPresent(item -> {
                if (!(item instanceof final ActivatableCustomItem activatableCustomItem)) {
                    return;
                }

                if (!activatableCustomItem.getSupportedActivateTypes().contains(activateType)) {
                    return;
                }

                final Block clickedBlock = event.getClickedBlock();

                // Consumable materials defer to vanilla unless the item opts in, so a custom golden apple is eaten rather than activated. Returning skips the activation entirely.
                // Example: return on a custom Ender Pearl because a right click would throw it.
                if (activateType == ActivateType.RIGHT_CLICK && !activatableCustomItem.activateOnItemUse(player, itemStack, activateType) && UtilMaterial.isUsable(itemStack.getType())) {
                    return;
                }

                // A block that responds on its own wins unless the item opts in, so a chest opens instead of activating. Sneaking is exempt, since vanilla skips the block entirely with a full hand. Returning skips the activation entirely.
                // Example: return on a Chest because a right click would open it, or on Grass because a right click with a Hoe would till it.
                if (activateType == ActivateType.RIGHT_CLICK && clickedBlock != null && !player.isSneaking() && !activatableCustomItem.activateOnBlockUse(player, itemStack, clickedBlock, activateType) && (UtilMaterial.isInteractable(clickedBlock.getType()) || UtilMaterial.isInteractableByHand(clickedBlock.getType(), itemStack))) {
                    return;
                }

                // Denies the material's own use by default, so an ender pearl activates without leaving the hand.
                event.setUseItemInHand(activatableCustomItem.useItemInHand(player, itemStack, activateType));

                // Leaves the block's response untouched by default, letting the item suppress it per block.
                if (clickedBlock != null) {
                    event.setUseInteractedBlock(activatableCustomItem.useInteractedBlock(player, itemStack, clickedBlock, activateType));
                }

                this.activate(player, itemStack, activatableCustomItem, activateType);
            });
        });
    }

    /**
     * Records a drop so the arm swing it provokes does not activate the item on its way out of the
     * inventory, then activates the item for {@link ActivateType#DROP_ITEM} when it supports it.
     * <p>
     * Dropping sends a swing packet the server reads as a left click, which would otherwise reach the
     * interact handler a tick later and fire the dropped item's left click action. Only a stack this
     * framework recognises is recorded, so a plain drop leaves a player's next real left click
     * untouched. The drop is recorded before anything else, so its phantom swing is swallowed either
     * way.
     * <p>
     * A drop landing in the same or the next tick as a genuine click is not treated as an activation.
     * Otherwise, for an item supporting {@link ActivateType#DROP_ITEM}, the drop is cancelled so the
     * item stays with the player, and the activation runs through the usual gate. This covers any drop
     * of the stack, including one thrown out of an open inventory.
     *
     * @param event the drop event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final ItemStack itemStack = event.getItemDrop().getItemStack();

        final CustomItem customItem = this.itemManager.getItemByItemStack(itemStack).orElse(null);
        if (customItem == null) {
            return;
        }

        final Player player = event.getPlayer();

        final int tick = Bukkit.getCurrentTick();

        this.blockedClickTickMap.put(player.getUniqueId(), tick);

        if (this.isRecent(this.clickTickMap, player.getUniqueId(), tick)) {
            return;
        }

        if (!(customItem instanceof final ActivatableCustomItem activatableCustomItem)) {
            return;
        }

        if (!activatableCustomItem.getSupportedActivateTypes().contains(ActivateType.DROP_ITEM)) {
            return;
        }

        event.setCancelled(true);

        this.activate(player, itemStack, activatableCustomItem, ActivateType.DROP_ITEM);
    }

    /**
     * Activates the item being swapped out of the main hand for {@link ActivateType#SWAP_HAND} when it
     * supports it.
     * <p>
     * The item leaving the main hand is the one the event reports as moving to the off hand. For an
     * item supporting {@link ActivateType#SWAP_HAND}, the swap is cancelled so the item stays in the
     * main hand, and the activation runs through the usual gate.
     *
     * @param event the swap hand items event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHandItems(final PlayerSwapHandItemsEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final ItemStack itemStack = event.getOffHandItem();

        final CustomItem customItem = this.itemManager.getItemByItemStack(itemStack).orElse(null);
        if (customItem == null) {
            return;
        }

        if (!(customItem instanceof final ActivatableCustomItem activatableCustomItem)) {
            return;
        }

        if (!(activatableCustomItem.getSupportedActivateTypes().contains(ActivateType.SWAP_HAND))) {
            return;
        }

        event.setCancelled(true);

        this.activate(event.getPlayer(), itemStack, activatableCustomItem, ActivateType.SWAP_HAND);
    }

    /**
     * Drops the leaving player's recorded ticks, so the maps hold only players who are online.
     *
     * @param event the quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.blockedClickTickMap.remove(event.getPlayer().getUniqueId());
        this.clickTickMap.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Ticks every active channel, ending the ones whose conditions no longer hold.
     * <p>
     * A channel survives the tick only while the player is online, still holding the item that
     * started it, still holding the use action, not vetoed by a cancelled {@link ItemChannelEvent},
     * and still passing {@code canChannel}. Failing any of those ends the channel and fires
     * {@code onStop}, so a player who logs out, swaps items, or lets go leaves cleanly without the
     * item having to watch for it.
     * <p>
     * The hold is read from {@code isHandRaised} rather than {@code isBlocking}, since blocking only
     * becomes true after the item's block delay has elapsed, which is at least a tick after the
     * activation that started the channel.
     * <p>
     * Ending a channel here is also what keeps the active set bounded: nothing else removes a
     * player, so an entry that outlived its conditions would otherwise stay forever and report the
     * player as channelling long after they stopped.
     */
    @Scheduler(period = 50, unit = TimeUnit.MILLISECONDS)
    public void onScheduler() {
        for (final CustomItem item : this.itemManager.getItems()) {
            if (!(item instanceof final ChannelCustomItem channelCustomItem)) {
                continue;
            }

            channelCustomItem.getActiveChannelSet().removeIf(uuid -> {
                final Player player = UtilServer.getOnlinePlayerById(uuid).orElse(null);
                if (player == null) {
                    return true;
                }

                final ItemStack itemStack = player.getInventory().getItemInMainHand();

                if (!channelCustomItem.isSimilarByIdentifier(itemStack) || !player.isHandRaised()) {
                    channelCustomItem.onStop(player, itemStack);
                    return true;
                }

                if (UtilEvent.supply(new ItemChannelEvent(channelCustomItem, player, itemStack)).isCancelled() || !channelCustomItem.canActivate(player, itemStack) || !channelCustomItem.canChannel(player, itemStack)) {
                    channelCustomItem.onStop(player, itemStack);
                    return true;
                }

                channelCustomItem.onChannel(player, itemStack);
                return false;
            });
        }
    }

    /**
     * Returns whether the player's recorded tick in the map is the current tick or the one before.
     * A player with no recorded tick is never recent.
     *
     * @param tickMap the map of recorded ticks
     * @param uuid    the player's identifier
     * @param tick    the current tick
     * @return {@code true} if the recorded tick is within one tick of the current one
     */
    private boolean isRecent(final Map<UUID, Integer> tickMap, final UUID uuid, final int tick) {
        final Integer recordedTick = tickMap.get(uuid);

        return recordedTick != null && tick - recordedTick <= 1;
    }

    /**
     * Runs an activation through the shared gate: a cancellable {@link ItemPreActivateEvent} first,
     * then the item's own {@code canActivate}, and only if both pass the item's {@code onActivate},
     * followed by an {@link ItemPostActivateEvent}.
     *
     * @param player                the player activating the item
     * @param itemStack             the specific stack being used
     * @param activatableCustomItem the item being activated
     * @param activateType          the kind of interaction
     */
    private void activate(final Player player, final ItemStack itemStack, final ActivatableCustomItem activatableCustomItem, final ActivateType activateType) {
        if (UtilEvent.supply(new ItemPreActivateEvent(activatableCustomItem, player, itemStack, activateType)).isCancelled()) {
            return;
        }

        if (!activatableCustomItem.canActivate(player, itemStack, activateType)) {
            return;
        }

        activatableCustomItem.onActivate(player, itemStack, activateType);

        UtilEvent.dispatch(new ItemPostActivateEvent(activatableCustomItem, player, itemStack, activateType));
    }
}