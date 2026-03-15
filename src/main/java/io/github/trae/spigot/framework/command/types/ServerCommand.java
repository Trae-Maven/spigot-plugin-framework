package io.github.trae.spigot.framework.command.types;

import io.github.trae.spigot.framework.SpigotManager;
import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.command.abstracts.AbstractCommand;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;

/**
 * A command restricted to {@link ConsoleCommandSender} senders.
 *
 * <p>Convenience base class for commands that can only be executed from the
 * server console. Player and other sender types will be rejected during sender
 * validation, triggering the invalid sender message via
 * {@link io.github.trae.spigot.framework.command.settings.ICommandSettings}.</p>
 *
 * @param <Plugin>  the concrete plugin type
 * @param <Manager> the concrete manager type
 */
public abstract class ServerCommand<Plugin extends SpigotPlugin, Manager extends SpigotManager<Plugin>> extends AbstractCommand<Plugin, Manager, ConsoleCommandSender> {

    /**
     * Constructs a new console-only command with the given name, description, and aliases.
     *
     * @param name        the primary label for this command
     * @param description a brief description of the command's purpose
     * @param aliases     alternative labels that trigger this command
     */
    public ServerCommand(final String name, final String description, final List<String> aliases) {
        super(name, description, aliases);
    }

    @Override
    public Class<ConsoleCommandSender> getClassOfCommandSender() {
        return ConsoleCommandSender.class;
    }
}