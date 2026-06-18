package io.github.trae.spigot.framework.utility;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.function.Predicate;

/**
 * Utility helpers for working with Adventure {@link Component}s.
 *
 * <p>Wraps common {@link Component} construction and joining patterns in shorter, null-tolerant
 * helpers. The {@code join} methods delegate to {@link Component#join}
 * with a configurable separator and inclusion predicate, while the {@code text} helpers are thin
 * conveniences over {@link Component#text} that accept colors and decorations directly. The
 * {@code empty}, {@code space}, and {@code newline} helpers expose the corresponding Adventure
 * constants.</p>
 */
@UtilityClass
public class UtilAdventure {

    /**
     * Joins the given components into a single {@link Component}, optionally inserting a separator
     * between them and filtering which components are included.
     * <p>
     * A {@code null} separator means no separator is inserted; a {@code null} predicate means every
     * component is included. Filtered-out components produce no surrounding separator.
     *
     * @param separator  the separator to place between components, or {@code null} for none
     * @param predicate  the filter deciding which components to include, or {@code null} to include all
     * @param components the components to join
     * @return the joined component
     */
    public static Component join(final ComponentLike separator, final Predicate<ComponentLike> predicate, final Component... components) {
        final JoinConfiguration.Builder builder = JoinConfiguration.builder();

        if (separator != null) {
            builder.separator(separator);
        }

        if (predicate != null) {
            builder.predicate(predicate);
        }

        return Component.join(builder.build(), components);
    }

    /**
     * Joins the given components with a single space between them, skipping empty components so no
     * stray spacing is produced.
     *
     * @param components the components to join
     * @return the joined component
     */
    public static Component join(final Component... components) {
        return join(Component.space(), componentLike -> componentLike != Component.empty(), components);
    }

    /**
     * Creates a text component with the given content, color, and decorations.
     *
     * @param content        the text content
     * @param namedTextColor the color to apply
     * @param textDecoration the decorations to apply (e.g. bold, italic)
     * @return the styled text component
     */
    public static TextComponent text(final String content, final NamedTextColor namedTextColor, final List<TextDecoration> textDecoration) {
        return Component.text(content, namedTextColor, textDecoration.toArray(new TextDecoration[0]));
    }

    /**
     * Creates a text component with the given content and color.
     *
     * @param content        the text content
     * @param namedTextColor the color to apply
     * @return the colored text component
     */
    public static TextComponent text(final String content, final NamedTextColor namedTextColor) {
        return Component.text(content, namedTextColor);
    }

    /**
     * Creates a plain, unstyled text component with the given content.
     *
     * @param content the text content
     * @return the text component
     */
    public static TextComponent text(final String content) {
        return Component.text(content);
    }

    /**
     * Returns the empty component.
     *
     * @return {@link Component#empty()}
     */
    public static TextComponent empty() {
        return Component.empty();
    }

    /**
     * Returns a component consisting of a single space.
     *
     * @return {@link Component#space()}
     */
    public static TextComponent space() {
        return Component.space();
    }

    /**
     * Returns a component consisting of a single newline.
     *
     * @return {@link Component#newline()}
     */
    public static TextComponent newline() {
        return Component.newline();
    }
}