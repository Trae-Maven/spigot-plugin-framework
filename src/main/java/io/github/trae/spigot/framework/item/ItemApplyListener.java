package io.github.trae.spigot.framework.item;

import io.github.trae.di.annotations.type.component.Singleton;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;

/**
 * Reconciles stacks at every point one enters a player's possession, delegating all stack work to
 * {@link ItemManager}.
 */
@AllArgsConstructor
@Singleton
public class ItemApplyListener implements Listener {

    private final ItemManager itemManager;

    /**
     * Reconciles a dropped stack as it is picked up, so an item obtained from the world arrives in
     * the inventory in its custom form.
     *
     * @param event the pickup event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onEntityPickupItem(final EntityPickupItemEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Item item = event.getItem();

        item.setItemStack(this.itemManager.apply(item.getItemStack()));
    }

    /**
     * Reconciles the crafting result while it is still a preview, so the player sees the custom item
     * on hover rather than the vanilla one, and receives it on click.
     *
     * @param event the craft preparation event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onPrepareItemCraft(final PrepareItemCraftEvent event) {
        final CraftingInventory craftingInventory = event.getInventory();

        craftingInventory.setResult(this.itemManager.apply(craftingInventory.getResult()));
    }

    /**
     * Reconciles a smelting result before it is placed into the furnace's output slot.
     *
     * @param event the smelt event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onFurnaceSmelt(final FurnaceSmeltEvent event) {
        if (event.isCancelled()) {
            return;
        }

        event.setResult(this.itemManager.apply(event.getResult()));
    }

    /**
     * Reconciles the player's whole inventory on join, catching stacks whose item definition changed
     * while they were offline.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        this.itemManager.updatePlayerInventory(event.getPlayer());
    }
}