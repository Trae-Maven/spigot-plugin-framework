package io.github.trae.spigot.framework.command.abstracts.interfaces;

import io.github.trae.utilities.UtilGeneric;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface ISharedCommand<Sender extends CommandSender> {

    String getLabel();

    String getDescription();

    String getPermission();

    @SuppressWarnings("unchecked")
    default Class<Sender> getClassOfCommandSender() {
        final Class<?> commandSenderClass = UtilGeneric.getGenericParameter(this.getClass(), ISharedCommand.class, 0);
        if (commandSenderClass == null) {
            throw new IllegalStateException("Could not resolve command sender type for %s".formatted(this.getClass().getName()));
        }

        return (Class<Sender>) commandSenderClass;
    }

    void execute(final Sender sender, final String[] args);

    List<String> getTabCompletion(final Sender sender, final String[] args);
}