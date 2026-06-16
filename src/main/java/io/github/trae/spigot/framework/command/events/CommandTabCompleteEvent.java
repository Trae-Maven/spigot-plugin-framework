package io.github.trae.spigot.framework.command.events;

import io.github.trae.spigot.framework.command.interfaces.SharedBaseCommand;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.CommandSender;

/**
 * Fired before tab-complete suggestions are returned via {@link SharedBaseCommand#$getTabComplete}.
 * <p>
 * Cancelling this event causes an empty suggestion list to be returned. Can be listened to
 * by any plugin to intercept or suppress tab-completion globally.
 */
@AllArgsConstructor
@Getter
public class CommandTabCompleteEvent extends CustomCancellableEvent {

    /**
     * The command whose tab-completion is being requested.
     */
    private final SharedBaseCommand<?> command;

    /**
     * The sender requesting tab-complete suggestions.
     */
    private final CommandSender sender;
}