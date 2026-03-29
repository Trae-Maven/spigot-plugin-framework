package io.github.trae.spigot.framework.scoreboard.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * Represents a single scoreboard registration from a system.
 *
 * <p>Each entry has a priority, title, and line content. When multiple
 * entries exist for the same player, the one with the highest priority
 * is rendered. If the highest priority entry is removed, the next
 * highest takes over automatically.</p>
 */
@AllArgsConstructor
@Getter
public class BoardEntry {

    /**
     * The priority of this board entry. Higher values take precedence.
     */
    private final int priority;

    /**
     * The scoreboard title displayed at the top of the sidebar.
     */
    private final Component title;

    /**
     * The composed lines to display on the sidebar.
     */
    private final Component[] lines;
}