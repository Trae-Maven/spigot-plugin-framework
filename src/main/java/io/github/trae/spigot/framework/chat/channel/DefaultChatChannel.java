package io.github.trae.spigot.framework.chat.channel;

/**
 * The channel every message starts in before any plugin moves it elsewhere.
 *
 * <p>Exactly one implementation is expected, declared as a {@code @Singleton} by the plugin that owns
 * chat, and injected into {@link io.github.trae.spigot.framework.chat.ChatManager}. It is what
 * {@link io.github.trae.spigot.framework.chat.events.ChatChannelEvent} is seeded with, so a player
 * whose state names no other channel chats here.</p>
 *
 * <p>A separate type rather than a flag on {@link ChatChannel}, so the injector can tell the default
 * apart from every other channel by type alone.</p>
 */
public interface DefaultChatChannel extends ChatChannel {
}