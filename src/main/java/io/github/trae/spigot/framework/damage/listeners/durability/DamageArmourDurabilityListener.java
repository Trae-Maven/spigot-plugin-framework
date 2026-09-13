package io.github.trae.spigot.framework.damage.listeners.durability;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.damage.events.durability.ArmourDurabilityEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.enums.ArmourType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

/**
 * Spends durability on the damagee's worn armour.
 *
 * <p>A quarter of the damage, floored at one, matching vanilla, so a heavier hit wears armour
 * faster. Causes that bypass armour entirely are skipped, since a piece that did not protect should
 * not pay.</p>
 *
 * <p>Runs at the end of the post stage, which means the figure it divides has already been reduced
 * by armour. Vanilla divides the pre-reduction damage instead, so armour here wears slightly
 * slower.</p>
 *
 * @see ArmourDurabilityEvent
 */
@Singleton
public class DamageArmourDurabilityListener implements Listener {

    private static final double ARMOUR_DURABILITY_DIVISOR = 4.0D;

    /**
     * Wears every piece the damagee is wearing.
     *
     * @param event the completed post stage
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onCustomPostDamage(final CustomPostDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getDamagee() instanceof final LivingEntity damagee)) {
            return;
        }

        if (!this.damagesArmour(event.getCause())) {
            return;
        }

        final ItemStack[] armourContents = event.getArmourContents();
        if (armourContents == null) {
            return;
        }

        final int durabilityAmount = this.getArmourDurabilityAmount(event);

        for (final ItemStack itemStack : armourContents) {
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }

            ArmourType.getByMaterial(itemStack.getType()).ifPresent(armourType -> {
                final ArmourDurabilityEvent armourDurabilityEvent = UtilEvent.supply(new ArmourDurabilityEvent(event, armourType, itemStack, durabilityAmount, durabilityAmount));
                if (armourDurabilityEvent.isCancelled()) {
                    return;
                }

                final int amount = armourDurabilityEvent.getAmount();
                if (amount <= 0) {
                    return;
                }

                damagee.damageItemStack(itemStack, amount);
            });
        }
    }

    /**
     * A quarter of the resolved damage, never less than one.
     *
     * @param event the completed post stage
     * @return the durability to spend per piece
     */
    private int getArmourDurabilityAmount(final CustomPostDamageEvent event) {
        return Math.max(1, (int) (event.getFinalDamage() / ARMOUR_DURABILITY_DIVISOR));
    }

    /**
     * Whether armour wears from this cause.
     *
     * <p>Mirrors the causes armour does not protect against, so the two stay consistent.</p>
     *
     * @param damageCause the cause of the damage
     * @return whether armour should be worn
     */
    private boolean damagesArmour(final DamageCause damageCause) {
        return switch (damageCause) {
            case DROWNING, STARVATION, SUFFOCATION, VOID, POISON, WITHER, MAGIC, FREEZE, DRYOUT, KILL, SUICIDE, CUSTOM -> false;
            default -> true;
        };
    }
}