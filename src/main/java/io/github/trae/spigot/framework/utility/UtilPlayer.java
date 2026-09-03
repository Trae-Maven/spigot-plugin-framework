package io.github.trae.spigot.framework.utility;

import io.github.trae.spigot.framework.utility.search.types.PlayerSearchEngine;
import lombok.experimental.UtilityClass;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Utility helpers for resolving online players from user-supplied input.
 *
 * <p>Wraps a shared {@link PlayerSearchEngine} so commands can turn a partial or full name into a
 * {@link Player} without handling the match and feedback logic themselves.</p>
 */
@UtilityClass
public class UtilPlayer {

    /**
     * Search engine backing the {@code searchPlayer} helpers.
     */
    private static final PlayerSearchEngine PLAYER_SEARCH_ENGINE = new PlayerSearchEngine();

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
    public static Optional<Player> searchPlayer(final CommandSender sender, final String input, final boolean inform, final Predicate<Player> predicate) {
        return PLAYER_SEARCH_ENGINE.find(sender, input, inform, predicate);
    }

    /**
     * Searches the online players for the player identified by the given input, without filtering.
     *
     * @param sender the sender to inform of the search outcome
     * @param input  the search input
     * @param inform whether to message the sender when the search fails to resolve
     * @return the resolved player, or {@link Optional#empty()} if the search was empty or ambiguous
     * @see #searchPlayer(CommandSender, String, boolean, Predicate)
     */
    public static Optional<Player> searchPlayer(final CommandSender sender, final String input, final boolean inform) {
        return searchPlayer(sender, input, inform, null);
    }
}