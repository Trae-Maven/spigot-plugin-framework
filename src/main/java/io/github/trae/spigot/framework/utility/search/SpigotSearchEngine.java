package io.github.trae.spigot.framework.utility.search;

import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.UtilMessage;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.utilities.search.AbstractSearchEngine;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.function.Supplier;

public abstract class SpigotSearchEngine<Type> extends AbstractSearchEngine<Type, CommandSender> {

    protected SpigotSearchEngine(final String name, final Supplier<Collection<? extends Type>> collectionSupplier) {
        super(name, collectionSupplier);
    }

    @Override
    protected void message(final CommandSender commandSender, final String prefix, final String message) {
        UtilMessage.message(commandSender, prefix, message);
    }

    @Override
    protected String getColorFormat(final String string) {
        return UtilColor.serialize(ChatColor.YELLOW.getColor(), string);
    }

    @Override
    protected String getMatchSeparator() {
        return UtilColor.serialize(ChatColor.GRAY.getColor(), ", ");
    }
}