package io.github.trae.spigot.framework.item;

import io.github.trae.spigot.framework.utility.UtilItemStack;
import io.github.trae.utilities.UtilHash;
import io.github.trae.utilities.UtilString;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * An {@link Item} that stamps its identity onto every stack it produces, so the stack can be
 * recognised later and reconciled against the item's current definition.
 * <p>
 * Two values are written into the stack's persistent data: an identifier naming which item it is,
 * and a version hashed from the item's full description. {@link ItemManager} reads the identifier to
 * find the owning item and compares the version to decide whether the stack is stale, replacing it
 * when the definition has since changed.
 * <p>
 * Subclasses are discovered automatically by {@link ItemApplyListener} via the dependency injector
 * and registered under their identifier. An item declaring {@link #naturallyObtainable()} is also
 * registered under its material, so any vanilla stack of that type a player obtains is converted
 * into the custom item. A subclass additionally implementing {@link Activatable} gains a click
 * action, routed by {@link ItemActivateListener}.
 */
public abstract class CustomItem extends Item {

    /**
     * Persistent data key holding the item's identifier on every stack it produces.
     */
    public static final NamespacedKey IDENTIFIER_KEY = new NamespacedKey("custom", "item_identifier");

    /**
     * Persistent data key holding the description hash the stack was created from.
     */
    public static final NamespacedKey VERSION_KEY = new NamespacedKey("custom", "item_version");

    /**
     * The unique identifier this item is registered under, written onto every stack it produces.
     */
    @Getter
    private final String identifier;

    /**
     * The lazily computed hash of this item's description. Cached after first use.
     */
    private String version;

    /**
     * Creates a custom item of the given material under the given identifier.
     *
     * @param material   the material every stack is created with
     * @param identifier the unique identifier to register and stamp under
     */
    protected CustomItem(final Material material, final String identifier) {
        super(material);

        this.identifier = identifier;
    }

    /**
     * Returns the hash of this item's description, computing it on first use.
     * <p>
     * Resolution is deferred rather than done in the constructor because
     * {@link #generateVersionEntries()} calls overridable hooks, which would run before a subclass
     * had assigned its own fields.
     *
     * @return the description hash
     */
    private String getVersion() {
        if (this.version == null) {
            this.version = UtilHash.hashToString("SHA-256", String.join("\u0000", this.generateVersionEntries()));
        }

        return this.version;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Writes this item's identifier and version into the stack's persistent data.</p>
     */
    @Override
    protected final void stamp(final ItemMeta itemMeta) {
        UtilItemStack.setPersistentDataType(itemMeta, IDENTIFIER_KEY, PersistentDataType.STRING, this.identifier);
        UtilItemStack.setPersistentDataType(itemMeta, VERSION_KEY, PersistentDataType.STRING, this.getVersion());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Defaults to {@code true} for custom items, since their tooltips are authored rather than
     * generated.</p>
     */
    @Override
    protected boolean hideAttributes() {
        return true;
    }

    /**
     * Returns whether players obtain this item through normal gameplay: mining, crafting, smelting,
     * or picking it up. When {@code true}, the item is registered under its material and any vanilla
     * stack of that type is converted into this item on the way into a player's inventory. Defaults
     * to {@code false}.
     *
     * @return {@code true} if vanilla stacks of this material should become this item
     */
    protected boolean naturallyObtainable() {
        return false;
    }

    /**
     * Returns the values the version hash is computed from, meaning every part of the description
     * that changes what the stack should look like.
     * <p>
     * Changing any of them changes the hash, which marks every existing stack as outdated and causes
     * {@link ItemManager#apply(ItemStack)} to replace it. Override to add subclass state that the
     * base description does not cover, concatenating onto {@code super.generateVersionEntries()}.
     *
     * @return the ordered values contributing to the version hash
     */
    protected List<String> generateVersionEntries() {
        return List.of(
                UtilString.pair("Material", this.getMaterial().name()),
                UtilString.pair("Display-Name", this.getDisplayName()),
                UtilString.pair("Lore", String.join("\u0001", this.getLore())),
                UtilString.pair("Color", Integer.toString(this.getColor().getRGB())),
                UtilString.pair("Model", this.getModel() != null ? this.getModel().asString() : ""),
                UtilString.pair("Tooltip-Style", this.getTooltipStyle() != null ? this.getTooltipStyle().asString() : ""),
                UtilString.pair("Hide-Attributes", Boolean.toString(this.hideAttributes())),
                UtilString.pair("Naturally-Obtainable", Boolean.toString(this.naturallyObtainable()))
        );
    }

    /**
     * Returns whether the given stack was produced by this item, matching on the stamped identifier
     * rather than material or meta.
     *
     * @param itemStack the stack to check
     * @return {@code true} if the stack carries this item's identifier
     */
    public final boolean isSimilarByIdentifier(final ItemStack itemStack) {
        return UtilItemStack.getPersistentData(itemStack, IDENTIFIER_KEY, PersistentDataType.STRING)
                .map(this.identifier::equals)
                .orElse(false);
    }

    /**
     * Returns whether the given stack was produced from an older version of this item's description
     * and should be replaced.
     * <p>
     * A stack carrying no version at all is treated as outdated, so stacks predating the version
     * system are reconciled on first sight.
     *
     * @param itemStack the stack to check
     * @return {@code true} if the stack's version differs from the current one or is absent
     */
    public final boolean isOutdatedByItemStack(final ItemStack itemStack) {
        return UtilItemStack.getPersistentData(itemStack, VERSION_KEY, PersistentDataType.STRING)
                .map(version -> !this.getVersion().equals(version))
                .orElse(true);
    }
}