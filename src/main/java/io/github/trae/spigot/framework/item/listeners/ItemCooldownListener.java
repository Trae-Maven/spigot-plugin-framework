package io.github.trae.spigot.framework.item.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.item.CustomItem;
import io.github.trae.spigot.framework.item.events.ItemStackUpdateEvent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * Maintains the vanilla use cooldown data component for custom items.
 * <p>
 * Custom items may opt out of their underlying material's vanilla use cooldown
 * through {@link CustomItem#removeUseCooldown()}. When enabled, the
 * {@link DataComponentTypes#USE_COOLDOWN} component is removed from the item
 * stack, preventing the material's normal use cooldown from applying.
 * <p>
 * When cooldown removal is disabled, this listener restores the cooldown
 * component if it is missing and the underlying material defines one by
 * default. Materials without a default use cooldown are left unchanged.
 * <p>
 * This keeps the cooldown component synchronized whenever a custom item stack
 * is created or updated without unnecessarily modifying stacks whose cooldown
 * state is already correct.
 */
@Singleton
public final class ItemCooldownListener implements Listener {

    /**
     * Synchronizes the use cooldown component of an updated custom item stack.
     * <p>
     * If {@link CustomItem#removeUseCooldown()} returns {@code true}, any
     * existing {@link DataComponentTypes#USE_COOLDOWN} component is removed.
     * Otherwise, a missing component is restored only when the underlying
     * material provides one by default.
     *
     * @param event the item stack update event
     */
    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void onItemStackUpdate(final ItemStackUpdateEvent event) {
        if (!(event.getItem() instanceof final CustomItem customItem)) {
            return;
        }

        final ItemStack itemStack = event.getItemStack();

        if (customItem.removeUseCooldown()) {
            if (itemStack.hasData(DataComponentTypes.USE_COOLDOWN)) {
                itemStack.unsetData(DataComponentTypes.USE_COOLDOWN);
            }
        } else {
            if (!itemStack.hasData(DataComponentTypes.USE_COOLDOWN)) {
                if (itemStack.getType().hasDefaultData(DataComponentTypes.USE_COOLDOWN)) {
                    itemStack.resetData(DataComponentTypes.USE_COOLDOWN);
                }
            }
        }
    }
}