package com.ebicep.warlords.database.repositories.games.pojos.pve.events.mithra.spidersdwelling;

import com.ebicep.warlords.commands.debugcommands.misc.GamesCommand;
import com.ebicep.warlords.database.repositories.events.pojos.GameEvents;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGameBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerBase;
import com.ebicep.warlords.database.repositories.games.pojos.pve.WavesCleared;
import com.ebicep.warlords.database.repositories.games.pojos.pve.events.DatabaseGamePlayerPvEEvent;
import com.ebicep.warlords.database.repositories.games.pojos.pve.events.DatabaseGamePvEEvent;
import com.ebicep.warlords.events.game.WarlordsGameTriggerWinEvent;
import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.game.option.Option;
import com.ebicep.warlords.game.option.wavedefense.WaveDefenseOption;
import com.ebicep.warlords.game.option.wavedefense.events.EventPointsOption;
import com.ebicep.warlords.game.option.wavedefense.events.modes.SpidersDwellingOption;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import org.bukkit.ChatColor;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class DatabaseGamePvEEventSpidersDwelling extends DatabaseGamePvEEvent implements WavesCleared {

    @Field("total_mobs_killed")
    private int totalMobsKilled;
    @Field("waves_cleared")
    private int wavesCleared;
    private List<DatabaseGamePlayerPvEEventSpidersDwelling> players = new ArrayList<>();

    public DatabaseGamePvEEventSpidersDwelling() {
    }

    public DatabaseGamePvEEventSpidersDwelling(@Nonnull Game game, @Nullable WarlordsGameTriggerWinEvent gameWinEvent, boolean counted) {
        super(game, counted);
        AtomicReference<WaveDefenseOption> waveDefenseOption = new AtomicReference<>();
        AtomicReference<EventPointsOption> eventPointsOption = new AtomicReference<>();
        AtomicReference<SpidersDwellingOption> spidersDwellingOption = new AtomicReference<>();
        for (Option option : game.getOptions()) {
            if (option instanceof WaveDefenseOption) {
                waveDefenseOption.set((WaveDefenseOption) option);
            } else if (option instanceof EventPointsOption) {
                eventPointsOption.set((EventPointsOption) option);
            } else if (option instanceof SpidersDwellingOption) {
                spidersDwellingOption.set((SpidersDwellingOption) option);
            }
        }
        if (waveDefenseOption.get() == null || eventPointsOption.get() == null || spidersDwellingOption.get() == null) {
            throw new IllegalStateException("Missing option");
        }
        game.warlordsPlayers()
            .forEach(warlordsPlayer -> players.add(new DatabaseGamePlayerPvEEventSpidersDwelling(warlordsPlayer,
                    waveDefenseOption.get(),
                    eventPointsOption.get()
            )));
        this.totalMobsKilled = players.stream().mapToInt(DatabaseGamePlayerBase::getTotalKills).sum();
        this.wavesCleared = waveDefenseOption.get().getWavesCleared();
    }

    @Override
    public GameEvents getEvent() {
        return GameEvents.MITHRA;
    }

    @Override
    public void updatePlayerStatsFromGame(DatabaseGameBase databaseGame, int multiplier) {
        players.forEach(databaseGamePlayerPvEEventSpidersDwelling -> {
            DatabaseGameBase.updatePlayerStatsFromTeam(databaseGame,
                    databaseGamePlayerPvEEventSpidersDwelling,
                    multiplier
            );
            GamesCommand.PLAYER_NAMES.add(databaseGamePlayerPvEEventSpidersDwelling.getName());
        });
    }

    @Override
    public void appendLastGameStats(Hologram hologram) {
        super.appendLastGameStats(hologram);
        hologram.getLines().appendText(ChatColor.YELLOW + "Waves Cleared: " + wavesCleared);
    }

    @Override
    public void addCustomHolograms(List<Hologram> holograms) {
        super.addCustomHolograms(holograms);

    }

    @Override
    public String getGameLabel() {
        return super.getGameLabel() + " - " + ChatColor.YELLOW + wavesCleared;
    }

    @Override
    public List<String> getExtraLore() {
        List<String> extraLore = super.getExtraLore();
        extraLore.add(ChatColor.GRAY + "Waves Cleared: " + ChatColor.YELLOW + wavesCleared);
        return extraLore;
    }

    @Override
    public List<DatabaseGamePlayerPvEEvent> getPlayers() {
        return new ArrayList<>(players);
    }

    @Override
    public Set<DatabaseGamePlayerBase> getBasePlayers() {
        return new HashSet<>(players);
    }

    @Override
    public int getWavesCleared() {
        return wavesCleared;
    }
}
