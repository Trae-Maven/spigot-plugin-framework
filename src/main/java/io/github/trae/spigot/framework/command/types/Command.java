package io.github.trae.spigot.framework.command.types;

import io.github.trae.spigot.framework.SpigotManager;
import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.command.abstracts.AbstractCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * A command that accepts any {@link CommandSender} type.
 *
 * <p>Convenience base class for commands that can be executed by both players
 * and the console. Binds the sender type to {@link CommandSender}, allowing
 * all sender types to pass the sender validation check.</p>
 *
 * @param <Plugin>  the concrete plugin type
 * @param <Manager> the concrete manager type
 */
public abstract class Command<Plugin extends SpigotPlugin, Manager extends SpigotManager<Plugin>> extends AbstractCommand<Plugin, Manager, CommandSender> {

    /**
     * Constructs a new command with the given name, description, and aliases.
     *
     * @param name        the primary label for this command
     * @param description a brief description of the command's purpose
     * @param aliases     alternative labels that trigger this command
     */
    public Command(final String name, final String description, final List<String> aliases) {
        super(name, description, aliases);
    }

    @Override
    public Class<CommandSender> getClassOfCommandSender() {
        return CommandSender.class;
    }
}