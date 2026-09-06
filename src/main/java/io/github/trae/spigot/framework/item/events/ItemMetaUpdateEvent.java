package io.github.trae.spigot.framework.item.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import io.github.trae.spigot.framework.item.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Fired while an item is building a stack, once its description has been written to the meta and
 * before that meta is applied.
 * <p>
 * Dispatched by {@link Item#create(int, int)} and {@link Item#update(org.bukkit.inventory.ItemStack)}
 * from inside the meta edit, so the meta is live and a listener mutates it directly. Use this for
 * anything that belongs to the meta: persistent data, enchantments, or a meta-specific option the
 * declarative description does not cover.
 * <p>
 * Anything that lives on the stack rather than the meta, a data component in particular, belongs in
 * {@link ItemStackUpdateEvent} instead. Setting one here would be discarded, since applying a meta
 * replaces the stack's whole component set.
 * <p>
 * Not cancellable.
 */
@AllArgsConstructor
@Getter
public class ItemMetaUpdateEvent extends CustomEvent {

    /**
     * The item building the stack.
     */
    private final Item item;

    /**
     * The meta being built, live and safe to mutate.
     */
    private final ItemMeta itemMeta;
}