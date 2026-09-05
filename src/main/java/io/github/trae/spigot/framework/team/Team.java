package io.github.trae.spigot.framework.team;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.minecraft.ChatFormatting;
import org.bukkit.entity.Player;

/**
 * Represents the nametag decoration applied to a player as seen by a specific viewer.
 * <p>
 * Subclasses are discovered automatically by {@link TeamManager} via the dependency injector.
 * Resolution happens per player/viewer pair, so the same target player can present different
 * decorations to different viewers. When multiple teams are eligible for a pair, the one with the
 * lowest {@link #priority} is applied. Both {@link #canDisplay()} (global) and
 * {@link #canDisplay(Player, Player)} (per-pair) must return {@code true} for a team to be eligible.
 * <p>
 * Every option hook returns {@code null} by default, which leaves the corresponding
 * {@link net.minecraft.world.scores.PlayerTeam} value at its default rather than overriding it.
 */
@AllArgsConstructor
@Getter
public abstract class Team {

    /**
     * The unique identifier for this team, used for matching scoped
     * {@link io.github.trae.spigot.framework.team.events.TeamUpdateEvent}s.
     */
    private final String identifier;

    /**
     * The priority of this team. Lower values win: the eligible team with the lowest priority is
     * the one applied.
     */
    private final int priority;

    /**
     * Returns whether this team is allowed to apply globally, irrespective of any specific pair
     * (e.g. gated behind a world event or server state). Defaults to {@code true}.
     *
     * @return {@code true} if the team may apply globally
     */
    protected boolean canDisplay() {
        return true;
    }

    /**
     * Returns whether this team is allowed to apply for the given player/viewer pair (e.g. gated
     * behind a relation between the two). Defaults to {@code true}.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return {@code true} if the team may apply for the pair
     */
    protected boolean canDisplay(final Player player, final Player viewer) {
        return true;
    }

    /**
     * Returns the team display name, or {@code null} to leave it at the default.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the display name component, or {@code null}
     */
    protected Component getDisplayName(final Player player, final Player viewer) {
        return null;
    }

    /**
     * Returns the nametag prefix shown before the player's name, or {@code null} to leave it at
     * the default. This is the primary hook for relation-aware coloring.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the prefix component, or {@code null}
     */
    protected Component getPrefix(final Player player, final Player viewer) {
        return null;
    }

    /**
     * Returns the nametag suffix shown after the player's name, or {@code null} to leave it at
     * the default.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the suffix component, or {@code null}
     */
    protected Component getSuffix(final Player player, final Player viewer) {
        return null;
    }

    /**
     * Returns whether friendly fire is allowed within this team, or {@code null} to leave it at
     * the default.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the friendly-fire flag, or {@code null}
     */
    protected Boolean allowFriendlyFire(final Player player, final Player viewer) {
        return null;
    }

    /**
     * Returns whether team members can see friendly invisibles, or {@code null} to leave it at
     * the default.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the see-friendly-invisibles flag, or {@code null}
     */
    protected Boolean seeFriendlyInvisibles(final Player player, final Player viewer) {
        return null;
    }

    /**
     * Returns the nametag visibility rule, or {@code null} to leave it at the default.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the nametag visibility, or {@code null}
     */
    protected net.minecraft.world.scores.Team.Visibility getNameTagVisibility(final Player player, final Player viewer) {
        return null;
    }

    /**
     * Returns the death message visibility rule, or {@code null} to leave it at the default.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the death message visibility, or {@code null}
     */
    protected net.minecraft.world.scores.Team.Visibility getDeathMessageVisibility(final Player player, final Player viewer) {
        return null;
    }

    /**
     * Returns the collision rule, or {@code null} to leave it at the default.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the collision rule, or {@code null}
     */
    protected net.minecraft.world.scores.Team.CollisionRule getCollisionRule(final Player player, final Player viewer) {
        return null;
    }

    /**
     * Returns the team color, which also determines the nametag name color, or {@code null} to
     * leave it at the default.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the team color, or {@code null}
     */
    protected ChatFormatting getColor(final Player player, final Player viewer) {
        return null;
    }
}