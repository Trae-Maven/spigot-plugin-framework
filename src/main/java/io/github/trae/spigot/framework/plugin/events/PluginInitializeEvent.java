package io.github.trae.spigot.framework.plugin.events;

import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.event.CustomEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired after a {@link SpigotPlugin} has completed initialization.
 *
 * <p>Dispatched at the end of {@link SpigotPlugin#initializePlugin()} after
 * all components have been discovered, wired, and initialized through the
 * hierarchy lifecycle. Listeners can use this event to perform post-startup
 * logic that depends on the plugin being fully ready.</p>
 */
@AllArgsConstructor
@Getter
public class PluginInitializeEvent extends CustomEvent {

    /**
     * The plugin that has finished initializing.
     */
    private final SpigotPlugin plugin;
}