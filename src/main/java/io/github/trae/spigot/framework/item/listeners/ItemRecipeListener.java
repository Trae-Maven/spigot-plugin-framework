package io.github.trae.spigot.framework.item.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.item.CustomItem;
import io.github.trae.spigot.framework.item.ItemManager;
import lombok.AllArgsConstructor;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

import java.util.stream.Stream;

/**
 * Refuses every vanilla recipe that would consume a {@link CustomItem} not declaring
 * {@link CustomItem#usableInRecipes()}, so a custom item is never spent as its bare material.
 * <p>
 * Crafting and smithing are refused at the preview by clearing the result, so the player never sees
 * an output they cannot take, with {@link CraftItemEvent} as a backstop. Crafters, cooking blocks,
 * and brewing stands have no preview and are cancelled outright, leaving the ingredient in place.
 */
@AllArgsConstructor
@Singleton
public class ItemRecipeListener implements Listener {

    private final ItemManager itemManager;

    /**
     * Returns whether the given stack belongs to a custom item that may not be used as an
     * ingredient. An empty slot or a stack with no owning item is never refused.
     *
     * @param itemStack the stack to check, which may be {@code null}
     * @return {@code true} if the stack must not be consumed by a recipe
     */
    private boolean isRefused(final ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        return this.itemManager.getItemByItemStack(itemStack)
                .map(customItem -> !customItem.usableInRecipes())
                .orElse(false);
    }

    /**
     * Clears the crafting result while it is still a preview when any ingredient in the grid is
     * refused. Runs before {@link ItemApplyListener} reconciles the result.
     *
     * @param event the craft preparation event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onPrepareItemCraft(final PrepareItemCraftEvent event) {
        if (Stream.of(event.getInventory().getMatrix()).anyMatch(this::isRefused)) {
            event.getInventory().setResult(null);
        }
    }

    /**
     * Cancels a craft whose grid holds a refused ingredient, catching any result that reached the
     * player despite the cleared preview.
     *
     * @param event the craft event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public final void onCraftItem(final CraftItemEvent event) {
        if (Stream.of(event.getInventory().getMatrix()).anyMatch(this::isRefused)) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancels a crafter block's craft when any ingredient in its grid is refused.
     *
     * @param event the crafter craft event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public final void onCrafterCraft(final CrafterCraftEvent event) {
        if (event.getBlock().getState(false) instanceof final Crafter crafter && Stream.of(crafter.getInventory().getContents()).anyMatch(this::isRefused)) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancels cooking in a furnace, smoker, blast furnace, or campfire when the source is refused.
     *
     * @param event the cook event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public final void onBlockCook(final BlockCookEvent event) {
        if (this.isRefused(event.getSource())) {
            event.setCancelled(true);
        }
    }

    /**
     * Clears the smithing result while it is still a preview when the template, equipment, or
     * mineral is refused.
     *
     * @param event the smithing preparation event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onPrepareSmithing(final PrepareSmithingEvent event) {
        final SmithingInventory smithingInventory = event.getInventory();

        if (Stream.of(smithingInventory.getInputTemplate(), smithingInventory.getInputEquipment(), smithingInventory.getInputMineral()).anyMatch(this::isRefused)) {
            event.setResult(null);
        }
    }

    /**
     * Cancels a brew when the brewing stand's ingredient is refused.
     *
     * @param event the brew event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public final void onBrew(final BrewEvent event) {
        if (this.isRefused(event.getContents().getIngredient())) {
            event.setCancelled(true);
        }
    }
}