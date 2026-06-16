package io.github.trae.spigot.framework.team.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

@AllArgsConstructor
@Getter
public class TeamUpdateEvent {

    private final String identifier;
    private final Player player;

    public TeamUpdateEvent(final Player player) {
        this(null, player);
    }
}