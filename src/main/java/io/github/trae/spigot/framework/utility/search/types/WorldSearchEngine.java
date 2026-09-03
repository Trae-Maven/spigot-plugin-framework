package io.github.trae.spigot.framework.utility.search.types;

import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.spigot.framework.utility.search.SpigotSearchEngine;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Locale;

public class WorldSearchEngine extends SpigotSearchEngine<World> {

    public WorldSearchEngine() {
        super("World Search", () -> Bukkit.getServer().getWorlds());
    }

    @Override
    protected String getTypeFormat(final World world) {
        return UtilColor.serialize(ChatColor.YELLOW.getColor(), world.getName());
    }

    @Override
    protected boolean isExact(final World world, final String result) {
        return world.getName().equalsIgnoreCase(result);
    }

    @Override
    protected boolean isMatching(final World world, final String result) {
        return world.getName().toLowerCase(Locale.ROOT).contains(result.toLowerCase(Locale.ROOT));
    }
}