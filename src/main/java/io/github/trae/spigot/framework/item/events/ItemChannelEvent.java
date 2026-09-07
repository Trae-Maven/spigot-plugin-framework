package io.github.trae.spigot.framework.item.events;

import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.item.listeners.ItemActivateListener;
import io.github.trae.spigot.framework.item.types.ChannelCustomItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Fired every tick a player is channelling an item, before the item's per-tick action runs.
 * <p>
 * Dispatched by {@link ItemActivateListener}. Cancelling ends
 * the channel outright: {@code onChannel} does not run for that tick, {@code onStop} fires, and the
 * player is dropped from the item's active set. It is the system-level equivalent of {@code
 * canChannel} returning {@code false}, for conditions external to the item such as a region
 * restriction or a global lockdown.
 * <p>
 * Fires once per channelling player per tick, so a listener here runs far more often than one on the
 * activation events and should stay cheap.
 */
@AllArgsConstructor
@Getter
public class ItemChannelEvent extends CustomCancellableEvent {

    /**
     * The item being channelled.
     */
    private final ChannelCustomItem item;

    /**
     * The player channelling it.
     */
    private final Player player;

    /**
     * The stack being channelled with.
     */
    private final ItemStack itemStack;
}