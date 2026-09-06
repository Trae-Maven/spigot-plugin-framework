package io.github.trae.spigot.framework.item;

import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.item.enums.ActivateType;
import io.github.trae.spigot.framework.item.events.ItemPostActivateEvent;
import io.github.trae.spigot.framework.item.events.ItemPreActivateEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.UtilServer;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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

    private final ItemManager itemManager;

    /**
     * Resolves the clicked stack to its item and activates it when eligible.
     * <p>
     * Only the main hand is handled, since the interaction event fires once per hand and an item
     * held in one hand would otherwise activate again on the other hand's pass.
     * <p>
     * The handler deliberately does not ignore cancelled events. An air click carries no block to
     * interact with, so the event arrives already reporting itself as cancelled, and skipping those
     * would leave an item that only responds to blocks.
     * <p>
     * The item's declared interaction results are applied before the action runs, so an item can
     * suppress the vanilla use of its material or of the block it was aimed at. A post event follows
     * a successful activation.
     *
     * @param event the interaction event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final ItemStack itemStack = event.getItem();
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        ActivateType.getByAction(event.getAction()).ifPresent(activateType -> {
            this.itemManager.getItemByItemStack(itemStack).ifPresent(item -> {
                if (!(item instanceof final ActivatableCustomItem activatableCustomItem)) {
                    return;
                }

                final Player player = event.getPlayer();

                if (UtilEvent.supply(new ItemPreActivateEvent(activatableCustomItem, player, itemStack, activateType)).isCancelled()) {
                    return;
                }

                if (!activatableCustomItem.canActivate(player, itemStack, activateType)) {
                    return;
                }

                event.setUseItemInHand(activatableCustomItem.useItemInHand(player, itemStack, activateType));
                event.setUseInteractedBlock(activatableCustomItem.useInteractedBlock(player, itemStack, event.getClickedBlock(), activateType));

                activatableCustomItem.onActivate(player, itemStack, activateType);

                UtilEvent.dispatch(new ItemPostActivateEvent(activatableCustomItem, player, itemStack, activateType));
            });
        });
    }

    /**
     * Ticks every active channel, ending the ones whose conditions no longer hold.
     * <p>
     * A channel survives the tick only while the player is online, still holding the item that
     * started it, still blocking, and still passing {@code canChannel}. Failing any of those ends
     * the channel and fires {@code onStop}, so a player who logs out, swaps items, or lowers their
     * shield leaves cleanly without the item having to watch for it.
     * <p>
     * Ending a channel here is also what keeps the active set bounded: nothing else removes a
     * player, so an entry that outlived its conditions would otherwise stay forever and report the
     * player as channelling long after they stopped.
     */
    @Scheduler(period = 50, unit = TimeUnit.MILLISECONDS)
    public final void onScheduler() {
        for (final CustomItem item : this.itemManager.getItems()) {
            if (!(item instanceof final ChannelActivableCustomItem channelActivableCustomItem)) {
                continue;
            }

            channelActivableCustomItem.getActiveChannelSet().removeIf(uuid -> {
                final Player player = UtilServer.getOnlinePlayerById(uuid).orElse(null);
                if (player == null || player.isDead() || !player.isValid()) {
                    return true;
                }

                final ItemStack itemStack = player.getInventory().getItemInMainHand();

                if (!channelActivableCustomItem.isSimilarByIdentifier(itemStack) || !player.isHandRaised() || !channelActivableCustomItem.canChannel(player, itemStack)) {
                    channelActivableCustomItem.onStop(player, itemStack);
                    return true;
                }

                channelActivableCustomItem.onChannel(player, itemStack);
                return false;
            });
        }
    }
}