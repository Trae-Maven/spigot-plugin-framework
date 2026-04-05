package io.github.trae.spigot.framework.scoreboard;

import io.github.trae.di.annotations.method.PostConstruct;
import io.github.trae.di.annotations.method.PreDestroy;
import io.github.trae.di.annotations.type.component.Service;
import io.github.trae.spigot.framework.scoreboard.data.BoardBuilder;
import io.github.trae.spigot.framework.scoreboard.data.BoardEntry;
import io.github.trae.spigot.framework.scoreboard.events.ScoreboardCleanupEvent;
import io.github.trae.spigot.framework.scoreboard.events.ScoreboardReceiveEvent;
import io.github.trae.spigot.framework.scoreboard.events.ScoreboardRemoveEvent;
import io.github.trae.spigot.framework.scoreboard.events.ScoreboardUpdateEvent;
import io.github.trae.spigot.framework.scoreboard.interfaces.IScoreboardManager;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.UtilNms;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous packet-based scoreboard manager with priority resolution.
 *
 * <p>Multiple systems (lobby, game, admin, etc.) can register scoreboards
 * for a player via {@link #set}. The highest priority board is rendered,
 * and if removed via {@link #remove}, the next highest takes over.</p>
 *
 * <p>Updates are diffed against the currently rendered state — only
 * changed lines and title produce packets, eliminating flicker and
 * minimising bandwidth.</p>
 *
 * <p>All diffing and packet construction runs on a dedicated single-threaded
 * executor. Packet sending is safe from any thread as it writes directly
 * to the Netty channel pipeline.</p>
 */
@Service
public class ScoreboardManager implements IScoreboardManager {

    /**
     * The objective name used for the sidebar display.
     */
    private static final String OBJECTIVE = "sidebar";

    /**
     * Maximum number of sidebar lines supported.
     */
    private static final int MAX_LINES = 32;

    /**
     * Unique owner strings per line slot.
     *
     * <p>Since 1.20.3+, the display component override controls what the
     * player sees — the owner string just needs to be unique and is never
     * rendered.</p>
     */
    private static final String[] SLOT_OWNERS = new String[MAX_LINES];

    static {
        for (int i = 0; i < MAX_LINES; i++) {
            SLOT_OWNERS[i] = "$slot_%02d".formatted(i);
        }
    }

    /**
     * All registered boards per player, keyed by source identifier.
     */
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, BoardEntry>> boardsMap = new ConcurrentHashMap<>();

    /**
     * The currently rendered title per player, used for diffing.
     */
    private final ConcurrentHashMap<UUID, Component> renderedTitleMap = new ConcurrentHashMap<>();

    /**
     * The currently rendered lines per player, used for diffing.
     */
    private final ConcurrentHashMap<UUID, Component[]> renderedLinesMap = new ConcurrentHashMap<>();

    /**
     * Tracks whether the objective creation packet has been sent for a player.
     */
    private final Set<UUID> initializedSet = ConcurrentHashMap.newKeySet();

    /**
     * Single-threaded executor for async diff and packet construction.
     */
    private ExecutorService executorService;

    @PostConstruct
    public void onPostConstruct() {
        this.executorService = Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "Scoreboard-Asynchronous");
            thread.setDaemon(true);
            return thread;
        });
    }

    @PreDestroy
    public void onPreDestroy() {
        if (this.executorService != null) {
            this.executorService.shutdown();
        }

        this.boardsMap.clear();
        this.renderedTitleMap.clear();
        this.renderedLinesMap.clear();
        this.initializedSet.clear();
    }

    /**
     * Registers or overwrites a scoreboard for the given key and priority.
     *
     * <p>If this becomes the highest priority, it renders immediately via async diff.</p>
     *
     * @param player       the player to display the scoreboard to
     * @param key          the unique source identifier for this scoreboard
     * @param priority     the priority of this scoreboard; highest priority is rendered
     * @param title        the sidebar title component
     * @param boardBuilder the builder containing the scoreboard lines
     */
    @Override
    public void set(final Player player, final String key, final int priority, final Component title, final BoardBuilder boardBuilder) {
        this.boardsMap.computeIfAbsent(player.getUniqueId(), __ -> new ConcurrentHashMap<>()).put(key, new BoardEntry(priority, title, boardBuilder.build()));

        this.executorService.execute(() -> this.resolve(player));
    }

    /**
     * Removes a scoreboard source for the given key.
     *
     * <p>If boards remain, the next highest priority is diffed and rendered.
     * If none remain, the sidebar is hidden.</p>
     *
     * @param player the player to remove the scoreboard from
     * @param key    the unique source identifier to remove
     */
    @Override
    public void remove(final Player player, final String key) {
        final ConcurrentHashMap<String, BoardEntry> playerBoardMap = this.boardsMap.get(player.getUniqueId());
        if (playerBoardMap != null) {
            playerBoardMap.remove(key);

            if (playerBoardMap.isEmpty()) {
                this.boardsMap.remove(player.getUniqueId());
            }
        }

        this.executorService.execute(() -> this.resolve(player));
    }

    /**
     * Removes all scoreboard sources for the given player.
     *
     * <p>The sidebar is hidden and the objective is removed from the client.</p>
     *
     * @param player the player to remove all scoreboards from
     */
    @Override
    public void remove(final Player player) {
        this.boardsMap.remove(player.getUniqueId());

        this.executorService.execute(() -> {
            this.resolve(player);
            UtilEvent.dispatch(new ScoreboardRemoveEvent(player));
        });
    }

    /**
     * Checks whether the scoreboard registered under the given key is
     * currently the active (highest priority) board for the player.
     *
     * @param player the player to check
     * @param key    the source identifier to check
     * @return {@code true} if the key's board is the currently rendered board
     */
    @Override
    public boolean isActive(final Player player, final String key) {
        final ConcurrentHashMap<String, BoardEntry> playerBoardMap = this.boardsMap.get(player.getUniqueId());
        if (playerBoardMap == null || playerBoardMap.isEmpty()) {
            return false;
        }

        final BoardEntry boardEntry = playerBoardMap.get(key);

        return boardEntry != null && boardEntry == this.highest(playerBoardMap);
    }

    /**
     * Cleans up all scoreboard state for a player.
     *
     * <p>Should be called when a player disconnects to prevent memory leaks.
     * Does not send any packets — assumes the client connection is already closed.</p>
     *
     * @param uuid the UUID of the player to clean up
     */
    @Override
    public void cleanup(final UUID uuid) {
        this.boardsMap.remove(uuid);
        this.renderedTitleMap.remove(uuid);
        this.renderedLinesMap.remove(uuid);
        this.initializedSet.remove(uuid);

        UtilEvent.dispatch(new ScoreboardCleanupEvent(uuid));
    }

    /**
     * Resolves the highest priority board for a player and diffs it
     * against the currently rendered state.
     *
     * <p>Only changed lines and title produce packets. If no boards
     * remain, the sidebar objective is removed from the client.</p>
     *
     * <p>Must only be called on the executor thread.</p>
     *
     * @param player the player to resolve for
     */
    private void resolve(final Player player) {
        final ConcurrentHashMap<String, BoardEntry> playerBoardMap = this.boardsMap.get(player.getUniqueId());

        final Scoreboard scoreboard = new Scoreboard();

        // No boards remaining — hide the sidebar
        if (playerBoardMap == null || playerBoardMap.isEmpty()) {
            if (this.initializedSet.remove(player.getUniqueId())) {
                final Objective objective = scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY, net.minecraft.network.chat.Component.empty(), ObjectiveCriteria.RenderType.INTEGER, true, null);

                UtilNms.sendPacket(player, new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_REMOVE));
            }

            this.renderedTitleMap.remove(player.getUniqueId());
            this.renderedLinesMap.remove(player.getUniqueId());
            return;
        }

        if (player.isOnline()) {
            final BoardEntry highestBoardEntry = this.highest(playerBoardMap);

            final List<Packet<?>> packetList = new ArrayList<>();

            final Objective objective = scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY, UtilNms.toNms(highestBoardEntry.getTitle()), ObjectiveCriteria.RenderType.INTEGER, true, null);

            // Create objective if this is the first time for this player
            if (this.initializedSet.add(player.getUniqueId())) {
                packetList.add(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD));
                packetList.add(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));

                this.renderedTitleMap.put(player.getUniqueId(), highestBoardEntry.getTitle());

                UtilEvent.dispatch(new ScoreboardReceiveEvent(player));
            }

            boolean updated = false;

            // Diff title — only send update if changed
            final Component oldTitle = this.renderedTitleMap.get(player.getUniqueId());
            if (oldTitle == null || !(oldTitle.equals(highestBoardEntry.getTitle()))) {
                packetList.add(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_CHANGE));

                this.renderedTitleMap.put(player.getUniqueId(), highestBoardEntry.getTitle());

                updated = true;
            }

            // Diff lines — only send packets for added, changed, or removed lines
            final Component[] oldLines = this.renderedLinesMap.getOrDefault(player.getUniqueId(), new Component[0]);
            final Component[] newLines = highestBoardEntry.getLines();

            final int max = Math.max(oldLines.length, newLines.length);

            for (int i = 0; i < max; i++) {
                final int score = newLines.length - i;

                if (i >= newLines.length) {
                    // Line was removed
                    packetList.add(new ClientboundResetScorePacket(SLOT_OWNERS[i], OBJECTIVE));
                    updated = true;
                } else if (i >= oldLines.length || !(newLines[i].equals(oldLines[i]))) {
                    // Line was added or changed
                    packetList.add(new ClientboundSetScorePacket(SLOT_OWNERS[i], OBJECTIVE, score, Optional.of(UtilNms.toNms(newLines[i])), Optional.of(BlankFormat.INSTANCE)));
                    updated = true;
                }
            }

            this.renderedLinesMap.put(player.getUniqueId(), newLines);

            // Send the delta
            if (!(packetList.isEmpty())) {
                for (final Packet<?> packet : packetList) {
                    UtilNms.sendPacket(player, packet);
                }
            }

            if (updated) {
                UtilEvent.dispatch(new ScoreboardUpdateEvent(player));
            }
        }
    }

    /**
     * Finds the board entry with the highest priority from the player's registered boards.
     *
     * @param playerBoardMap the player's registered board entries
     * @return the entry with the highest priority
     * @throws java.util.NoSuchElementException if the map is empty
     */
    private BoardEntry highest(final ConcurrentHashMap<String, BoardEntry> playerBoardMap) {
        return playerBoardMap.values().stream().max(Comparator.comparingInt(BoardEntry::getPriority)).orElseThrow();
    }

    /**
     * Creates a new {@link BoardBuilder} for composing scoreboard lines.
     *
     * @return a new board builder instance
     */
    public static BoardBuilder board() {
        return new BoardBuilder();
    }
}