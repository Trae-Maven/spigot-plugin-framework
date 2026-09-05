package io.github.trae.spigot.framework.item.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import io.github.trae.spigot.framework.item.CustomItem;
import io.github.trae.spigot.framework.item.enums.ActivateType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Fired after an item's activation has run.
 * <p>
 * Dispatched by {@link io.github.trae.spigot.framework.item.ItemActivateListener}. Not cancellable,
 * since the action has already happened: this is for reacting to a successful activation, such as
 * recording a cooldown, incrementing a statistic, or logging.
 * <p>
 * Only fires for an activation that actually ran, so an attempt refused by
 * {@link io.github.trae.spigot.framework.item.Activatable#canActivate} or by a cancelled
 * {@link ItemPreActivateEvent} produces no post event.
 */
@AllArgsConstructor
@Getter
public class ItemPostActivateEvent extends CustomEvent {

    /**
     * The item that was activated.
     */
    private final CustomItem item;

    /**
     * The player who activated it.
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