package io.github.trae.spigot.framework.item.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import io.github.trae.spigot.framework.item.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

/**
 * Fired once an item has finished building a stack and its meta has been applied.
 * <p>
 * Dispatched by {@link Item#create(int, int)} and {@link Item#update(ItemStack)} after the meta
 * edit closes, so the stack is finished and a listener mutates it directly. Data components must be
 * set here rather than in {@link ItemMetaUpdateEvent}: applying a meta replaces the stack's whole
 * component set, so one written during the meta edit would be wiped by the write that follows it.
 * <p>
 * Not cancellable.
 */
@AllArgsConstructor
@Getter
public class ItemStackUpdateEvent extends CustomEvent {

    /**
     * The item that built the stack.
     */
    private final Item item;

    /**
     * The finished stack, live and safe to mutate.
     */
    private final ItemStack itemStack;
}