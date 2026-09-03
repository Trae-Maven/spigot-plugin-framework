package io.github.trae.spigot.framework.utility.search.types;

import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.spigot.framework.utility.search.SpigotSearchEngine;
import io.github.trae.utilities.UtilString;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

public class MaterialSearchEngine extends SpigotSearchEngine<Material> {

    public MaterialSearchEngine() {
        super("Material Search", () -> List.of(Material.values()));
    }

    @Override
    protected String getTypeFormat(final Material material) {
        return UtilColor.serialize(ChatColor.YELLOW.getColor(), UtilString.clean(material.name()));
    }

    @Override
    protected boolean isExact(final Material material, final String result) {
        return material.name().equalsIgnoreCase(result);
    }

    @Override
    protected boolean isMatching(final Material material, final String result) {
        return material.name().toLowerCase(Locale.ROOT).contains(result.toLowerCase(Locale.ROOT));
    }
}