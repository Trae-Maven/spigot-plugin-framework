package io.github.trae.spigot.framework.utility;

import lombok.experimental.UtilityClass;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Provides utility methods for working with Spigot {@link CommandSender command senders}.
 */
@UtilityClass
public class UtilCommandSender {

    private static final UUID CONSOLE_ID = new UUID(0L, 0L);
    private static final String CONSOLE_NAME = "Console";

    /**
     * Gets the unique identifier of a command sender.
     *
     * @param commandSender the command sender
     * @return the player's unique identifier, the console identifier for the console, or {@code null} if unsupported
     */
    public static UUID getCommandSenderId(final CommandSender commandSender) {
        if (commandSender instanceof final Player player) {
            return player.getUniqueId();
        }

        if (commandSender instanceof ConsoleCommandSender) {
            return CONSOLE_ID;
        }

        return null;
    }

    /**
     * Gets the name of a command sender.
     *
     * @param commandSender the command sender
     * @return the player's name, {@code Console} for the console, or {@code null} if unsupported
     */
    public static String getCommandSenderName(final CommandSender commandSender) {
        if (commandSender instanceof final Player player) {
            return player.getName();
        }

        if (commandSender instanceof ConsoleCommandSender) {
            return CONSOLE_NAME;
        }

        return null;
    }
}