package io.github.trae.spigot.framework.command.abstracts.interfaces;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface ISharedCommand<Sender extends CommandSender> {

    String getLabel();

    String getDescription();

    String getPermission();

    Class<Sender> getClassOfCommandSender();

    void execute(final Sender sender, final String[] args);

    List<String> getTabCompletion(final Sender sender, final String[] args);
}