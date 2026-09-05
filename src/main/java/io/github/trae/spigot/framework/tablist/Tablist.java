package io.github.trae.spigot.framework.tablist;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Represents the header and footer text displayed above and below the player list.
 * <p>
 * Subclasses are discovered automatically by {@link TablistManager} via the dependency injector.
 * When multiple tablists are eligible for a player, the one with the lowest {@link #priority}
 * is displayed. Both {@link #canDisplay()} (global) and {@link #canDisplay(Player)} (per-player)
 * must return {@code true} for a tablist to be eligible.
 */
@AllArgsConstructor
@Getter
public abstract class Tablist {

    /**
     * The priority of this tablist. Lower values win: the eligible tablist with the lowest
     * priority is the one displayed.
     */
    private final int priority;

    /**
     * Returns whether this tablist is allowed to display globally, irrespective of any specific
     * player (e.g. gated behind a world event or server state). Defaults to {@code true}.
     *
     * @return {@code true} if the tablist may display globally
     */
    protected boolean canDisplay() {
        return true;
    }

    /**
     * Returns whether this tablist is allowed to display for the given player (e.g. gated behind
     * faction membership or rank). Defaults to {@code true}.
     *
     * @param player the player to check
     * @return {@code true} if the tablist may display for the player
     */
    protected boolean canDisplay(final Player player) {
        return true;
    }

    /**
     * Returns the header component rendered above the player list for the given player.
     *
     * @param player the player the tablist is rendered for
     * @return the header component
     */
    protected abstract Component getHeader(final Player player);

    /**
     * Returns the footer component rendered below the player list for the given player.
     *
     * @param player the player the tablist is rendered for
     * @return the footer component
     */
    protected abstract Component getFooter(final Player player);
}