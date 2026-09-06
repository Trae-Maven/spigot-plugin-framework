package io.github.trae.spigot.framework.item;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.utility.UtilItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the registry of {@link CustomItem}s and reconciles stacks against it.
 * <p>
 * Three lookups are maintained: every item by its identifier, every naturally obtainable item by its
 * material, and a lazily built {@link DefaultItem} per material. {@link #apply(ItemStack)} uses the
 * first to recognise a stack this framework produced, the second to convert a vanilla stack into its
 * custom counterpart, and the third to carry everything else through the update events so a listener
 * sees every stack rather than only the custom ones.
 * <p>
 * The registry populates itself on first use rather than at a fixed point in startup, so a lookup
 * is correct whenever it happens without the manager depending on any particular plugin's enable
 * order. {@link ItemApplyListener} drives reconciliation from the pickup, crafting, smelting, join,
 * and inventory-open flows, and {@link ItemActivateListener} reads the same registry to route
 * interactions to {@link ActivatableCustomItem} items. A scheduler sweeps online inventories
 * periodically so stacks left untouched are still brought up to date after an item's definition
 * changes.
 * <p>
 * Because population happens once, an item registered with the injector after the first lookup is
 * not picked up. Items are declared as components and constructed during their application's boot,
 * so this only matters for a plugin enabled well after the server has started.
 */
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
     * The default definition for each material, built on first use.
     * <p>
     * Cached rather than created per call, since a definition carries no per-stack state and the
     * fallback path in {@link #apply(ItemStack)} runs for every unrecognised stack.
     */
    private final Map<Material, DefaultItem> defaultItemMap = new EnumMap<>(Material.class);

    /**
     * Whether the registry has been built. Set before the scan runs, so a lookup performed from
     * within it cannot recurse.
     */
    private boolean populated;

    /**
     * Builds the identifier and material lookups from every {@link CustomItem} the injector knows
     * about, once. The default lookup is not touched, since it fills lazily as unrecognised
     * materials come through.
     * <p>
     * Deferring this to first use rather than binding it to a startup event keeps the manager
     * independent of plugin enable order: whichever lookup happens first sees every item registered
     * by that point.
     */
    private void populateIfNecessary() {
        if (this.populated) {
            return;
        }

        this.populated = true;

        for (final CustomItem customItem : InjectorApi.getAll(CustomItem.class)) {
            this.identifierItemMap.put(customItem.getIdentifier(), customItem);

            if (customItem.naturallyObtainable()) {
                this.obtainableItemMap.put(customItem.getMaterial(), customItem);
            }
        }
    }

    /**
     * Returns every registered item.
     * <p>
     * The list is an immutable copy taken at call time, in no meaningful order.
     *
     * @return the registered items
     */
    public final List<CustomItem> getItems() {
        return List.copyOf(this.identifierItemMap.values());
    }

    /**
     * Returns the item registered under the given identifier, building the registry first if this
     * is the first lookup.
     *
     * @param identifier the identifier to look up
     * @return an {@link Optional} containing the item, or empty if none is registered
     */
    public final Optional<CustomItem> getItemByIdentifier(final String identifier) {
        this.populateIfNecessary();

        return Optional.ofNullable(this.identifierItemMap.get(identifier));
    }

    /**
     * Returns the naturally obtainable item registered under the given material, building the
     * registry first if this is the first lookup.
     *
     * @param material the material to look up
     * @return an {@link Optional} containing the item, or empty if none is registered
     */
    public final Optional<CustomItem> getObtainableItemByMaterial(final Material material) {
        this.populateIfNecessary();

        return Optional.ofNullable(this.obtainableItemMap.get(material));
    }

    /**
     * Returns the item that produced the given stack, resolved from the identifier stamped into its
     * persistent data.
     * <p>
     * A stack this framework never produced carries no identifier and resolves to empty, as does one
     * whose identifier names an item that is no longer registered.
     *
     * @param itemStack the stack to resolve, may be {@code null}
     * @return an {@link Optional} containing the owning item, or empty if the stack carries no known
     * identifier
     */
    public final Optional<CustomItem> getItemByItemStack(final ItemStack itemStack) {
        return UtilItemStack.getPersistentData(itemStack, CustomItem.IDENTIFIER_KEY, PersistentDataType.STRING).flatMap(this::getItemByIdentifier);
    }

    /**
     * Reconciles a stack against the registry and returns the stack that should take its place.
     * <p>
     * A stack carrying a known identifier is updated when its version is outdated and refreshed when
     * it is not, so a listener runs either way rather than only on the ones that happened to need
     * rewriting. A stack carrying no identifier whose material belongs to a naturally obtainable item
     * is converted into that item. Anything else is refreshed under its {@link DefaultItem}, which
     * writes nothing to it.
     * <p>
     * Only the obtainable path replaces a stack outright, and it does so deliberately: the material
     * is being reinterpreted as a custom item. Every other path returns the input by reference, so an
     * identity comparison tells a caller whether the stack was replaced rather than merely altered.
     *
     * @param itemStack the stack to reconcile, may be {@code null} or empty
     * @return the reconciled stack
     */
    public final ItemStack apply(final ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            // Identifier Check
            final CustomItem identifierItem = this.getItemByItemStack(itemStack).orElse(null);
            if (identifierItem != null) {
                return identifierItem.isOutdatedByItemStack(itemStack) ? identifierItem.update(itemStack) : identifierItem.refresh(itemStack);
            }

            // Obtainable Check
            final CustomItem obtainableItem = this.getObtainableItemByMaterial(itemStack.getType()).orElse(null);
            if (obtainableItem != null) {
                return obtainableItem.create(itemStack);
            }

            return this.defaultItemMap.computeIfAbsent(itemStack.getType(), DefaultItem::new).refresh(itemStack);
        }

        return itemStack;
    }

    /**
     * Reconciles every slot in the given inventory against the item registry, writing back only
     * stacks that {@link #apply(ItemStack)} replaces.
     * <p>
     * The check is by reference, so in practice only the obtainable path triggers a write. Every
     * other path edits the stack in place and returns it, which for a live inventory stack is enough
     * for the change to stick.
     *
     * @param inventory the inventory whose contents should be reconciled
     */
    public final void updateInventory(final Inventory inventory) {
        final ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length; i++) {
            final ItemStack existingItemStack = contents[i];
            final ItemStack updatedItemStack = this.apply(existingItemStack);

            if (updatedItemStack != existingItemStack) {
                inventory.setItem(i, updatedItemStack);
            }
        }
    }
}