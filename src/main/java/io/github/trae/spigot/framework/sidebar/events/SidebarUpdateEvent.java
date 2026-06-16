package io.github.trae.spigot.framework.sidebar.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

@AllArgsConstructor
@Getter
public class SidebarUpdateEvent extends CustomEvent {

    private final String identifier;
    private final Player player;

    public SidebarUpdateEvent(final Player player) {
        this(null, player);
    }
}