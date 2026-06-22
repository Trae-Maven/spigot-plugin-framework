package io.github.trae.spigot.framework.utility;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utility methods for querying the state of the server.
 */
@UtilityClass
public class UtilServer {

    /**
     * Returns the currently online players, optionally filtered by a predicate.
     *
     * @param predicate the filter to apply; players failing the test are excluded.
     *                  If {@code null}, all online players are returned.
     * @return a mutable {@link List} of matching online players
     */
    public static List<Player> getOnlinePlayers(final Predicate<Player> predicate) {
        final List<Player> playerList = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());

        if (predicate != null) {
            playerList.removeIf(predicate.negate());
        }

        return playerList;
    }

    /**
     * Returns all currently online players.
     *
     * @return a mutable {@link List} of all online players
     */
    public static List<Player> getOnlinePlayers() {
        return getOnlinePlayers(null);
    }
}