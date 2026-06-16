package io.github.trae.spigot.framework.team;

import net.kyori.adventure.text.Component;
import net.minecraft.ChatFormatting;
import org.bukkit.entity.Player;

public interface Team {

    String getIdentifier();

    int getPriority();

    default boolean canDisplay() {
        return true;
    }

    default boolean canDisplay(final Player player, final Player viewer) {
        return true;
    }

    default Component getDisplayName(final Player player, final Player viewer) {
        return null;
    }

    default Component getPrefix(final Player player, final Player viewer) {
        return null;
    }

    default Component getSuffix(final Player player, final Player viewer) {
        return null;
    }

    default Boolean allowFriendlyFire(final Player player, final Player viewer) {
        return null;
    }

    default Boolean seeFriendlyInvisibles(final Player player, final Player viewer) {
        return null;
    }

    default net.minecraft.world.scores.Team.Visibility getNameTagVisibility(final Player player, final Player viewer) {
        return null;
    }

    default net.minecraft.world.scores.Team.Visibility getDeathMessageVisibility(final Player player, final Player viewer) {
        return null;
    }

    default net.minecraft.world.scores.Team.CollisionRule getCollisionRule(final Player player, final Player viewer) {
        return null;
    }

    default ChatFormatting getColor(final Player player, final Player viewer) {
        return null;
    }
}