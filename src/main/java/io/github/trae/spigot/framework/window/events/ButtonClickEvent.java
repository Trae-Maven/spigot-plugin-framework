package io.github.trae.spigot.framework.window.events;

import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.window.Button;
import io.github.trae.spigot.framework.window.Window;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired when a player clicks a button, before the button's action runs.
 * <p>
 * Dispatched by {@link io.github.trae.spigot.framework.window.WindowListener}. Cancelling suppresses
 * the action; the underlying inventory click is cancelled regardless, so nothing moves either way.
 * <p>
 * This is the system-level gate, for conditions external to the button itself — a world restriction,
 * a global lockdown. A condition the button owns belongs in
 * {@link Button#canClick(Player, org.bukkit.event.inventory.ClickType)} instead, which is why the
 * click type is deliberately absent here.
 */
@AllArgsConstructor
@Getter
public class ButtonClickEvent extends CustomCancellableEvent {

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