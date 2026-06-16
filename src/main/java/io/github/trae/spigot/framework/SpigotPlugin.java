package io.github.trae.spigot.framework;

import io.github.trae.di.InjectorApi;
import io.github.trae.hf.Plugin;
import io.github.trae.spigot.framework.command.BaseCommand;
import io.github.trae.spigot.framework.command.BaseSubCommand;
import io.github.trae.spigot.framework.plugin.events.PluginInitializeEvent;
import io.github.trae.spigot.framework.plugin.events.PluginShutdownEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.UtilPlugin;
import io.github.trae.spigot.framework.utility.UtilTask;
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
     * Creates a new {@link SpigotPlugin} and configures the dependency
     * injection framework for this application.
     *
     * <p>Registers this plugin's data directory as the configuration
     * directory for {@link io.github.trae.di.configuration.annotations.Configuration @Configuration}
     * file resolution, and sets up the per-application synchronous and
     * asynchronous executors for dispatching
     * {@link io.github.trae.di.annotations.method.Scheduler @Scheduler}
     * tasks onto the Bukkit thread pool via {@link UtilTask}.</p>
     */
    public SpigotPlugin() {
        InjectorApi.setConfigurationDirectory(this.getClass(), this.getDataPath());

        InjectorApi.setSynchronousExecutor(this.getClass(), UtilTask::executeSynchronous);
        InjectorApi.setAsynchronousExecutor(this.getClass(), UtilTask::executeAsynchronous);
    }

    /**
     * Initializes the plugin by running the hierarchy lifecycle via
     * {@link Plugin#initializePlugin()}, then dispatching a
     * {@link PluginInitializeEvent} to notify listeners that the
     * plugin is fully initialized.
     */
    @Override
    public void initializePlugin() {
        Plugin.super.initializePlugin();

        UtilEvent.dispatch(new PluginInitializeEvent(this));

        UtilPlugin.addInternalPlugin(this);
    }

    /**
     * Dispatches a {@link PluginShutdownEvent} to notify listeners
     * that the plugin is about to shut down, then runs the hierarchy
     * teardown via {@link Plugin#shutdownPlugin()}.
     */
    @Override
    public void shutdownPlugin() {
        UtilEvent.dispatch(new PluginShutdownEvent(this));

        Plugin.super.shutdownPlugin();

        UtilPlugin.removeInternalPlugin(this);
    }

    /**
     * Called when a component is initialized within the hierarchy.
     *
     * <p>Delegates to {@link Plugin#onComponentInitialize(Object)} to
     * invoke {@link io.github.trae.hf.Frame#initializeFrame()}, then
     * performs automatic Bukkit registration based on the component type:</p>
     * <ul>
     *     <li>{@link Listener} — registered with the Bukkit event system</li>
     *     <li>{@link BaseCommand} — registered with the server's {@link org.bukkit.command.CommandMap}</li>
     *     <li>{@link BaseSubCommand} — attached to its parent command's subcommand map</li>
     * </ul>
     *
     * @param instance the component being initialized
     */
    @Override
    public void onComponentInitialize(final Object instance) {
        Plugin.super.onComponentInitialize(instance);

        if (instance instanceof final Listener listener) {
            Bukkit.getServer().getPluginManager().registerEvents(listener, this);
        }

        if (instance instanceof final BaseCommand<?, ?, ?> baseCommand) {
            Bukkit.getServer().getCommandMap().register(baseCommand.getLabel(), this.getName(), baseCommand.getSpigotCommandWrapper());
        }

        if (instance instanceof final BaseSubCommand<?, ?, ?> baseSubCommand) {
            baseSubCommand.getModule().$addSubCommand(baseSubCommand);
        }
    }

    /**
     * Called when a component is shut down within the hierarchy.
     *
     * <p>Delegates to {@link Plugin#onComponentShutdown(Object)} to
     * invoke {@link io.github.trae.hf.Frame#shutdownFrame()}, then
     * performs automatic Bukkit deregistration based on the component type:</p>
     * <ul>
     *     <li>{@link Listener} — unregistered from all handler lists</li>
     *     <li>{@link BaseCommand} — unregistered from the server's {@link org.bukkit.command.CommandMap}</li>
     *     <li>{@link BaseSubCommand} — removed from its parent command's subcommand map</li>
     * </ul>
     *
     * @param instance the component being shut down
     */
    @Override
    public void onComponentShutdown(final Object instance) {
        if (instance instanceof final Listener listener) {
            HandlerList.unregisterAll(listener);
        }

        if (instance instanceof final BaseCommand<?, ?, ?> baseCommand) {
            baseCommand.getSpigotCommandWrapper().unregister(Bukkit.getServer().getCommandMap());
        }

        if (instance instanceof final BaseSubCommand<?, ?, ?> baseSubCommand) {
            baseSubCommand.getModule().$removeSubCommand(baseSubCommand);
        }

        Plugin.super.onComponentShutdown(instance);
    }
}