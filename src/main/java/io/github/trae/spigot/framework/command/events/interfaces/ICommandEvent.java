package io.github.trae.spigot.framework.command.events.interfaces;

import org.bukkit.command.CommandSender;

public interface ICommandEvent {

    CommandSender getSender();

    String[] getArgs();
}