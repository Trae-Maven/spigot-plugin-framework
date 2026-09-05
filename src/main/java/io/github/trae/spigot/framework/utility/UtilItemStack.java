package io.github.trae.spigot.framework.utility;

import lombok.experimental.UtilityClass;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Reading and writing helpers for the persistent data attached to an {@link ItemStack}.
 * <p>
 * Writes take an {@link ItemMeta} rather than a stack, since they are performed inside an
 * {@code editMeta} block where the meta is already open. Reads take the stack itself and fold both
 * the missing-stack and missing-meta cases into an empty {@link Optional}, so a caller never has to
 * null-check either.
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
}