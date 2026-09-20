package io.github.trae.spigot.framework.chat.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.chat.ChatManager;
import io.github.trae.spigot.framework.chat.channel.DefaultChatChannel;
import io.github.trae.spigot.framework.chat.events.ChatChannelEvent;
import io.github.trae.spigot.framework.chat.events.ChatSendEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * The entry point of the chat system.
 *
 * <p>Takes vanilla chat over: the chat event is cancelled, so vanilla never sends the message, and the
 * framework's own events carry it from there. The channel is resolved first through
 * {@link ChatChannelEvent}, then the message is sent in it through {@link ChatSendEvent}.</p>
 *
 * <p>Both events are dispatched on the chat event's own thread rather than handed to another, so the
 * whole message is settled before this handler returns.</p>
 *
 * @see CustomChatListener
 */
@AllArgsConstructor
@Singleton
public final class PreChatListener implements Listener {

    private final ChatManager chatManager;

    /**
     * Cancels the vanilla message and sends it through the framework instead.
     *
     * <p>Runs at {@code MONITOR} so every other plugin has had its say on the vanilla event first. A
     * message another plugin already cancelled is left alone.</p>
     *
     * @param event the vanilla chat event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncChat(final AsyncChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final DefaultChatChannel defaultChatChannel = this.chatManager.getDefaultChatChannel();
        if (defaultChatChannel == null) {
            return;
        }

        event.setCancelled(true);

        final Player player = event.getPlayer();

        UtilEvent.dispatch(new ChatSendEvent(player, UtilEvent.supply(new ChatChannelEvent(player, defaultChatChannel)).getChannel(), event.message()));
    }
}