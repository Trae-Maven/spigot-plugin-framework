package io.github.trae.spigot.framework.item.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.item.ItemManager;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;

/**
 * Reconciles stacks wherever one enters a player's possession or an inventory they open, delegating
 * all stack work to {@link ItemManager}.
 */
@AllArgsConstructor
@Singleton
public class ItemApplyListener implements Listener {

    /**
     * The registry every stack is reconciled against.
     */
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
    public final void onPlayerJoin(final PlayerJoinEvent event) {
        this.itemManager.updateInventory(event.getPlayer().getInventory());
    }

    /**
     * Reconciles the contents of a block-backed inventory as it is opened, so stacks stored in a
     * chest, barrel, or shulker are brought up to date the moment a player looks at them.
     * <p>
     * Restricted to {@link BlockInventoryHolder} inventories, which excludes crafting grids, anvils,
     * and the framework's own windows, whose contents are transient or managed elsewhere.
     *
     * @param event the inventory open event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onInventoryOpen(final InventoryOpenEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Inventory topInventory = event.getView().getTopInventory();

        if (!(topInventory.getHolder() instanceof BlockInventoryHolder)) {
            return;
        }

        this.itemManager.updateInventory(topInventory);
    }
}