package io.github.trae.spigot.framework.death;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.DamageManager;

/**
 * Whether the death system has the damage pipeline behind it.
 *
 * <p>Both subsystems are opt-in, so the death system cannot assume the damage one was scanned in.
 * The answer decides which shape of death event is dispatched: the enriched one when there is a
 * damage pass to hang off, the vanilla one when there is not.</p>
 *
 * @see io.github.trae.spigot.framework.death.listeners.DeathListener
 */
@Singleton
public final class DeathManager {

    /**
     * The cached answer, {@code null} until it is first asked for.
     */
    private Boolean damageManagerRegistered;

    /**
     * Whether the damage manager is registered.
     *
     * <p>Resolved once and kept, since registration is settled at startup and this sits on the path
     * of every death. A lookup that throws is taken as not registered, since the death system has to
     * keep working either way.</p>
     *
     * @return {@code true} when the damage pipeline is available
     */
    public boolean isDamageManagerRegistered() {
        if (this.damageManagerRegistered == null) {
            try {
                this.damageManagerRegistered = InjectorApi.get(DamageManager.class) != null;
            } catch (final Exception e) {
                this.damageManagerRegistered = false;
            }
        }

        return this.damageManagerRegistered;
    }
}