package io.github.trae.spigot.framework.utility;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Utility class for sending formatted messages using the Adventure {@link MiniMessage} format.
 *
 * <p>All string-based message methods deserialize through {@link MiniMessage}, allowing
 * full tag support (e.g. {@code <green>}, {@code <bold>}, {@code <gradient:red:blue>})
 * in message content. Prefixes are constructed programmatically via
 * {@link Component#text(String)} with configurable {@link NamedTextColor} defaults.</p>
 *
 * <p>The default prefix format is {@code "[%s] "} rendered in {@link NamedTextColor#BLUE},
 * with message body text colored {@link NamedTextColor#GRAY}. These defaults can be
 * changed globally via the provided setters.</p>
 */
@UtilityClass
public class UtilMessage {

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

    /**
     * Builds a MiniMessage-serialized prefix string with the given color and name.
     *
     * <p>The prefix is constructed as a {@link Component} using {@link #getPrefixFormat()}
     * and then serialized back to a MiniMessage string for later deserialization.</p>
     *
     * @param color  the {@link NamedTextColor} to apply to the prefix
     * @param prefix the prefix name to insert into the format string
     * @return a MiniMessage-encoded string representing the colored prefix
     */
    public static String resolvePrefix(final NamedTextColor color, final String prefix) {
        return MiniMessage.miniMessage().serialize(Component.text(getPrefixFormat().formatted(prefix)).color(color));
    }

    /**
     * Builds a MiniMessage-serialized prefix string using the default
     * {@link #getPrefixNamedTextColor()}.
     *
     * @param prefix the prefix name to insert into the format string
     * @return a MiniMessage-encoded string representing the colored prefix
     * @see #resolvePrefix(NamedTextColor, String)
     */
    public static String resolvePrefix(final String prefix) {
        return resolvePrefix(getPrefixNamedTextColor(), prefix);
    }

    /**
     * Sends a pre-built {@link Component} to a {@link CommandSender}.
     *
     * <p>If the sender is {@code null}, the message is silently dropped.</p>
     *
     * @param sender    the recipient of the message (may be {@code null})
     * @param component the component to send
     */
    public static void message(final CommandSender sender, final Component component) {
        if (sender == null) {
            return;
        }

        sender.sendMessage(component);
    }

    /**
     * Sends a prefixed {@link Component} message to a {@link CommandSender}.
     *
     * <p>The prefix is resolved via {@link #resolvePrefix(String)} and prepended
     * to the component. When a prefix is present, the message body is colored
     * with {@link #getMessageNamedTextColor()}; otherwise, the component is sent as-is.</p>
     *
     * @param sender    the recipient of the message (may be {@code null})
     * @param prefix    the prefix name (may be {@code null} for no prefix)
     * @param component the message component to send
     */
    public static void message(final CommandSender sender, final String prefix, final Component component) {
        message(sender, MiniMessage.miniMessage().deserialize(resolvePrefix(prefix)).append(prefix != null ? component.color(getMessageNamedTextColor()) : component));
    }

    /**
     * Sends a prefixed MiniMessage string to a {@link CommandSender}.
     *
     * <p>The message string is deserialized via {@link MiniMessage}, allowing
     * full tag support (e.g. {@code <green>}, {@code <bold>}, {@code <hover:show_text:'tip'>}).</p>
     *
     * @param sender  the recipient of the message (may be {@code null})
     * @param prefix  the prefix name (may be {@code null} for no prefix)
     * @param message the MiniMessage-formatted string to send
     */
    public static void message(final CommandSender sender, final String prefix, final String message) {
        message(sender, prefix, MiniMessage.miniMessage().deserialize(message));
    }

    /**
     * Sends a MiniMessage string to a {@link CommandSender} with no prefix.
     *
     * @param sender  the recipient of the message (may be {@code null})
     * @param message the MiniMessage-formatted string to send
     */
    public static void message(final CommandSender sender, final String message) {
        message(sender, null, message);
    }

    /**
     * Sends a prefixed MiniMessage string to a collection of players, with optional
     * filtering and ignore list support.
     *
     * <p>Players whose UUID appears in the {@code ignored} list are skipped.
     * Players that do not pass the {@code predicate} test are also skipped.</p>
     *
     * @param players   the collection of players to message
     * @param prefix    the prefix name (may be {@code null} for no prefix)
     * @param message   the MiniMessage-formatted string to send
     * @param predicate an optional filter; players that fail the test are skipped (may be {@code null})
     * @param ignored   an optional list of UUIDs to exclude from receiving the message (may be {@code null})
     */
    public static void message(final Collection<? extends Player> players, final String prefix, final String message, final Predicate<Player> predicate, final List<UUID> ignored) {
        for (final Player player : players) {
            if (ignored != null && ignored.contains(player.getUniqueId())) {
                continue;
            }

            if (predicate != null && !(predicate.test(player))) {
                continue;
            }

            message(player, prefix, message);
        }
    }

    /**
     * Broadcasts a prefixed MiniMessage string to all online players,
     * excluding those in the ignore list.
     *
     * @param prefix  the prefix name (may be {@code null} for no prefix)
     * @param message the MiniMessage-formatted string to broadcast
     * @param ignored an optional list of UUIDs to exclude (may be {@code null})
     */
    public static void broadcast(final String prefix, final String message, final List<UUID> ignored) {
        message(Bukkit.getServer().getOnlinePlayers(), prefix, message, null, ignored);
    }

    /**
     * Broadcasts a MiniMessage string with no prefix to all online players,
     * excluding those in the ignore list.
     *
     * @param message the MiniMessage-formatted string to broadcast
     * @param ignored an optional list of UUIDs to exclude (may be {@code null})
     */
    public static void broadcast(final String message, final List<UUID> ignored) {
        broadcast(null, message, ignored);
    }

    /**
     * Broadcasts a prefixed MiniMessage string to all online players.
     *
     * @param prefix  the prefix name (may be {@code null} for no prefix)
     * @param message the MiniMessage-formatted string to broadcast
     */
    public static void broadcast(final String prefix, final String message) {
        broadcast(prefix, message, null);
    }

    /**
     * Broadcasts a MiniMessage string with no prefix to all online players.
     *
     * @param message the MiniMessage-formatted string to broadcast
     */
    public static void broadcast(final String message) {
        broadcast(null, message, null);
    }

    /**
     * Logs a prefixed MiniMessage string to the server console.
     *
     * @param prefix  the prefix name (may be {@code null} for no prefix)
     * @param message the MiniMessage-formatted string to log
     */
    public static void log(final String prefix, final String message) {
        message(Bukkit.getServer().getConsoleSender(), prefix, message);
    }

    /**
     * Logs a MiniMessage string with no prefix to the server console.
     *
     * @param message the MiniMessage-formatted string to log
     */
    public static void log(final String message) {
        log(null, message);
    }
}