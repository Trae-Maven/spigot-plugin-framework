package io.github.trae.spigot.framework.chat;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.chat.channel.DefaultChatChannel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Holds what the chat system needs to route a message.
 *
 * <p>The chat system takes vanilla chat over entirely, so it cannot run without somewhere to send a
 * message. That somewhere is the plugin's {@link DefaultChatChannel}, injected here.</p>
 *
 * @see io.github.trae.spigot.framework.chat.listeners.PreChatListener
 */
@RequiredArgsConstructor
@Getter
@Singleton
public class ChatManager {

    /**
     * The channel every message starts in, supplied by the plugin that owns chat.
     */
    private final DefaultChatChannel defaultChatChannel;
}