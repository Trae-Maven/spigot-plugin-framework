package io.github.trae.spigot.framework.scoreboard.data;

import io.github.trae.spigot.framework.scoreboard.data.interfaces.IBoardBuilder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for constructing scoreboard line layouts.
 *
 * <p>Provides a fluent API for composing scoreboards with bold coloured
 * labels, white values, and blank spacers.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ScoreboardManager.board()
 *     .pair(NamedTextColor.GRAY, "Server", "Lobby-1")
 *     .pair(NamedTextColor.GOLD, "Rank", "Owner")
 *     .pair(NamedTextColor.GREEN, "Gems", "1,500")
 *     .lineCompact(Component.text("play.server.com", NamedTextColor.RED, TextDecoration.BOLD));
 * }</pre>
 */
@Getter
public class BoardBuilder implements IBoardBuilder {

    private final List<Component> lines = new ArrayList<>();

    /**
     * Adds a single component line with no trailing blank spacer.
     *
     * @param line the component to add
     * @return this builder for chaining
     */
    @Override
    public BoardBuilder lineCompact(final Component line) {
        this.getLines().add(line);
        return this;
    }

    /**
     * Adds a single component line followed by a blank spacer.
     *
     * @param line the component to add
     * @return this builder for chaining
     */
    @Override
    public BoardBuilder line(final Component line) {
        this.getLines().add(line);
        this.getLines().add(Component.empty());
        return this;
    }

    /**
     * Adds a label/value pair:
     * bold coloured label, white value, then a blank spacer.
     *
     * <p>Produces three lines internally:</p>
     * <ol>
     *     <li>Bold coloured label</li>
     *     <li>White value</li>
     *     <li>Blank spacer</li>
     * </ol>
     *
     * @param labelColor the colour for the bold label
     * @param label      the label text
     * @param value      the value text (rendered in white)
     * @return this builder for chaining
     */
    @Override
    public BoardBuilder pair(final NamedTextColor labelColor, final String label, final String value) {
        this.getLines().addAll(List.of(
                Component.text(label, labelColor, TextDecoration.BOLD),
                Component.text(value, NamedTextColor.WHITE),
                Component.empty()
        ));
        return this;
    }

    /**
     * Adds a blank spacer line.
     *
     * @return this builder for chaining
     */
    @Override
    public BoardBuilder blank() {
        this.getLines().add(Component.empty());
        return this;
    }

    /**
     * Builds the final line array for use in a {@link BoardEntry}.
     *
     * @return the composed lines as a component array
     */
    @Override
    public Component[] build() {
        return this.getLines().toArray(new Component[0]);
    }
}