package io.github.trae.spigot.framework.damage.listeners.reduction;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.events.damage.CustomPreDamageEvent;
import io.github.trae.spigot.framework.damage.events.reduction.WeaponReductionEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import io.github.trae.spigot.framework.utility.UtilEvent;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

/**
 * Resolves what the attacker's held item contributes in damage.
 *
 * <p>Runs at the pre stage, so the contribution is in place before an ability sets its own figure at
 * the damage stage. Written with {@code setModifier} rather than added, since the key holds one
 * thing: this item's damage.</p>
 *
 * <p>The values are vanilla's displayed attack damage by material, so replacing this listener is how
 * a game mode moves off vanilla's weapon balance entirely.</p>
 *
 * @see WeaponReductionEvent
 */
@Singleton
public class DamageWeaponReductionListener implements Listener {

    private static final double SHARPNESS_BASE = 0.5D;
    private static final double SHARPNESS_PER_LEVEL = 0.5D;

    /**
     * Files the held item's damage under {@link DamageModifier#WEAPON}.
     *
     * @param event the pre stage
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public final void onCustomPreDamage(final CustomPreDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!this.usesWeapon(event.getCause())) {
            return;
        }

        final ItemStack itemStack = event.getItemStack().orElse(null);
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        final double damage = this.getWeaponDamage(itemStack.getType()) + this.getSharpness(itemStack);

        final WeaponReductionEvent weaponReductionEvent = UtilEvent.supply(new WeaponReductionEvent(event, itemStack, damage, damage));
        if (weaponReductionEvent.isCancelled()) {
            return;
        }

        final double amount = weaponReductionEvent.getAmount();
        if (amount <= 0.0D) {
            return;
        }

        event.setModifier(DamageModifier.WEAPON, amount);
    }

    /**
     * The bonus from sharpness: half a point for the first level, half a point for each after.
     *
     * @param itemStack the held item
     * @return the bonus damage
     */
    private double getSharpness(final ItemStack itemStack) {
        final int level = itemStack.getEnchantmentLevel(Enchantment.SHARPNESS);

        return level <= 0 ? 0.0D : SHARPNESS_BASE + (level - 1) * SHARPNESS_PER_LEVEL;
    }

    /**
     * Vanilla's displayed attack damage for a material.
     *
     * <p>Anything not listed falls to one, which covers bare hands and every non-weapon item, both
     * of which vanilla treats the same way.</p>
     *
     * @param material the held material
     * @return the base damage
     */
    private double getWeaponDamage(final Material material) {
        return switch (material) {
            case WOODEN_PICKAXE, GOLDEN_PICKAXE -> 2.0D;
            case WOODEN_SHOVEL, GOLDEN_SHOVEL -> 2.5D;
            case STONE_PICKAXE, COPPER_PICKAXE -> 3.0D;
            case STONE_SHOVEL, COPPER_SHOVEL -> 3.5D;
            case WOODEN_SWORD, GOLDEN_SWORD, IRON_PICKAXE -> 4.0D;
            case IRON_SHOVEL -> 4.5D;
            case STONE_SWORD, COPPER_SWORD, DIAMOND_PICKAXE -> 5.0D;
            case DIAMOND_SHOVEL -> 5.5D;
            case IRON_SWORD, MACE, NETHERITE_PICKAXE -> 6.0D;
            case NETHERITE_SHOVEL -> 6.5D;
            case DIAMOND_SWORD, WOODEN_AXE, GOLDEN_AXE -> 7.0D;
            case NETHERITE_SWORD -> 8.0D;
            case STONE_AXE, COPPER_AXE, IRON_AXE, DIAMOND_AXE, TRIDENT -> 9.0D;
            case NETHERITE_AXE -> 10.0D;
            default -> 1.0D;
        };
    }

    /**
     * Whether the held item contributes damage for this cause.
     *
     * @param damageCause the cause of the damage
     * @return whether the weapon applies
     */
    private boolean usesWeapon(final DamageCause damageCause) {
        return damageCause == DamageCause.ENTITY_ATTACK || damageCause == DamageCause.ENTITY_SWEEP_ATTACK;
    }
}