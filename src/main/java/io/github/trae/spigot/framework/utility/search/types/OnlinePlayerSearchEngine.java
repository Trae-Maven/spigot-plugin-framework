package io.github.trae.spigot.framework.utility.search.types;

import io.github.trae.spigot.framework.utility.UtilServer;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.spigot.framework.utility.search.SpigotSearchEngine;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.util.Locale;

/**
 * Search engine resolving currently online players.
 *
 * <p>Candidates are read live from {@link UtilServer#getOnlinePlayers()} on every search, and matched
 * on username: case-insensitive equality for an exact hit, case-insensitive substring for a partial
 * one.</p>
 */
public class OnlinePlayerSearchEngine extends SpigotSearchEngine<Player> {

    /**
     * Creates a search engine over the online player set.
     */
    public OnlinePlayerSearchEngine() {
        super("Online Player Search", UtilServer::getOnlinePlayers);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the player's username.
     */
    @Override
    protected String getTypeName(final Player player, final CommandSender commandSender) {
        return player.getName();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Every player is displayed in yellow.
     */
    @Override
    protected Color getTypeColor(final Player player, final CommandSender commandSender) {
        return ChatColor.YELLOW.getColor();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Compares the player's username to the input, ignoring case.
     */
    @Override
    protected boolean isExact(final Player player, final String result) {
        return player.getName().equalsIgnoreCase(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Tests whether the player's username contains the input, ignoring case.
     */
    @Override
    protected boolean isMatching(final Player player, final String result) {
        return player.getName().toLowerCase(Locale.ROOT).contains(result.toLowerCase(Locale.ROOT));
    }
}