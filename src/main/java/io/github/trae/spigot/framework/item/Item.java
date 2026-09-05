package io.github.trae.spigot.framework.item;

import io.github.trae.spigot.framework.utility.UtilMessage;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.awt.Color;
import java.util.List;

/**
 * Describes an {@link ItemStack} declaratively: its material, display name, lore, and presentation
 * options. Subclasses supply the description; {@link #create(int, int)} and
 * {@link #update(ItemStack)} turn it into a stack.
 * <p>
 * This base type carries no identity, so the stacks it produces are indistinguishable from any
 * other. It suits transient stacks such as window icons, which are never picked up, persisted, or
 * reconciled. {@link CustomItem} adds an identifier and version for stacks that live in player
 * inventories and need to be recognised and kept up to date.
 * <p>
 * Every option hook has a default, so the minimum a subclass supplies is
 * {@link #getDisplayName()} and {@link #getLore()}.
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public abstract class Item {

    /**
     * The material every stack produced by this item is created with.
     */
    private final Material material;

    /**
     * Writes any persistent data this item needs onto the meta before the display options are
     * applied. Does nothing by default.
     *
     * @param itemMeta the meta being built
     */
    protected void stamp(final ItemMeta itemMeta) {
    }

    /**
     * Returns whether every {@link ItemFlag} is applied, hiding attribute modifiers, enchantments,
     * and similar generated tooltip lines. Defaults to {@code false}.
     *
     * @return {@code true} to hide generated tooltip content
     */
    protected boolean hideAttributes() {
        return false;
    }

    /**
     * Returns the item model key applied to the stack, or {@code null} to leave it at the default.
     *
     * @return the item model key, or {@code null}
     */
    protected NamespacedKey getModel() {
        return null;
    }

    /**
     * Returns the tooltip style key applied to the stack, or {@code null} to leave it at the
     * default.
     *
     * @return the tooltip style key, or {@code null}
     */
    protected NamespacedKey getTooltipStyle() {
        return null;
    }

    /**
     * Returns the colour applied to the display name when the name itself carries none. Defaults to
     * white.
     *
     * @return the display name colour, never {@code null}
     */
    protected Color getColor() {
        return ChatColor.WHITE.getColor();
    }

    /**
     * Returns the display name, or {@code null} to leave the stack's name at the vanilla default.
     * The name is deserialized through {@link UtilMessage} and coloured with {@link #getColor()}
     * where it carries no colour of its own.
     *
     * @return the display name, or {@code null}
     */
    protected abstract String getDisplayName();

    /**
     * Returns the lore lines, or an empty list for no lore. Each line is deserialized through
     * {@link UtilMessage} and defaults to white where it carries no colour of its own.
     *
     * @return the lore lines
     */
    protected abstract List<String> getLore();

    /**
     * Returns whether the given stack shares this item's material. Material alone says nothing about
     * identity, so this matches any stack of that type, custom or vanilla.
     *
     * @param itemStack the stack to check
     * @return {@code true} if the stack is non-null and of this item's material
     */
    public final boolean isSimilarByMaterial(final ItemStack itemStack) {
        return itemStack != null && this.material == itemStack.getType();
    }

    /**
     * Creates a new stack of this item at the given amount and durability.
     *
     * @param amount     the stack size
     * @param durability the damage value, applied only when positive and the meta is
     *                   {@link Damageable}
     * @return the created stack
     */
    public final ItemStack create(final int amount, final int durability) {
        final ItemStack itemStack = ItemStack.of(this.getMaterial(), amount);

        itemStack.editMeta(itemMeta -> {
            if (itemMeta instanceof final Damageable damageable && durability > 0) {
                damageable.setDamage(durability);
            }

            this.applyItemMeta(itemMeta);
        });

        return itemStack;
    }

    /**
     * Creates a new undamaged stack of this item at the given amount.
     *
     * @param amount the stack size
     * @return the created stack
     */
    public final ItemStack create(final int amount) {
        return this.create(amount, 0);
    }

    /**
     * Creates a single undamaged stack of this item.
     *
     * @return the created stack
     */
    public final ItemStack create() {
        return this.create(1, 0);
    }

    /**
     * Creates a stack of this item carrying the amount and durability of an existing stack. Used to
     * convert a vanilla stack into its custom counterpart without losing either.
     *
     * @param itemStack the stack to take the amount and durability from
     * @return the created stack
     */
    public final ItemStack create(final ItemStack itemStack) {
        final int durability = itemStack.getItemMeta() instanceof final Damageable damageable ? damageable.getDamage() : 0;

        return this.create(itemStack.getAmount(), durability);
    }

    /**
     * Re-applies this item's description to an existing stack, preserving its amount and durability.
     * Used to bring a stack a player already owns back in line with the item's current definition.
     * <p>
     * Returns a new stack rather than editing in place, so callers can tell by reference whether
     * anything changed.
     *
     * @param itemStack the stack to update
     * @return the updated stack
     */
    public final ItemStack update(final ItemStack itemStack) {
        final ItemStack newItemStack = itemStack.withType(this.getMaterial());

        newItemStack.editMeta(this::applyItemMeta);

        return newItemStack;
    }

    /**
     * Applies this item's full description to a meta: the subclass stamp first, then display name,
     * lore, model, tooltip style, and item flags. Options returning {@code null} are skipped,
     * leaving the vanilla default in place.
     *
     * @param itemMeta the meta to write to
     */
    private void applyItemMeta(final ItemMeta itemMeta) {
        // Stamp
        this.stamp(itemMeta);

        // Display Name
        if (this.getDisplayName() != null) {
            itemMeta.displayName(UtilMessage.deserialize(this.getDisplayName()).colorIfAbsent(TextColor.color(this.getColor().getRGB() & 0xFFFFFF)).decoration(TextDecoration.ITALIC, false));
        }

        // Lore
        if (this.getLore() != null && !this.getLore().isEmpty()) {
            itemMeta.lore(this.getLore().stream().map(line -> UtilMessage.deserialize(line).colorIfAbsent(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)).toList());
        }

        // Model
        if (this.getModel() != null) {
            itemMeta.setItemModel(this.getModel());
        }

        // Tooltip Style
        if (this.getTooltipStyle() != null) {
            itemMeta.setTooltipStyle(this.getTooltipStyle());
        }

        // Hide Attributes
        if (this.hideAttributes()) {
            itemMeta.addItemFlags(ItemFlag.values());
        }
    }
}