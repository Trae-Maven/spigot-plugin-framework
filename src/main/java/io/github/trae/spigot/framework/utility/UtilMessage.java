package io.github.trae.spigot.framework.utility;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Utility for sending MiniMessage-formatted messages to players, broadcasts, and the console.
 *
 * <p>Supports the full MiniMessage tag set: named colors ({@code <red>}), hex colors
 * ({@code <#ff5555>}), formatting ({@code <bold>}, {@code <italic>}, {@code <underlined>}),
 * clickable links ({@code <click:open_url:'https://...'>text</click>}), translations
 * ({@code <lang:key>}), and more.</p>
 *
 * <p>Both the message body and the prefix accept either a {@link String} (parsed as MiniMessage)
 * or a pre-built {@link Component}.</p>
 */
@UtilityClass
public class UtilMessage {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * The {@link NamedTextColor} used for the prefix portion of messages.
     *
     * <p>Defaults to {@link NamedTextColor#BLUE}.</p>
     */
    @Getter
    @Setter
    private static NamedTextColor prefixNamedTextColor = NamedTextColor.BLUE;

    /**
     * The {@link NamedTextColor} applied to the message body when a prefix is present.
     *
     * <p>Defaults to {@link NamedTextColor#GRAY}.</p>
     */
    @Getter
    @Setter
    private static NamedTextColor messageNamedTextColor = NamedTextColor.GRAY;

    /**
     * The {@link NamedTextColor} used as a reset/default color.
     *
     * <p>Defaults to {@link NamedTextColor#WHITE}.</p>
     */
    @Getter
    @Setter
    private static NamedTextColor resetNamedTextColor = NamedTextColor.WHITE;

    /**
     * The format string used to construct the prefix text.
     * Must contain a single {@code %s} placeholder for the prefix name.
     *
     * <p>Defaults to {@code "[%s] "}.</p>
     */
    @Getter
    @Setter
    private static String prefixFormat = "[%s] ";

    // -----------------------------------------------------------------------
    // Parsing & prefix resolution
    // -----------------------------------------------------------------------

    /**
     * Parses a MiniMessage string into a {@link Component}.
     *
     * @param message the raw MiniMessage string
     * @return the parsed component
     */
    public static Component parse(final String message) {
        return MINI_MESSAGE.deserialize(message);
    }

    /**
     * Builds a prefix {@link Component} from the configured {@link #prefixFormat} using the given color.
     *
     * @param namedTextColor the color to apply to the prefix
     * @param prefix         the prefix label, or {@code null} for an empty component
     * @return the formatted prefix component
     */
    public static Component resolvePrefix(final NamedTextColor namedTextColor, final String prefix) {
        if (prefix == null) {
            return Component.empty();
        }

        return Component.text(prefixFormat.formatted(prefix), namedTextColor);
    }

    /**
     * Builds a prefix {@link Component} from the configured {@link #prefixFormat}, using the default
     * {@link #prefixNamedTextColor}.
     *
     * @param prefix the prefix label, or {@code null} for an empty component
     * @return the formatted prefix component
     */
    public static Component resolvePrefix(final String prefix) {
        return resolvePrefix(prefixNamedTextColor, prefix);
    }

    /**
     * Returns the given pre-built {@link Component} prefix as-is.
     *
     * @param prefix the pre-built prefix component, or {@code null} for an empty component
     * @return the prefix component, or {@link Component#empty()} if {@code null}
     */
    public static Component resolvePrefix(final Component prefix) {
        return prefix == null ? Component.empty() : prefix;
    }

    /**
     * Parses a MiniMessage body and applies the appropriate base color where none was specified.
     * Uses {@link #messageNamedTextColor} when a prefix is present, otherwise {@link #resetNamedTextColor}.
     *
     * @param prefixed whether a prefix is present
     * @param message  the raw MiniMessage body
     * @return the parsed, base-colored body component
     */
    private static Component resolveBody(final boolean prefixed, final String message) {
        return parse(message).colorIfAbsent(prefixed ? messageNamedTextColor : resetNamedTextColor);
    }

    // -----------------------------------------------------------------------
    // Single-recipient messaging
    // -----------------------------------------------------------------------

    /**
     * Sends a pre-built {@link Component} to a single audience.
     *
     * @param audience the target audience, or {@code null} (no-op)
     * @param message  the component to send
     */
    public static void message(final Audience audience, final Component message) {
        if (audience == null) {
            return;
        }

        audience.sendMessage(message);
    }

    /**
     * Parses a MiniMessage string and sends it to a single audience.
     *
     * @param audience the target audience
     * @param message  the raw MiniMessage string
     */
    public static void message(final Audience audience, final String message) {
        message(audience, resolveBody(false, message));
    }

    /**
     * Sends a {@link Component}-prefixed, pre-built {@link Component} to a single audience.
     *
     * @param audience the target audience
     * @param prefix   the pre-built prefix component
     * @param message  the pre-built body component
     */
    public static void message(final Audience audience, final Component prefix, final Component message) {
        message(audience, resolvePrefix(prefix).append(message));
    }

    /**
     * Sends a {@link String}-prefixed, pre-built {@link Component} to a single audience.
     *
     * @param audience the target audience
     * @param prefix   the prefix label
     * @param message  the pre-built body component
     */
    public static void message(final Audience audience, final String prefix, final Component message) {
        message(audience, resolvePrefix(prefix).append(message));
    }

    /**
     * Parses a MiniMessage body and sends it with a {@link Component} prefix to a single audience.
     *
     * @param audience the target audience
     * @param prefix   the pre-built prefix component, or {@code null} for no prefix
     * @param message  the raw MiniMessage body
     */
    public static void message(final Audience audience, final Component prefix, final String message) {
        message(audience, resolvePrefix(prefix).append(resolveBody(prefix != null, message)));
    }

    /**
     * Parses a MiniMessage body and sends it with a {@link String} prefix to a single audience.
     *
     * @param audience the target audience
     * @param prefix   the prefix label, or {@code null} for no prefix
     * @param message  the raw MiniMessage body
     */
    public static void message(final Audience audience, final String prefix, final String message) {
        message(audience, resolvePrefix(prefix).append(resolveBody(prefix != null, message)));
    }

    // -----------------------------------------------------------------------
    // Multi-recipient messaging
    // -----------------------------------------------------------------------

    /**
     * Sends a {@link Component}-prefixed, pre-built {@link Component} to a collection of players,
     * optionally ignoring specific UUIDs.
     *
     * @param players the target players
     * @param prefix  the pre-built prefix component
     * @param message the pre-built body component
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void message(final Collection<? extends Player> players, final Component prefix, final Component message, final List<UUID> ignored) {
        for (final Player player : players) {
            if (ignored != null && ignored.contains(player.getUniqueId())) {
                continue;
            }

            message(player, prefix, message);
        }
    }

    /**
     * Sends a {@link String}-prefixed, pre-built {@link Component} to a collection of players,
     * optionally ignoring specific UUIDs.
     *
     * @param players the target players
     * @param prefix  the prefix label
     * @param message the pre-built body component
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void message(final Collection<? extends Player> players, final String prefix, final Component message, final List<UUID> ignored) {
        for (final Player player : players) {
            if (ignored != null && ignored.contains(player.getUniqueId())) {
                continue;
            }

            message(player, prefix, message);
        }
    }

    /**
     * Parses a MiniMessage body and sends it with a {@link Component} prefix to a collection of
     * players, optionally ignoring specific UUIDs.
     *
     * @param players the target players
     * @param prefix  the pre-built prefix component
     * @param message the raw MiniMessage body
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void message(final Collection<? extends Player> players, final Component prefix, final String message, final List<UUID> ignored) {
        for (final Player player : players) {
            if (ignored != null && ignored.contains(player.getUniqueId())) {
                continue;
            }

            message(player, prefix, message);
        }
    }

    /**
     * Parses a MiniMessage body and sends it with a {@link String} prefix to a collection of
     * players, optionally ignoring specific UUIDs.
     *
     * @param players the target players
     * @param prefix  the prefix label
     * @param message the raw MiniMessage body
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void message(final Collection<? extends Player> players, final String prefix, final String message, final List<UUID> ignored) {
        for (final Player player : players) {
            if (ignored != null && ignored.contains(player.getUniqueId())) {
                continue;
            }

            message(player, prefix, message);
        }
    }

    // -----------------------------------------------------------------------
    // Broadcast (all online players)
    // -----------------------------------------------------------------------

    /**
     * Broadcasts a {@link Component}-prefixed, pre-built {@link Component} to all online players,
     * optionally ignoring specific UUIDs.
     *
     * @param prefix  the pre-built prefix component
     * @param message the pre-built body component
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void broadcast(final Component prefix, final Component message, final List<UUID> ignored) {
        message(Bukkit.getServer().getOnlinePlayers(), prefix, message, ignored);
    }

    /**
     * Broadcasts a {@link Component}-prefixed, pre-built {@link Component} to all online players.
     *
     * @param prefix  the pre-built prefix component
     * @param message the pre-built body component
     */
    public static void broadcast(final Component prefix, final Component message) {
        broadcast(prefix, message, null);
    }

    /**
     * Broadcasts a {@link String}-prefixed, pre-built {@link Component} to all online players,
     * optionally ignoring specific UUIDs.
     *
     * @param prefix  the prefix label
     * @param message the pre-built body component
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void broadcast(final String prefix, final Component message, final List<UUID> ignored) {
        message(Bukkit.getServer().getOnlinePlayers(), prefix, message, ignored);
    }

    /**
     * Broadcasts a {@link String}-prefixed, pre-built {@link Component} to all online players.
     *
     * @param prefix  the prefix label
     * @param message the pre-built body component
     */
    public static void broadcast(final String prefix, final Component message) {
        broadcast(prefix, message, null);
    }

    /**
     * Broadcasts a pre-built {@link Component} to all online players, optionally ignoring specific UUIDs.
     *
     * @param message the component to broadcast
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void broadcast(final Component message, final List<UUID> ignored) {
        broadcast((String) null, message, ignored);
    }

    /**
     * Broadcasts a pre-built {@link Component} to all online players.
     *
     * @param message the component to broadcast
     */
    public static void broadcast(final Component message) {
        broadcast((String) null, message, null);
    }

    /**
     * Parses and broadcasts a {@link Component}-prefixed MiniMessage string to all online players,
     * optionally ignoring specific UUIDs.
     *
     * @param prefix  the pre-built prefix component
     * @param message the raw MiniMessage body
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void broadcast(final Component prefix, final String message, final List<UUID> ignored) {
        message(Bukkit.getServer().getOnlinePlayers(), prefix, message, ignored);
    }

    /**
     * Parses and broadcasts a {@link Component}-prefixed MiniMessage string to all online players.
     *
     * @param prefix  the pre-built prefix component
     * @param message the raw MiniMessage body
     */
    public static void broadcast(final Component prefix, final String message) {
        broadcast(prefix, message, null);
    }

    /**
     * Parses and broadcasts a {@link String}-prefixed MiniMessage string to all online players,
     * optionally ignoring specific UUIDs.
     *
     * @param prefix  the prefix label
     * @param message the raw MiniMessage body
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void broadcast(final String prefix, final String message, final List<UUID> ignored) {
        message(Bukkit.getServer().getOnlinePlayers(), prefix, message, ignored);
    }

    /**
     * Parses and broadcasts a {@link String}-prefixed MiniMessage string to all online players.
     *
     * @param prefix  the prefix label
     * @param message the raw MiniMessage body
     */
    public static void broadcast(final String prefix, final String message) {
        broadcast(prefix, message, null);
    }

    /**
     * Parses and broadcasts a MiniMessage string to all online players, optionally ignoring specific UUIDs.
     *
     * @param message the raw MiniMessage string
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void broadcast(final String message, final List<UUID> ignored) {
        broadcast((String) null, message, ignored);
    }

    /**
     * Parses and broadcasts a MiniMessage string to all online players.
     *
     * @param message the raw MiniMessage string
     */
    public static void broadcast(final String message) {
        broadcast((String) null, message, null);
    }

    // -----------------------------------------------------------------------
    // Console logging
    // -----------------------------------------------------------------------

    /**
     * Sends a {@link Component}-prefixed, pre-built {@link Component} to the server console.
     *
     * @param prefix  the pre-built prefix component
     * @param message the pre-built body component
     */
    public static void log(final Component prefix, final Component message) {
        message(Bukkit.getServer().getConsoleSender(), prefix, message);
    }

    /**
     * Sends a {@link String}-prefixed, pre-built {@link Component} to the server console.
     *
     * @param prefix  the prefix label
     * @param message the pre-built body component
     */
    public static void log(final String prefix, final Component message) {
        message(Bukkit.getServer().getConsoleSender(), prefix, message);
    }

    /**
     * Sends a pre-built {@link Component} to the server console.
     *
     * @param message the component to log
     */
    public static void log(final Component message) {
        message(Bukkit.getServer().getConsoleSender(), message);
    }

    /**
     * Parses a MiniMessage body and sends it with a {@link Component} prefix to the server console.
     *
     * @param prefix  the pre-built prefix component
     * @param message the raw MiniMessage body
     */
    public static void log(final Component prefix, final String message) {
        message(Bukkit.getServer().getConsoleSender(), prefix, message);
    }

    /**
     * Parses a MiniMessage body and sends it with a {@link String} prefix to the server console.
     *
     * @param prefix  the prefix label
     * @param message the raw MiniMessage body
     */
    public static void log(final String prefix, final String message) {
        message(Bukkit.getServer().getConsoleSender(), prefix, message);
    }

    /**
     * Parses a MiniMessage string and sends it to the server console.
     *
     * @param message the raw MiniMessage string
     */
    public static void log(final String message) {
        message(Bukkit.getServer().getConsoleSender(), message);
    }
}