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
import org.bukkit.inventory.ItemStack;

/**
 * Reconciles stacks wherever one enters a player's possession or an inventory they open, delegating
 * all stack work to {@link ItemManager}.
 * <p>
 * A {@code null} from {@link ItemManager#apply(ItemStack)} means the stack should cease to exist,
 * which each handler carries out in whatever way its own event allows: clearing a slot where one can
 * be cleared, and cancelling the event where the API will not accept a missing stack.
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
     * <p>
     * A removed stack cancels the pickup and kills the item entity, since an item entity is required
     * to hold a stack and cannot be emptied in place.
     *
     * @param event the pickup event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onEntityPickupItem(final EntityPickupItemEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Item item = event.getItem();

        final ItemStack itemStack = this.itemManager.apply(item.getItemStack());

        if (itemStack == null) {
            event.setCancelled(true);
            item.remove();
            return;
        }

        item.setItemStack(itemStack);
    }

    /**
     * Reconciles the crafting result while it is still a preview, so the player sees the custom item
     * on hover rather than the vanilla one, and receives it on click.
     * <p>
     * A removed result empties the output slot, so the recipe simply produces nothing.
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
     * <p>
     * A removed result cancels the smelt rather than clearing it, since the event's result cannot be
     * set to nothing. The input is consumed either way, so the fuel and ore are still spent.
     *
     * @param event the smelt event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onFurnaceSmelt(final FurnaceSmeltEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final ItemStack itemStack = this.itemManager.apply(event.getResult());

        if (itemStack == null) {
            event.setCancelled(true);
            return;
        }

        event.setResult(itemStack);
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