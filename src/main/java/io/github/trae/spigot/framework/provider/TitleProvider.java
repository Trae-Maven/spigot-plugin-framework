package io.github.trae.spigot.framework.provider;

import io.github.trae.spigot.framework.utility.UtilServer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A title, with the subtitle and timings to show it with, resolved once and replayable anywhere.
 *
 * <p>Timings are given in ticks, twenty to a second, the same unit the client counts a title's fade and stay in. The
 * {@link Title} is built fresh on every send, so one provider can be held in a static field and sent as often as
 * needed.</p>
 *
 * <p>A {@code null} title or subtitle is shown as {@link Component#empty()}, so a provider built straight from a
 * nullable source needs no guard, and a title with no subtitle, or a subtitle with no title, is built by passing
 * {@code null} for the missing part.</p>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class TitleProvider {

    /**
     * The large text in the centre of the screen and the smaller text shown beneath it.
     */
    private final Component title, subTitle;

    /**
     * The ticks the title takes to fade in, stays fully visible for, and takes to fade out.
     */
    private final long fadeInDuration, stayDuration, fadeOutDuration;

    /**
     * Creates a provider for the given title and subtitle.
     *
     * @param title           the title text, or {@code null} for none
     * @param subTitle        the subtitle text, or {@code null} for none
     * @param fadeInDuration  the ticks to fade in over
     * @param stayDuration    the ticks to stay fully visible for
     * @param fadeOutDuration the ticks to fade out over
     * @return the provider
     */
    public static TitleProvider of(final Component title, final Component subTitle, final long fadeInDuration, final long stayDuration, final long fadeOutDuration) {
        return new TitleProvider(Objects.requireNonNullElse(title, Component.empty()), Objects.requireNonNullElse(subTitle, Component.empty()), fadeInDuration, stayDuration, fadeOutDuration);
    }

    /**
     * Creates a provider for the given title and subtitle, fading in and out over one second each.
     *
     * @param title        the title text, or {@code null} for none
     * @param subTitle     the subtitle text, or {@code null} for none
     * @param stayDuration the ticks to stay fully visible for
     * @return the provider
     */
    public static TitleProvider of(final Component title, final Component subTitle, final long stayDuration) {
        return of(title, subTitle, 20, stayDuration, 20);
    }

    /**
     * Shows the title to every player in the given list.
     *
     * <p>The {@link Title} is built once and shared across the list.</p>
     *
     * @param playerList the players to show to
     */
    public void send(final List<Player> playerList) {
        final Title title = this.build();

        for (final Player player : playerList) {
            player.showTitle(title);
        }
    }

    /**
     * Shows the title to the given player alone.
     *
     * @param player the player to show to
     */
    public void send(final Player player) {
        this.send(Collections.singletonList(player));
    }

    /**
     * Shows the title to every online player.
     */
    public void broadcast() {
        this.send(UtilServer.getOnlinePlayers());
    }

    /**
     * Builds the {@link Title} every send shows.
     *
     * <p>The durations are passed through as ticks, the unit {@link Title#title(Component, Component, int, int, int)}
     * takes, so no conversion is applied.</p>
     *
     * @return the title
     */
    private Title build() {
        return Title.title(this.title, this.subTitle, (int) this.fadeInDuration, (int) this.stayDuration, (int) this.fadeOutDuration);
    }
}