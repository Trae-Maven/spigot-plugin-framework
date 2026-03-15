package io.github.trae.spigot.framework.command.subcommand.types;

import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.command.abstracts.AbstractCommand;
import io.github.trae.spigot.framework.command.subcommand.abstracts.AbstractSubCommand;
import org.bukkit.command.ConsoleCommandSender;

/**
 * A subcommand restricted to {@link ConsoleCommandSender} senders.
 *
 * <p>Convenience base class for subcommands that can only be executed from the
 * server console. Player and other sender types will be rejected during sender
 * validation in the parent {@link AbstractCommand}'s execution chain.</p>
 *
 * @param <Plugin> the concrete plugin type
 * @param <Module> the parent command type
 */
public abstract class ServerSubCommand<Plugin extends SpigotPlugin, Module extends AbstractCommand<Plugin, ?, ?>> extends AbstractSubCommand<Plugin, Module, ConsoleCommandSender> {

    /**
     * Constructs a new console-only subcommand with the given label and description.
     *
     * @param label       the subcommand label used for routing
     * @param description a brief description of the subcommand's purpose
     */
    public ServerSubCommand(final String label, final String description) {
        super(label, description);
    }

    @Override
    public Class<ConsoleCommandSender> getClassOfCommandSender() {
        return ConsoleCommandSender.class;
    }
}