package io.github.trae.spigot.framework.command;

import io.github.trae.hf.Manager;
import io.github.trae.hf.Module;
import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.command.constants.DefaultSuggestions;
import io.github.trae.spigot.framework.command.interfaces.IBaseCommand;
import io.github.trae.spigot.framework.command.interfaces.SharedBaseCommand;
import io.github.trae.spigot.framework.command.wrappers.SpigotCommandWrapper;
import lombok.Getter;
import org.bukkit.command.CommandSender;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Abstract base class for top-level plugin commands.
 * <p>
 * Combines {@link Module}, {@link SharedBaseCommand}, and {@link IBaseCommand} to provide
 * a fully typed, self-registering command with built-in subcommand management.
 * A {@link SpigotCommandWrapper} is created automatically on construction and used
 * to bridge this command into Bukkit's command map.
 *
 * @param <Plugin>        the plugin type this command belongs to
 * @param <SpigotManager> the manager type this command is scoped to
 * @param <Sender>        the expected {@link CommandSender} type
 */
@Getter
public abstract class BaseCommand<Plugin extends SpigotPlugin, SpigotManager extends Manager<Plugin>, Sender extends CommandSender> implements Module<Plugin, SpigotManager>, SharedBaseCommand<Sender>, IBaseCommand {

    private final String label, description;
    private final List<String> aliases;
    private final String permission;

    private final LinkedHashMap<String, BaseSubCommand<?, ?, ?>> subCommands = new LinkedHashMap<>();

    private final SpigotCommandWrapper spigotCommandWrapper;

    /**
     * Constructs a command with a permission node.
     *
     * @param label       the primary command label
     * @param description a short description of the command
     * @param aliases     alternative labels for this command
     * @param permission  the permission node required to execute this command
     */
    public BaseCommand(final String label, final String description, final List<String> aliases, final String permission) {
        this.label = label;
        this.description = description;
        this.permission = permission;
        this.aliases = aliases;

        this.spigotCommandWrapper = new SpigotCommandWrapper(this);
    }

    /**
     * Constructs a command without a permission node.
     * <p>
     * Equivalent to calling {@link #BaseCommand(String, String, List, String)} with {@code null}
     * as the permission, meaning all senders of the correct type may execute it.
     *
     * @param label       the primary command label
     * @param description a short description of the command
     * @param aliases     alternative labels for this command
     */
    public BaseCommand(final String label, final String description, final List<String> aliases) {
        this(label, description, aliases, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For the first argument, suggests the labels of registered subcommands; deeper arguments fall
     * back to the {@link SharedBaseCommand} default.
     */
    @Override
    public List<String> getTabComplete(final Sender sender, final String[] args) {
        if (args.length == 1) {
            return DefaultSuggestions.SUB_COMMANDS.apply(this, sender, args[0]);
        }

        return SharedBaseCommand.super.getTabComplete(sender, args);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code /<label>}.
     */
    @Override
    public String getUsage() {
        return "/%s".formatted(this.getLabel());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Indexes the subcommand by its lowercase label.
     */
    @Override
    public void $addSubCommand(final BaseSubCommand<?, ?, ?> baseSubCommand) {
        this.subCommands.put(baseSubCommand.getLabel().toLowerCase(Locale.ROOT), baseSubCommand);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void $removeSubCommand(final BaseSubCommand<?, ?, ?> baseSubCommand) {
        this.subCommands.remove(baseSubCommand.getLabel().toLowerCase(Locale.ROOT));
    }

    /**
     * {@inheritDoc}
     * <p>
     * First checks the label index directly, then scans all subcommand aliases as a fallback.
     */
    @Override
    public Optional<BaseSubCommand<?, ?, ?>> getSubCommandByLabel(final String label) {
        final String lowerLabel = label.toLowerCase(Locale.ROOT);

        final BaseSubCommand<?, ?, ?> baseSubCommand = this.subCommands.get(lowerLabel);
        if (baseSubCommand != null) {
            return Optional.of(baseSubCommand);
        }

        return this.subCommands.values().stream().filter(value -> value.getAliases().contains(lowerLabel)).findFirst();
    }
}