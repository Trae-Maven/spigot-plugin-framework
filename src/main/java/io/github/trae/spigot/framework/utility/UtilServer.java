package io.github.trae.spigot.framework.utility;

import io.github.trae.spigot.framework.utility.search.types.OfflinePlayerSearchEngine;
import io.github.trae.spigot.framework.utility.search.types.OnlinePlayerSearchEngine;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Utility methods for querying the state of the server.
 *
 * <p>Covers online and offline player lookups by unique id, exact name, and user-supplied search
 * input, plus filtered snapshots of the online and offline player sets. Offline lookups are
 * consistently restricted to players who have joined before, so a never-seen name or id resolves
 * to an empty result rather than a fabricated {@link OfflinePlayer}.</p>
 */
@UtilityClass
public class UtilServer {

    /**
     * Search engine backing the {@code searchPlayer} helpers.
     */
    private static final OnlinePlayerSearchEngine ONLINE_PLAYER_SEARCH_ENGINE = new OnlinePlayerSearchEngine();

    /**
     * Search engine backing the {@code searchOfflinePlayer} helpers.
     */
    private static final OfflinePlayerSearchEngine OFFLINE_PLAYER_SEARCH_ENGINE = new OfflinePlayerSearchEngine();

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
     * Searches the online players for the player identified by the given input.
     *
     * <p>An exact name match wins immediately, otherwise a single partial match is returned. An empty
     * or ambiguous search yields an empty result and, when informing is enabled, messages the sender
     * with the outcome.</p>
     *
     * @param sender    the sender to inform of the search outcome
     * @param input     the search input
     * @param inform    whether to message the sender when the search fails to resolve
     * @param predicate an optional filter applied before matching, or null to consider every player
     * @return the resolved player, or {@link Optional#empty()} if the search was empty or ambiguous
     */
    public static Optional<Player> searchOnlinePlayer(final CommandSender sender, final String input, final boolean inform, final Predicate<Player> predicate) {
        return ONLINE_PLAYER_SEARCH_ENGINE.find(sender, input, inform, predicate);
    }

    /**
     * Searches the online players for the player identified by the given input, without filtering.
     *
     * @param sender the sender to inform of the search outcome
     * @param input  the search input
     * @param inform whether to message the sender when the search fails to resolve
     * @return the resolved player, or {@link Optional#empty()} if the search was empty or ambiguous
     * @see #searchOnlinePlayer(CommandSender, String, boolean, Predicate)
     */
    public static Optional<Player> searchOnlinePlayer(final CommandSender sender, final String input, final boolean inform) {
        return searchOnlinePlayer(sender, input, inform, null);
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

    /**
     * Searches the known offline players for the player identified by the given input.
     *
     * <p>An exact name match wins immediately, otherwise a single partial match is returned. An empty
     * or ambiguous search yields an empty result and, when informing is enabled, messages the sender
     * with the outcome.</p>
     *
     * @param sender    the sender to inform of the search outcome
     * @param input     the search input
     * @param inform    whether to message the sender when the search fails to resolve
     * @param predicate an optional filter applied before matching, or null to consider every player
     * @return the resolved offline player, or {@link Optional#empty()} if the search was empty or
     * ambiguous
     */
    public static Optional<OfflinePlayer> searchOfflinePlayer(final CommandSender sender, final String input, final boolean inform, final Predicate<OfflinePlayer> predicate) {
        return OFFLINE_PLAYER_SEARCH_ENGINE.find(sender, input, inform, predicate);
    }

    /**
     * Searches the known offline players for the player identified by the given input, without
     * filtering.
     *
     * @param sender the sender to inform of the search outcome
     * @param input  the search input
     * @param inform whether to message the sender when the search fails to resolve
     * @return the resolved offline player, or {@link Optional#empty()} if the search was empty or
     * ambiguous
     * @see #searchOfflinePlayer(CommandSender, String, boolean, Predicate)
     */
    public static Optional<OfflinePlayer> searchOfflinePlayer(final CommandSender sender, final String input, final boolean inform) {
        return searchOfflinePlayer(sender, input, inform, null);
    }
}