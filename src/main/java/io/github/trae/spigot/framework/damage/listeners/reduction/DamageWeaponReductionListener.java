package io.github.trae.spigot.framework.damage.listeners.reduction;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.DamageManager;
import io.github.trae.spigot.framework.damage.configs.DamageConfig;
import io.github.trae.spigot.framework.damage.events.damage.CustomPreDamageEvent;
import io.github.trae.spigot.framework.damage.events.reduction.WeaponReductionEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves what a player's held item contributes in melee damage.
 *
 * <p>Runs at the pre stage, so the contribution is in place before an ability sets its own figure at
 * the damage stage. Written with {@code setModifier} rather than added, since the key holds one
 * thing: this item's damage.</p>
 *
 * <p>The values are vanilla's displayed attack damage by material, so replacing this listener is how
 * a game mode moves off vanilla's weapon balance entirely. Only players are resolved here, since a
 * mob's damage comes from its own attributes and is already the base of its pass.</p>
 *
 * <p>With old combat disabled, the contribution is scaled by how charged the swing was, as vanilla
 * scales it: the item's damage by the configured minimum plus the configured range times the charge
 * squared, and sharpness by the charge alone. Vanilla resets the charge before the damage event
 * fires, so it is captured when the attack begins and consumed by the pass that attack produces.</p>
 *
 * <p>The sharpness and charge figures are read from {@link DamageConfig.WeaponReduction} on each hit,
 * so a reload takes effect on the next one.</p>
 *
 * @see WeaponReductionEvent
 */
@RequiredArgsConstructor
@Singleton
public class DamageWeaponReductionListener implements Listener {

    private final DamageManager damageManager;

    /**
     * The attack charge each player had when their current attack began, keyed by their identifier
     * and consumed by the damage pass it belongs to.
     */
    private final Map<UUID, Float> attackChargeMap = new HashMap<>();

    /**
     * Records the attacker's charge before vanilla resets it.
     *
     * <p>Runs at {@code MONITOR} so only an attack that is actually going ahead is recorded. A newer
     * attack overwrites an older record, so a record left behind by a refused hit never reaches a
     * later one.</p>
     *
     * @param event the pre attack event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onPrePlayerAttackEntity(final PrePlayerAttackEntityEvent event) {
        if (event.isCancelled() || !event.willAttack()) {
            return;
        }

        this.attackChargeMap.put(event.getPlayer().getUniqueId(), event.getPlayer().getAttackCooldown());
    }

    /**
     * Files the held item's damage under {@link DamageModifier#WEAPON}.
     *
     * <p>An empty hand is valued as air, which falls to a single point, the same as vanilla's bare
     * fist. The charge is applied after {@link WeaponReductionEvent}, so a listener rebalancing the
     * item sets what a full swing is worth, and a partial swing is scaled from that.</p>
     *
     * <p>The recorded charge is consumed before the cancellation check, so a hit refused earlier in
     * the stage does not leave it behind.</p>
     *
     * @param event the pre stage
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public final void onCustomPreDamage(final CustomPreDamageEvent event) {
        if (!(event.getDamager() instanceof final Player player) || event.getProjectile() != null || !this.usesWeapon(event.getCause())) {
            return;
        }

        final Float attackCharge = event.getCause() == DamageCause.ENTITY_ATTACK ? this.attackChargeMap.remove(player.getUniqueId()) : null;

        if (event.isCancelled()) {
            return;
        }

        final DamageConfig damageConfig = this.damageManager.getDamageConfig();
        final DamageConfig.WeaponReduction weaponReduction = damageConfig.getWeaponReduction();

        final ItemStack itemStack = event.getItemStack().orElse(ItemStack.empty());

        final double weaponDamage = this.getWeaponDamage(itemStack.getType());
        final double sharpness = this.getSharpness(itemStack, weaponReduction);
        final double damage = weaponDamage + sharpness;

        final WeaponReductionEvent weaponReductionEvent = UtilEvent.supply(new WeaponReductionEvent(event, itemStack, damage, damage));
        if (weaponReductionEvent.isCancelled()) {
            return;
        }

        final double amount = weaponReductionEvent.getAmount();
        if (amount <= 0.0D) {
            return;
        }

        if (attackCharge == null || damageConfig.isOldCombatEnabled()) {
            event.setModifier(DamageModifier.WEAPON, amount);
            return;
        }

        final double charge = Math.clamp(attackCharge, 0.0D, 1.0D);
        final double scale = (weaponDamage * (weaponReduction.getChargeMinimum() + charge * charge * weaponReduction.getChargeRange()) + sharpness * charge) / damage;

        event.setModifier(DamageModifier.WEAPON, amount * scale);
    }

    /**
     * Drops the leaving player's recorded charge, so the map holds only players who are online.
     *
     * @param event the quit event
     */
    @EventHandler
    public final void onPlayerQuit(final PlayerQuitEvent event) {
        this.attackChargeMap.remove(event.getPlayer().getUniqueId());
    }

    /**
     * The bonus from sharpness: the configured base for the first level, plus the configured amount
     * for each level after.
     *
     * @param itemStack       the held item
     * @param weaponReduction the weapon settings
     * @return the bonus damage
     */
    private double getSharpness(final ItemStack itemStack, final DamageConfig.WeaponReduction weaponReduction) {
        final int level = itemStack.getEnchantmentLevel(Enchantment.SHARPNESS);

        return level <= 0 ? 0.0D : weaponReduction.getSharpnessBase() + (level - 1) * weaponReduction.getSharpnessPerLevel();
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
     * <p>Kept in step with the causes {@link CustomPreDamageEvent} starts from a zero base for, since
     * a player's melee hit has no damage beyond what this listener resolves.</p>
     *
     * @param damageCause the cause of the damage
     * @return whether the weapon applies
     */
    private boolean usesWeapon(final DamageCause damageCause) {
        return damageCause == DamageCause.ENTITY_ATTACK || damageCause == DamageCause.ENTITY_SWEEP_ATTACK;
    }
}