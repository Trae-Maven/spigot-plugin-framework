package io.github.trae.spigot.framework.utility;

import io.github.trae.spigot.framework.utility.enums.ChatColor;
import lombok.experimental.UtilityClass;

import java.awt.*;

/**
 * Utility class for color-related operations in chat formatting.
 */
@UtilityClass
public class UtilColor {

    /**
     * Wraps a string in Adventure MiniMessage color tags.
     *
     * <p>If the color matches a known {@link ChatColor} name, the named tag is used
     * (e.g. {@code <red>text</red>}). Otherwise, falls back to a hex color tag
     * (e.g. {@code <#ff00aa>text</#ff00aa>}).</p>
     *
     * @param color  the color to apply
     * @param string the text to wrap
     * @return the text wrapped in opening and closing MiniMessage color tags
     */
    public static String serialize(final Color color, final String string) {
        if (color == null) {
            return string;
        }

        final String colorTag;

        final String chatColorName = ChatColor.getNameByRgb(color.getRGB());

        if (chatColorName != null) {
            colorTag = chatColorName.toLowerCase();
        } else {
            colorTag = "#%06x".formatted(color.getRGB() & 0xFFFFFF);
        }

        return "<%s>%s</%s>".formatted(colorTag, string, colorTag);
    }
}