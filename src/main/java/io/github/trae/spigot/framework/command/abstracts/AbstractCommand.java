package io.github.trae.spigot.framework.command.abstracts;

import io.github.trae.di.annotations.field.Inject;
import io.github.trae.di.annotations.type.DependsOn;
import io.github.trae.spigot.framework.SpigotManager;
import io.github.trae.spigot.framework.SpigotModule;
import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.command.abstracts.interfaces.IAbstractCommand;
import io.github.trae.spigot.framework.command.events.CommandExecuteEvent;
import io.github.trae.spigot.framework.command.events.CommandTabCompleteEvent;
import io.github.trae.spigot.framework.command.events.SubCommandExecuteEvent;
import io.github.trae.spigot.framework.command.settings.ICommandSettings;
import io.github.trae.spigot.framework.command.subcommand.abstracts.AbstractSubCommand;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.utilities.UtilJava;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Abstract base for all commands within the framework.
 *
 * <p>Extends Bukkit's {@link Command} and integrates with the hierarchy framework as a
 * {@link SpigotModule}. Provides built-in subcommand routing, sender type validation,
 * permission checking, and event dispatch at each stage of execution.</p>
 *
 * <p>Command settings (permission checks, messaging) are resolved via the dependency
 * injector through {@link ICommandSettings}. Consumers must register a concrete
 * implementation of {@link ICommandSettings} as a component in their plugin hierarchy.</p>
 *
 * <p>Execution flow:</p>
 * <ol>
 *     <li>If arguments are present, attempt to match and route to a registered subcommand</li>
 *     <li>Validate the sender type against {@link IAbstractCommand#getClassOfCommandSender()}</li>
 *     <li>Check permissions via {@link ICommandSettings#hasPermission}</li>
 *     <li>Fire a cancellable {@link CommandExecuteEvent} or {@link SubCommandExecuteEvent}</li>
 *     <li>Delegate to the concrete {@link IAbstractCommand#execute} implementation</li>
 * </ol>
 *
 * <p>Tab completion follows an identical validation and routing chain, firing
 * a {@link CommandTabCompleteEvent} before delegating to the concrete implementation.</p>
 *
 * @param <Plugin>  the concrete plugin type
 * @param <Manager> the concrete manager type
 * @param <Sender>  the required sender type for this command
 */
@DependsOn(values = ICommandSettings.class)
public abstract class AbstractCommand<Plugin extends SpigotPlugin, Manager extends SpigotManager<Plugin>, Sender extends CommandSender> extends Command implements SpigotModule<Plugin, Manager>, IAbstractCommand<Sender, AbstractSubCommand<?, ?, ?>> {

    @Getter(AccessLevel.PRIVATE)
    private final LinkedHashMap<String, AbstractSubCommand<?, ?, ?>> subCommandMap;

    @Inject
    private ICommandSettings commandSettings;

    /**
     * Constructs a new command with the given name, description, and aliases.
     *
     * <p>The usage message is automatically derived from the command name
     * in the format {@code /<name>}.</p>
     *
     * @param name        the primary label for this command
     * @param description a brief description of the command's purpose
     * @param aliases     alternative labels that trigger this command
     */
    public AbstractCommand(final String name, final String description, final List<String> aliases) {
        super(name, description, "/%s".formatted(name), aliases);

        this.subCommandMap = new LinkedHashMap<>();
    }

    /**
     * Returns an unmodifiable snapshot of all registered subcommands.
     *
     * @return an unmodifiable list of subcommands
     */
    @Override
    public List<AbstractSubCommand<?, ?, ?>> getSubCommands() {
        return List.copyOf(this.getSubCommandMap().values());
    }

    /**
     * Registers a subcommand under this command.
     *
     * <p>The subcommand's label is normalized to lowercase for case-insensitive matching.</p>
     *
     * @param subCommand the subcommand to register
     */
    @Override
    public void addSubCommand(final AbstractSubCommand<?, ?, ?> subCommand) {
        this.getSubCommandMap().put(subCommand.getLabel().toLowerCase(Locale.ROOT), subCommand);
    }

    /**
     * Removes a previously registered subcommand from this command.
     *
     * @param subCommand the subcommand to remove
     */
    @Override
    public void removeSubCommand(final AbstractSubCommand<?, ?, ?> subCommand) {
        this.getSubCommandMap().remove(subCommand.getLabel().toLowerCase(Locale.ROOT));
    }

    /**
     * Handles command execution with subcommand routing, validation, and event dispatch.
     *
     * <p>If the first argument matches a registered subcommand, execution is delegated
     * to that subcommand with the remaining arguments. Otherwise, the command's own
     * execution logic is invoked. Both paths enforce sender type validation, permission
     * checks via {@link ICommandSettings}, and fire cancellable events before proceeding.</p>
     *
     * @param sender the command sender
     * @param alias  the alias used to invoke this command
     * @param args   the arguments passed to this command
     * @return true if execution completed successfully, false if blocked
     */
    @Override
    public boolean execute(@NotNull final CommandSender sender, @NotNull final String alias, @NotNull final String @NotNull [] args) {
        if (args.length > 0) {
            final AbstractSubCommand<?, ?, ?> abstractSubCommand = this.getSubCommandMap().get(args[0].toLowerCase(Locale.ROOT));
            if (abstractSubCommand != null) {
                if (!(abstractSubCommand.getClassOfCommandSender().isInstance(sender))) {
                    this.commandSettings.sendInvalidCommandSenderMessage(abstractSubCommand, sender);
                    return false;
                }

                if (!(this.commandSettings.hasPermission(abstractSubCommand, sender))) {
                    this.commandSettings.sendInsufficientPermissionMessage(abstractSubCommand, sender);
                    return false;
                }

                if (UtilEvent.supply(new SubCommandExecuteEvent(abstractSubCommand, sender, args)).isCancelled()) {
                    return false;
                }

                abstractSubCommand._internalExecute(sender, Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
        }

        if (!(this.getClassOfCommandSender().isInstance(sender))) {
            this.commandSettings.sendInvalidCommandSenderMessage(this, sender);
            return false;
        }

        if (!(this.commandSettings.hasPermission(this, sender))) {
            this.commandSettings.sendInsufficientPermissionMessage(this, sender);
            return false;
        }

        if (UtilEvent.supply(new CommandExecuteEvent(this, sender, args)).isCancelled()) {
            return false;
        }

        this.execute(UtilJava.cast(this.getClassOfCommandSender(), sender), args);
        return true;
    }

    /**
     * Handles tab completion with subcommand routing, validation, and event dispatch.
     *
     * <p>If the first argument matches a registered subcommand, tab completion is
     * delegated to that subcommand. Otherwise, the command's own tab completion logic
     * is invoked. Both paths enforce sender type validation and permission checks
     * via {@link ICommandSettings}, returning an empty list if any check fails.</p>
     *
     * @param sender the command sender requesting tab completions
     * @param alias  the alias used to invoke this command
     * @param args   the current arguments being typed
     * @return a list of tab completion suggestions, or an empty list if blocked
     * @throws IllegalArgumentException if any parameter is null
     */
    @Override
    public @NotNull List<String> tabComplete(@NotNull final CommandSender sender, @NotNull final String alias, @NotNull final String @NotNull [] args) throws IllegalArgumentException {
        if (args.length > 0) {
            final AbstractSubCommand<?, ?, ?> abstractSubCommand = this.getSubCommandMap().get(args[0].toLowerCase(Locale.ROOT));
            if (abstractSubCommand != null) {
                if (!(abstractSubCommand.getClassOfCommandSender().isInstance(sender))) {
                    return Collections.emptyList();
                }

                if (!(this.commandSettings.hasPermission(abstractSubCommand, sender))) {
                    return Collections.emptyList();
                }

                if (UtilEvent.supply(new SubCommandExecuteEvent(abstractSubCommand, sender, args)).isCancelled()) {
                    return Collections.emptyList();
                }

                return abstractSubCommand._internalGetTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        if (!(this.getClassOfCommandSender().isInstance(sender))) {
            return Collections.emptyList();
        }

        if (!(this.commandSettings.hasPermission(this, sender))) {
            return Collections.emptyList();
        }

        if (UtilEvent.supply(new CommandTabCompleteEvent(this, sender, args)).isCancelled()) {
            return Collections.emptyList();
        }

        return this.getTabCompletion(UtilJava.cast(this.getClassOfCommandSender(), sender), args);
    }
}