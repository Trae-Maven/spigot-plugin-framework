package io.github.trae.spigot.framework.utility.search.types;

import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.spigot.framework.utility.search.SpigotSearchEngine;
import io.github.trae.utilities.UtilString;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.awt.Color;
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
     * <p>Returns the material's constant name cleaned by {@link UtilString#clean} for display.
     */
    @Override
    protected String getTypeName(final Material material, final CommandSender commandSender) {
        return UtilString.clean(material.name());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Every material is displayed in yellow.
     */
    @Override
    protected Color getTypeColor(final Material material, final CommandSender commandSender) {
        return ChatColor.YELLOW.getColor();
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