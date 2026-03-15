package io.github.trae.spigot.framework.command.settings;

import io.github.trae.spigot.framework.command.abstracts.interfaces.ISharedCommand;
import io.github.trae.utilities.UtilString;
import org.bukkit.command.CommandSender;

/**
 * Interface for command settings within the framework.
 *
 * <p>Defines permission checking and messaging behaviour for all commands and
 * subcommands. Consumers must provide a concrete implementation of this interface
 * as a component in their plugin hierarchy, which is then resolved via the
 * dependency injector into {@link io.github.trae.spigot.framework.command.abstracts.AbstractCommand}.</p>
 *
 * <p>The default {@link #hasPermission} implementation grants access if the command
 * has no permission set, or if the sender is an operator or holds the required permission.</p>
 */
public interface ICommandSettings {

    /**
     * Checks whether the given sender has permission to execute the command.
     *
     * <p>Returns true if the command has no permission set (null or empty),
     * or if the sender is an operator, or if the sender holds the required permission.</p>
     *
     * @param command the command or subcommand being checked
     * @param sender  the sender attempting to execute the command
     * @return true if the sender has permission, false otherwise
     */
    default boolean hasPermission(final ISharedCommand<?> command, final CommandSender sender) {
        final String permission = command.getPermission();
        if (UtilString.isEmpty(permission)) {
            return true;
        }

        return sender.isOp() || sender.hasPermission(permission);
    }

    /**
     * Sends a message to the sender indicating that the command cannot be
     * executed by their sender type.
     *
     * <p>Called when a sender attempts to execute a command that requires a
     * different sender type, such as a console sender executing a player-only command.</p>
     *
     * @param command the command or subcommand that was attempted
     * @param sender  the sender who attempted the command
     */
    void sendInvalidCommandSenderMessage(final ISharedCommand<?> command, final CommandSender sender);

    /**
     * Sends a message to the sender indicating that they lack the required
     * permission to execute the command.
     *
     * @param command the command or subcommand that was attempted
     * @param sender  the sender who attempted the command
     */
    void sendInsufficientPermissionMessage(final ISharedCommand<?> command, final CommandSender sender);
}