package io.github.trae.spigot.framework.damage.events.reduction;

import io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.utility.enums.ArmourType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

/**
 * Fired once per worn armour piece to establish what that piece is worth.
 *
 * <p>The amount is the piece's own armour value, not a reduction. Every piece's value is summed and
 * the total is what the reduction formula resolves against, so raising one piece makes the whole set
 * stronger. Cancelling excludes that piece from the sum entirely.</p>
 *
 * <p>This is the hook a game mode uses to rebalance armour by class rather than by material: set
 * each piece's value from the class, and leave the formula alone.</p>
 *
 * @see io.github.trae.spigot.framework.damage.listeners.reduction.DamageArmourReductionListener
 */
@AllArgsConstructor
@Getter
@Setter
public class ArmourReductionEvent extends CustomCancellableEvent {

    /**
     * The damage pass this belongs to.
     */
    private final AbstractCustomDamageEvent damageEvent;

    /**
     * Which slot this piece occupies.
     */
    private final ArmourType armourType;

    /**
     * The piece being valued.
     */
    private final ItemStack itemStack;

    /**
     * What the framework calculated before any listener changed them.
     */
    private final double originalAmount, originalToughness;

    /**
     * This piece's armour value, and its toughness contribution. Toughness only matters against
     * heavy hits; setting it to zero collapses the formula to a flat armour curve.
     */
    private double amount, toughness;
}