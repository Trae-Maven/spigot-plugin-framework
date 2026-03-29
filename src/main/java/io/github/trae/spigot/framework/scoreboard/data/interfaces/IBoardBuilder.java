package io.github.trae.spigot.framework.scoreboard.data.interfaces;

import io.github.trae.spigot.framework.scoreboard.data.BoardBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public interface IBoardBuilder {

    BoardBuilder lineCompact(final Component line);

    BoardBuilder line(final Component line);

    BoardBuilder pair(final NamedTextColor labelColor, final String label, final String value);

    BoardBuilder blank();

    Component[] build();
}