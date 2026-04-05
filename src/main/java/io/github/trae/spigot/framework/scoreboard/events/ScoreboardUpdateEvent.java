package io.github.trae.spigot.framework.scoreboard.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

@AllArgsConstructor
@Getter
public class ScoreboardUpdateEvent extends CustomEvent {

    private final Player player;
}