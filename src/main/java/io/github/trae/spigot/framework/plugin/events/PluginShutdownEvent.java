package io.github.trae.spigot.framework.plugin.events;

import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.event.CustomEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired before a {@link SpigotPlugin} begins its shutdown sequence.
 *
 * <p>Dispatched at the start of {@link SpigotPlugin#shutdownPlugin()} before
 * components are torn down through the hierarchy lifecycle. Listeners can use
 * this event to perform cleanup or persistence logic while the plugin's
 * services are still available.</p>
 */
@AllArgsConstructor
@Getter
public class PluginShutdownEvent extends CustomEvent {

    /**
     * The plugin that is about to shut down.
     */
    private final SpigotPlugin plugin;
}