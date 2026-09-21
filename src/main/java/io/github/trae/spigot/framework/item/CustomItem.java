package io.github.trae.spigot.framework.item;

import io.github.trae.spigot.framework.item.listeners.ItemActivateListener;
import io.github.trae.spigot.framework.item.listeners.ItemApplyListener;
import io.github.trae.spigot.framework.item.types.ActivatableCustomItem;
import io.github.trae.spigot.framework.utility.UtilItemStack;
import io.github.trae.utilities.UtilHash;
import io.github.trae.utilities.UtilString;
import lombok.Getter;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An {@link Item} that stamps its identity onto every stack it produces, so the stack can be
 * recognised later and reconciled against the item's current definition.
 * <p>
 * Two values are written into the stack's persistent data: an identifier saying which item produced
 * it, and a version hashed from the item's full description. {@link ItemManager} reads the identifier
 * to find the owning item and compares the version to decide whether the stack is stale, updating it
 * when the definition has since changed.
 * <p>
 * Identity and name are deliberately separate. The identifier is opaque and permanent, so an item can
 * be renamed, restyled, or moved between packages without orphaning the stacks already in circulation;
 * the namespace is the readable key those stacks are never stamped with, and exists for anything a
 * human or a config has to name the item by.
 * <p>
 * Subclasses are discovered automatically by {@link ItemApplyListener} via the dependency injector
 * and registered under their identifier. An item declaring {@link #naturallyObtainable()} is also
 * registered under its material, so any vanilla stack of that type a player obtains is converted
 * into the custom item. Extend {@link ActivatableCustomItem} instead of this class for an item that
 * also does something when clicked, routed by {@link ItemActivateListener}.
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
     * Persistent data key marking a stack that should be deleted rather than reset once its item is
     * no longer registered.
     */
    public static final NamespacedKey DELETABLE_KEY = new NamespacedKey("custom", "item_deletable");

    /**
     * The opaque, permanent identity of this item, written onto every stack it produces, and the
     * readable key it is named by.
     * <p>
     * The identifier never changes for the life of the item, which is what lets a stack outlive a
     * rename: only it is stamped, so nothing on an existing stack refers to the namespace. The
     * namespace carries no identity at all and exists for commands, configuration, and anywhere else
     * a person rather than the framework has to refer to the item.
     */
    @Getter
    private final String identifier, namespace;

    /**
     * The lazily computed hash of this item's description. Cached after first use.
     */
    private String version;

    /**
     * Creates a custom item of the given material.
     *
     * @param material   the material every stack is created with
     * @param identifier the opaque, permanent identity to stamp onto every stack, which must not
     *                   change once stacks carrying it exist
     * @param namespace  the readable key this item is named by
     */
    public CustomItem(final Material material, final String identifier, final String namespace) {
        super(material);

        this.identifier = identifier;
        this.namespace = namespace;
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
     * <p>Writes this item's identifier, version, and deletable answer into the stack's persistent
     * data. The answer is written either way rather than only when true, so flipping it off actually
     * takes effect on existing stacks. The namespace is not written, since it names the item rather
     * than identifying it and may change.</p>
     */
    @Override
    protected final void stamp(final ItemMeta itemMeta) {
        UtilItemStack.setPersistentDataType(itemMeta, IDENTIFIER_KEY, PersistentDataType.STRING, this.identifier);
        UtilItemStack.setPersistentDataType(itemMeta, VERSION_KEY, PersistentDataType.STRING, this.getVersion());

        UtilItemStack.setPersistentDataType(itemMeta, DELETABLE_KEY, PersistentDataType.BOOLEAN, this.deleteIfRemoved());
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
     * Returns whether the material's vanilla use cooldown should be removed from stacks of this
     * item. Defaults to {@code true}.
     *
     * @return {@code true} to remove the vanilla use cooldown
     */
    public boolean removeUseCooldown() {
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
     * Returns what should happen to a stack this item produced once the item itself is gone from the
     * registry. When {@code true}, the stack is deleted outright; when {@code false}, it is reset to
     * a plain stack of its material. Defaults to {@code false}.
     * <p>
     * The answer is stamped onto each stack rather than read from the item at the time, since by then
     * there is no item left to ask. A stack produced before this was enabled therefore carries the
     * old answer until it is updated.
     * <p>
     * Deleting suits an item whose material is meaningless without it, where leaving the bare
     * material behind would hand the player something they never earned. Resetting suits one built on
     * a material that stands on its own.
     *
     * @return {@code true} to delete orphaned stacks rather than reset them
     */
    protected boolean deleteIfRemoved() {
        return false;
    }

    /**
     * Returns the values the version hash is computed from, meaning every part of the description
     * that changes how the stack should look or behave.
     * <p>
     * Changing any of them changes the hash, which marks every existing stack as outdated and causes
     * {@link ItemManager#apply(ItemStack)} to update it. Neither the identifier nor the namespace
     * contributes: the identifier never changes, and the namespace changing is precisely the case
     * this design exists to make harmless. Override to add subclass state that the base description
     * does not cover, concatenating onto {@code super.generateVersionEntries()}.
     *
     * @return the ordered values contributing to the version hash
     */
    protected List<String> generateVersionEntries() {
        return List.of(
                UtilString.pair("Material", this.getMaterial().name()),
                UtilString.pair("Name", this.getName()),
                UtilString.pair("Lore", this.getLore() != null ? String.join("\u0001", this.getLore()) : ""),
                UtilString.pair("Color", Integer.toString(this.getColor().getRGB())),
                UtilString.pair("Decorations", this.getDecorations() != null ? this.getDecorations().stream().map(TextDecoration::name).collect(Collectors.joining("\u0001")) : ""),
                UtilString.pair("Model", this.getModel() != null ? this.getModel().asString() : ""),
                UtilString.pair("Tooltip-Style", this.getTooltipStyle() != null ? this.getTooltipStyle().asString() : ""),
                UtilString.pair("Hide-Attributes", Boolean.toString(this.hideAttributes())),
                UtilString.pair("Remove-Use-Cooldown", Boolean.toString(this.removeUseCooldown())),
                UtilString.pair("Naturally-Obtainable", Boolean.toString(this.naturallyObtainable())),
                UtilString.pair("Delete-If-Removed", Boolean.toString(this.deleteIfRemoved())),
                UtilString.pair("Style-Name", this.getStyle() != null ? this.getStyle().getName() : ""),
                UtilString.pair("Style-Tag", this.getStyle() != null ? this.getStyle().getTag() : "")
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
     * and should be updated.
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

    /**
     * Returns whether the entity is holding this item in its main hand, matching on the stamped
     * identifier rather than material or meta.
     * <p>
     * Only the main hand is checked, and an entity with no equipment at all resolves to
     * {@code false}.
     *
     * @param livingEntity the entity to check
     * @return {@code true} if the entity's main hand holds a stack this item produced
     */
    public final boolean isHolding(final LivingEntity livingEntity) {
        return Optional.ofNullable(livingEntity.getEquipment())
                .map(EntityEquipment::getItemInMainHand)
                .map(this::isSimilarByIdentifier)
                .orElse(false);
    }
}