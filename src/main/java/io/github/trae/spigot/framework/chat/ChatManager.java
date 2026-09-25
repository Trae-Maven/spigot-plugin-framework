package io.github.trae.spigot.framework.chat;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.chat.channel.ChatChannel;
import io.github.trae.spigot.framework.chat.channel.DefaultChatChannel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Holds what the chat system needs to route a message.
 *
 * <p>The chat system takes vanilla chat over entirely, so it cannot run without somewhere to send a
 * message. That somewhere is the plugin's {@link DefaultChatChannel}, injected here.</p>
 *
 * @see io.github.trae.spigot.framework.chat.listeners.PreChatListener
 */
@RequiredArgsConstructor
@Singleton
public final class ChatManager {

    /**
     * The channel every message starts in, supplied by the plugin that owns chat.
     */
    @Getter
    private final DefaultChatChannel defaultChatChannel;

    /**
     * Stores chat channels by their normalised names.
     *
     * <p>The map is populated lazily from all registered {@link ChatChannel}
     * implementations when a channel is first looked up.</p>
     */
    private Map<String, ChatChannel> chatChannelMap;

    /**
     * Gets a registered chat channel by name.
     *
     * <p>Channel names are matched case-insensitively. The channel map is lazily
     * populated from all {@link ChatChannel} implementations registered with the
     * injector on the first lookup.</p>
     *
     * @param name the name of the chat channel
     * @return the matching chat channel, or {@link Optional#empty()} if the name is {@code null} or no channel is registered with that name
     */
    public Optional<ChatChannel> getChatChannelByName(final String name) {
        if (name == null) {
            return Optional.empty();
        }

        if (this.chatChannelMap == null) {
            this.chatChannelMap = new HashMap<>();

            InjectorApi.getAll(ChatChannel.class).forEach(chatChannel -> this.chatChannelMap.put(chatChannel.getName().toUpperCase(Locale.ROOT), chatChannel));
        }

        return Optional.ofNullable(this.chatChannelMap.get(name.toUpperCase(Locale.ROOT)));
    }
}