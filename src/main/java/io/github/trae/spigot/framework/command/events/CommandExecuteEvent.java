package io.github.trae.spigot.framework.command.events;

import io.github.trae.spigot.framework.command.abstracts.AbstractCommand;
import io.github.trae.spigot.framework.command.events.interfaces.ICommandEvent;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.CommandSender;

/**
 * Fired when a root command is about to be executed.
 *
 * <p>This cancellable event is dispatched after sender type validation and
 * permission checks have passed, but before the command's concrete execution
 * logic is invoked. Cancelling this event prevents execution.</p>
 */
@AllArgsConstructor
@Getter
public class CommandExecuteEvent extends CustomCancellableEvent implements ICommandEvent {

    /**
     * The command being executed.
     */
    private final AbstractCommand<?, ?, ?> command;

    /**
     * The sender who invoked the command.
     */
    private final CommandSender sender;

    /**
     * The arguments passed to the command.
     */
    private final String[] args;
}