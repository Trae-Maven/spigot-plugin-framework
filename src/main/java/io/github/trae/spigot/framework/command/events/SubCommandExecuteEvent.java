package io.github.trae.spigot.framework.command.events;

import io.github.trae.spigot.framework.command.events.interfaces.ICommandEvent;
import io.github.trae.spigot.framework.command.subcommand.abstracts.AbstractSubCommand;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.CommandSender;

/**
 * Fired when a subcommand is about to be executed.
 *
 * <p>This cancellable event is dispatched after the parent command has routed
 * to the subcommand and sender type validation and permission checks have
 * passed, but before the subcommand's concrete execution logic is invoked.
 * Cancelling this event prevents execution.</p>
 */
@AllArgsConstructor
@Getter
public class SubCommandExecuteEvent extends CustomCancellableEvent implements ICommandEvent {

    /**
     * The subcommand being executed.
     */
    private final AbstractSubCommand<?, ?, ?> subCommand;

    /**
     * The sender who invoked the subcommand.
     */
    private final CommandSender sender;

    /**
     * The full arguments passed to the parent command.
     */
    private final String[] args;
}