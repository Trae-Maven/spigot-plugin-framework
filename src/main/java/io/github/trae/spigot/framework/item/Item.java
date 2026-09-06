package io.github.trae.spigot.framework.item;

import io.github.trae.spigot.framework.item.events.ItemMetaUpdateEvent;
import io.github.trae.spigot.framework.item.events.ItemStackUpdateEvent;
import io.github.trae.spigot.framework.item.style.ItemStyle;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.UtilMessage;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.utilities.UtilJava;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Describes an {@link ItemStack} declaratively: its material, display name, lore, and presentation
 * options. Subclasses supply the description; {@link #create(int, int)} and
 * {@link #update(ItemStack)} turn it into a stack, and {@link #refresh(ItemStack)} dispatches the
 * update events against one that needs no rewriting.
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
     * Applies any meta-specific options this item needs, after the display options have been
     * written. Does nothing by default.
     * <p>
     * This is the general-purpose hook for anything the declarative description does not cover:
     * cast the meta to the type the material actually produces and set what you need. It exists
     * separately from {@link #stamp(ItemMeta)} because {@link CustomItem} marks that method final
     * to write its identifier and version, leaving subclasses no way to touch the meta otherwise.
     *
     * <pre>{@code
     * @Override
     * protected void editMeta(final ItemMeta itemMeta) {
     *     if (itemMeta instanceof final LeatherArmorMeta leatherArmorMeta) {
     *         leatherArmorMeta.setColor(Color.fromRGB(0x228B22));
     *     }
     * }
     * }</pre>
     *
     * <p>Running last means an option set here overrides the equivalent display option, so an item
     * setting a display name in both places keeps the one written here.</p>
     *
     * <p>Anything that lives on the stack rather than the meta, a data component in particular,
     * belongs in an {@link ItemStackUpdateEvent} listener instead, since the meta write that follows
     * this hook would discard it.</p>
     *
     * @param itemMeta the meta being built
     */
    protected void editMeta(final ItemMeta itemMeta) {
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
     * Returns the shared visual style this item belongs to, or {@code null} for none.
     * <p>
     * A style supplies the display name colour, the tooltip style, and a tag appended to the lore,
     * so an item declaring one gets all three from a single decision rather than setting each
     * itself. The framework attaches no meaning to a style beyond those three values; grouping them
     * into rarities, tiers, or anything else is a decision for the plugin that defines them.
     *
     * @return the style, or {@code null}
     */
    protected ItemStyle getStyle() {
        return null;
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
     * default. Resolved from {@link #getStyle()} unless overridden.
     *
     * @return the tooltip style key, or {@code null}
     */
    protected NamespacedKey getTooltipStyle() {
        return this.getStyle() != null ? this.getStyle().getTooltipStyle() : null;
    }

    /**
     * Returns the colour applied to the display name when the name itself carries none. Resolved
     * from {@link #getStyle()}, falling back to white for an item with no style.
     *
     * @return the display name colour, never {@code null}
     */
    protected Color getColor() {
        return this.getStyle() != null ? this.getStyle().getColor() : ChatColor.WHITE.getColor();
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
     * <p>
     * An item with a style has its tag appended beneath these lines, separated by a blank, so a
     * subclass never writes the tag itself.
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
     * <p>
     * Two events are dispatched as the stack is built: an {@link ItemMetaUpdateEvent} from inside the
     * meta edit, for listeners writing to the meta, and an {@link ItemStackUpdateEvent} once that
     * edit has been applied, for listeners writing to the stack itself. The order matters, since
     * applying a meta replaces the stack's whole component set.
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

            UtilEvent.dispatch(new ItemMetaUpdateEvent(this, itemMeta));
        });

        UtilEvent.dispatch(new ItemStackUpdateEvent(this, itemStack));

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
     * convert a vanilla stack into its custom counterpart.
     * <p>
     * Only the amount and durability cross over. A fresh stack is built, so enchantments, an anvil
     * name, and any persistent data another plugin wrote are not carried across: the material is
     * being reinterpreted as this item rather than the stack being preserved.
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
     * The stack is edited in place when its material already matches, and copied only when the
     * material has to change, so nothing it carries is discarded either way.
     * <p>
     * Dispatches the same {@link ItemMetaUpdateEvent} and {@link ItemStackUpdateEvent} pair as
     * {@link #create(int, int)}, so a listener applies to a reconciled stack exactly as it does to a
     * fresh one.
     *
     * @param itemStack the stack to update
     * @return the updated stack, which is the input itself unless the material changed
     */
    public final ItemStack update(final ItemStack itemStack) {
        final ItemStack newItemStack = itemStack.getType() == this.material ? itemStack : itemStack.withType(this.getMaterial());

        newItemStack.editMeta(itemMeta -> {
            this.applyItemMeta(itemMeta);

            UtilEvent.dispatch(new ItemMetaUpdateEvent(this, itemMeta));
        });

        UtilEvent.dispatch(new ItemStackUpdateEvent(this, newItemStack));

        return newItemStack;
    }

    /**
     * Dispatches this item's stack update event against a stack without re-applying its description.
     * <p>
     * The stack is left exactly as it is, so nothing it carries is rewritten. Only
     * {@link ItemStackUpdateEvent} fires: there is no meta being built for an
     * {@link ItemMetaUpdateEvent} listener to contribute to, and opening one purely to dispatch would
     * cost a meta serialisation per stack for nothing.
     * <p>
     * This is what separates it from {@link #update(ItemStack)}, which writes the description before
     * dispatching. Use that when the stack is out of date, and this when it is not, so that a
     * listener applying something the version hash cannot know about still runs either way.
     *
     * @param itemStack the stack to dispatch for
     * @return the same stack
     */
    public final ItemStack refresh(final ItemStack itemStack) {
        UtilEvent.dispatch(new ItemStackUpdateEvent(this, itemStack));

        return itemStack;
    }

    /**
     * Applies this item's full description to a meta: the subclass stamp first, then display name,
     * lore, model, tooltip style, and item flags. Options returning {@code null} are skipped,
     * leaving the vanilla default in place.
     * <p>
     * The style's tag is appended beneath the item's own lore, separated by a blank line, so it
     * always reads as a footer rather than as another lore line.
     *
     * @param itemMeta the meta to write to
     */
    private void applyItemMeta(final ItemMeta itemMeta) {
        // Stamp
        this.stamp(itemMeta);

        // Edit Meta
        this.editMeta(itemMeta);

        // Display Name
        if (this.getDisplayName() != null) {
            itemMeta.displayName(UtilMessage.deserialize(this.getDisplayName()).colorIfAbsent(TextColor.color(this.getColor().getRGB() & 0xFFFFFF)).decoration(TextDecoration.ITALIC, false));
        }

        // Lore
        if (this.getLore() != null) {
            final List<String> lore = UtilJava.createCollection(new ArrayList<>(), list -> {
                if (!this.getLore().isEmpty()) {
                    list.addAll(this.getLore());
                }

                if (this.getStyle() != null) {
                    list.add("");
                    list.add("<font:custom:tags>%s</font>".formatted(this.getStyle().getTag()));
                }
            });

            itemMeta.lore(lore.stream().map(line -> UtilMessage.deserialize(line).colorIfAbsent(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)).toList());
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