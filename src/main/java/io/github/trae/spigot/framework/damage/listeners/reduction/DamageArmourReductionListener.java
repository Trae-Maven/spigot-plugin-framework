package io.github.trae.spigot.framework.damage.listeners.reduction;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.DamageManager;
import io.github.trae.spigot.framework.damage.configs.DamageConfig;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.damage.events.reduction.ArmourReductionEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.enums.ArmourType;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.tag.DamageTypeTags;

import java.util.ArrayList;
import java.util.List;

/**
 * Reduces incoming damage by armour and protection enchantments.
 *
 * <p>Runs early in the post stage, so it acts on whatever an ability decided at the damage stage
 * rather than on the weapon's raw contribution. Whether each applies is decided by the damage type's
 * tags, as vanilla decides it, rather than by the cause. Resistance is applied alongside by the
 * potion effect listener.</p>
 *
 * <p>Armour values come from a per-piece event rather than the entity's armour attribute, which is
 * what lets a game mode rebalance armour by class without replacing this listener. The formula's
 * figures are read from {@link DamageConfig.ArmourReduction} on each hit, so a reload takes effect on
 * the next one.</p>
 *
 * @see ArmourReductionEvent
 * @see io.github.trae.spigot.framework.damage.listeners.DamagePotionEffectListener
 */
@RequiredArgsConstructor
@Singleton
public final class DamageArmourReductionListener implements Listener {

    private final DamageManager damageManager;

    /**
     * Applies both reductions, in vanilla's order.
     *
     * @param event the post stage
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onCustomPostDamage(final CustomPostDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getDamagee() instanceof LivingEntity)) {
            return;
        }

        final DamageConfig.ArmourReduction armourReduction = this.damageManager.getDamageConfig().getArmourReduction();

        this.applyArmour(event, armourReduction);
        this.applyProtection(event, armourReduction);
    }

    /**
     * Reduces by the damagee's worn armour.
     *
     * <p>Each piece reports its own value and toughness through its event; the totals then go
     * through vanilla's curve, where heavier hits cut through more armour and toughness blunts that
     * effect. The floor at a share of the total is what stops a large hit negating armour entirely,
     * and the cap is applied last, so a total too large for the floor to fit under still resolves to
     * the cap rather than failing.</p>
     *
     * @param event           the post stage
     * @param armourReduction the armour settings
     */
    private void applyArmour(final CustomPostDamageEvent event, final DamageConfig.ArmourReduction armourReduction) {
        if (this.isTagged(event.getSource(), DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }

        final List<ItemStack> itemStackList = this.getArmourPieces(event);
        if (itemStackList.isEmpty()) {
            return;
        }

        double armour = 0.0D;
        double toughness = 0.0D;

        for (final ItemStack itemStack : itemStackList) {
            final ArmourType armourType = ArmourType.getByMaterial(itemStack.getType()).orElse(null);
            if (armourType == null) {
                continue;
            }

            final double points = this.getArmourPoints(itemStack.getType());
            final double pointsToughness = this.getArmourToughness(itemStack.getType());

            final ArmourReductionEvent armourReductionEvent = UtilEvent.supply(new ArmourReductionEvent(event, armourType, itemStack, points, pointsToughness, points, pointsToughness));
            if (armourReductionEvent.isCancelled()) {
                continue;
            }

            armour += armourReductionEvent.getAmount();
            toughness += armourReductionEvent.getToughness();
        }

        if (armour <= 0.0D) {
            return;
        }

        final double damage = event.getFinalDamage();
        final double worn = armour - damage / (armourReduction.getToughnessBase() + Math.max(0.0D, toughness) / armourReduction.getToughnessDivisor());
        final double resolved = Math.min(Math.max(worn, armour / armourReduction.getArmourMinimumDivisor()), armourReduction.getArmourCap());

        event.addModifier(DamageModifier.ARMOUR, -(damage * (resolved / armourReduction.getArmourDivisor())));
    }

    /**
     * Reduces by protection enchantment levels across the whole set.
     *
     * <p>Only plain protection counts. Vanilla's type-specific variants, fire, blast and projectile
     * protection, are not handled, so armour enchanted against a specific damage type behaves as if
     * it were not.</p>
     *
     * @param event           the post stage
     * @param armourReduction the armour settings
     */
    private void applyProtection(final CustomPostDamageEvent event, final DamageConfig.ArmourReduction armourReduction) {
        if (this.isTagged(event.getSource(), DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            return;
        }

        double points = 0.0D;

        for (final ItemStack itemStack : this.getArmourPieces(event)) {
            points += itemStack.getEnchantmentLevel(Enchantment.PROTECTION);
        }

        if (points <= 0.0D) {
            return;
        }

        event.setMultiplier(DamageModifier.PROTECTION, Math.max(0.0D, 1.0D - Math.min(armourReduction.getProtectionCap(), points) / armourReduction.getProtectionDivisor()));
    }

    /**
     * The damagee's worn pieces, with empty slots dropped.
     *
     * @param event the post stage
     * @return the worn pieces
     */
    private List<ItemStack> getArmourPieces(final CustomPostDamageEvent event) {
        final ItemStack[] armourContents = event.getArmourContents();
        if (armourContents == null) {
            return List.of();
        }

        final List<ItemStack> itemStackList = new ArrayList<>(armourContents.length);

        for (final ItemStack itemStack : armourContents) {
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }

            itemStackList.add(itemStack);
        }

        return itemStackList;
    }

    /**
     * Vanilla's armour points for a material.
     *
     * @param material the worn material
     * @return the armour points
     */
    private double getArmourPoints(final Material material) {
        return switch (material) {
            case LEATHER_HELMET, LEATHER_BOOTS, CHAINMAIL_BOOTS, GOLDEN_BOOTS, COPPER_BOOTS -> 1.0D;
            case LEATHER_LEGGINGS, CHAINMAIL_HELMET, IRON_HELMET, GOLDEN_HELMET, IRON_BOOTS, TURTLE_HELMET, COPPER_HELMET -> 2.0D;
            case LEATHER_CHESTPLATE, DIAMOND_HELMET, NETHERITE_HELMET, DIAMOND_BOOTS, NETHERITE_BOOTS, GOLDEN_LEGGINGS, COPPER_LEGGINGS -> 3.0D;
            case CHAINMAIL_LEGGINGS, COPPER_CHESTPLATE -> 4.0D;
            case CHAINMAIL_CHESTPLATE, GOLDEN_CHESTPLATE, IRON_LEGGINGS -> 5.0D;
            case IRON_CHESTPLATE, DIAMOND_LEGGINGS, NETHERITE_LEGGINGS -> 6.0D;
            case DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE -> 8.0D;
            default -> 0.0D;
        };
    }

    /**
     * Vanilla's armour toughness for a material.
     *
     * <p>Only diamond and netherite have any, which is what makes them hold up against heavy hits
     * where lower tiers do not.</p>
     *
     * @param material the worn material
     * @return the toughness
     */
    private double getArmourToughness(final Material material) {
        return switch (material) {
            case DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS -> 2.0D;
            case NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> 3.0D;
            default -> 0.0D;
        };
    }

    /**
     * Whether the damage source's type carries the given tag.
     *
     * <p>A pass with no source is treated as untagged, matching the generic source the damage
     * manager falls back to when it applies one.</p>
     *
     * @param damageSource the damage source, or {@code null}
     * @param tag          the damage type tag to test
     * @return whether the source's type is tagged
     */
    private boolean isTagged(final DamageSource damageSource, final Tag<DamageType> tag) {
        return damageSource != null && tag.isTagged(damageSource.getDamageType());
    }
}