package io.github.trae.spigot.framework;

import io.github.trae.di.InjectorApi;
import io.github.trae.hf.Plugin;
import io.github.trae.spigot.framework.command.abstracts.AbstractCommand;
import io.github.trae.spigot.framework.command.subcommand.abstracts.AbstractSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Base class for all Spigot plugins using the framework.
 *
 * <p>Extends {@link JavaPlugin} and implements {@link Plugin} from the hierarchy framework,
 * bridging the Bukkit plugin lifecycle with the component-based architecture. Automatically
 * handles registration and teardown of listeners, commands, and subcommands as components
 * are initialized and shut down through the hierarchy.</p>
 */
public abstract class SpigotPlugin extends JavaPlugin implements Plugin {

    /**
     * Initializes the plugin by setting the configuration directory to the
     * plugin's data folder and then running the hierarchy lifecycle.
     *
     * <p>Sets the configuration directory via
     * {@link InjectorApi#setConfigurationDirectory(java.nio.file.Path)} so that
     * {@link io.github.trae.di.configuration.annotations.Configuration @Configuration}
     * files are stored under the plugin's data folder, then delegates to
     * {@link Plugin#initializePlugin()} to trigger component discovery and
     * initialization.</p>
     */
    @Override
    public void initializePlugin() {
        InjectorApi.setConfigurationDirectory(this.getDataPath());

        Plugin.super.initializePlugin();
    }

    /**
     * Called when a component is initialized within the hierarchy.
     *
     * <p>Performs automatic Bukkit registration based on the component type:</p>
     * <ul>
     *     <li>{@link Listener} — registered with the Bukkit event system</li>
     *     <li>{@link AbstractCommand} — registered with the server's {@link org.bukkit.command.CommandMap}</li>
     *     <li>{@link AbstractSubCommand} — attached to its parent command's subcommand map</li>
     * </ul>
     *
     * @param instance the component being initialized
     */
    @Override
    public void onComponentInitialize(final Object instance) {
        if (instance instanceof final Listener listener) {
            Bukkit.getServer().getPluginManager().registerEvents(listener, this);
        }

        if (instance instanceof final AbstractCommand<?, ?, ?> abstractCommand) {
            Bukkit.getServer().getCommandMap().register(abstractCommand.getLabel(), this.getName(), abstractCommand);
        }

        if (instance instanceof final AbstractSubCommand<?, ?, ?> abstractSubCommand) {
            abstractSubCommand.getModule().addSubCommand(abstractSubCommand);
        }
    }

    /**
     * Called when a component is shut down within the hierarchy.
     *
     * <p>Performs automatic Bukkit deregistration based on the component type:</p>
     * <ul>
     *     <li>{@link Listener} — unregistered from all handler lists</li>
     *     <li>{@link AbstractCommand} — unregistered from the server's {@link org.bukkit.command.CommandMap}</li>
     *     <li>{@link AbstractSubCommand} — removed from its parent command's subcommand map</li>
     * </ul>
     *
     * @param instance the component being shut down
     */
    @Override
    public void onComponentShutdown(final Object instance) {
        if (instance instanceof final Listener listener) {
            HandlerList.unregisterAll(listener);
        }

        if (instance instanceof final AbstractCommand<?, ?, ?> abstractCommand) {
            abstractCommand.unregister(Bukkit.getServer().getCommandMap());
        }

        if (instance instanceof final AbstractSubCommand<?, ?, ?> abstractSubCommand) {
            abstractSubCommand.getModule().removeSubCommand(abstractSubCommand);
        }
    }
}