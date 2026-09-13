package io.github.trae.spigot.framework.tablist;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.tablist.events.TablistUpdateEvent;
import io.github.trae.spigot.framework.tablist.listeners.TablistListener;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.UtilServer;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages each player's tab list header and footer by resolving the lowest-priority eligible
 * {@link Tablist} (discovered via the dependency injector) and sending it to the player.
 * <p>
 * An asynchronous scheduler dispatches a {@link TablistUpdateEvent} for every online player once a
 * second, so dynamic header and footer content is re-resolved and re-sent on that interval. The set
 * of players currently showing a tablist is tracked so the clearing packet only fires once on the
 * transition to "no tablist", rather than on every dispatch.
 * <p>
 * Unlike the sidebar, nothing is cached to diff against, so every dispatch is a send. That is why
 * the interval is a second rather than a tick: at a high player count, a faster interval is
 * bandwidth spent re-sending content that has not changed.
 * <p>
 * Update events and player quit are handled by {@link TablistListener}.
 */
@Getter
@Singleton
public class TablistManager {

    /**
     * Every registered tablist, sorted by priority.
     * <p>
     * Resolved on first use rather than on every lookup, since the set never changes once the
     * container has finished scanning.
     */
    private List<Tablist> tablistList;

    /**
     * The players currently showing a tablist, so the clearing packet fires once on the transition
     * away from one rather than on every dispatch.
     */
    private final Set<UUID> activeTablistSet = ConcurrentHashMap.newKeySet();

    /**
     * Dispatches a {@link TablistUpdateEvent} for every online player once a second, driving the
     * per-player resolution in {@link TablistListener}.
     */
    @Scheduler(period = 1, unit = TimeUnit.SECONDS, asynchronous = true)
    public final void onScheduler() {
        UtilServer.getOnlinePlayers().forEach(player -> UtilEvent.dispatch(new TablistUpdateEvent(player)));
    }

    /**
     * Sends the resolved tablist's header and footer to the player and marks the player as actively
     * displaying a tablist.
     * <p>
     * This is intentionally not guarded by {@link #activeTablistSet}: the header and footer are
     * re-resolved and re-sent on every dispatch so that dynamic content stays current.
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
     * Resolves the eligible tablist for the player, the one with the lowest priority that passes
     * both the global and per-player display checks.
     * <p>
     * Populates the sorted list on first use, so each call costs only a walk of an already-ordered
     * list rather than a fresh scan and sort.
     *
     * @param player the player to resolve for
     * @return an {@link Optional} containing the eligible tablist, or empty if none qualify
     */
    public final Optional<Tablist> getEligibleTablist(final Player player) {
        if (this.tablistList == null) {
            this.tablistList = InjectorApi.getAll(Tablist.class).stream().sorted(Comparator.comparingInt(Tablist::getPriority)).toList();
        }

        return this.tablistList.stream().filter(tablist -> tablist.canDisplay() && tablist.canDisplay(player)).findFirst();
    }
}