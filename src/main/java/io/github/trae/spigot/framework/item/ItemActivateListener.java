package io.github.trae.spigot.framework.item;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.item.enums.ActivateType;
import io.github.trae.spigot.framework.item.events.ItemPostActivateEvent;
import io.github.trae.spigot.framework.item.events.ItemPreActivateEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Routes player interactions to the {@link Activatable} item behind the clicked stack.
 * <p>
 * A click reaches {@link Activatable#onActivate} only after resolving to a registered item that
 * implements the interface, passing {@link Activatable#canActivate}, and surviving a cancellable
 * {@link ItemPreActivateEvent}. Anything else is left entirely alone, so vanilla items and custom
 * items without the capability behave normally.
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
     * The item's declared interaction results are applied before the action runs, so an item can
     * suppress the vanilla use of its material or of the block it was aimed at. A post event follows
     * a successful activation.
     *
     * @param event the interaction event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final ItemStack itemStack = event.getItem();
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        ActivateType.getByAction(event.getAction()).ifPresent(activateType -> {
            this.itemManager.getItemByItemStack(itemStack).ifPresent(item -> {
                if (!(item instanceof final Activatable activatable)) {
                    return;
                }

                final Player player = event.getPlayer();

                if (!activatable.canActivate(player, itemStack, activateType)) {
                    return;
                }

                if (UtilEvent.supply(new ItemPreActivateEvent(item, player, itemStack, activateType)).isCancelled()) {
                    return;
                }

                event.setUseItemInHand(activatable.useItemInHand(player, itemStack, activateType));
                event.setUseInteractedBlock(activatable.useInteractedBlock(player, itemStack, activateType));

                activatable.onActivate(player, itemStack, activateType);

                UtilEvent.dispatch(new ItemPostActivateEvent(item, player, itemStack, activateType));
            });
        });
    }
}