package io.github.trae.spigot.framework.utility.search.types;

import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.spigot.framework.utility.search.SpigotSearchEngine;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Locale;

/**
 * Search engine resolving the worlds currently loaded on the server.
 *
 * <p>Candidates are read live from {@link org.bukkit.Server#getWorlds()} on every search, so worlds
 * loaded or unloaded since the last call are reflected, and matched on world name: case-insensitive
 * equality for an exact hit, case-insensitive substring for a partial one.</p>
 */
public class WorldSearchEngine extends SpigotSearchEngine<World> {

    /**
     * Creates a search engine over the loaded worlds.
     */
    public WorldSearchEngine() {
        super("World Search", () -> Bukkit.getServer().getWorlds());
    }

    /**
     * {@inheritDoc}
     *
     * @return the world name serialized in yellow
     */
    @Override
    protected String getTypeFormat(final World world) {
        return UtilColor.serialize(ChatColor.YELLOW.getColor(), world.getName());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Compares the world name to the input, ignoring case.
     */
    @Override
    protected boolean isExact(final World world, final String result) {
        return world.getName().equalsIgnoreCase(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Tests whether the world name contains the input, ignoring case.
     */
    @Override
    protected boolean isMatching(final World world, final String result) {
        return world.getName().toLowerCase(Locale.ROOT).contains(result.toLowerCase(Locale.ROOT));
    }
}