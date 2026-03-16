package io.github.trae.spigot.framework.command.subcommand.types;

import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.command.abstracts.AbstractCommand;
import io.github.trae.spigot.framework.command.subcommand.abstracts.AbstractSubCommand;
import org.bukkit.entity.Player;

/**
 * A subcommand restricted to {@link Player} senders.
 *
 * <p>Convenience base class for subcommands that can only be executed by in-game
 * players. Console and other sender types will be rejected during sender
 * validation in the parent {@link AbstractCommand}'s execution chain.</p>
 *
 * @param <BasePlugin> the concrete plugin type
 * @param <BaseModule> the parent command type
 */
public abstract class PlayerSubCommand<BasePlugin extends SpigotPlugin, BaseModule extends AbstractCommand<BasePlugin, ?, ?>> extends AbstractSubCommand<BasePlugin, BaseModule, Player> {

    /**
     * Constructs a new player-only subcommand with the given label and description.
     *
     * @param label       the subcommand label used for routing
     * @param description a brief description of the subcommand's purpose
     */
    public PlayerSubCommand(final String label, final String description) {
        super(label, description);
    }

    @Override
    public Class<Player> getClassOfCommandSender() {
        return Player.class;
    }
}