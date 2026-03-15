package io.github.trae.spigot.framework.command.events;

import io.github.trae.spigot.framework.command.abstracts.AbstractCommand;
import io.github.trae.spigot.framework.command.events.interfaces.ICommandEvent;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.CommandSender;

/**
 * Fired when a root command's tab completion is about to be processed.
 *
 * <p>This cancellable event is dispatched after sender type validation and
 * permission checks have passed, but before the command's concrete tab
 * completion logic is invoked. Cancelling this event returns an empty
 * completion list to the sender.</p>
 */
@AllArgsConstructor
@Getter
public class CommandTabCompleteEvent extends CustomCancellableEvent implements ICommandEvent {

    /**
     * The command being tab-completed.
     */
    private final AbstractCommand<?, ?, ?> command;

    /**
     * The sender requesting tab completions.
     */
    private final CommandSender sender;

    /**
     * The current arguments being typed.
     */
    private final String[] args;
}