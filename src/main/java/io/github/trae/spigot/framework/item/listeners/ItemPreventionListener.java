package io.github.trae.spigot.framework.item.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.item.ItemManager;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Keeps custom items out of the vanilla stations that would rewrite them, namely the anvil and the
 * enchanting table.
 * <p>
 * A custom item owns its entire description, and {@link ItemManager} reconciles every stack against
 * that description whenever it is seen. Anything vanilla writes onto a stack, a new name, an added
 * enchantment, is therefore temporary: the next reconciliation discards it. Rather than let a player
 * spend levels and materials on a change that silently reverts, the operation is refused up front.
 * <p>
 * Both input slots are checked, not just the one being modified. A custom item in the sacrifice or
 * lapis slot is being consumed as a reagent, which destroys it just as surely, so its presence blocks
 * the operation too.
 * <p>
 * Handlers run at {@link EventPriority#LOWEST} so the refusal is decided before other plugins shape
 * the outcome. The enchant path is covered twice, at preparation and at application, because the
 * first only governs which offers appear and can be undone by a later listener, while the second is
 * where the enchantment is actually applied.
 */
@AllArgsConstructor
@Singleton
public final class ItemPreventionListener implements Listener {

    /**
     * The registry a slot's contents are resolved against.
     */
    private final ItemManager itemManager;

    /**
     * Clears the anvil's output when either input slot holds a custom item, covering renaming,
     * repairing, and combining alike.
     *
     * @param event the anvil preparation event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareAnvil(final PrepareAnvilEvent event) {
        if (event.getResult() == null) {
            return;
        }

        final AnvilInventory anvilInventory = event.getInventory();

        if (!this.isCustomItem(anvilInventory.getFirstItem(), anvilInventory.getSecondItem())) {
            return;
        }

        event.setResult(null);
    }

    /**
     * Suppresses the enchanting table's offers when the item being enchanted, or the lapis paying for
     * it, is a custom item, so no offer is ever shown for an enchantment that cannot be kept.
     * <p>
     * The primary stack is taken from the event rather than the inventory, so it is still checked in
     * the event that the container is not an {@link EnchantingInventory} and the secondary slot
     * cannot be read.
     *
     * @param event the enchantment preparation event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareItemEnchant(final PrepareItemEnchantEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final ItemStack secondary = event.getInventory() instanceof final EnchantingInventory enchantingInventory ? enchantingInventory.getSecondary() : null;

        if (!this.isCustomItem(event.getItem(), secondary)) {
            return;
        }

        event.setCancelled(true);
    }

    /**
     * Refuses the enchantment itself when the item being enchanted, or the lapis paying for it, is a
     * custom item.
     * <p>
     * This is the enforcing half of the pair. An offer suppressed at preparation can be restored by
     * another plugin, and the stack in the slot can change between the offer being shown and the
     * button being pressed, so the same check is repeated at the point the enchantment would be
     * applied.
     *
     * @param event the enchantment event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEnchantItem(final EnchantItemEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final ItemStack secondary = event.getInventory() instanceof final EnchantingInventory enchantingInventory ? enchantingInventory.getSecondary() : null;

        if (!this.isCustomItem(event.getItem(), secondary)) {
            return;
        }

        event.setCancelled(true);
    }

    /**
     * Returns whether any of the given stacks was produced by a custom item, resolved through
     * {@link ItemManager#getItemByItemStack(ItemStack)} rather than by inspecting material or meta.
     * <p>
     * Empty slots are expected and ignored, so a slot's contents can be passed straight in without
     * being checked first.
     *
     * @param itemStacks the stacks to check, any of which may be {@code null}
     * @return {@code true} if at least one stack belongs to a custom item
     */
    private boolean isCustomItem(final ItemStack... itemStacks) {
        return Stream.of(itemStacks).filter(Objects::nonNull).anyMatch(itemStack -> this.itemManager.getItemByItemStack(itemStack).isPresent());
    }
}