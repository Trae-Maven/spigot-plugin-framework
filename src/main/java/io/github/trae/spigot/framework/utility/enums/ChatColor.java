package io.github.trae.spigot.framework.utility.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard chat color palette mirroring Minecraft's legacy chat colors, matching Adventure
 * MiniMessage's named colors. Each entry maps to a legacy code character and an RGB {@link Color}
 * value, enabling color lookups by name or RGB and translation of legacy codes into MiniMessage
 * tags.
 */
@AllArgsConstructor
@Getter
public enum ChatColor {

    BLACK('0', new Color(0x000000)),
    DARK_BLUE('1', new Color(0x0000AA)),
    DARK_GREEN('2', new Color(0x00AA00)),
    DARK_AQUA('3', new Color(0x00AAAA)),
    DARK_RED('4', new Color(0xAA0000)),
    DARK_PURPLE('5', new Color(0xAA00AA)),
    GOLD('6', new Color(0xFFAA00)),
    GRAY('7', new Color(0xAAAAAA)),
    DARK_GRAY('8', new Color(0x555555)),
    BLUE('9', new Color(0x5555FF)),
    GREEN('a', new Color(0x55FF55)),
    AQUA('b', new Color(0x55FFFF)),
    RED('c', new Color(0xFF5555)),
    LIGHT_PURPLE('d', new Color(0xFF55FF)),
    YELLOW('e', new Color(0xFFFF55)),
    WHITE('f', new Color(0xFFFFFF));

    private static final char SECTION_CHARACTER = '\u00A7';
    private static final char AMPERSAND_CHARACTER = '&';

    private static final Map<Integer, String> RGB_TO_NAME = new HashMap<>();
    private static final Map<Character, String> LEGACY_TO_TAG = new HashMap<>();

    private final char code;
    private final Color color;

    static {
        for (final ChatColor chatColor : values()) {
            RGB_TO_NAME.put(chatColor.getColor().getRGB(), chatColor.name());
            LEGACY_TO_TAG.put(chatColor.getCode(), chatColor.name().toLowerCase());
        }

        for (final ChatFormat chatFormat : ChatFormat.values()) {
            LEGACY_TO_TAG.put(chatFormat.getCode(), chatFormat.getTag());
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

    /**
     * Translates legacy section or ampersand color and format codes in the given text into their
     * MiniMessage tag equivalents, leaving unrecognised codes and the rest of the text untouched.
     * <p>
     * Codes are matched case-insensitively, so {@code &L} and {@code &l} both yield
     * {@code <bold>}. As with legacy codes, the resulting tags are not closed.
     *
     * @param text the text containing legacy codes
     * @return the text with recognised codes replaced by MiniMessage tags
     */
    public static String translate(final String text) {
        final StringBuilder builder = new StringBuilder(text.length());

        for (int index = 0; index < text.length(); index++) {
            final char character = text.charAt(index);
            final char code = index + 1 < text.length() ? Character.toLowerCase(text.charAt(index + 1)) : 0;
            final String tag = (character == AMPERSAND_CHARACTER || character == SECTION_CHARACTER) ? LEGACY_TO_TAG.get(code) : null;

            if (tag == null) {
                builder.append(character);
                continue;
            }

            builder.append('<').append(tag).append('>');
            index++;
        }

        return builder.toString();
    }

    /**
     * Legacy chat format (decoration) codes and their MiniMessage tag equivalents. Separate from
     * {@link ChatColor} because these carry no RGB value, unlike the colors themselves.
     */
    @AllArgsConstructor
    @Getter
    public enum ChatFormat {

        MAGIC('k', "obfuscated"),
        BOLD('l', "bold"),
        STRIKETHROUGH('m', "strikethrough"),
        UNDERLINE('n', "underlined"),
        ITALIC('o', "italic"),
        RESET('r', "reset");

        /**
         * The legacy code character following the section or ampersand prefix.
         */
        private final char code;

        /**
         * The MiniMessage tag name this code maps to, excluding the angle brackets.
         */
        private final String tag;
    }
}