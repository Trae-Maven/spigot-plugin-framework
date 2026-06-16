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

public class AbstractSidebarManager<Plugin extends SpigotPlugin> implements Manager<Plugin>, Listener {

    private final ConcurrentHashMap<UUID, Sidebar> activeSidebarMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Component> cachedTitleMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<Component>> cachedLinesMap = new ConcurrentHashMap<>();

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

        for (int index = 0; index < lines.size(); index++) {
            this.sendLine(player, identifier, lines.get(index), index);
        }

        this.cachedTitleMap.put(player.getUniqueId(), title);
        this.cachedLinesMap.put(player.getUniqueId(), lines);
    }

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

    private void updateLines(final Player player, final Sidebar sidebar) {
        final List<Component> newLines = sidebar.getLines(player);
        final List<Component> oldLines = this.cachedLinesMap.getOrDefault(player.getUniqueId(), Collections.emptyList());

        for (int index = 0; index < newLines.size(); index++) {
            final Component newLine = newLines.get(index);
            if (index >= oldLines.size() || !(newLine.equals(oldLines.get(index)))) {
                this.sendLine(player, sidebar.getIdentifier(), newLine, index);
            }
        }

        for (int index = newLines.size(); index < oldLines.size(); index++) {
            this.removeLine(player, sidebar.getIdentifier(), index);
        }

        this.cachedLinesMap.put(player.getUniqueId(), newLines);
    }

    private void sendLine(final Player player, final String identifier, final Component line, final int score) {
        UtilNms.sendPacket(player, new ClientboundSetScorePacket(
                "%s_%s".formatted(player.getName(), score),
                identifier,
                score,
                Optional.of(UtilNms.toNms(line)),
                Optional.empty()
        ));
    }

    private void removeLine(final Player player, final String identifier, final int score) {
        UtilNms.sendPacket(player, new ClientboundResetScorePacket(
                "%s_%s".formatted(player.getName(), score),
                identifier
        ));
    }

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

    private Optional<Sidebar> getEligibleSidebar(final Player player) {
        return InjectorApi.getAll(Sidebar.class)
                .stream()
                .sorted(Comparator.comparingInt(Sidebar::getPriority))
                .filter(sidebar -> sidebar.canDisplay() && sidebar.canDisplay(player))
                .findFirst();
    }

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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        this.getEligibleSidebar(player).ifPresent(sidebar -> {
            this.create(player, sidebar);
            this.activeSidebarMap.put(player.getUniqueId(), sidebar);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.clear(event.getPlayer());
    }
}