package io.github.trae.spigot.framework.utility.search.types;

import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.spigot.framework.utility.search.SpigotSearchEngine;
import io.github.trae.utilities.UtilString;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

/**
 * Search engine resolving {@link Material} values from the full material registry.
 *
 * <p>Matching runs against the raw enum constant name, so the input is expected in
 * {@code UNDERSCORE_CASE} form, while listed matches are cleaned by {@link UtilString#clean} for
 * display.</p>
 */
public class MaterialSearchEngine extends SpigotSearchEngine<Material> {

    /**
     * Creates a search engine over every {@link Material} constant.
     */
    public MaterialSearchEngine() {
        super("Material Search", () -> List.of(Material.values()));
    }

    /**
     * {@inheritDoc}
     *
     * @return the cleaned material name serialized in yellow
     */
    @Override
    protected String getTypeFormat(final Material material) {
        return UtilColor.serialize(ChatColor.YELLOW.getColor(), UtilString.clean(material.name()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Compares the material's constant name to the input, ignoring case.
     */
    @Override
    protected boolean isExact(final Material material, final String result) {
        return material.name().equalsIgnoreCase(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Tests whether the material's constant name contains the input, ignoring case.
     */
    @Override
    protected boolean isMatching(final Material material, final String result) {
        return material.name().toLowerCase(Locale.ROOT).contains(result.toLowerCase(Locale.ROOT));
    }
}