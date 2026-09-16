package io.github.trae.spigot.framework.chat.events;

import io.github.trae.spigot.framework.chat.channel.ChatChannel;
import io.github.trae.spigot.framework.event.CustomAsynchronousCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * One recipient is about to receive a message.
 *
 * <p>Fired once per recipient, after {@link ChatSendEvent} has survived, so each copy can be handled on
 * its own. Cancel to hide the message from this recipient alone, such as one who ignores the sender,
 * or change the message to vary it for them, such as highlighting their name.</p>
 *
 * @see ChatSendEvent
 */
@AllArgsConstructor
@Getter
@Setter
public class ChatReceiveEvent extends CustomAsynchronousCancellableEvent {

    /**
     * The player who sent the message, and the player receiving this copy of it.
     */
    private final Player sender, recipient;

    /**
     * The channel the message was sent in.
     */
    private final ChatChannel channel;

    /**
     * The message this recipient will see. Starts as the message the send event settled on.
     */
    private Component message;

    /**
     * Builds one recipient's copy from the message being sent.
     *
     * @param chatSendEvent the send event this copy comes from
     * @param recipient     the player receiving this copy
     */
    public ChatReceiveEvent(final ChatSendEvent chatSendEvent, final Player recipient) {
        this(chatSendEvent.getSender(), recipient, chatSendEvent.getChannel(), chatSendEvent.getMessage());
    }
}