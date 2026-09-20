package io.github.trae.spigot.framework.chat.events;

import io.github.trae.spigot.framework.chat.channel.ChatChannel;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * A player is switching which channel they chat in.
 *
 * <p>Dispatched by the plugin, typically from a command such as {@code /staffchat},
 * {@code /factionchat} or {@code /allychat}, so every switch passes through one event whichever
 * command caused it. The framework does not act on it: the plugin records the new channel in its own
 * state once the event survives, and resolves it from there on {@link ChatChannelEvent}.</p>
 *
 * <p>Cancel to refuse the switch, such as a player without the rank for staff chat or with no
 * faction to chat in.</p>
 */
@AllArgsConstructor
@Getter
public final class ChatSwitchChannelEvent extends CustomCancellableEvent {

    /**
     * The player switching channels.
     */
    private final Player player;

    /**
     * The channel they are switching to.
     */
    private final ChatChannel channel;
}