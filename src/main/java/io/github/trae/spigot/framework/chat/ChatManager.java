package io.github.trae.spigot.framework.chat;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.chat.channel.ChatChannel;
import io.github.trae.spigot.framework.chat.channel.DefaultChatChannel;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.intellij.lang.annotations.Subst;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Holds what the chat system needs to route and render a message.
 *
 * <p>Every registered {@link ChatChannel} is collected once the application is ready. When several share
 * a name, the one with the lowest priority is kept, and the lowest-priority {@link DefaultChatChannel}
 * is the one every message starts in.</p>
 *
 * @see io.github.trae.spigot.framework.chat.listeners.PreChatListener
 */
@Singleton
public final class ChatManager implements Listener {

    /**
     * Matches a simple tag, capturing its name.
     */
    private static final Pattern TAG_PATTERN = Pattern.compile("<([a-z0-9_-]+)>");

    /**
     * The channels by their upper-cased names, the lowest priority kept for each.
     */
    private Map<String, ChatChannel> chatChannelMap = Map.of();

    /**
     * The channel every message starts in, or {@code null} if no plugin supplies one.
     */
    @Getter
    private DefaultChatChannel defaultChatChannel;

    /**
     * Collects every registered channel, keeping the lowest priority for each name.
     */
    @EventHandler
    public void onServerLoad(final ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }

        this.chatChannelMap = Map.copyOf(InjectorApi.getAll(ChatChannel.class).stream().collect(Collectors.toMap(chatChannel -> chatChannel.getName().toUpperCase(Locale.ROOT), Function.identity(), BinaryOperator.minBy(Comparator.comparingInt(ChatChannel::getPriority)))));

        this.defaultChatChannel = InjectorApi.getAll(DefaultChatChannel.class).stream()
                .min(Comparator.comparingInt(ChatChannel::getPriority))
                .orElse(null);
    }

    /**
     * Gets a registered chat channel by name.
     *
     * <p>Channel names are matched case-insensitively. When several channels share the name, the one
     * with the lowest priority is returned.</p>
     *
     * @param name the name of the chat channel
     * @return the matching chat channel, or {@link Optional#empty()} if the name is {@code null} or no channel is registered with that name
     */
    public Optional<ChatChannel> getChatChannelByName(final String name) {
        if (name == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.chatChannelMap.get(name.toUpperCase(Locale.ROOT)));
    }

    /**
     * Renders one recipient's line from a channel's format and its placeholder values.
     *
     * <p>Every tag in the format that is not a standard MiniMessage tag is a placeholder, and its value
     * is asked for once. Values are inserted as components, so nothing a player types is parsed. A
     * placeholder whose value is {@code null} is removed from the format along with one adjacent space,
     * so an absent part leaves no gap.</p>
     *
     * @param chatChannel the channel the message was sent in
     * @param sender      the player sending the message
     * @param recipient   the player receiving this copy
     * @param message     the message
     * @return the line
     */
    public Component render(final ChatChannel chatChannel, final Player sender, final Player recipient, final Component message) {
        String format = chatChannel.getFormat();

        final List<String> keyList = TAG_PATTERN.matcher(format).results()
                .map(matchResult -> matchResult.group(1))
                .distinct()
                .filter(key -> !TagResolver.standard().has(key))
                .toList();

        final TagResolver.Builder tagResolverBuilder = TagResolver.builder();

        for (@Subst("placeholder") final String key : keyList) {
            final Component value = chatChannel.getPlaceholderValue(key, sender, recipient, message);

            if (value == null) {
                final String tag = "<%s>".formatted(key);

                format = format.replace("%s ".formatted(tag), "").replace(" %s".formatted(tag), "").replace(tag, "");
                continue;
            }

            tagResolverBuilder.resolver(Placeholder.component(key, value));
        }

        return MiniMessage.miniMessage().deserialize(format, tagResolverBuilder.build());
    }
}