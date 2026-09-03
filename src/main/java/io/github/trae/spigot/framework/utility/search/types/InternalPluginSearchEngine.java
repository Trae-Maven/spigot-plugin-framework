package io.github.trae.spigot.framework.utility.search.types;

import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.UtilPlugin;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.spigot.framework.utility.search.SpigotSearchEngine;

import java.util.Locale;

public class InternalPluginSearchEngine extends SpigotSearchEngine<SpigotPlugin> {

    public InternalPluginSearchEngine() {
        super("Internal Plugin Search", () -> UtilPlugin.getInternalPluginMap().values());
    }

    @Override
    protected String getTypeFormat(final SpigotPlugin spigotPlugin) {
        return UtilColor.serialize(ChatColor.YELLOW.getColor(), spigotPlugin.getPluginName());
    }

    @Override
    protected boolean isExact(final SpigotPlugin spigotPlugin, final String result) {
        return spigotPlugin.getPluginName().equalsIgnoreCase(result);
    }

    @Override
    protected boolean isMatching(final SpigotPlugin spigotPlugin, final String result) {
        return spigotPlugin.getPluginName().toLowerCase(Locale.ROOT).contains(result.toLowerCase(Locale.ROOT));
    }
}