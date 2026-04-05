package io.github.trae.spigot.framework.scoreboard.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class ScoreboardCleanupEvent extends CustomEvent {

    private final UUID uuid;
}