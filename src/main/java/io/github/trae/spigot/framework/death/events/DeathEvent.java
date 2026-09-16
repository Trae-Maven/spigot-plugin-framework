package io.github.trae.spigot.framework.death.events;

import io.github.trae.spigot.framework.damage.data.CustomReason;
import io.github.trae.spigot.framework.damage.data.Reason;
import io.github.trae.spigot.framework.sound.SoundProvider;
import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.UtilMessage;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.utilities.UtilString;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * What every death the framework dispatches has in common.
 *
 * <p>Implemented by both shapes of death event, so a listener that only cares who died, who killed
 * them and what it is attributed to can take either without knowing whether the damage pipeline was
 * involved. The same goes for the parts a listener can still change: drops, experience and the death
 * sound are exposed here, so either shape can be adjusted through this interface alone.</p>
 *
 * <p>The message formatting lives here rather than in the listener, so both shapes read the same way
 * whichever produced them.</p>
 *
 * @see CustomDeathEvent
 * @see VanillaDeathEvent
 */
public interface DeathEvent {

    /**
     * The entity that died.
     *
     * @return the dead entity
     */
    LivingEntity getEntity();

    /**
     * The entity credited with the kill.
     *
     * @return the killer, or {@code null} for an environmental death
     */
    Entity getKiller();

    /**
     * What the killing blow was recorded as.
     *
     * @return the damage cause behind the death
     */
    DamageCause getCause();

    /**
     * The cause as it belongs in a message, stripped of its enum shape and coloured.
     *
     * <p>Used only as the last resort in a message, when there is no killer to name instead.</p>
     *
     * @return the cause name
     */
    default Component getCauseName() {
        return Component.text(UtilString.clean(this.getCause().name())).color(UtilColor.toTextColor(ChatColor.YELLOW.getColor()));
    }

    /**
     * What the death is attributed to, such as the weapon that landed it or an ability that claimed
     * it earlier.
     *
     * @return the reason, or {@code null} when there is nothing to attribute it to
     */
    Reason getReason();

    /**
     * The items that will be dropped as part of the death.
     *
     * <p>The list is mutable and is written back to the vanilla death after dispatch, so adding,
     * removing or replacing entries changes what actually drops.</p>
     *
     * @return the drops
     */
    List<ItemStack> getDrops();

    /**
     * The amount of experience that will be dropped as part of the death.
     *
     * @return the experience to drop
     */
    int getDropExp();

    /**
     * Changes the amount of experience dropped as part of the death.
     *
     * @param dropExp the experience to drop, or {@code 0} to suppress it
     */
    void setDropExp(final int dropExp);

    /**
     * The sound played for the death.
     *
     * <p>Starts as the vanilla death sound. An entity with no death sound still gets a provider, one
     * that plays nothing.</p>
     *
     * @return the sound, or {@code null} when a listener has cleared it to silence the death
     */
    SoundProvider getSoundProvider();

    /**
     * Changes the sound played for the death.
     *
     * @param soundProvider the sound to play, or {@code null} to keep the death silent
     */
    void setSoundProvider(final SoundProvider soundProvider);

    /**
     * The reason with an article in front of it, ready to drop into a sentence.
     *
     * <p>A {@link CustomReason} is left bare: it names an ability rather than a thing, and reads
     * wrong with an article attached.</p>
     *
     * @return the article-prefixed reason, or {@code null} when there is no reason to name
     */
    default Component getFormattedReason() {
        final Reason reason = this.getReason();
        if (reason == null) {
            return null;
        }

        final Component reasonName = reason.getName();
        if (reasonName == null) {
            return null;
        }

        final String indefiniteArticlePrefix = reason instanceof CustomReason ? "" : UtilString.getIndefiniteArticlePrefix(PlainTextComponentSerializer.plainText().serialize(reasonName));

        return UtilMessage.deserialize(indefiniteArticlePrefix + UtilMessage.serializeWithReset(reasonName));
    }
}