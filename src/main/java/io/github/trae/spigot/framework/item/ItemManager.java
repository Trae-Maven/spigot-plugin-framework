package io.github.trae.spigot.framework.item;

import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.utility.UtilItemStack;
import io.github.trae.spigot.framework.utility.UtilServer;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Holds the registry of {@link CustomItem}s and reconciles stacks against it.
 * <p>
 * Two lookups are maintained: every item by its identifier, and every naturally obtainable item by
 * its material. {@link #apply(ItemStack)} uses the first to recognise a stack this framework
 * produced and the second to convert a vanilla stack into its custom counterpart.
 * <p>
 * The registry is populated by {@link ItemListener} at server load, which is also where the stack
 * reconciliation is wired into player, crafting, and smelting events. A scheduler sweeps online
 * inventories periodically so stacks left untouched are still brought up to date after an item's
 * definition changes.
 */
@Getter
@Singleton
public class ItemManager {

    /**
     * Every registered item, keyed by its identifier.
     */
    private final Map<String, CustomItem> identifierItemMap = new HashMap<>();

    /**
     * Every item declaring {@link CustomItem#naturallyObtainable()}, keyed by its material.
     */
    private final Map<Material, CustomItem> obtainableItemMap = new HashMap<>();

    /**
     * Periodic sweep reconciling every online player's inventory.
     * <p>
     * The event handlers cover stacks as they enter an inventory, so this catches the remaining
     * case: a stack sitting untouched when an item's definition changes at runtime.
     */
    @Scheduler(period = 30, unit = TimeUnit.SECONDS)
    public final void onScheduler() {
        UtilServer.getOnlinePlayers().forEach(this::updatePlayerInventory);
    }

    /**
     * Returns the item registered under the given identifier.
     *
     * @param identifier the identifier to look up
     * @return an {@link Optional} containing the item, or empty if none is registered
     */
    public final Optional<CustomItem> getItemByIdentifier(final String identifier) {
        return Optional.ofNullable(this.identifierItemMap.get(identifier));
    }

    /**
     * Returns the naturally obtainable item registered under the given material.
     *
     * @param material the material to look up
     * @return an {@link Optional} containing the item, or empty if none is registered
     */
    public final Optional<CustomItem> getObtainableItemByMaterial(final Material material) {
        return Optional.ofNullable(this.obtainableItemMap.get(material));
    }

    /**
     * Reconciles a stack against the registry and returns the stack that should take its place.
     * <p>
     * A stack carrying a known identifier is replaced when its version is outdated, and returned
     * untouched otherwise. A stack carrying no identifier whose material belongs to a naturally
     * obtainable item is converted into that item, preserving amount and durability. Anything else
     * is returned unchanged.
     * <p>
     * The returned reference is the input itself when nothing changed, so callers can skip the write
     * with an identity comparison.
     *
     * @param itemStack the stack to reconcile, may be {@code null} or empty
     * @return the reconciled stack, or the input unchanged
     */
    public final ItemStack apply(final ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            // Identifier Check
            final CustomItem identifierItem = UtilItemStack.getPersistentData(itemStack, CustomItem.IDENTIFIER_KEY, PersistentDataType.STRING).flatMap(this::getItemByIdentifier).orElse(null);
            if (identifierItem != null) {
                return identifierItem.isOutdatedByItemStack(itemStack) ? identifierItem.update(itemStack) : itemStack;
            }

            // Obtainable Check
            final CustomItem obtainableItem = this.getObtainableItemByMaterial(itemStack.getType()).orElse(null);
            if (obtainableItem != null) {
                return obtainableItem.create(itemStack);
            }
        }

        return itemStack;
    }

    /**
     * Reconciles every slot of the player's inventory, including armour and offhand, writing back
     * only the slots {@link #apply(ItemStack)} actually changed.
     *
     * @param player the player whose inventory to reconcile
     */
    public final void updatePlayerInventory(final Player player) {
        final PlayerInventory playerInventory = player.getInventory();

        final ItemStack[] contents = playerInventory.getContents();

        for (int i = 0; i < contents.length; i++) {
            final ItemStack existingItemStack = contents[i];
            final ItemStack updatedItemStack = this.apply(existingItemStack);

            if (updatedItemStack != existingItemStack) {
                playerInventory.setItem(i, updatedItemStack);
            }
        }
    }
}