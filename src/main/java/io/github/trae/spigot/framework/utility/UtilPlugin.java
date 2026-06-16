package io.github.trae.spigot.framework.utility;

import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.utilities.UtilJava;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Utility class for retrieving active {@link SpigotPlugin} instances.
 *
 * <p>Provides static accessors to plugin instances via Bukkit's
 * {@link JavaPlugin#getPlugin(Class)} mechanism. Used internally by
 * framework utilities that require a plugin reference without direct
 * access to the plugin hierarchy.</p>
 */
@UtilityClass
public class UtilPlugin {

    @Getter
    private static final LinkedHashMap<String, SpigotPlugin> internalPluginMap = new LinkedHashMap<>();

    public static void addInternalPlugin(final SpigotPlugin spigotPlugin) {
        internalPluginMap.put(spigotPlugin.getPluginName().toUpperCase(Locale.ROOT), spigotPlugin);
    }

    public static void removeInternalPlugin(final SpigotPlugin spigotPlugin) {
        internalPluginMap.remove(spigotPlugin.getPluginName().toUpperCase(Locale.ROOT));
    }

    public static SpigotPlugin getInternalPluginByName(final String name) {
        return internalPluginMap.getOrDefault(name.toUpperCase(Locale.ROOT), null);
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