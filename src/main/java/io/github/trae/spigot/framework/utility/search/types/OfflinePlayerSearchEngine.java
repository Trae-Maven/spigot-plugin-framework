package io.github.trae.spigot.framework.utility.search.types;

import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.UtilServer;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.spigot.framework.utility.search.SpigotSearchEngine;
import io.github.trae.utilities.UtilString;
import org.bukkit.OfflinePlayer;

import java.util.Locale;
import java.util.Objects;

/**
 * Search engine resolving offline players known to the server.
 *
 * <p>Candidates are read live from {@link UtilServer#getOfflinePlayers()} on every search and
 * pre-filtered to those with a cached username, since {@link OfflinePlayer#getName()} is nullable
 * for a profile the server has only ever seen by unique id. Matching then runs on that username:
 * case-insensitive equality for an exact hit, case-insensitive substring for a partial one.</p>
 */
public class OfflinePlayerSearchEngine extends SpigotSearchEngine<OfflinePlayer> {

    /**
     * Creates a search engine over the known offline players that have a cached username.
     */
    public OfflinePlayerSearchEngine() {
        super("Offline Player Search", () -> UtilServer.getOfflinePlayers().stream().filter(offlinePlayer -> !UtilString.isEmpty(offlinePlayer.getName())).toList());
    }

    /**
     * {@inheritDoc}
     *
     * @return the player's username serialized in yellow
     */
    @Override
    protected String getTypeFormat(final OfflinePlayer offlinePlayer) {
        return UtilColor.serialize(ChatColor.YELLOW.getColor(), offlinePlayer.getName());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Compares the player's username to the input, ignoring case. The name is non-null because the
     * supplier filters out unnamed profiles.
     */
    @Override
    protected boolean isExact(final OfflinePlayer offlinePlayer, final String result) {
        return Objects.requireNonNull(offlinePlayer.getName()).equalsIgnoreCase(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Tests whether the player's username contains the input, ignoring case. The name is non-null
     * because the supplier filters out unnamed profiles.
     */
    @Override
    protected boolean isMatching(final OfflinePlayer offlinePlayer, final String result) {
        return Objects.requireNonNull(offlinePlayer.getName()).toLowerCase(Locale.ROOT).contains(result.toLowerCase(Locale.ROOT));
    }
}