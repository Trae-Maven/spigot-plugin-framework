package io.github.trae.spigot.framework.command.subcommand.abstracts.interfaces;

import io.github.trae.spigot.framework.command.abstracts.interfaces.ISharedCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface IAbstractSubCommand<Sender extends CommandSender> extends ISharedCommand<Sender> {

    void _internalExecute(final CommandSender sender, final String[] args);

    List<String> _internalGetTabComplete(final CommandSender sender, final String[] args);
}