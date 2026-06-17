package io.github.trae.spigot.framework.command.wrappers;

import io.github.trae.spigot.framework.command.BaseCommand;
import io.github.trae.spigot.framework.command.BaseSubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Bukkit-facing wrapper that bridges a {@link BaseCommand} into the server's command map.
 * <p>
 * Registered automatically by {@link BaseCommand} on construction. Delegates execution and
 * tab-completion to the appropriate {@link BaseSubCommand} if the first argument matches a
 * registered subcommand label or alias, otherwise falls through to the parent command.
 */
public class SpigotCommandWrapper extends BukkitCommand {

    private final BaseCommand<?, ?, ?> baseCommand;

    /**
     * Constructs a wrapper for the given {@link BaseCommand}, forwarding its label,
     * description, usage, and aliases to the underlying {@link BukkitCommand}.
     *
     * @param baseCommand the command to wrap
     */
    public SpigotCommandWrapper(final BaseCommand<?, ?, ?> baseCommand) {
        super(baseCommand.getLabel(), baseCommand.getDescription(), baseCommand.getUsage(), baseCommand.getAliases());

        this.baseCommand = baseCommand;
    }

    /**
     * Routes execution to a matching subcommand if {@code args[0]} resolves to one,
     * stripping the subcommand label from the argument array before delegating.
     * Falls through to the parent command if no subcommand matches.
     *
     * @param commandSender the sender executing the command
     * @param commandLabel  the label used to invoke the command
     * @param args          the command arguments
     * @return {@code true} if execution was handled successfully
     */
    @Override
    public boolean execute(@NotNull final CommandSender commandSender, @NotNull final String commandLabel, @NotNull final String @NotNull [] args) {
        if (args.length > 0) {
            final Optional<BaseSubCommand<?, ?, ?>> baseSubCommandOptional = this.baseCommand.getSubCommandByLabel(args[0]);
            if (baseSubCommandOptional.isPresent()) {
                return baseSubCommandOptional.get().$execute(commandSender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return this.baseCommand.$execute(commandSender, args);
    }

    /**
     * Routes tab-completion to a matching subcommand if {@code args[0]} resolves to one,
     * stripping the subcommand label from the argument array before delegating.
     * Falls through to the parent command if no subcommand matches.
     *
     * @param commandSender the sender requesting completions
     * @param alias         the alias used to invoke the command
     * @param args          the current argument input
     * @return a list of suggestions
     */
    @Override
    public @NotNull List<String> tabComplete(@NotNull final CommandSender commandSender, @NotNull final String alias, @NotNull final String @NotNull [] args) throws IllegalArgumentException {
        if (args.length > 0) {
            final Optional<BaseSubCommand<?, ?, ?>> baseSubCommandOptional = this.baseCommand.getSubCommandByLabel(args[0]);
            if (baseSubCommandOptional.isPresent()) {
                return baseSubCommandOptional.get().$getTabComplete(commandSender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return this.baseCommand.$getTabComplete(commandSender, args);
    }
}