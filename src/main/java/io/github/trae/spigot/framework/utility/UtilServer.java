package io.github.trae.spigot.framework.utility;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

    /**
     * Returns the offline players known to the server, restricted to players who have joined before
     * and optionally filtered by a predicate.
     *
     * <p>Backed by {@link org.bukkit.Server#getOfflinePlayers()} and pre-filtered by
     * {@link OfflinePlayer#hasPlayedBefore()} so the result matches the semantics of the single
     * offline-player lookups.</p>
     *
     * @param predicate the filter to apply; players failing the test are excluded.
     *                  If {@code null}, all known offline players are returned.
     * @return a mutable {@link List} of matching offline players
     */
    public static List<OfflinePlayer> getOfflinePlayers(final Predicate<OfflinePlayer> predicate) {
        final List<OfflinePlayer> playerList = new ArrayList<>(Stream.of(Bukkit.getServer().getOfflinePlayers()).filter(OfflinePlayer::hasPlayedBefore).toList());

        if (predicate != null) {
            playerList.removeIf(predicate.negate());
        }

        return playerList;
    }

    /**
     * Returns all offline players known to the server that have joined before.
     *
     * @return a mutable {@link List} of all known offline players
     */
    public static List<OfflinePlayer> getOfflinePlayers() {
        return getOfflinePlayers(null);
    }

    /**
     * Resolves an online player by their unique id.
     *
     * @param id the player's unique id
     * @return an {@link Optional} containing the player, or empty if none is online with that id
     */
    public static Optional<Player> getOnlinePlayerById(final UUID id) {
        return Optional.ofNullable(Bukkit.getServer().getPlayer(id));
    }

    /**
     * Resolves an online player by their exact username.
     *
     * @param name the player's exact username
     * @return an {@link Optional} containing the player, or empty if none is online with that name
     */
    public static Optional<Player> getOnlinePlayerByName(final String name) {
        return Optional.ofNullable(Bukkit.getServer().getPlayerExact(name));
    }

    /**
     * Resolves an offline player by their unique id, restricted to players who have joined before.
     *
     * <p>{@link org.bukkit.Server#getOfflinePlayer(UUID)} never returns {@code null} and will
     * fabricate an entry for an unknown id, so the result is filtered by
     * {@link OfflinePlayer#hasPlayedBefore()} to exclude ids the server has never seen.</p>
     *
     * @param id the player's unique id
     * @return an {@link Optional} containing the offline player, or empty if they have never joined
     */
    public static Optional<OfflinePlayer> getOfflinePlayerById(final UUID id) {
        return Optional.of(Bukkit.getServer().getOfflinePlayer(id)).filter(OfflinePlayer::hasPlayedBefore);
    }

    /**
     * Resolves an offline player by their username from the server's cache, restricted to players
     * who have joined before.
     *
     * <p>Uses {@link org.bukkit.Server#getOfflinePlayerIfCached(String)}, which performs no blocking
     * lookup and returns {@code null} for an uncached name; the result is additionally filtered by
     * {@link OfflinePlayer#hasPlayedBefore()} so it matches the id-based lookup's semantics.</p>
     *
     * @param name the player's username
     * @return an {@link Optional} containing the offline player, or empty if uncached or never joined
     */
    public static Optional<OfflinePlayer> getOfflinePlayerByName(final String name) {
        return Optional.ofNullable(Bukkit.getServer().getOfflinePlayerIfCached(name)).filter(OfflinePlayer::hasPlayedBefore);
    }
}