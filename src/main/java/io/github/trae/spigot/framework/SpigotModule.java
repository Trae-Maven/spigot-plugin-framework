package io.github.trae.spigot.framework;

import io.github.trae.hf.Module;

/**
 * Spigot-specific marker interface for modules within the hierarchy framework.
 *
 * <p>Extends {@link Module} and binds the plugin and manager types to their Spigot
 * counterparts, ensuring type safety across the component hierarchy. Modules represent
 * discrete features or systems within a manager, such as commands or listeners.</p>
 *
 * @param <Plugin>  the concrete plugin type
 * @param <Manager> the concrete manager type
 */
public interface SpigotModule<Plugin extends SpigotPlugin, Manager extends SpigotManager<Plugin>> extends Module<Plugin, Manager> {
}