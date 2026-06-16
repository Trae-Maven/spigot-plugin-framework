package io.github.trae.spigot.framework.sidebar.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired to request a sidebar refresh for a player.
 * <p>
 * Dispatched via {@link io.github.trae.spigot.framework.utility.UtilEvent} and handled by
 * {@link io.github.trae.spigot.framework.sidebar.AbstractSidebarManager}. When the
 * {@link #identifier} is {@code null}, the player's active sidebar is re-resolved and refreshed.
 * When an identifier is supplied, the update is only applied if the player's active sidebar
 * matches that identifier.
 */
@AllArgsConstructor
@Getter
public class SidebarUpdateEvent extends CustomEvent {

    /**
     * The identifier of the sidebar to scope this update to, or {@code null} to update the active sidebar.
     */
    private final String identifier;

    /**
     * The player whose sidebar should be refreshed.
     */
    private final Player player;

    /**
     * Creates an unscoped update event that refreshes whatever sidebar is currently active for
     * the player.
     *
     * @param player the player whose sidebar should be refreshed
     */
    public SidebarUpdateEvent(final Player player) {
        this(null, player);
    }
}