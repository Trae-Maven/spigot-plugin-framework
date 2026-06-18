package io.github.trae.spigot.framework.utility.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard chat color palette mirroring Minecraft's legacy chat colors, matching Adventure
 * MiniMessage's named colors. Each entry maps to an RGB {@link Color} value, enabling color
 * lookups by name or RGB.
 */
@AllArgsConstructor
@Getter
public enum ChatColor {

    BLACK(new Color(0x000000)),
    DARK_BLUE(new Color(0x0000AA)),
    DARK_GREEN(new Color(0x00AA00)),
    DARK_AQUA(new Color(0x00AAAA)),
    DARK_RED(new Color(0xAA0000)),
    DARK_PURPLE(new Color(0xAA00AA)),
    GOLD(new Color(0xFFAA00)),
    GRAY(new Color(0xAAAAAA)),
    DARK_GRAY(new Color(0x555555)),
    BLUE(new Color(0x5555FF)),
    GREEN(new Color(0x55FF55)),
    AQUA(new Color(0x55FFFF)),
    RED(new Color(0xFF5555)),
    LIGHT_PURPLE(new Color(0xFF55FF)),
    YELLOW(new Color(0xFFFF55)),
    WHITE(new Color(0xFFFFFF));

    private static final Map<Integer, String> RGB_TO_NAME = new HashMap<>();

    private final Color color;

    static {
        for (final ChatColor chatColor : values()) {
            RGB_TO_NAME.put(chatColor.getColor().getRGB(), chatColor.name());
        }
    }

    /**
     * Resolves a chat color name from a packed RGB integer.
     *
     * @param rgb the packed RGB value (as returned by {@link Color#getRGB()})
     * @return the enum name if a match exists, or {@code null} if no match is found
     */
    public static String getNameByRgb(final int rgb) {
        return RGB_TO_NAME.get(rgb);
    }
}