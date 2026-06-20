package io.github.trae.spigot.framework.team.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired to request a team refresh for a player across all viewers.
 * <p>
 * Dispatched via {@link io.github.trae.spigot.framework.utility.UtilEvent} and handled by
 * {@link io.github.trae.spigot.framework.team.AbstractTeamManager}. When the {@link #identifier}
 * is {@code null}, the player's team is re-resolved and pushed to every viewer (or removed if no
 * team is eligible). When an identifier is supplied, the update is only applied for viewers whose
 * eligible team matches that identifier.
 */
@AllArgsConstructor
@Getter
public class TeamUpdateEvent extends CustomEvent {

    /**
     * The identifier of the team to scope this update to, or {@code null} to update the eligible team.
     */
    private final String identifier;

    /**
     * The player whose team should be refreshed across all viewers.
     */
    private final Player player;

    /**
     * Creates an unscoped update event that re-resolves and refreshes the player's team for every
     * viewer.
     *
     * @param player the player whose team should be refreshed
     */
    public TeamUpdateEvent(final Player player) {
        this(null, player);
    }
}