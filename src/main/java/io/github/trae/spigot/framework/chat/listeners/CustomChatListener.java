package io.github.trae.spigot.framework.chat.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.chat.ChatManager;
import io.github.trae.spigot.framework.chat.events.ChatReceiveEvent;
import io.github.trae.spigot.framework.chat.events.ChatSendEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Delivers a sent message to its recipients.
 *
 * <p>A message that survives {@link ChatSendEvent} is split into one {@link ChatReceiveEvent} per
 * recipient, and each copy that survives its own event is rendered for that recipient through
 * {@link ChatManager} and delivered. That split is what lets a plugin hide or change a message for one
 * player without affecting anyone else.</p>
 *
 * @see PreChatListener
 */
@AllArgsConstructor
@Singleton
public final class CustomChatListener implements Listener {

    private final ChatManager chatManager;

    /**
     * Dispatches a receive event for every recipient the channel names.
     *
     * <p>Runs at {@code MONITOR} so the message and its cancellation are final before it is split.</p>
     *
     * @param event the send event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatSend(final ChatSendEvent event) {
        if (event.isCancelled()) {
            return;
        }

        for (final Player recipient : event.getChannel().getRecipients(event.getSender())) {
            UtilEvent.dispatch(new ChatReceiveEvent(event, recipient));
        }
    }

    /**
     * Renders one recipient's copy of the message and delivers it.
     *
     * <p>Runs at {@code MONITOR} so the copy is final before it is rendered, which is what keeps the
     * channel's wrapping out of reach of listeners that only meant to change the message.</p>
     *
     * @param event the receive event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatReceive(final ChatReceiveEvent event) {
        if (event.isCancelled()) {
            return;
        }

        event.getRecipient().sendMessage(this.chatManager.render(event.getChannel(), event.getSender(), event.getRecipient(), event.getMessage()));
    }
}