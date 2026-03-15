package io.github.trae.spigot.framework;

import io.github.trae.hf.SubModule;

/**
 * Spigot-specific marker interface for sub-modules within the hierarchy framework.
 *
 * <p>Extends {@link SubModule} and binds the plugin and module types to their Spigot
 * counterparts. Sub-modules represent the lowest level of the hierarchy, such as
 * subcommands attached to a parent command module.</p>
 *
 * @param <Plugin> the concrete plugin type
 * @param <Module> the concrete module type
 */
public interface SpigotSubModule<Plugin extends SpigotPlugin, Module extends SpigotModule<Plugin, ?>> extends SubModule<Plugin, Module> {
}