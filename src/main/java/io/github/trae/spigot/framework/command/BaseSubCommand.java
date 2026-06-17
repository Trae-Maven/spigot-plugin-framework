package io.github.trae.spigot.framework.command;

import io.github.trae.hf.SubModule;
import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.command.interfaces.SharedBaseCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Abstract base class for subcommands belonging to a {@link BaseCommand}.
 * <p>
 * Combines {@link SubModule} and {@link SharedBaseCommand} to provide a typed subcommand
 * with its own sender validation, permission check, and tab-complete logic. Subcommands
 * are registered into their parent via {@link io.github.trae.spigot.framework.command.interfaces.IBaseCommand#$addSubCommand}.
 *
 * @param <Plugin>        the plugin type this subcommand belongs to
 * @param <ParentCommand> the parent {@link BaseCommand} type
 * @param <Sender>        the expected {@link CommandSender} type
 */
@AllArgsConstructor
@Getter
public abstract class BaseSubCommand<Plugin extends SpigotPlugin, ParentCommand extends BaseCommand<Plugin, ?, ?>, Sender extends CommandSender> implements SubModule<Plugin, ParentCommand>, SharedBaseCommand<Sender> {

    private final String label, description;
    private final List<String> aliases;
    private final String permission;

    /**
     * Constructs a subcommand without a permission node.
     * <p>
     * Equivalent to calling {@link #BaseSubCommand(String, String, List, String)} with {@code null}
     * as the permission, meaning all senders of the correct type may execute it.
     *
     * @param label       the primary label used to invoke this subcommand
     * @param description a short description of this subcommand
     * @param aliases     alternative labels for this subcommand
     */
    public BaseSubCommand(final String label, final String description, final List<String> aliases) {
        this(label, description, aliases, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code <parentUsage> <label>}, e.g. {@code /faction create}.
     */
    @Override
    public String getUsage() {
        return "%s %s".formatted(this.getModule().getUsage(), this.getLabel());
    }
}