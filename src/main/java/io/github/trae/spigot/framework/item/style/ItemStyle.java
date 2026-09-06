package io.github.trae.spigot.framework.item.style;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.NamespacedKey;

import java.awt.Color;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ItemStyle {

    private final String name;
    private final Color color;
    private final NamespacedKey tooltipStyle;
    private final String tag;

    public static ItemStyle of(final String name, final Color color, final NamespacedKey tooltipStyle, final String tag) {
        return new ItemStyle(name, color, tooltipStyle, tag);
    }
}