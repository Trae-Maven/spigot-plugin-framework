package io.github.trae.spigot.framework.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Default {@link Item} definition for a material without a naturally obtainable
 * {@link CustomItem}.
 * <p>
 * Instances are created lazily and cached by {@link ItemManager}, so an ordinary Minecraft stack
 * still reaches the item update events without being assigned a custom identifier or definition
 * version. Nothing is written to such a stack: this definition declares no display name and no lore,
 * and {@link ItemManager} reaches it through {@link Item#refresh(ItemStack)} rather than
 * {@link Item#update(ItemStack)}, so enchantments, an anvil name, and any persistent data another
 * plugin wrote are all left intact.
 * <p>
 * This exists so a listener can apply something uniformly across every stack, rather than only
 * across the ones this framework defines.
 */
public class DefaultItem extends Item {

    /**
     * Creates the default definition for the given material.
     *
     * @param material the material represented by this definition
     */
    protected DefaultItem(final Material material) {
        super(material);
    }

    /**
     * Default items do not define a custom display name.
     *
     * @return {@code null}
     */
    @Override
    public String getDisplayName() {
        return null;
    }

    /**
     * Default items do not define custom lore.
     *
     * @return {@code null}
     */
    @Override
    public List<String> getLore() {
        return null;
    }
}