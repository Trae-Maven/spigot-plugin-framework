package io.github.trae.spigot.framework.item.enums;

import io.github.trae.spigot.framework.item.types.ActivatableCustomItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.event.block.Action;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The kinds of interaction that activate an {@link ActivatableCustomItem}.
 * <p>
 * The click types each group the vanilla {@link Action}s that mean the same thing to an item, so an
 * implementation reacts to a left click without caring whether the player was aiming at a block or
 * at air. Actions with no constant, such as physical pressure plate triggers, resolve to empty and
 * activate nothing.
 * <p>
 * {@link #DROP_ITEM} and {@link #SWAP_HAND} cover no action, since neither is a click. They are
 * resolved from their own events rather than through {@link #getByAction(Action)}, which never
 * returns them.
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
    RIGHT_CLICK("Right-Click", List.of(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK)),

    /**
     * Dropping the item. A drop that activates is cancelled, so the item stays with the player.
     */
    DROP_ITEM("Drop Item", Collections.emptyList()),

    /**
     * Swapping the item from the main hand to the off hand. A swap that activates is cancelled, so
     * the item stays in the main hand.
     */
    SWAP_HAND("Swap-Hand", Collections.emptyList());

    /**
     * Reverse lookup from a vanilla action to the type covering it, built once at class load.
     */
    private static final Map<Action, ActivateType> BY_ACTION_MAP = new HashMap<>();

    /**
     * The display name for this type, suitable for lore and messages.
     */
    private final String name;

    /**
     * The vanilla actions this type covers, empty for a type that is not a click.
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
     * Returns the click type covering the given vanilla action.
     *
     * @param action the action to resolve
     * @return an {@link Optional} containing the type, or empty if no type covers that action
     */
    public static Optional<ActivateType> getByAction(final Action action) {
        return Optional.ofNullable(BY_ACTION_MAP.get(action));
    }
}