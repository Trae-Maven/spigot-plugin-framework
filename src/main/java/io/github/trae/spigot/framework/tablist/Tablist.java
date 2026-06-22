package io.github.trae.spigot.framework.tablist;

import org.bukkit.entity.Player;

/**
 * A tab list header/footer provider, discovered via the dependency injector and resolved per player
 * by the tablist manager.
 * <p>
 * When multiple tablists are registered, the one with the lowest {@link #getPriority()} that passes
 * both display checks is the one shown. Header and footer are supplied as raw strings and
 * deserialized by the manager, so they may carry markup and dynamic placeholders that are
 * re-evaluated each update.
 */
public interface Tablist {

    /**
     * Returns this tablist's priority. Lower values win when several tablists are eligible for the
     * same player.
     *
     * @return the priority, lower being higher precedence
     */
    int getPriority();

    /**
     * Returns the raw header string, deserialized by the manager before being sent.
     *
     * @return the header content
     */
    String getHeader();

    /**
     * Returns the raw footer string, deserialized by the manager before being sent.
     *
     * @return the footer content
     */
    String getFooter();

    /**
     * Global gate controlling whether this tablist is eligible at all, independent of any specific
     * player. Defaults to {@code true}.
     *
     * @return {@code true} if this tablist may be displayed
     */
    default boolean canDisplay() {
        return true;
    }

    /**
     * Per-player gate controlling whether this tablist is eligible for the given player. Defaults to
     * {@code true}.
     *
     * @param player the player the tablist would be shown to
     * @return {@code true} if this tablist may be displayed to the player
     */
    default boolean canDisplay(final Player player) {
        return true;
    }
}