package io.github.trae.spigot.framework.death.events;

import io.github.trae.spigot.framework.damage.data.Reason;
import io.github.trae.spigot.framework.event.CustomEvent;
import io.github.trae.spigot.framework.sound.SoundProvider;
import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * An entity has died without the damage pipeline behind it.
 *
 * <p>Dispatched when the damage system is not registered, so the death system still works standing
 * on its own. Everything here is read back off the entity and the vanilla damage source after the
 * fact, which is thinner than a damage pass: no renamed sides, and no attribution beyond whatever
 * the killer happened to be holding.</p>
 *
 * @see io.github.trae.spigot.framework.death.listeners.DeathListener
 * @see CustomDeathEvent
 */
@Getter
@Setter
public class VanillaDeathEvent extends CustomEvent implements DeathEvent {

    /**
     * The entity that died.
     */
    private final LivingEntity entity;

    /**
     * The entity that killed it, or {@code null} for an environmental death.
     */
    private final Entity killer;

    /**
     * What the killing blow was recorded as.
     *
     * <p>Supplied by the death system, which falls back to {@code CUSTOM} when the entity kept no
     * last damage cause.</p>
     */
    private final DamageCause cause;

    /**
     * What the killer was holding in its main hand when it landed, or {@code null} when there was no
     * killer, it held nothing, or it was not the kind of entity that holds anything.
     */
    private final ItemStack itemStack;

    /**
     * The items that will be dropped as part of the death.
     *
     * <p>The list remains mutable so listeners can add, remove or replace drops before they are
     * ultimately handled.</p>
     */
    private final List<ItemStack> drops;

    /**
     * The amount of experience that will be dropped as part of the death.
     *
     * <p>Mutable so listeners can change or suppress the experience awarded by the death.</p>
     */
    private int dropExp;

    /**
     * The sound played for the death, or {@code null} for none.
     *
     * <p>Mutable so listeners can replace the sound, or clear it to keep the death silent.</p>
     */
    private SoundProvider soundProvider;

    /**
     * Captures the held item at construction rather than on demand, since by the time a listener
     * reads it the killer may have swapped, dropped or broken what it killed with.
     *
     * @param entity        the entity that died
     * @param killer        the entity that killed it, or {@code null} for an environmental death
     * @param cause         the damage cause behind the death
     * @param drops         the items that will be dropped as part of the death
     * @param dropExp       the amount of experience that will be dropped as part of the death
     * @param soundProvider the sound played for the death, or {@code null} for none
     */
    public VanillaDeathEvent(final LivingEntity entity, final Entity killer, final DamageCause cause, final List<ItemStack> drops, final int dropExp, final SoundProvider soundProvider) {
        this.entity = entity;
        this.killer = killer;
        this.cause = cause;

        this.itemStack = Optional.ofNullable(killer)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .map(LivingEntity::getEquipment)
                .map(EntityEquipment::getItemInMainHand)
                .filter(value -> !value.isEmpty())
                .orElse(null);

        this.drops = drops;
        this.dropExp = dropExp;
        this.soundProvider = soundProvider;
    }

    /**
     * The killer's weapon, standing in for the reason a damage pass would have supplied.
     *
     * <p>Built from the item's effective name with the item's hover attached, so a renamed or
     * enchanted weapon reads in chat the way it reads in an inventory, and an unnamed one still reads
     * as its item type. The name is coloured green unless it already carries a colour of its own.</p>
     *
     * @return the weapon as a reason, or {@code null} when the killer had nothing in hand
     */
    @Override
    public final Reason getReason() {
        return Optional.ofNullable(this.itemStack)
                .map(value -> value.effectiveName().hoverEvent(value.asHoverEvent()))
                .map(component -> component.colorIfAbsent(UtilColor.toTextColor(ChatColor.GREEN.getColor())))
                .map(Reason::of)
                .orElse(null);
    }
}