package io.github.trae.spigot.framework.damage.listeners.reduction;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.damage.events.reduction.ArmourReductionEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.enums.ArmourType;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Reduces incoming damage by armour, protection enchantments and resistance.
 *
 * <p>Runs early in the post stage, so it acts on whatever an ability decided at the damage stage
 * rather than on the weapon's raw contribution. Each of the three has its own set of causes it does
 * not apply to, mirroring vanilla's damage type tags.</p>
 *
 * <p>Armour values come from a per-piece event rather than the entity's armour attribute, which is
 * what lets a game mode rebalance armour by class without replacing this listener.</p>
 *
 * @see ArmourReductionEvent
 */
@Singleton
public class DamageArmourReductionListener implements Listener {

    private static final double ARMOUR_CAP = 20.0D;
    private static final double ARMOUR_DIVISOR = 25.0D;
    private static final double ARMOUR_MINIMUM_DIVISOR = 5.0D;
    private static final double TOUGHNESS_BASE = 2.0D;
    private static final double TOUGHNESS_DIVISOR = 4.0D;

    private static final double PROTECTION_CAP = 20.0D;
    private static final double PROTECTION_DIVISOR = 25.0D;

    private static final double RESISTANCE_PER_LEVEL = 0.2D;
    private static final int RESISTANCE_CAP = 5;

    /**
     * Applies all three reductions, in vanilla's order.
     *
     * @param event the post stage
     */
    @EventHandler(priority = EventPriority.LOW)
    public final void onCustomPostDamage(final CustomPostDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getDamagee() instanceof final LivingEntity damagee)) {
            return;
        }

        this.applyArmour(event);
        this.applyProtection(event);
        this.applyResistance(event, damagee);
    }

    /**
     * Reduces by the damagee's worn armour.
     *
     * <p>Each piece reports its own value and toughness through its event; the totals then go
     * through vanilla's curve, where heavier hits cut through more armour and toughness blunts that
     * effect. The floor at a fifth of the total is what stops a large hit negating armour
     * entirely.</p>
     *
     * @param event the post stage
     */
    private void applyArmour(final CustomPostDamageEvent event) {
        if (this.bypassesArmour(event.getCause())) {
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
        final double resolved = Math.clamp(armour - damage / (TOUGHNESS_BASE + Math.max(0.0D, toughness) / TOUGHNESS_DIVISOR), armour / ARMOUR_MINIMUM_DIVISOR, ARMOUR_CAP);

        event.addModifier(DamageModifier.ARMOUR, -(damage * (resolved / ARMOUR_DIVISOR)));
    }

    /**
     * Reduces by protection enchantment levels across the whole set.
     *
     * <p>Only plain protection counts. Vanilla's type-specific variants, fire, blast and projectile
     * protection, are not handled, so armour enchanted against a specific damage type behaves as if
     * it were not.</p>
     *
     * @param event the post stage
     */
    private void applyProtection(final CustomPostDamageEvent event) {
        if (this.bypassesEnchantments(event.getCause())) {
            return;
        }

        double points = 0.0D;

        for (final ItemStack itemStack : this.getArmourPieces(event)) {
            points += itemStack.getEnchantmentLevel(Enchantment.PROTECTION);
        }

        if (points <= 0.0D) {
            return;
        }

        event.setMultiplier(DamageModifier.PROTECTION, 1.0D - Math.min(PROTECTION_CAP, points) / PROTECTION_DIVISOR);
    }

    /**
     * Reduces by the damagee's resistance effect, twenty percent per level.
     *
     * <p>Capped at five levels, which is total immunity.</p>
     *
     * @param event   the post stage
     * @param damagee the entity taking the damage
     */
    private void applyResistance(final CustomPostDamageEvent event, final LivingEntity damagee) {
        if (this.bypassesResistance(event.getCause())) {
            return;
        }

        final PotionEffect potionEffect = damagee.getPotionEffect(PotionEffectType.RESISTANCE);
        if (potionEffect == null) {
            return;
        }

        final int level = Math.min(RESISTANCE_CAP, potionEffect.getAmplifier() + 1);

        event.setMultiplier(DamageModifier.RESISTANCE, Math.max(0.0D, 1.0D - level * RESISTANCE_PER_LEVEL));
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
     * Whether armour protects against this cause.
     *
     * <p>Mirrors vanilla's rule that armour is physical: it stops nothing already inside you, and
     * nothing a helmet could not block.</p>
     *
     * @param damageCause the cause of the damage
     * @return whether armour is skipped
     */
    private boolean bypassesArmour(final DamageCause damageCause) {
        return switch (damageCause) {
            case DROWNING, STARVATION, SUFFOCATION, VOID, POISON, WITHER, MAGIC, FREEZE, DRYOUT, KILL, SUICIDE, CUSTOM -> true;
            default -> false;
        };
    }

    /**
     * Whether protection enchantments apply to this cause.
     *
     * @param damageCause the cause of the damage
     * @return whether enchantments are skipped
     */
    private boolean bypassesEnchantments(final DamageCause damageCause) {
        return switch (damageCause) {
            case VOID, KILL, SUICIDE, CUSTOM, STARVATION -> true;
            default -> false;
        };
    }

    /**
     * Whether resistance applies to this cause.
     *
     * @param damageCause the cause of the damage
     * @return whether resistance is skipped
     */
    private boolean bypassesResistance(final DamageCause damageCause) {
        return switch (damageCause) {
            case VOID, KILL, SUICIDE, CUSTOM, STARVATION -> true;
            default -> false;
        };
    }
}