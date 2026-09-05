package io.github.trae.spigot.framework.item.events;

import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.item.Activatable;
import io.github.trae.spigot.framework.item.CustomItem;
import io.github.trae.spigot.framework.item.enums.ActivateType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when a player activates an item, before the item's action runs.
 * <p>
 * Dispatched by {@link io.github.trae.spigot.framework.item.ItemActivateListener}. Cancelling
 * suppresses the activation entirely, so neither the action nor the interaction results the item
 * declares are applied.
 * <p>
 * This is the system-level gate, for conditions external to the item, such as a region restriction
 * or a global lockdown. A condition the item owns belongs in
 * {@link Activatable#canActivate(Player, ItemStack, ActivateType)} instead, which is checked first.
 */
@AllArgsConstructor
@Getter
public class ItemPreActivateEvent extends CustomCancellableEvent {

    /**
     * The item being activated.
     */
    private final CustomItem item;

    /**
     * The player activating it.
     */
    private final Player player;

    /**
     * The specific stack that was clicked with.
     */
    private final ItemStack itemStack;

    /**
     * The kind of click that triggered the activation.
     */
    private final ActivateType activateType;
}