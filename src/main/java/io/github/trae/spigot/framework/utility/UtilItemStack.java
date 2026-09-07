package io.github.trae.spigot.framework.utility;

import io.github.trae.spigot.framework.item.CustomItem;
import lombok.experimental.UtilityClass;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.Optional;

/**
 * Helpers for working with an {@link ItemStack}: reading and writing its persistent data, comparing
 * two stacks, and moving stacks in and out of a player's inventory.
 * <p>
 * Persistent data writes take an {@link ItemMeta} rather than a stack, since they are performed
 * inside an {@code editMeta} block where the meta is already open. Reads take the stack itself and
 * fold both the missing-stack and missing-meta cases into an empty {@link Optional}, so a caller
 * never has to null-check either.
 */
@UtilityClass
public class UtilItemStack {

    /**
     * Writes a value into the meta's persistent data container under the given key.
     *
     * @param <P>                the primitive type the value is stored as
     * @param <C>                the complex type the value is held as
     * @param itemMeta           the meta to write to
     * @param namespacedKey      the key to store under
     * @param persistentDataType the type describing the conversion
     * @param value              the value to store
     */
    public static <P, C> void setPersistentDataType(final ItemMeta itemMeta, final NamespacedKey namespacedKey, final PersistentDataType<P, C> persistentDataType, final C value) {
        itemMeta.getPersistentDataContainer().set(namespacedKey, persistentDataType, value);
    }

    /**
     * Reads a value from the stack's persistent data container under the given key.
     *
     * @param <P>                the primitive type the value is stored as
     * @param <C>                the complex type the value is held as
     * @param itemStack          the stack to read from, may be {@code null}
     * @param namespacedKey      the key to look up
     * @param persistentDataType the type describing the conversion
     * @return an {@link Optional} containing the value, or empty if the stack is {@code null}, has
     * no meta, or carries no value under that key
     */
    public static <P, C> Optional<C> getPersistentData(final ItemStack itemStack, final NamespacedKey namespacedKey, final PersistentDataType<P, C> persistentDataType) {
        return Optional.ofNullable(itemStack).map(ItemStack::getItemMeta).map(itemMeta -> itemMeta.getPersistentDataContainer().get(namespacedKey, persistentDataType));
    }

    /**
     * Gives a stack to a player, dropping at their feet whatever does not fit.
     * <p>
     * The inventory is filled first, merging into partial stacks before using empty slots, so a
     * large amount is spread across as many slots as it needs rather than being capped at one. Only
     * the genuine remainder is dropped, so nothing is silently lost when the inventory is full.
     *
     * @param player    the player to give the stack to
     * @param itemStack the stack to give
     */
    public static void insert(final Player player, final ItemStack itemStack) {
        player.getInventory().addItem(itemStack).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    /**
     * Returns whether the player holds at least the given amount of a stack, counted across every
     * slot including armour and offhand.
     * <p>
     * Matching is by {@link #isSimilar(ItemStack, ItemStack)}, so a custom item is found by its
     * identifier rather than its exact meta and a stack predating a lore change still counts.
     *
     * @param player    the player whose inventory to search
     * @param itemStack the stack to look for
     * @param amount    the total amount required
     * @return {@code true} if the player holds at least that amount
     */
    public static boolean contains(final Player player, final ItemStack itemStack, final int amount) {
        int found = 0;

        for (final ItemStack content : player.getInventory().getContents()) {
            if (content == null || !isSimilar(itemStack, content)) {
                continue;
            }

            found += content.getAmount();

            if (found >= amount) {
                return true;
            }
        }

        return false;
    }

    /**
     * Takes the given amount of a stack from the player's inventory, spread across as many slots as
     * it takes.
     * <p>
     * All or nothing: the inventory is checked first and left untouched if the player is short, so a
     * caller never has to undo a partial removal. Matching is by
     * {@link #isSimilar(ItemStack, ItemStack)}, so a custom item is taken by identifier rather than
     * exact meta.
     *
     * @param player    the player whose inventory to take from
     * @param itemStack the stack to take
     * @param amount    the total amount to take
     * @return {@code true} if the amount was taken, {@code false} if the player was short and
     * nothing was removed
     */
    public static boolean remove(final Player player, final ItemStack itemStack, final int amount) {
        if (!contains(player, itemStack, amount)) {
            return false;
        }

        int remaining = amount;

        final PlayerInventory playerInventory = player.getInventory();

        final ItemStack[] contents = playerInventory.getContents();

        for (int index = 0; index < contents.length && remaining > 0; index++) {
            final ItemStack content = contents[index];
            if (content == null || !isSimilar(itemStack, content)) {
                continue;
            }

            final int taken = Math.min(remaining, content.getAmount());

            remaining -= taken;

            if (taken == content.getAmount()) {
                playerInventory.setItem(index, null);
            } else {
                content.setAmount(content.getAmount() - taken);
            }
        }

        return true;
    }

    /**
     * Returns whether two stacks should be treated as the same item, ignoring amount and durability.
     * <p>
     * When both carry a {@link CustomItem} identifier, only that is compared, so two stacks of the
     * same custom item match even when one predates a change to the item's description.
     * <p>
     * Otherwise the comparison is type, display name, lore, and enchantments. Damage is deliberately
     * excluded, since Bukkit's own {@code isSimilar} treats a worn tool and a fresh one as different
     * items, which is right for stacking and wrong for asking whether a player has one. Everything
     * else a component can carry, such as custom model data, item flags, and attribute modifiers, is
     * ignored too.
     * <p>
     * A stack carrying an identifier never matches one without, since one is a custom item and the
     * other is not.
     *
     * @param itemStack the stack to compare
     * @param content   the stack to compare it against
     * @return {@code true} if the two represent the same item
     */
    public static boolean isSimilar(final ItemStack itemStack, final ItemStack content) {
        if (itemStack == null || content == null) {
            return false;
        }

        if (itemStack.isEmpty() && content.isEmpty()) {
            return true;
        }

        if (itemStack.isEmpty() || content.isEmpty()) {
            return false;
        }

        final String itemStackIdentifier = getPersistentData(itemStack, CustomItem.IDENTIFIER_KEY, PersistentDataType.STRING).orElse(null);
        final String contentIdentifier = getPersistentData(content, CustomItem.IDENTIFIER_KEY, PersistentDataType.STRING).orElse(null);
        if (itemStackIdentifier != null && contentIdentifier != null) {
            return itemStackIdentifier.equals(contentIdentifier);
        }

        if (itemStack.getType() != content.getType()) {
            return false;
        }

        final ItemMeta itemStackMeta = itemStack.getItemMeta();
        final ItemMeta contentMeta = content.getItemMeta();

        if (itemStackMeta == null || contentMeta == null) {
            return itemStackMeta == contentMeta;
        }

        if (!Objects.equals(itemStackMeta.displayName(), contentMeta.displayName())) {
            return false;
        }

        if (!Objects.equals(itemStackMeta.lore(), contentMeta.lore())) {
            return false;
        }

        if (!itemStackMeta.getEnchants().equals(contentMeta.getEnchants())) {
            return false;
        }

        return true;
    }
}