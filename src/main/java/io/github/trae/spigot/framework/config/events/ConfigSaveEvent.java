package io.github.trae.spigot.framework.config.events;

import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.event.CustomEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired after a configuration belonging to a {@link SpigotPlugin}
 * has been successfully saved.
 *
 * <p>Provides the plugin that owns the configuration and the
 * configuration class that was saved.</p>
 */
@AllArgsConstructor
@Getter
public final class ConfigSaveEvent extends CustomEvent {

    /**
     * The plugin that owns the saved configuration.
     */
    private final SpigotPlugin plugin;

    /**
     * The configuration class that was saved.
     */
    private final Class<?> configurationClass;
}