package io.github.trae.spigot.framework.item.styles;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;

import java.awt.Color;
import java.util.List;

/**
 * Describes the shared visual style of an item.
 * <p>
 * A style groups the name, color, tooltip style, and tag used when rendering an
 * {@link io.github.trae.spigot.framework.item.Item}. Instances are immutable and created through
 * {@link #of(String, Color, NamespacedKey, String)}.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class ItemStyle {

    /**
     * The name of this style.
     */
    private final String name;

    /**
     * The color applied to item text using this style.
     */
    private final Color color;

    private final List<TextDecoration> decorations;

    /**
     * The tooltip style applied to items using this style.
     */
    private final NamespacedKey tooltipStyle;

    /**
     * The tag appended to item lore using this style.
     */
    private final String tag;

    /**
     * Creates an item style from the given presentation properties.
     *
     * @param name         the name of the style
     * @param color        the color applied to item text
     * @param tooltipStyle the tooltip style applied to the item
     * @param tag          the tag appended to item lore
     * @return the created item style
     */
    public static ItemStyle of(final String name, final Color color, final List<TextDecoration> decorations, final NamespacedKey tooltipStyle, final String tag) {
        return new ItemStyle(name, color, decorations, tooltipStyle, tag);
    }

    public static ItemStyle of(final String name, final Color color, final NamespacedKey tooltipStyle, final String tag) {
        return of(name, color, null, tooltipStyle, tag);
    }
}