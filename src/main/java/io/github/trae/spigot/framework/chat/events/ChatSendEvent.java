package io.github.trae.spigot.framework.chat.events;

import io.github.trae.spigot.framework.chat.channel.ChatChannel;
import io.github.trae.spigot.framework.event.CustomAsynchronousCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * A player is sending a message in a channel, before anyone receives it.
 *
 * <p>Fired once per message, after {@link ChatChannelEvent} has settled the channel. Cancel to refuse
 * the message for everyone, such as a mute or a spam filter. Changing the message here changes it for
 * every recipient, where {@link ChatReceiveEvent} changes it for one.</p>
 *
 * <p>The channel is fixed by this point. A listener that needs a different channel sets it on
 * {@link ChatChannelEvent} instead.</p>
 */
@AllArgsConstructor
@Getter
@Setter
public class ChatSendEvent extends CustomAsynchronousCancellableEvent {

    /**
     * The player sending the message.
     */
    private final Player sender;

    /**
     * The channel the message is sent in, as resolved by {@link ChatChannelEvent}.
     */
    private final ChatChannel channel;

    /**
     * The message every recipient starts from.
     */
    private Component message;
}