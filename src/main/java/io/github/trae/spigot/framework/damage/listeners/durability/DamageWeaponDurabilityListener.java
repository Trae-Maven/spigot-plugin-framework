package io.github.trae.spigot.framework.damage.listeners.durability;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.damage.events.durability.WeaponDurabilityEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * Spends durability on the attacker's held item.
 *
 * <p>One point per hit, matching vanilla, which charges the same regardless of how much damage was
 * dealt. Runs at the end of the post stage, so it only fires for damage that actually landed.</p>
 *
 * @see WeaponDurabilityEvent
 */
@Singleton
public class DamageWeaponDurabilityListener implements Listener {

    private static final int WEAPON_DURABILITY_AMOUNT = 1;

    /**
     * Wears the attacker's held item.
     *
     * @param event the completed post stage
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onCustomPostDamage(final CustomPostDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getDamager() instanceof final LivingEntity damager)) {
            return;
        }

        final ItemStack itemStack = event.getItemStack().orElse(null);
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        final WeaponDurabilityEvent weaponDurabilityEvent = UtilEvent.supply(new WeaponDurabilityEvent(event, itemStack, WEAPON_DURABILITY_AMOUNT, WEAPON_DURABILITY_AMOUNT));
        if (weaponDurabilityEvent.isCancelled()) {
            return;
        }

        final int amount = weaponDurabilityEvent.getAmount();
        if (amount <= 0) {
            return;
        }

        damager.damageItemStack(itemStack, amount);
    }
}