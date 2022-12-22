package com.ebicep.warlords.game.option.wavedefense;

import com.ebicep.warlords.events.player.ingame.WarlordsDeathEvent;
import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.game.option.Option;
import com.ebicep.warlords.game.option.marker.scoreboard.ScoreboardHandler;
import com.ebicep.warlords.game.option.marker.scoreboard.SimpleScoreboardHandler;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsPlayer;
import com.ebicep.warlords.util.java.NumberFormat;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CurrencyOnEventOption implements Option, Listener {

    public static final int SCOREBOARD_PRIORITY = 15;
    private static final int BASE_CURRENCY_ON_KILL = 100;
    private int baseCurrencyToAdd;
    private int startingCurrency = 0;

    public CurrencyOnEventOption() {
        this(BASE_CURRENCY_ON_KILL);
    }

    public CurrencyOnEventOption(int baseCurrencyToAdd) {
        this.baseCurrencyToAdd = baseCurrencyToAdd;
    }

    public CurrencyOnEventOption(int baseCurrencyToAdd, int startingCurrency) {
        this.baseCurrencyToAdd = baseCurrencyToAdd;
        this.startingCurrency = startingCurrency;
    }

    @Override
    public void register(@Nonnull Game game) {
        game.registerEvents(this);

        game.registerGameMarker(ScoreboardHandler.class, new SimpleScoreboardHandler(SCOREBOARD_PRIORITY, "currency") {
            @Nonnull
            @Override
            public List<String> computeLines(@Nullable WarlordsPlayer player) {
                return Collections.singletonList(player != null ? "Insignia: " + ChatColor.GOLD + "❂ " + NumberFormat.addCommas(player.getCurrency()) : "");
            }
        });
    }

    @Override
    public void onWarlordsEntityCreated(@Nonnull WarlordsEntity player) {
        player.addCurrency(startingCurrency);
    }

    @EventHandler
    public void onKill(WarlordsDeathEvent event) {
        WarlordsEntity mob = event.getPlayer();
        for (WarlordsEntity player : PlayerFilter
                .playingGame(mob.getGame())
                .aliveEnemiesOf(mob)
        ) {
            if (player instanceof WarlordsPlayer && !player.isDead() && !mob.getName().equals("Tormented Soul")) {
                player.addCurrency(baseCurrencyToAdd);
            }
        }
    }
}
