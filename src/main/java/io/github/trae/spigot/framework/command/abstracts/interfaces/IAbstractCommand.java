package io.github.trae.spigot.framework.command.abstracts.interfaces;

import io.github.trae.spigot.framework.command.subcommand.abstracts.AbstractSubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface IAbstractCommand<Sender extends CommandSender, SubCommand extends AbstractSubCommand<?, ?, ?>> extends ISharedCommand<Sender> {

    List<SubCommand> getSubCommands();

    void addSubCommand(final SubCommand subCommand);

    void removeSubCommand(final SubCommand subCommand);
}