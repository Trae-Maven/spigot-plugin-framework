package io.github.trae.spigot.framework.sidebar;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

public interface Sidebar {

    String getIdentifier();

    int getPriority();

    default boolean canDisplay() {
        return true;
    }

    default boolean canDisplay(final Player player) {
        return true;
    }

    default boolean isStaticTitle() {
        return true;
    }

    Component getTitle(final Player player);

    List<Component> getLines(final Player player);
}