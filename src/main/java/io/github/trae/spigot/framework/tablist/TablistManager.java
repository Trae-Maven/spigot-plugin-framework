package io.github.trae.spigot.framework.tablist;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.tablist.events.TablistUpdateEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.UtilServer;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages each player's tab list header and footer by resolving the lowest-priority eligible
 * {@link Tablist} (discovered via the dependency injector) and sending it to the player.
 * <p>
 * An asynchronous scheduler dispatches a {@link TablistUpdateEvent} for every online player on a
 * fixed interval, so dynamic header/footer content is re-resolved and re-sent each tick. The set of
 * players currently showing a tablist is tracked so the clearing packet only fires once on the
 * transition to "no tablist", rather than every tick.
 * <p>
 * Update events and player quit are handled by {@link TablistListener}.
 */
@Getter
@Singleton
public class TablistManager {

    private final Set<UUID> activeTablistSet = ConcurrentHashMap.newKeySet();

    /**
     * Dispatches a {@link TablistUpdateEvent} for every online player on a fixed interval, driving
     * the per-player resolution in {@link TablistListener}.
     */
    @Scheduler(period = 100, unit = TimeUnit.MILLISECONDS, asynchronous = true)
    public final void onScheduler() {
        UtilServer.getOnlinePlayers().forEach(player -> UtilEvent.dispatch(new TablistUpdateEvent(player)));
    }

    /**
     * Sends the resolved tablist's header and footer to the player and marks the player as actively
     * displaying a tablist.
     * <p>
     * This is intentionally not guarded by {@link #activeTablistSet}: the header and footer are
     * re-deserialized and re-sent on every dispatch so that dynamic content stays current.
     *
     * @param player  the player the tablist is sent to
     * @param tablist the tablist supplying the header and footer
     */
    public final void create(final Player player, final Tablist tablist) {
        player.sendPlayerListHeaderAndFooter(tablist.getHeader(player), tablist.getFooter(player));

        this.activeTablistSet.add(player.getUniqueId());
    }

    /**
     * Clears the player's tab list header and footer by sending empty components.
     * <p>
     * No-ops when the player has no tablist currently registered, so this is safe to call every
     * dispatch and only emits the clearing packet once, on the transition away from an active
     * tablist.
     *
     * @param player the player whose tablist to clear
     */
    public final void remove(final Player player) {
        if (!this.activeTablistSet.remove(player.getUniqueId())) {
            return;
        }

        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
    }

    /**
     * Resolves the eligible tablist for the player — the one with the lowest priority that passes
     * both the global and per-player display checks.
     *
     * @param player the player to resolve for
     * @return an {@link Optional} containing the eligible tablist, or empty if none qualify
     */
    public final Optional<Tablist> getEligibleTablist(final Player player) {
        return InjectorApi.getAll(Tablist.class)
                .stream()
                .sorted(Comparator.comparingInt(Tablist::getPriority))
                .filter(tablist -> tablist.canDisplay() && tablist.canDisplay(player))
                .findFirst();
    }
}