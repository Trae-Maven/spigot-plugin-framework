package io.github.trae.spigot.framework.command.events;

import io.github.trae.spigot.framework.command.events.interfaces.ICommandEvent;
import io.github.trae.spigot.framework.command.subcommand.abstracts.AbstractSubCommand;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.CommandSender;

/**
 * Fired when a subcommand's tab completion is about to be processed.
 *
 * <p>This cancellable event is dispatched after the parent command has routed
 * to the subcommand and sender type validation and permission checks have
 * passed, but before the subcommand's concrete tab completion logic is invoked.
 * Cancelling this event returns an empty completion list to the sender.</p>
 */
@AllArgsConstructor
@Getter
public class SubCommandTabCompleteEvent extends CustomCancellableEvent implements ICommandEvent {

    /**
     * The subcommand being tab-completed.
     */
    private final AbstractSubCommand<?, ?, ?> subCommand;

    /**
     * The sender requesting tab completions.
     */
    private final CommandSender sender;

    /**
     * The full arguments passed to the parent command.
     */
    private final String[] args;
}