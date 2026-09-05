package io.github.trae.spigot.framework.item.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.event.block.Action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The kinds of interaction that activate an {@link io.github.trae.spigot.framework.item.Activatable}
 * item.
 * <p>
 * Each constant groups the vanilla {@link Action}s that mean the same thing to an item, so an
 * implementation reacts to a left click without caring whether the player was aiming at a block or
 * at air. Actions with no constant, such as physical pressure plate triggers, resolve to empty and
 * activate nothing.
 */
@AllArgsConstructor
@Getter
public enum ActivateType {

    /**
     * A left click, whether aimed at a block or at air.
     */
    LEFT_CLICK("Left-Click", List.of(Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK)),

    /**
     * A right click, whether aimed at a block or at air.
     */
    RIGHT_CLICK("Right-Click", List.of(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK));

    /**
     * Reverse lookup from a vanilla action to the type covering it, built once at class load.
     */
    private static final Map<Action, ActivateType> BY_ACTION_MAP = new HashMap<>();

    /**
     * The display name for this type, suitable for lore and messages.
     */
    private final String name;

    /**
     * The vanilla actions this type covers.
     */
    private final List<Action> actions;

    static {
        for (final ActivateType activateType : values()) {
            for (final Action action : activateType.getActions()) {
                BY_ACTION_MAP.put(action, activateType);
            }
        }
    }

    /**
     * Returns the type covering the given vanilla action.
     *
     * @param action the action to resolve
     * @return an {@link Optional} containing the type, or empty if no type covers that action
     */
    public static Optional<ActivateType> getByAction(final Action action) {
        return Optional.ofNullable(BY_ACTION_MAP.get(action));
    }
}