package io.github.trae.spigot.framework.team;

import net.kyori.adventure.text.Component;
import net.minecraft.ChatFormatting;
import org.bukkit.entity.Player;

/**
 * Represents a team applied to a player and rendered individually for each viewer, used to drive
 * nametag prefixes, suffixes, and other scoreboard-team options.
 * <p>
 * Implementations are discovered automatically by {@link AbstractTeamManager} via the dependency
 * injector. When multiple teams are eligible for a player/viewer pair, the one with the lowest
 * {@link #getPriority()} is applied. Both {@link #canDisplay()} (global) and
 * {@link #canDisplay(Player, Player)} (per-pair) must return {@code true} for a team to be eligible.
 * <p>
 * Because resolution is per-pair, the same target player can present different prefixes/suffixes
 * to different viewers — for example showing relation-based coloring (ally, enemy, neutral).
 * <p>
 * All option accessors return {@code null} by default, in which case the corresponding option is
 * left at its {@link net.minecraft.world.scores.PlayerTeam} default rather than being overridden.
 */
public interface Team {

    /**
     * Returns the unique identifier for this team, used for matching scoped
     * {@link io.github.trae.spigot.framework.team.events.TeamUpdateEvent}s.
     *
     * @return the team identifier
     */
    String getIdentifier();

    /**
     * Returns the priority of this team. Lower values win — the eligible team with the lowest
     * priority is the one applied.
     *
     * @return the team priority
     */
    int getPriority();

    /**
     * Returns whether this team is allowed to apply globally, irrespective of any specific
     * player/viewer pair (e.g. gated behind a world event or server state). Defaults to {@code true}.
     *
     * @return {@code true} if the team may apply globally
     */
    default boolean canDisplay() {
        return true;
    }

    /**
     * Returns whether this team is allowed to apply for the given player as seen by the given
     * viewer (e.g. gated behind faction membership). Defaults to {@code true}.
     *
     * @param player the target player the team is applied to
     * @param viewer the viewer the team is rendered for
     * @return {@code true} if the team may apply for this pair
     */
    default boolean canDisplay(final Player player, final Player viewer) {
        return true;
    }

    /**
     * Returns the team display name, or {@code null} to leave it at the default.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the display name component, or {@code null}
     */
    default Component getDisplayName(final Player player, final Player viewer) {
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
    default Component getPrefix(final Player player, final Player viewer) {
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
    default Component getSuffix(final Player player, final Player viewer) {
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
    default Boolean allowFriendlyFire(final Player player, final Player viewer) {
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
    default Boolean seeFriendlyInvisibles(final Player player, final Player viewer) {
        return null;
    }

    /**
     * Returns the nametag visibility rule, or {@code null} to leave it at the default.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the nametag visibility, or {@code null}
     */
    default net.minecraft.world.scores.Team.Visibility getNameTagVisibility(final Player player, final Player viewer) {
        return null;
    }

    /**
     * Returns the death message visibility rule, or {@code null} to leave it at the default.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the death message visibility, or {@code null}
     */
    default net.minecraft.world.scores.Team.Visibility getDeathMessageVisibility(final Player player, final Player viewer) {
        return null;
    }

    /**
     * Returns the collision rule, or {@code null} to leave it at the default.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the collision rule, or {@code null}
     */
    default net.minecraft.world.scores.Team.CollisionRule getCollisionRule(final Player player, final Player viewer) {
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
    default ChatFormatting getColor(final Player player, final Player viewer) {
        return null;
    }
}