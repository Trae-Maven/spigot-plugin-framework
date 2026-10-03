package io.github.trae.spigot.framework.chat.channel;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * A chat channel, registered as a singleton and looked up by name.
 *
 * <p>A line is rendered by {@link io.github.trae.spigot.framework.chat.ChatManager} from
 * {@link #getFormat()}, a MiniMessage string such as {@code "<rank> <username> <message>"}. Every tag
 * that is not a standard MiniMessage tag is a placeholder, filled from
 * {@link #getPlaceholderValue(String, Player, Player, Component)}.</p>
 *
 * <p>When several channels share a name, the one with the highest {@link #getPriority()} is used, so a
 * plugin replaces another plugin's channel by extending it, raising the priority, and overriding the
 * format and the placeholder values it changes, handing the rest to {@code super}.</p>
 */
public interface ChatChannel {

    /**
     * Returns the name the channel is looked up by, matched case-insensitively.
     *
     * @return the name
     */
    String getName();

    /**
     * Returns which channel is used when several share a name, the highest winning.
     *
     * @return the priority, zero by default
     */
    default int getPriority() {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns the players a message sent by the given player is delivered to.
     *
     * @param sender the player sending the message
     * @return the recipients
     */
    List<Player> getRecipients(final Player sender);

    /**
     * Returns the MiniMessage format every line is rendered from.
     *
     * @return the format
     */
    String getFormat();

    /**
     * Returns one placeholder's value for one recipient's line.
     *
     * @param key       the placeholder's name, as written in the format
     * @param sender    the player sending the message
     * @param recipient the player receiving this copy
     * @param message   the message
     * @return the value, or {@code null} to leave the placeholder out
     */
    Component getPlaceholderValue(final String key, final Player sender, final Player recipient, final Component message);
}