package io.github.trae.spigot.framework.command.subcommand.types;

import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.command.abstracts.AbstractCommand;
import io.github.trae.spigot.framework.command.subcommand.abstracts.AbstractSubCommand;
import org.bukkit.command.CommandSender;

/**
 * A subcommand that accepts any {@link CommandSender} type.
 *
 * <p>Convenience base class for subcommands that can be executed by both players
 * and the console. Binds the sender type to {@link CommandSender}, allowing
 * all sender types to pass the sender validation check.</p>
 *
 * @param <Plugin> the concrete plugin type
 * @param <Module> the parent command type
 */
public abstract class SubCommand<Plugin extends SpigotPlugin, Module extends AbstractCommand<Plugin, ?, ?>> extends AbstractSubCommand<Plugin, Module, CommandSender> {

    /**
     * Constructs a new subcommand with the given label and description.
     *
     * @param label       the subcommand label used for routing
     * @param description a brief description of the subcommand's purpose
     */
    public SubCommand(final String label, final String description) {
        super(label, description);
    }

    @Override
    public Class<CommandSender> getClassOfCommandSender() {
        return CommandSender.class;
    }
}