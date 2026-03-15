package io.github.trae.spigot.framework.command.subcommand.abstracts;

import io.github.trae.hf.SubModule;
import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.command.abstracts.AbstractCommand;
import io.github.trae.spigot.framework.command.subcommand.abstracts.interfaces.IAbstractSubCommand;
import io.github.trae.utilities.UtilJava;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public abstract class AbstractSubCommand<Plugin extends SpigotPlugin, Module extends AbstractCommand<Plugin, ?, ?>, Sender extends CommandSender> implements SubModule<Plugin, Module>, IAbstractSubCommand<Sender> {

    private final String label, description;

    @Setter
    private String permission;

    @Override
    public void _internalExecute(final CommandSender sender, final String[] args) {
        if (sender == null) {
            return;
        }

        if (!(this.getClassOfCommandSender().isInstance(sender))) {
            return;
        }

        this.execute(UtilJava.cast(this.getClassOfCommandSender(), sender), args);
    }

    @Override
    public List<String> _internalGetTabComplete(final CommandSender sender, final String[] args) {
        if (sender == null) {
            return Collections.emptyList();
        }

        if (!(this.getClassOfCommandSender().isInstance(sender))) {
            return Collections.emptyList();
        }

        return this.getTabCompletion(UtilJava.cast(this.getClassOfCommandSender(), sender), args);
    }
}