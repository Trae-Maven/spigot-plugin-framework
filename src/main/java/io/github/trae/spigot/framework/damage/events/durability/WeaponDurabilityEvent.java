package io.github.trae.spigot.framework.damage.events.durability;

import io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

/**
 * Fired before the attacker's held item spends durability.
 *
 * <p>Cancel to spare the item, or set the amount to zero for the same result. Fires for any held
 * item, not only weapons, matching vanilla, where hitting with a pickaxe wears it too.</p>
 *
 * @see io.github.trae.spigot.framework.damage.listeners.durability.DamageWeaponDurabilityListener
 */
@AllArgsConstructor
@Getter
@Setter
public class WeaponDurabilityEvent extends CustomCancellableEvent {

    /**
     * The damage pass this belongs to.
     */
    private final AbstractCustomDamageEvent damageEvent;

    /**
     * The item about to be worn.
     */
    private final ItemStack itemStack;

    /**
     * What the framework calculated before any listener changed it.
     */
    private final int originalAmount;

    /**
     * The durability to spend. Zero or below spares the item.
     */
    private int amount;
}