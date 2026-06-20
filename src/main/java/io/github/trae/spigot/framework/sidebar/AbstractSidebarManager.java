package io.github.trae.spigot.framework.sidebar;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.hf.Manager;
import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.sidebar.events.SidebarUpdateEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.UtilNms;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages per-player sidebars using direct NMS scoreboard packets.
 * <p>
 * Resolves the lowest-priority eligible {@link Sidebar} for each player (discovered via the
 * dependency injector) and sends create, update, and clear packets directly through
 * {@link UtilNms}. The currently displayed sidebar, along with its rendered title and lines, is
 * cached per player so that updates only send packets for content that actually changed,
 * eliminating flicker.
 * <p>
 * A scheduler re-evaluates eligibility and drives animated (non-static) titles. Player join and
 * quit are handled automatically, as are {@link SidebarUpdateEvent}s.
 *
 * @param <Plugin> the plugin type this manager belongs to
 */
public class AbstractSidebarManager<Plugin extends SpigotPlugin> implements Manager<Plugin>, Listener {

    private final ConcurrentHashMap<UUID, Sidebar> activeSidebarMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Component> cachedTitleMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<Component>> cachedLinesMap = new ConcurrentHashMap<>();

    /**
     * Periodic tick that, for each online player, re-resolves eligibility of their active sidebar
     * and updates the title if it is animated.
     * <p>
     * If the active sidebar is no longer eligible (failing either {@link Sidebar#canDisplay()} or
     * {@link Sidebar#canDisplay(Player)}), a {@link SidebarUpdateEvent} is dispatched to re-resolve
     * and switch to the next eligible sidebar. Otherwise {@link #updateTitle(Player)} is called to
     * refresh animated titles.
     */
    @Scheduler(period = 100, unit = TimeUnit.MILLISECONDS)
    public void onScheduler() {
        for (final Player player : Bukkit.getServer().getOnlinePlayers()) {
            final Sidebar activeSidebar = this.activeSidebarMap.get(player.getUniqueId());
            if (activeSidebar != null) {
                if (!(activeSidebar.canDisplay()) || !(activeSidebar.canDisplay(player))) {
                    UtilEvent.dispatch(new SidebarUpdateEvent(player));
                    continue;
                }
            }

            this.updateTitle(player);
        }
    }

    /**
     * Creates and displays the given sidebar for the player by sending the objective, display-slot,
     * and all line packets, then caches the rendered title and lines.
     * <p>
     * Line {@code i} is sent at score {@code (size - 1 - i)} so the first line renders at the top.
     *
     * @param player  the player to display the sidebar to
     * @param sidebar the sidebar to create
     */
    private void create(final Player player, final Sidebar sidebar) {
        final String identifier = sidebar.getIdentifier();
        final Component title = sidebar.getTitle(player);
        final List<Component> lines = sidebar.getLines(player);

        UtilNms.sendPacket(player, new ClientboundSetObjectivePacket(
                new Objective(
                        new Scoreboard(),
                        identifier,
                        ObjectiveCriteria.DUMMY,
                        UtilNms.toNms(title),
                        ObjectiveCriteria.RenderType.INTEGER,
                        false,
                        null
                ),
                ClientboundSetObjectivePacket.METHOD_ADD
        ));

        UtilNms.sendPacket(player, new ClientboundSetDisplayObjectivePacket(
                DisplaySlot.SIDEBAR,
                new Objective(
                        new Scoreboard(),
                        identifier,
                        ObjectiveCriteria.DUMMY,
                        UtilNms.toNms(title),
                        ObjectiveCriteria.RenderType.INTEGER,
                        false,
                        null
                )
        ));

        final int size = lines.size();

        for (int index = 0; index < size; index++) {
            this.sendLine(player, identifier, lines.get(index), size - 1 - index);
        }

        this.cachedTitleMap.put(player.getUniqueId(), title);
        this.cachedLinesMap.put(player.getUniqueId(), lines);
    }

    /**
     * Re-resolves the active sidebar's title for the player and, if it changed since last render,
     * sends a title-change packet and updates the cache.
     * <p>
     * No-op if the player has no active sidebar or its title is static.
     *
     * @param player the player whose title to refresh
     */
    private void updateTitle(final Player player) {
        final Sidebar activeSidebar = this.activeSidebarMap.get(player.getUniqueId());
        if (activeSidebar == null || activeSidebar.isStaticTitle()) {
            return;
        }

        final Component newTitle = activeSidebar.getTitle(player);
        final Component cachedTitle = this.cachedTitleMap.get(player.getUniqueId());

        if (newTitle.equals(cachedTitle)) {
            return;
        }

        UtilNms.sendPacket(player, new ClientboundSetObjectivePacket(
                new Objective(new Scoreboard(), activeSidebar.getIdentifier(), ObjectiveCriteria.DUMMY, UtilNms.toNms(newTitle), ObjectiveCriteria.RenderType.INTEGER, false, null),
                ClientboundSetObjectivePacket.METHOD_CHANGE
        ));

        this.cachedTitleMap.put(player.getUniqueId(), newTitle);
    }

    /**
     * Diffs the sidebar's current lines against the cached lines for the player and sends packets
     * only for changed lines, removing any lines that no longer exist. Updates the line cache
     * afterwards.
     * <p>
     * Line {@code i} maps to score {@code (size - 1 - i)}, matching {@link #create(Player, Sidebar)}.
     * Lines are compared by their list index directly, so adding or removing a line only resends the
     * lines whose content actually changed, and stale trailing scores from the previous (longer)
     * render are removed.
     *
     * @param player  the player whose lines to update
     * @param sidebar the sidebar providing the new lines
     */
    private void updateLines(final Player player, final Sidebar sidebar) {
        final List<Component> newLines = sidebar.getLines(player);
        final List<Component> oldLines = this.cachedLinesMap.getOrDefault(player.getUniqueId(), Collections.emptyList());

        final int newSize = newLines.size();
        final int oldSize = oldLines.size();

        for (int index = 0; index < newSize; index++) {
            final Component newLine = newLines.get(index);
            final Component oldLine = index < oldSize ? oldLines.get(index) : null;

            if (!(newLine.equals(oldLine))) {
                this.sendLine(player, sidebar.getIdentifier(), newLine, newSize - 1 - index);
            }
        }

        for (int index = newSize; index < oldSize; index++) {
            this.removeLine(player, sidebar.getIdentifier(), oldSize - 1 - index);
        }

        this.cachedLinesMap.put(player.getUniqueId(), newLines);
    }

    /**
     * Sends a single score line to the player. The score entry is keyed by player UUID and score to
     * keep each slot unique and stable across renames, and the score value drives the line ordering.
     *
     * @param player     the player to send the line to
     * @param identifier the objective identifier
     * @param line       the line component
     * @param score      the score value (higher renders nearer the top)
     */
    private void sendLine(final Player player, final String identifier, final Component line, final int score) {
        UtilNms.sendPacket(player, new ClientboundSetScorePacket(
                this.getScoreOwner(player, score),
                identifier,
                score,
                Optional.of(UtilNms.toNms(line)),
                Optional.empty()
        ));
    }

    /**
     * Removes a single score line from the player by resetting its score entry.
     *
     * @param player     the player to remove the line from
     * @param identifier the objective identifier
     * @param score      the score value to remove
     */
    private void removeLine(final Player player, final String identifier, final int score) {
        UtilNms.sendPacket(player, new ClientboundResetScorePacket(this.getScoreOwner(player, score), identifier));
    }

    /**
     * Clears the player's sidebar, evicting their title and line caches and removing the active
     * sidebar entry, then sending an objective-remove packet if a sidebar was active.
     *
     * @param player the player whose sidebar to clear
     */
    private void clear(final Player player) {
        this.cachedTitleMap.remove(player.getUniqueId());
        this.cachedLinesMap.remove(player.getUniqueId());

        final Sidebar activeSidebar = this.activeSidebarMap.remove(player.getUniqueId());
        if (activeSidebar == null) {
            return;
        }

        UtilNms.sendPacket(player, new ClientboundSetObjectivePacket(
                new Objective(
                        new Scoreboard(),
                        activeSidebar.getIdentifier(),
                        ObjectiveCriteria.DUMMY,
                        UtilNms.toNms(activeSidebar.getTitle(player)),
                        ObjectiveCriteria.RenderType.INTEGER,
                        false,
                        null
                ),
                ClientboundSetObjectivePacket.METHOD_REMOVE
        ));
    }

    /**
     * Resolves the eligible sidebar for the player — the one with the lowest priority that passes
     * both the global and per-player display checks.
     *
     * @param player the player to resolve a sidebar for
     * @return an {@link Optional} containing the eligible sidebar, or empty if none qualify
     */
    private Optional<Sidebar> getEligibleSidebar(final Player player) {
        return InjectorApi.getAll(Sidebar.class)
                .stream()
                .sorted(Comparator.comparingInt(Sidebar::getPriority))
                .filter(sidebar -> sidebar.canDisplay() && sidebar.canDisplay(player))
                .findFirst();
    }

    /**
     * Handles a {@link SidebarUpdateEvent}.
     * <p>
     * If the event is scoped to an identifier that does not match the player's active sidebar, the
     * event is ignored. Otherwise the eligible sidebar is re-resolved: if none qualify the sidebar
     * is cleared; if the same sidebar remains active its lines are diffed and updated; if a
     * different sidebar wins the old one is cleared and the new one created.
     *
     * @param event the sidebar update event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSidebarUpdate(final SidebarUpdateEvent event) {
        final Player player = event.getPlayer();

        final Sidebar activeSidebar = this.activeSidebarMap.get(player.getUniqueId());

        if (event.getIdentifier() != null && (activeSidebar == null || !(activeSidebar.getIdentifier().equals(event.getIdentifier())))) {
            return;
        }

        final Optional<Sidebar> eligibleSidebarOptional = this.getEligibleSidebar(player);
        if (eligibleSidebarOptional.isEmpty()) {
            this.clear(player);
            return;
        }

        final Sidebar eligibleSidebar = eligibleSidebarOptional.get();

        if (activeSidebar != null && activeSidebar.getIdentifier().equals(eligibleSidebar.getIdentifier())) {
            this.updateLines(player, eligibleSidebar);
        } else {
            this.clear(player);
            this.create(player, eligibleSidebar);
        }

        this.activeSidebarMap.put(player.getUniqueId(), eligibleSidebar);
    }

    /**
     * Creates the eligible sidebar for a player when they join, if any qualifies.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        this.getEligibleSidebar(player).ifPresent(sidebar -> {
            this.create(player, sidebar);
            this.activeSidebarMap.put(player.getUniqueId(), sidebar);
        });
    }

    /**
     * Clears all sidebar state for a player when they quit.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.clear(event.getPlayer());
    }

    /**
     * Builds the unique score-holder name for a line slot from the player's UUID and score, keeping
     * each line's entry stable across renames and distinct per score.
     *
     * @param player the player the line belongs to
     * @param score  the score value identifying the line slot
     * @return the score-holder name in the format {@code {uuid}:{score}}
     */
    private String getScoreOwner(final Player player, final int score) {
        return "%s:%s".formatted(player.getUniqueId(), score);
    }
}