package io.github.trae.spigot.framework.command.constants;

import io.github.trae.hf.Plugin;
import io.github.trae.spigot.framework.command.BaseCommand;
import io.github.trae.spigot.framework.command.BaseSubCommand;
import io.github.trae.spigot.framework.utility.UtilPlugin;
import io.github.trae.utilities.UtilString;
import io.github.trae.utilities.objects.function.BiFunction;
import io.github.trae.utilities.objects.function.TriFunction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Reusable tab-complete suggestion providers for use in {@link BaseCommand} and
 * {@link BaseSubCommand} implementations.
 * <p>
 * All suggestions are case-insensitively filtered against the current argument input
 * using {@link #CUSTOM}.
 */
public class DefaultSuggestions {

    /**
     * Filters a provided list of strings to those that start with the given argument,
     * case-insensitively.
     * <p>
     * Used internally by all other suggestion providers as the base filter.
     *
     * <p>Usage:
     * <pre>{@code
     * DefaultSuggestions.CUSTOM.apply(List.of("create", "delete", "list"), "cr");
     * // returns ["create"]
     * }</pre>
     */
    public static final BiFunction<List<String>, String, List<String>> CUSTOM = (list, arg) -> list.stream().filter(string -> string.toLowerCase(Locale.ROOT).startsWith(arg.toLowerCase(Locale.ROOT))).toList();

    /**
     * Suggests the labels of subcommands registered to the given {@link BaseCommand} that
     * the sender is permitted to use.
     * <p>
     * Filters by both {@link BaseSubCommand#isValidSender} and {@link BaseSubCommand#hasPermission}
     * before applying the {@link #CUSTOM} prefix filter.
     *
     * <p>Usage:
     * <pre>{@code
     * DefaultSuggestions.SUB_COMMANDS.apply(baseCommand, sender, arg);
     * }</pre>
     */
    public static final TriFunction<BaseCommand<?, ?, ?>, CommandSender, String, List<String>> SUB_COMMANDS = (baseCommand, commandSender, arg) -> CUSTOM.apply(
            baseCommand
                    .getSubCommands()
                    .values()
                    .stream()
                    .filter(baseSubCommand -> baseSubCommand.isValidSender(commandSender) && baseSubCommand.hasPermission(commandSender))
                    .map(BaseSubCommand::getLabel).toList(),
            arg
    );

    /**
     * Suggests the names of online players matching the given predicate, filtered by the
     * current argument input.
     *
     * <p>Usage:
     * <pre>{@code
     * DefaultSuggestions.PLAYERS.apply(player -> player.getWorld().getName().equals("world"), arg);
     * }</pre>
     */
    public static final BiFunction<Predicate<Player>, String, List<String>> PLAYERS = (predicate, arg) -> CUSTOM.apply(
            Bukkit.getServer().getOnlinePlayers().stream().filter(predicate).map(Player::getName).toList(),
            arg
    );

    /**
     * Suggests the names of all online players, filtered by the current argument input.
     */
    public static final Function<String, List<String>> ALL_PLAYERS = arg -> PLAYERS.apply(__ -> true, arg);

    /**
     * Suggests the names of all loaded worlds, filtered by the current argument input.
     */
    public static final Function<String, List<String>> WORLDS = arg -> CUSTOM.apply(
            Bukkit.getServer().getWorlds().stream().map(WorldInfo::getName).toList(),
            arg
    );

    /**
     * Suggests all {@link Material} names in cleaned, underscore-separated form,
     * filtered by the current argument input.
     */
    public static final Function<String, List<String>> MATERIALS = arg -> CUSTOM.apply(
            Stream.of(Material.values()).map(material -> UtilString.clean(material.name()).replace(" ", "_")).toList(),
            arg
    );

    /**
     * Suggests the names of all registered internal plugins, filtered by the current argument input.
     */
    public static final Function<String, List<String>> INTERNAL_PLUGINS = arg -> CUSTOM.apply(
            UtilPlugin.getInternalPluginMap().values().stream().map(Plugin::getPluginName).toList(),
            arg
    );

    /**
     * Returns the executing player's current X coordinate as a string, or an empty string
     * if the sender is not a player.
     */
    public static final Function<CommandSender, String> POSITION_X = commandSender -> commandSender instanceof Player player ? String.valueOf(player.getLocation().getX()) : "";

    /**
     * Returns the executing player's current Y coordinate as a string, or an empty string
     * if the sender is not a player.
     */
    public static final Function<CommandSender, String> POSITION_Y = commandSender -> commandSender instanceof Player player ? String.valueOf(player.getLocation().getY()) : "";

    /**
     * Returns the executing player's current Z coordinate as a string, or an empty string
     * if the sender is not a player.
     */
    public static final Function<CommandSender, String> POSITION_Z = commandSender -> commandSender instanceof Player player ? String.valueOf(player.getLocation().getZ()) : "";
}