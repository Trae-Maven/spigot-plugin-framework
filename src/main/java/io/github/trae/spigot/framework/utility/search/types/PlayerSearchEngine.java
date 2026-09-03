package io.github.trae.spigot.framework.utility.search.types;

import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.UtilServer;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.spigot.framework.utility.search.SpigotSearchEngine;
import org.bukkit.entity.Player;

import java.util.Locale;

public class PlayerSearchEngine extends SpigotSearchEngine<Player> {

    public PlayerSearchEngine() {
        super("Player Search", UtilServer::getOnlinePlayers);
    }

    @Override
    protected String getTypeFormat(final Player player) {
        return UtilColor.serialize(ChatColor.YELLOW.getColor(), player.getName());
    }

    @Override
    protected boolean isExact(final Player player, final String result) {
        return player.getName().equalsIgnoreCase(result);
    }

    @Override
    protected boolean isMatching(final Player player, final String result) {
        return player.getName().toLowerCase(Locale.ROOT).contains(result.toLowerCase(Locale.ROOT));
    }
}