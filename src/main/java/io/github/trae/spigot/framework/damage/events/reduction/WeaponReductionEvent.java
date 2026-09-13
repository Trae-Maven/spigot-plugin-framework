package io.github.trae.spigot.framework.damage.events.reduction;

import io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

/**
 * Fired to establish what the attacker's held item is worth in damage.
 *
 * <p>The amount is the item's damage contribution, not a reduction. Set it to rebalance a weapon
 * without touching the item itself, or cancel to have the item contribute nothing.</p>
 *
 * <p>This lands under {@link io.github.trae.spigot.framework.damage.modifier.DamageModifier#WEAPON},
 * so an ability with a flat damage figure can discard it.</p>
 *
 * @see io.github.trae.spigot.framework.damage.listeners.reduction.DamageWeaponReductionListener
 */
@AllArgsConstructor
@Getter
@Setter
public class WeaponReductionEvent extends CustomCancellableEvent {

    /**
     * The damage pass this belongs to.
     */
    private final AbstractCustomDamageEvent damageEvent;

    /**
     * The item being valued.
     */
    private final ItemStack itemStack;

    /**
     * What the framework calculated before any listener changed it.
     */
    private final double originalAmount;

    /**
     * The item's damage contribution. Zero or below contributes nothing.
     */
    private double amount;
}