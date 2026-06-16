package io.github.trae.spigot.framework.command.interfaces;

import io.github.trae.spigot.framework.command.BaseSubCommand;

import java.util.Optional;

/**
 * Defines the subcommand management contract for a top-level command.
 * <p>
 * Implemented by {@link io.github.trae.spigot.framework.command.BaseCommand} to expose
 * registration, removal, and lookup of {@link BaseSubCommand} instances.
 */
public interface IBaseCommand {

    /**
     * Registers a subcommand under this command.
     * <p>
     * The subcommand is indexed by its lowercase label for fast lookup.
     *
     * @param baseSubCommand the subcommand to register
     */
    void $addSubCommand(final BaseSubCommand<?, ?, ?> baseSubCommand);

    /**
     * Removes a previously registered subcommand from this command.
     *
     * @param baseSubCommand the subcommand to remove
     */
    void $removeSubCommand(final BaseSubCommand<?, ?, ?> baseSubCommand);

    /**
     * Looks up a registered subcommand by label or alias.
     * <p>
     * First attempts an exact label match (case-insensitive), then falls back to scanning
     * all registered subcommands for a matching alias.
     *
     * @param label the label or alias to look up
     * @return an {@link Optional} containing the matching subcommand, or empty if none found
     */
    Optional<BaseSubCommand<?, ?, ?>> getSubCommandByLabel(final String label);
}