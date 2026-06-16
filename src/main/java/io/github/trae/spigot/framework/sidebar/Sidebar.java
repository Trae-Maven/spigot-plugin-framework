package io.github.trae.spigot.framework.sidebar;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Represents a sidebar (scoreboard objective in the {@code SIDEBAR} display slot) that can be
 * shown to a player.
 * <p>
 * Implementations are discovered automatically by {@link AbstractSidebarManager} via the
 * dependency injector. When multiple sidebars are eligible for a player, the one with the
 * lowest {@link #getPriority()} is displayed. Both {@link #canDisplay()} (global) and
 * {@link #canDisplay(Player)} (per-player) must return {@code true} for a sidebar to be eligible.
 */
public interface Sidebar {

    /**
     * Returns the unique identifier for this sidebar, used as the scoreboard objective name
     * and for matching scoped {@link io.github.trae.spigot.framework.sidebar.events.SidebarUpdateEvent}s.
     *
     * @return the sidebar identifier
     */
    String getIdentifier();

    /**
     * Returns the priority of this sidebar. Lower values win — the eligible sidebar with the
     * lowest priority is the one displayed.
     *
     * @return the sidebar priority
     */
    int getPriority();

    /**
     * Returns whether this sidebar is allowed to display globally, irrespective of any specific
     * player (e.g. gated behind a world event or server state). Defaults to {@code true}.
     *
     * @return {@code true} if the sidebar may display globally
     */
    default boolean canDisplay() {
        return true;
    }

    /**
     * Returns whether this sidebar is allowed to display for the given player (e.g. gated behind
     * faction membership or rank). Defaults to {@code true}.
     *
     * @param player the player to check
     * @return {@code true} if the sidebar may display for the player
     */
    default boolean canDisplay(final Player player) {
        return true;
    }

    /**
     * Returns whether this sidebar's title is static. When {@code false}, the manager's scheduler
     * re-resolves the title each tick and pushes an update if it changed, enabling animated titles.
     * Defaults to {@code true}.
     *
     * @return {@code true} if the title never changes after creation
     */
    default boolean isStaticTitle() {
        return true;
    }

    /**
     * Returns the title component shown at the top of the sidebar for the given player.
     *
     * @param player the player the sidebar is rendered for
     * @return the title component
     */
    Component getTitle(final Player player);

    /**
     * Returns the line components rendered top-to-bottom for the given player. The manager diffs
     * these against the previously rendered lines and only sends packets for changes.
     *
     * @param player the player the sidebar is rendered for
     * @return the ordered list of line components
     */
    List<Component> getLines(final Player player);
}