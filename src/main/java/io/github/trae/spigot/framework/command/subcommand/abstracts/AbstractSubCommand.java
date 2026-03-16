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

/**
 * Abstract base for all subcommands within the framework.
 *
 * <p>Implements {@link SubModule} to integrate with the hierarchy framework under
 * a parent {@link AbstractCommand}. Provides sender type validation and safe casting
 * before delegating to the concrete execution and tab completion implementations.</p>
 *
 * <p>Subcommands are registered on their parent command via
 * {@link AbstractCommand#addSubCommand}. Permission checking and event dispatch
 * are handled by the parent command before reaching the subcommand's internal
 * methods, so this class only performs sender type validation as a final guard.</p>
 *
 * @param <BasePlugin> the concrete plugin type
 * @param <BaseModule> the parent command type this subcommand belongs to
 * @param <Sender>     the required sender type for this subcommand
 */
@Getter
@RequiredArgsConstructor
public abstract class AbstractSubCommand<BasePlugin extends SpigotPlugin, BaseModule extends AbstractCommand<BasePlugin, ?, ?>, Sender extends CommandSender> implements SubModule<BasePlugin, BaseModule>, IAbstractSubCommand<Sender> {

    private final String label, description;

    @Setter
    private String permission;

    /**
     * Internal execution entry point invoked by the parent command after routing.
     *
     * <p>Validates that the sender is non-null and matches the required sender type
     * before casting and delegating to the concrete {@link IAbstractSubCommand#execute}
     * implementation.</p>
     *
     * @param sender the command sender
     * @param args   the arguments passed to this subcommand (excluding the subcommand label)
     */
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

    /**
     * Internal tab completion entry point invoked by the parent command after routing.
     *
     * <p>Validates that the sender is non-null and matches the required sender type
     * before casting and delegating to the concrete
     * {@link IAbstractSubCommand#getTabCompletion} implementation.</p>
     *
     * @param sender the command sender requesting tab completions
     * @param args   the current arguments being typed (excluding the subcommand label)
     * @return a list of tab completion suggestions, or an empty list if the sender is
     * invalid or null
     */
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