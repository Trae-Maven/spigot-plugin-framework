package io.github.trae.spigot.framework.utility;

import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.utility.search.types.InternalPluginSearchEngine;
import io.github.trae.utilities.UtilJava;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Utility class for registering and retrieving active {@link SpigotPlugin} instances.
 *
 * <p>Maintains an internal registry of framework plugins keyed by upper-cased plugin name,
 * exposing direct lookup by name and name-style searching backed by
 * {@link InternalPluginSearchEngine}. Also provides static accessors to plugin instances via
 * Bukkit's {@link JavaPlugin#getPlugin(Class)} mechanism. Used internally by framework utilities
 * that require a plugin reference without direct access to the plugin hierarchy.</p>
 */
@UtilityClass
public class UtilPlugin {

    /**
     * Registry of framework plugins keyed by upper-cased plugin name, in registration order.
     */
    private static final LinkedHashMap<String, SpigotPlugin> internalPluginMap = new LinkedHashMap<>();

    /**
     * Search engine used to resolve plugin names for
     * {@link #searchInternalPlugin(CommandSender, String, boolean, Predicate)}.
     */
    private static final InternalPluginSearchEngine INTERNAL_PLUGIN_SEARCH_ENGINE = new InternalPluginSearchEngine();

    /**
     * Returns an immutable snapshot of all registered internal plugins.
     *
     * @return an unmodifiable list of all {@link SpigotPlugin} instances
     */
    public static List<SpigotPlugin> getInternalPlugins() {
        return List.copyOf(internalPluginMap.values());
    }

    /**
     * Registers a plugin in the internal registry, replacing any existing entry under the same name.
     *
     * @param spigotPlugin the plugin to register
     */
    public static void addInternalPlugin(final SpigotPlugin spigotPlugin) {
        internalPluginMap.put(spigotPlugin.getPluginName().toUpperCase(Locale.ROOT), spigotPlugin);
    }

    /**
     * Removes a plugin from the internal registry, doing nothing if it was never registered.
     *
     * @param spigotPlugin the plugin to remove
     */
    public static void removeInternalPlugin(final SpigotPlugin spigotPlugin) {
        internalPluginMap.remove(spigotPlugin.getPluginName().toUpperCase(Locale.ROOT));
    }

    /**
     * Looks up a registered plugin by exact name, ignoring case.
     *
     * @param name the plugin name to resolve
     * @return the registered plugin, or null if no plugin is registered under that name
     */
    public static SpigotPlugin getInternalPluginByName(final String name) {
        return internalPluginMap.getOrDefault(name.toUpperCase(Locale.ROOT), null);
    }

    /**
     * Searches the internal registry for the plugin identified by the given input.
     *
     * <p>An exact name match wins immediately, otherwise a single partial match is returned. An empty
     * or ambiguous search yields an empty result and, when informing is enabled, messages the sender
     * with the outcome.</p>
     *
     * @param sender    the sender to inform of the search outcome
     * @param input     the search input
     * @param inform    whether to message the sender when the search fails to resolve
     * @param predicate an optional filter applied before matching, or null to consider every plugin
     * @return the resolved plugin, or {@link Optional#empty()} if the search was empty or ambiguous
     */
    public static Optional<SpigotPlugin> searchInternalPlugin(final CommandSender sender, final String input, final boolean inform, final Predicate<SpigotPlugin> predicate) {
        return INTERNAL_PLUGIN_SEARCH_ENGINE.find(
                sender,
                input,
                inform,
                predicate
        );
    }

    /**
     * Searches the internal registry for the plugin identified by the given input, without filtering.
     *
     * @param sender the sender to inform of the search outcome
     * @param input  the search input
     * @param inform whether to message the sender when the search fails to resolve
     * @return the resolved plugin, or {@link Optional#empty()} if the search was empty or ambiguous
     * @see #searchInternalPlugin(CommandSender, String, boolean, Predicate)
     */
    public static Optional<SpigotPlugin> searchInternalPlugin(final CommandSender sender, final String input, final boolean inform) {
        return searchInternalPlugin(sender, input, inform, null);
    }

    /**
     * Returns the active plugin instance for the given concrete plugin class.
     *
     * <p>Resolves the plugin via Bukkit's plugin loader and casts it to the
     * specified type using {@link UtilJava#cast}.</p>
     *
     * @param clazz    the concrete plugin class to resolve
     * @param <Plugin> the concrete plugin type
     * @return the plugin instance cast to the specified type
     * @throws IllegalStateException if no plugin of the given class is loaded
     */
    public static <Plugin extends SpigotPlugin> Plugin getInstanceByClass(final Class<Plugin> clazz) {
        return UtilJava.cast(clazz, JavaPlugin.getPlugin(clazz));
    }

    /**
     * Returns the active {@link SpigotPlugin} instance registered with Bukkit.
     *
     * <p>Convenience method equivalent to calling
     * {@link #getInstanceByClass(Class)} with {@link SpigotPlugin SpigotPlugin.class}.</p>
     *
     * @return the plugin instance
     * @throws IllegalStateException if no plugin extending {@link SpigotPlugin} is loaded
     */
    public static SpigotPlugin getInstance() {
        return getInstanceByClass(SpigotPlugin.class);
    }
}