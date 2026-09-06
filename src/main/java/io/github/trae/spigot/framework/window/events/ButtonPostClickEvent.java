package io.github.trae.spigot.framework.window.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import io.github.trae.spigot.framework.window.Button;
import io.github.trae.spigot.framework.window.Window;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired after a button's action has run.
 * <p>
 * Dispatched by {@link io.github.trae.spigot.framework.window.WindowListener}. Not cancellable,
 * since the action has already happened: this is for reacting to a click that succeeded, such as
 * logging it or recording a statistic.
 * <p>
 * Only fires for a click that actually ran, so an attempt refused by {@code canClick} or by a
 * cancelled {@link ButtonPreClickEvent} produces no post event.
 */
@AllArgsConstructor
@Getter
public class ButtonPostClickEvent extends CustomEvent {

    /**
     * The window the button belongs to.
     */
    private final Window window;

    /**
     * The button that was clicked.
     */
    private final Button button;

    /**
     * The player who clicked.
     */
    private final Player player;
}