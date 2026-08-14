package io.github.trae.spigot.framework.utility;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
 * <p>Both the message body and the prefix accept either a {@link String} (deserialized as MiniMessage)
 * or a pre-built {@link Component}.</p>
 *
 * <p>Bodies passed as a {@link String} receive a base color where none was specified; bodies passed as
 * a pre-built {@link Component} are sent untouched, so any styling must already be applied to them.</p>
 */
@UtilityClass
public class UtilMessage {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * The {@link TextColor} used for the prefix portion of messages.
     *
     * <p>Defaults to {@link NamedTextColor#BLUE}.</p>
     */
    @Getter
    @Setter
    private static TextColor prefixTextColor = NamedTextColor.BLUE;

    /**
     * The {@link TextColor} applied to the message body when a prefix is present.
     *
     * <p>Only applied to bodies deserialized from a MiniMessage {@link String}, and only where the
     * body did not specify its own color.</p>
     *
     * <p>Defaults to {@link NamedTextColor#GRAY}.</p>
     */
    @Getter
    @Setter
    private static TextColor messageTextColor = NamedTextColor.GRAY;

    /**
     * The {@link TextColor} used as a reset/default color, applied to the message body when no prefix
     * is present.
     *
     * <p>Only applied to bodies deserialized from a MiniMessage {@link String}, and only where the
     * body did not specify its own color.</p>
     *
     * <p>Defaults to {@link NamedTextColor#WHITE}.</p>
     */
    @Getter
    @Setter
    private static TextColor resetTextColor = NamedTextColor.WHITE;

    /**
     * Whether broadcasts should also be sent to the server console.
     *
     * <p>Applies to the {@code broadcast} methods only; direct {@code message} calls are never
     * mirrored to the console.</p>
     *
     * <p>Defaults to {@code false}.</p>
     */
    @Getter
    @Setter
    private static boolean broadcastForConsole = false;

    /**
     * The format string used to construct the prefix text.
     * Must contain a single {@code %s} placeholder for the prefix name.
     *
     * <p>Applies only to prefixes supplied as a {@link String}; a pre-built {@link Component} prefix
     * is used verbatim.</p>
     *
     * <p>Defaults to {@code "[%s] "}.</p>
     */
    @Getter
    @Setter
    private static String prefixFormat = "[%s] ";

    // -----------------------------------------------------------------------
    // Serialization & prefix resolution
    // -----------------------------------------------------------------------

    /**
     * Serializes a {@link Component} into its MiniMessage string representation.
     *
     * @param component the component to serialize
     * @return the MiniMessage string
     */
    public static String serialize(final Component component) {
        return MINI_MESSAGE.serialize(component);
    }

    /**
     * Serializes a {@link Component} into its MiniMessage string representation,
     * appending a trailing {@code <reset>} so styling does not bleed into surrounding
     * text when the result is inlined into a larger MiniMessage string.
     *
     * <p>Not a clean round-trip: the trailing {@code <reset>} means
     * {@code deserialize(serializeWithReset(component))} is not equivalent to
     * {@code component}. Use {@link #serialize(Component)} where an exact round-trip is
     * needed. Note also that {@code <reset>} clears color and decorations only, not
     * interaction data such as hover or click events.</p>
     *
     * @param component the component to serialize
     * @return the MiniMessage string with a trailing {@code <reset>}
     */
    public static String serializeWithReset(final Component component) {
        return MINI_MESSAGE.serialize(component) + "<reset>";
    }

    /**
     * Deserializes a MiniMessage string into a {@link Component}.
     *
     * @param string the raw MiniMessage string
     * @return the deserialized component
     */
    public static Component deserialize(final String string) {
        return MINI_MESSAGE.deserialize(string);
    }

    /**
     * Builds a prefix {@link Component} from the configured {@link #prefixFormat} using the given color.
     *
     * @param textColor the color to apply to the prefix
     * @param prefix    the prefix label, or {@code null} for an empty component
     * @return the formatted prefix component
     */
    public static Component resolvePrefix(final TextColor textColor, final String prefix) {
        if (prefix == null) {
            return Component.empty();
        }

        return Component.text(prefixFormat.formatted(prefix), textColor);
    }

    /**
     * Builds a prefix {@link Component} from the configured {@link #prefixFormat}, using the default
     * {@link #prefixTextColor}.
     *
     * @param prefix the prefix label, or {@code null} for an empty component
     * @return the formatted prefix component
     */
    public static Component resolvePrefix(final String prefix) {
        return resolvePrefix(prefixTextColor, prefix);
    }

    /**
     * Returns the given pre-built {@link Component} prefix as-is, without applying
     * {@link #prefixFormat} or {@link #prefixTextColor}.
     *
     * @param prefix the pre-built prefix component, or {@code null} for an empty component
     * @return the prefix component, or {@link Component#empty()} if {@code null}
     */
    public static Component resolvePrefix(final Component prefix) {
        return prefix == null ? Component.empty() : prefix;
    }

    /**
     * Deserializes a MiniMessage body and applies the appropriate base color where none was specified.
     * Uses {@link #messageTextColor} when a prefix is present, otherwise {@link #resetTextColor}.
     *
     * @param prefixed whether a prefix is present
     * @param message  the raw MiniMessage body
     * @return the deserialized, base-colored body component
     */
    private static Component resolveBody(final boolean prefixed, final String message) {
        return deserialize(message).colorIfAbsent(prefixed ? messageTextColor : resetTextColor);
    }

    // -----------------------------------------------------------------------
    // Single-recipient messaging
    // -----------------------------------------------------------------------

    /**
     * Sends a pre-built {@link Component} to a single audience, unmodified.
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
     * Deserializes a MiniMessage string and sends it to a single audience, applying
     * {@link #resetTextColor} where the body specified no color.
     *
     * @param audience the target audience, or {@code null} (no-op)
     * @param message  the raw MiniMessage string
     */
    public static void message(final Audience audience, final String message) {
        message(audience, resolveBody(false, message));
    }

    /**
     * Sends a {@link Component}-prefixed, pre-built {@link Component} to a single audience.
     * Neither part is recolored.
     *
     * @param audience the target audience, or {@code null} (no-op)
     * @param prefix   the pre-built prefix component, or {@code null} for no prefix
     * @param message  the pre-built body component
     */
    public static void message(final Audience audience, final Component prefix, final Component message) {
        message(audience, resolvePrefix(prefix).append(message));
    }

    /**
     * Sends a {@link String}-prefixed, pre-built {@link Component} to a single audience.
     * The body is not recolored.
     *
     * @param audience the target audience, or {@code null} (no-op)
     * @param prefix   the prefix label, or {@code null} for no prefix
     * @param message  the pre-built body component
     */
    public static void message(final Audience audience, final String prefix, final Component message) {
        message(audience, resolvePrefix(prefix).append(message));
    }

    /**
     * Deserializes a MiniMessage body and sends it with a {@link Component} prefix to a single audience.
     *
     * @param audience the target audience, or {@code null} (no-op)
     * @param prefix   the pre-built prefix component, or {@code null} for no prefix
     * @param message  the raw MiniMessage body
     */
    public static void message(final Audience audience, final Component prefix, final String message) {
        message(audience, resolvePrefix(prefix).append(resolveBody(prefix != null, message)));
    }

    /**
     * Deserializes a MiniMessage body and sends it with a {@link String} prefix to a single audience.
     *
     * @param audience the target audience, or {@code null} (no-op)
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
     * @param prefix  the pre-built prefix component, or {@code null} for no prefix
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
     * @param prefix  the prefix label, or {@code null} for no prefix
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
     * Deserializes a MiniMessage body and sends it with a {@link Component} prefix to a collection of
     * players, optionally ignoring specific UUIDs. The body is deserialized once per recipient.
     *
     * @param players the target players
     * @param prefix  the pre-built prefix component, or {@code null} for no prefix
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
     * Deserializes a MiniMessage body and sends it with a {@link String} prefix to a collection of
     * players, optionally ignoring specific UUIDs. The body is deserialized once per recipient.
     *
     * @param players the target players
     * @param prefix  the prefix label, or {@code null} for no prefix
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
     * optionally ignoring specific UUIDs. Also sent to the console when
     * {@link #broadcastForConsole} is enabled, regardless of {@code ignored}.
     *
     * @param prefix  the pre-built prefix component, or {@code null} for no prefix
     * @param message the pre-built body component
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void broadcast(final Component prefix, final Component message, final List<UUID> ignored) {
        message(Bukkit.getServer().getOnlinePlayers(), prefix, message, ignored);

        if (broadcastForConsole) {
            log(prefix, message);
        }
    }

    /**
     * Broadcasts a {@link Component}-prefixed, pre-built {@link Component} to all online players,
     * and to the console when {@link #broadcastForConsole} is enabled.
     *
     * @param prefix  the pre-built prefix component, or {@code null} for no prefix
     * @param message the pre-built body component
     */
    public static void broadcast(final Component prefix, final Component message) {
        broadcast(prefix, message, null);
    }

    /**
     * Broadcasts a {@link String}-prefixed, pre-built {@link Component} to all online players,
     * optionally ignoring specific UUIDs. Also sent to the console when
     * {@link #broadcastForConsole} is enabled, regardless of {@code ignored}.
     *
     * @param prefix  the prefix label, or {@code null} for no prefix
     * @param message the pre-built body component
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void broadcast(final String prefix, final Component message, final List<UUID> ignored) {
        message(Bukkit.getServer().getOnlinePlayers(), prefix, message, ignored);

        if (broadcastForConsole) {
            log(prefix, message);
        }
    }

    /**
     * Broadcasts a {@link String}-prefixed, pre-built {@link Component} to all online players,
     * and to the console when {@link #broadcastForConsole} is enabled.
     *
     * @param prefix  the prefix label, or {@code null} for no prefix
     * @param message the pre-built body component
     */
    public static void broadcast(final String prefix, final Component message) {
        broadcast(prefix, message, null);
    }

    /**
     * Broadcasts an unprefixed, pre-built {@link Component} to all online players, optionally ignoring
     * specific UUIDs. Also sent to the console when {@link #broadcastForConsole} is enabled.
     *
     * @param message the component to broadcast
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void broadcast(final Component message, final List<UUID> ignored) {
        broadcast((String) null, message, ignored);
    }

    /**
     * Broadcasts an unprefixed, pre-built {@link Component} to all online players, and to the console
     * when {@link #broadcastForConsole} is enabled.
     *
     * @param message the component to broadcast
     */
    public static void broadcast(final Component message) {
        broadcast((String) null, message, null);
    }

    /**
     * Deserializes and broadcasts a {@link Component}-prefixed MiniMessage string to all online players,
     * optionally ignoring specific UUIDs. Also sent to the console when
     * {@link #broadcastForConsole} is enabled, regardless of {@code ignored}.
     *
     * @param prefix  the pre-built prefix component, or {@code null} for no prefix
     * @param message the raw MiniMessage body
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void broadcast(final Component prefix, final String message, final List<UUID> ignored) {
        message(Bukkit.getServer().getOnlinePlayers(), prefix, message, ignored);

        if (broadcastForConsole) {
            log(prefix, message);
        }
    }

    /**
     * Deserializes and broadcasts a {@link Component}-prefixed MiniMessage string to all online players,
     * and to the console when {@link #broadcastForConsole} is enabled.
     *
     * @param prefix  the pre-built prefix component, or {@code null} for no prefix
     * @param message the raw MiniMessage body
     */
    public static void broadcast(final Component prefix, final String message) {
        broadcast(prefix, message, null);
    }

    /**
     * Deserializes and broadcasts a {@link String}-prefixed MiniMessage string to all online players,
     * optionally ignoring specific UUIDs. Also sent to the console when
     * {@link #broadcastForConsole} is enabled, regardless of {@code ignored}.
     *
     * @param prefix  the prefix label, or {@code null} for no prefix
     * @param message the raw MiniMessage body
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void broadcast(final String prefix, final String message, final List<UUID> ignored) {
        message(Bukkit.getServer().getOnlinePlayers(), prefix, message, ignored);

        if (broadcastForConsole) {
            log(prefix, message);
        }
    }

    /**
     * Deserializes and broadcasts a {@link String}-prefixed MiniMessage string to all online players,
     * and to the console when {@link #broadcastForConsole} is enabled.
     *
     * @param prefix  the prefix label, or {@code null} for no prefix
     * @param message the raw MiniMessage body
     */
    public static void broadcast(final String prefix, final String message) {
        broadcast(prefix, message, null);
    }

    /**
     * Deserializes and broadcasts an unprefixed MiniMessage string to all online players, optionally
     * ignoring specific UUIDs. Also sent to the console when {@link #broadcastForConsole} is enabled.
     *
     * @param message the raw MiniMessage string
     * @param ignored UUIDs to skip, or {@code null} to send to all
     */
    public static void broadcast(final String message, final List<UUID> ignored) {
        broadcast((String) null, message, ignored);
    }

    /**
     * Deserializes and broadcasts an unprefixed MiniMessage string to all online players, and to the
     * console when {@link #broadcastForConsole} is enabled.
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
     * @param prefix  the pre-built prefix component, or {@code null} for no prefix
     * @param message the pre-built body component
     */
    public static void log(final Component prefix, final Component message) {
        message(Bukkit.getServer().getConsoleSender(), prefix, message);
    }

    /**
     * Sends a {@link String}-prefixed, pre-built {@link Component} to the server console.
     *
     * @param prefix  the prefix label, or {@code null} for no prefix
     * @param message the pre-built body component
     */
    public static void log(final String prefix, final Component message) {
        message(Bukkit.getServer().getConsoleSender(), prefix, message);
    }

    /**
     * Sends an unprefixed, pre-built {@link Component} to the server console.
     *
     * @param message the component to log
     */
    public static void log(final Component message) {
        message(Bukkit.getServer().getConsoleSender(), message);
    }

    /**
     * Deserializes a MiniMessage body and sends it with a {@link Component} prefix to the server console.
     *
     * @param prefix  the pre-built prefix component, or {@code null} for no prefix
     * @param message the raw MiniMessage body
     */
    public static void log(final Component prefix, final String message) {
        message(Bukkit.getServer().getConsoleSender(), prefix, message);
    }

    /**
     * Deserializes a MiniMessage body and sends it with a {@link String} prefix to the server console.
     *
     * @param prefix  the prefix label, or {@code null} for no prefix
     * @param message the raw MiniMessage body
     */
    public static void log(final String prefix, final String message) {
        message(Bukkit.getServer().getConsoleSender(), prefix, message);
    }

    /**
     * Deserializes an unprefixed MiniMessage string and sends it to the server console.
     *
     * @param message the raw MiniMessage string
     */
    public static void log(final String message) {
        message(Bukkit.getServer().getConsoleSender(), message);
    }
}