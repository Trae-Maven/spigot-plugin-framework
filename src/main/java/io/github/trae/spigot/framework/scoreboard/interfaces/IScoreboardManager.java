package io.github.trae.spigot.framework.scoreboard.interfaces;

import io.github.trae.spigot.framework.scoreboard.data.BoardBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface IScoreboardManager {

    void set(final Player player, final String key, final int priority, final Component title, final BoardBuilder boardBuilder);

    void remove(final Player player, final String key);

    void remove(final Player player);

    boolean isActive(final Player player, final String key);

    void cleanup(final UUID uuid);
}