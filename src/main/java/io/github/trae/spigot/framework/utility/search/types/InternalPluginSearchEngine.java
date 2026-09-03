package io.github.trae.spigot.framework.utility.search.types;

import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.UtilPlugin;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.spigot.framework.utility.search.SpigotSearchEngine;

import java.util.Locale;

/**
 * Search engine resolving framework plugins registered with {@link UtilPlugin}.
 *
 * <p>Candidates are read live from the internal plugin registry on every search, and matched on
 * plugin name: case-insensitive equality for an exact hit, case-insensitive substring for a partial
 * one.</p>
 */
public class InternalPluginSearchEngine extends SpigotSearchEngine<SpigotPlugin> {

    /**
     * Creates a search engine over the internal plugin registry.
     */
    public InternalPluginSearchEngine() {
        super("Internal Plugin Search", () -> UtilPlugin.getInternalPluginMap().values());
    }

    /**
     * {@inheritDoc}
     *
     * @return the plugin name serialized in yellow
     */
    @Override
    protected String getTypeFormat(final SpigotPlugin spigotPlugin) {
        return UtilColor.serialize(ChatColor.YELLOW.getColor(), spigotPlugin.getPluginName());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Compares the plugin name to the input, ignoring case.
     */
    @Override
    protected boolean isExact(final SpigotPlugin spigotPlugin, final String result) {
        return spigotPlugin.getPluginName().equalsIgnoreCase(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Tests whether the plugin name contains the input, ignoring case.
     */
    @Override
    protected boolean isMatching(final SpigotPlugin spigotPlugin, final String result) {
        return spigotPlugin.getPluginName().toLowerCase(Locale.ROOT).contains(result.toLowerCase(Locale.ROOT));
    }
}