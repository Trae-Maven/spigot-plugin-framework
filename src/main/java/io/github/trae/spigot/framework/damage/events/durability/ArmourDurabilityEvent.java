package io.github.trae.spigot.framework.damage.events.durability;

import io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.utility.enums.ArmourType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

/**
 * Fired once per worn armour piece before its durability is spent.
 *
 * <p>Cancel to spare that piece, or set the amount to zero for the same result. Setting a higher
 * amount wears it faster, which is how a piece can be made fragile without touching its material.</p>
 *
 * <p>Only fires for causes where armour actually protects, since a piece that does not reduce the
 * damage should not pay for it either.</p>
 *
 * @see io.github.trae.spigot.framework.damage.listeners.durability.DamageArmourDurabilityListener
 */
@AllArgsConstructor
@Getter
@Setter
public class ArmourDurabilityEvent extends CustomCancellableEvent {

    /**
     * The damage pass this belongs to.
     */
    private final AbstractCustomDamageEvent damageEvent;

    /**
     * Which slot this piece occupies.
     */
    private final ArmourType armourType;

    /**
     * The piece about to be worn.
     */
    private final ItemStack itemStack;

    /**
     * What the framework calculated before any listener changed it.
     */
    private final int originalAmount;

    /**
     * The durability to spend. Zero or below spares the piece.
     */
    private int amount;
}