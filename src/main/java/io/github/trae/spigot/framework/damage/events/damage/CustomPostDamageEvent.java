package io.github.trae.spigot.framework.damage.events.damage;

import io.github.trae.spigot.framework.damage.data.Reason;
import io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import io.github.trae.spigot.framework.displayname.DisplayName;
import io.github.trae.spigot.framework.sound.SoundProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * The final stage of a damage pass, for reductions and side effects.
 *
 * <p>By the time this fires the base is settled, so reductions here act on whatever an ability
 * decided. Armour, protection and resistance run early in this stage; durability, knockback, the
 * hit sound and the damage delay run at the end of it.</p>
 *
 * <p>Cancelling still prevents the damage being applied, since the manager runs after this stage
 * completes.</p>
 *
 * <p>This is also the stage the manager retains per entity, so the death system can read who dealt
 * the killing blow, with what, and why.</p>
 *
 * @see CustomDamageEvent
 * @see io.github.trae.spigot.framework.damage.DamageManager
 */
public class CustomPostDamageEvent extends AbstractCustomDamageEvent {

    /**
     * Assigns the pass state directly.
     *
     * <p>Protected because this stage is only ever built from the damage stage: use the copying
     * constructor rather than assembling one field by field.</p>
     *
     * @param systemTime     when the pass began, in milliseconds
     * @param additiveMap    the flat contributions, shared between stages
     * @param multiplierMap  the proportional contributions, shared between stages
     * @param damagee        the entity taking the damage
     * @param damager        the entity dealing it, or {@code null} for environmental causes
     * @param projectile     the projectile that carried it, or {@code null} for a direct attack
     * @param source         the vanilla damage source
     * @param cause          what caused the damage
     * @param itemStack      the damager's held item, or {@code null}
     * @param armourContents the damagee's worn armour, or {@code null}
     * @param originalDamage what vanilla would have dealt
     * @param critical       whether the attack was a critical hit
     * @param damage         the base damage before modifiers
     * @param soundProvider  the sound played when the damage lands, or {@code null}
     * @param delay          the immunity period afterwards, in milliseconds
     * @param damageeName    how the damagee is named in messages
     * @param damagerName    how the damager is named in messages, or {@code null}
     * @param causeName      how the cause reads in messages
     * @param reason         what the damage is attributed to, or {@code null}
     */
    protected CustomPostDamageEvent(final long systemTime, final Map<DamageModifier, Double> additiveMap, final Map<DamageModifier, Double> multiplierMap, final Entity damagee, final Entity damager, final Projectile projectile, final DamageSource source, final EntityDamageEvent.DamageCause cause, final ItemStack itemStack, final ItemStack[] armourContents, final double originalDamage, final boolean critical, final SoundProvider soundProvider, final double damage, final long delay, final DisplayName damageeName, final DisplayName damagerName, final Component causeName, final Reason reason) {
        super(systemTime, additiveMap, multiplierMap, damagee, damager, projectile, source, cause, itemStack, armourContents, originalDamage, critical, soundProvider, damage, delay, damageeName, damagerName, causeName, reason);
    }

    /**
     * Carries the damage stage forward.
     *
     * <p>The modifier maps are passed by reference rather than copied, so everything contributed by
     * the earlier stages is still in place here. Names, sound and reason carry forward by value.</p>
     *
     * @param event the damage stage that produced this one
     */
    public CustomPostDamageEvent(final CustomDamageEvent event) {
        this(
                event.getSystemTime(),
                event.getAdditiveMap(),
                event.getMultiplierMap(),
                event.getDamagee(),
                event.getDamager(),
                event.getProjectile(),
                event.getSource(),
                event.getCause(),
                event.getItemStack().orElse(null),
                event.getArmourContents(),
                event.getOriginalDamage(),
                event.isCritical(),
                event.getSoundProvider(),
                event.getDamage(),
                event.getDelay(),
                event.getDamageeName(),
                event.getDamagerName(),
                event.getCauseName(),
                event.getReason()
        );
    }
}