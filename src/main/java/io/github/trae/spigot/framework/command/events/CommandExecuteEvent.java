package io.github.trae.spigot.framework.command.events;

import io.github.trae.spigot.framework.command.interfaces.SharedCommand;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.CommandSender;

/**
 * Fired before a command is executed via {@link SharedCommand#$execute}.
 * <p>
 * Cancelling this event prevents the command from being executed. Can be listened to
 * by any plugin to intercept or block command execution globally.
 */
@AllArgsConstructor
@Getter
public class CommandExecuteEvent extends CustomCancellableEvent {

    /**
     * The command that is about to be executed.
     */
    private final SharedCommand<?> command;

    /**
     * The sender attempting to execute the command.
     */
    private final CommandSender sender;
}