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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Routes player interactions to the {@link ActivatableCustomItem} behind the clicked stack, and
 * ticks any channel those activations started.
 * <p>
 * A click reaches the item's own {@code onActivate} only after resolving to a registered item of
 * that type, surviving a cancellable {@link ItemPreActivateEvent}, and passing the item's own
 * {@code canActivate}. Anything else is left entirely alone, so vanilla items and custom items
 * without the capability behave normally.
 */
@AllArgsConstructor
@Singleton
public class ItemActivateListener implements Listener {

    /**
     * The registry a clicked stack is resolved against, and the source of the items whose channels
     * are ticked.
     */
    private final ItemManager itemManager;

    /**
     * The tick each player last performed an action that provokes a phantom left click, keyed by
     * their identifier and cleared when they leave.
     */
    private final Map<UUID, Integer> blockedClickTickMap = new HashMap<>();

    /**
     * Resolves the clicked stack to its item and activates it when eligible.
     * <p>
     * Only the main hand is handled, since the interaction event fires once per hand and an item
     * held in one hand would otherwise activate again on the other hand's pass.
     * <p>
     * A denied right click desyncs the client, which answers with an arm swing the server reads as a
     * left click, so a left click landing within a tick of a right click is discarded before anything
     * else runs. Without it an item that suppresses its material's use would activate twice per
     * click.
     * <p>
     * The handler deliberately does not ignore cancelled events. An air click carries no block to
     * interact with, so the event arrives already reporting itself as cancelled, and skipping those
     * would leave an item that only responds to blocks.
     * <p>
     * A right click that competes with vanilla is skipped entirely rather than fought over: the
     * material's own use wins unless the item declares otherwise through
     * {@link ActivatableCustomItem#activateOnItemUse(Player, ItemStack, ActivateType)}, and so does
     * the clicked block through
     * {@link ActivatableCustomItem#activateOnBlockUse(Player, ItemStack, Block, ActivateType)}. A
     * sneaking player is exempt from the block check, since vanilla skips the block's response with a
     * full hand. Left clicks compete with nothing and are never gated.
     * <p>
     * Past those, the item's declared interaction results are applied before the action runs, so an
     * item can suppress the vanilla use of its material or of the block it was aimed at. A post event
     * follows a successful activation.
     *
     * @param event the interaction event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final Player player = event.getPlayer();
        final Action action = event.getAction();
        final int tick = Bukkit.getCurrentTick();

        if (action.isRightClick()) {
            this.blockedClickTickMap.put(player.getUniqueId(), tick);
        } else if (tick - this.blockedClickTickMap.getOrDefault(player.getUniqueId(), Integer.MIN_VALUE) <= 1) {
            return;
        }

        final ItemStack itemStack = event.getItem();
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        ActivateType.getByAction(action).ifPresent(activateType -> {
            this.itemManager.getItemByItemStack(itemStack).ifPresent(item -> {
                if (!(item instanceof final ActivatableCustomItem activatableCustomItem)) {
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

                if (UtilEvent.supply(new ItemPreActivateEvent(activatableCustomItem, player, itemStack, activateType)).isCancelled()) {
                    return;
                }

                if (!activatableCustomItem.canActivate(player, itemStack, activateType)) {
                    return;
                }

                activatableCustomItem.onActivate(player, itemStack, activateType);

                if (activatableCustomItem.clearMaterialCooldown(player, itemStack, activateType)) {
                    player.setCooldown(itemStack, 0);
                }

                UtilEvent.dispatch(new ItemPostActivateEvent(activatableCustomItem, player, itemStack, activateType));
            });
        });
    }

    /**
     * Records a drop so the arm swing it provokes does not activate the item on its way out of the
     * inventory.
     * <p>
     * Dropping sends a swing packet the server reads as a left click, which would otherwise reach the
     * interact handler a tick later and fire the dropped item's left click action. Only a stack this
     * framework recognises is recorded, so a plain drop leaves a player's next real left click
     * untouched.
     *
     * @param event the drop event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onPlayerDropItem(final PlayerDropItemEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (this.itemManager.getItemByItemStack(event.getItemDrop().getItemStack()).isEmpty()) {
            return;
        }

        this.blockedClickTickMap.put(event.getPlayer().getUniqueId(), Bukkit.getCurrentTick());
    }

    /**
     * Drops the leaving player's recorded tick, so the map holds only players who are online.
     *
     * @param event the quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onPlayerQuit(final PlayerQuitEvent event) {
        this.blockedClickTickMap.remove(event.getPlayer().getUniqueId());
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
    public final void onScheduler() {
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
}