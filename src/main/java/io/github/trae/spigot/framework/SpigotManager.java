package io.github.trae.spigot.framework;

import io.github.trae.hf.Manager;

/**
 * Spigot-specific marker interface for managers within the hierarchy framework.
 *
 * <p>Extends {@link Manager} and binds the plugin type to {@link SpigotPlugin},
 * ensuring all managers in a Spigot plugin hierarchy are associated with a valid
 * Spigot plugin instance.</p>
 *
 * @param <Plugin> the concrete plugin type
 */
public interface SpigotManager<Plugin extends SpigotPlugin> extends Manager<Plugin> {
}