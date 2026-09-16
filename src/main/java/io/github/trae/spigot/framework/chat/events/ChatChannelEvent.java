package io.github.trae.spigot.framework.chat.events;

import io.github.trae.spigot.framework.chat.channel.ChatChannel;
import io.github.trae.spigot.framework.event.CustomAsynchronousEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

/**
 * Resolves which channel a player's message is sent in.
 *
 * <p>Fired for every message before it is sent, seeded with the
 * {@link io.github.trae.spigot.framework.chat.channel.DefaultChatChannel}. The framework keeps no
 * record of who is in which channel: that state is the plugin's, such as a channel stored on the
 * player's account, and a listener reads it here and sets the channel to match.</p>
 *
 * <p>Several listeners may set the channel, and the last to run wins, so priority decides between
 * them. A plugin can resolve everything in one listener, or split it, with a lower priority for a
 * player's chosen channel and a higher one for a state that overrides it, such as a mute that forces
 * them into a restricted channel.</p>
 *
 * <p>Not cancellable, since every message needs a channel. Refusing a message belongs on
 * {@link ChatSendEvent}.</p>
 *
 * @see ChatSwitchChannelEvent
 */
@AllArgsConstructor
@Getter
@Setter
public class ChatChannelEvent extends CustomAsynchronousEvent {

    /**
     * The player whose message is being routed.
     */
    private final Player sender;

    /**
     * The channel the message will be sent in. Starts as the default channel.
     */
    private ChatChannel channel;
}