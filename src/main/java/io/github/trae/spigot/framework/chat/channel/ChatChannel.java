package io.github.trae.spigot.framework.chat.channel;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * A channel a chat message can be sent in, such as global, staff, faction or ally chat.
 *
 * <p>A channel decides two things about a message: who receives it, and how it reads. Both are
 * decided per sender, so one channel implementation serves every player in it, and a faction
 * channel resolves each sender's own faction rather than holding one per faction.</p>
 *
 * <p>Implementations are plugin-side. The framework only routes messages through whichever channel
 * the sender resolves to, as settled by {@link io.github.trae.spigot.framework.chat.events.ChatChannelEvent}.</p>
 *
 * @see DefaultChatChannel
 */
public interface ChatChannel {

    /**
     * The channel's name, used to identify it in commands, messages and logs.
     *
     * @return the channel name
     */
    String getName();

    /**
     * Everyone who receives a message the given player sends in this channel.
     *
     * <p>Resolved once per message, so membership reflects the moment it was sent. The sender is not
     * added automatically: include them if they should see their own message.</p>
     *
     * <p>Called off the main thread for a message a player typed, so an implementation must only
     * read state that is safe to read there.</p>
     *
     * @param sender the player sending the message
     * @return the recipients, in the order they are messaged
     */
    List<Player> getRecipients(final Player sender);

    /**
     * How a message the given player sends reads in this channel.
     *
     * <p>Wraps the message with whatever the channel puts around it, such as a channel tag, the
     * sender's name and a separator, so every recipient sees the same line unless a listener on
     * {@link io.github.trae.spigot.framework.chat.events.ChatReceiveEvent} changes their copy.</p>
     *
     * @param sender  the player sending the message
     * @param message the message as the player typed it
     * @return the formatted line
     */
    Component getFormat(final Player sender, final String message);
}