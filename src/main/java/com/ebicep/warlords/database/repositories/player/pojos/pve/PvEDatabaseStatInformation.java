package com.ebicep.warlords.database.repositories.player.pojos.pve;

import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGameBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerResult;
import com.ebicep.warlords.database.repositories.games.pojos.pve.*;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.AbstractDatabaseStatInformation;
import com.ebicep.warlords.game.GameMode;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedHashMap;
import java.util.Map;

public class PvEDatabaseStatInformation extends AbstractDatabaseStatInformation {

    //CUMULATIVE STATS
    @Field("experience_pve")
    protected long experiencePvE;
    @Field("total_waves_cleared")
    protected int totalWavesCleared;
    @Field("total_time_played")
    protected long totalTimePlayed = 0;
    @Field("mob_kills")
    protected Map<String, Long> mobKills = new LinkedHashMap<>();
    @Field("mob_assists")
    protected Map<String, Long> mobAssists = new LinkedHashMap<>();
    @Field("mob_deaths")
    protected Map<String, Long> mobDeaths = new LinkedHashMap<>();

    //TOP STATS
    @Field("highest_wave_cleared")
    protected int highestWaveCleared;
    @Field("most_damage_in_round")
    protected long mostDamageInRound;
    @Field("most_damage_in_wave")
    protected long mostDamageInWave;
    @Field("fastest_game_finished")
    protected long fastestGameFinished = 0;

    public PvEDatabaseStatInformation() {
    }

    @Override
    public void updateCustomStats(
            DatabaseGameBase databaseGame,
            GameMode gameMode,
            DatabaseGamePlayerBase gamePlayer,
            DatabaseGamePlayerResult result,
            int multiplier,
            PlayersCollections playersCollection
    ) {
        assert gamePlayer instanceof DatabaseGamePlayerPvE;
        DatabaseGamePlayerPvE databaseGamePlayerPvE = (DatabaseGamePlayerPvE) gamePlayer;
        databaseGamePlayerPvE.getMobKills().forEach((s, aLong) -> this.mobKills.merge(s, aLong * multiplier, Long::sum));
        databaseGamePlayerPvE.getMobAssists().forEach((s, aLong) -> this.mobAssists.merge(s, aLong * multiplier, Long::sum));
        databaseGamePlayerPvE.getMobDeaths().forEach((s, aLong) -> this.mobDeaths.merge(s, aLong * multiplier, Long::sum));

        if (databaseGame instanceof WavesCleared wavesCleared) {
            if (multiplier > 0) {
                this.highestWaveCleared = Math.max(wavesCleared.getWavesCleared(), this.highestWaveCleared);
            } else if (this.highestWaveCleared == wavesCleared.getWavesCleared()) {
                this.highestWaveCleared = 0;
            }
            if (databaseGame instanceof TimeElapsed timeElapsed && databaseGame instanceof Difficulty difficulty) {
                if (multiplier > 0) {
                    if (wavesCleared.getWavesCleared() == difficulty.getDifficulty().getMaxWaves() &&
                            (this.fastestGameFinished == 0 || timeElapsed.getTimeElapsed() < fastestGameFinished)
                    ) {
                        this.fastestGameFinished = timeElapsed.getTimeElapsed();
                    }
                } else if (this.fastestGameFinished == timeElapsed.getTimeElapsed()) {
                    this.fastestGameFinished = 0;
                }
            }
        }
        if (gamePlayer instanceof MostDamageInRound mostDamageInRound) {
            if (multiplier > 0) {
                this.mostDamageInRound = Math.max(this.mostDamageInRound, mostDamageInRound.getMostDamageInRound());
            } else if (this.mostDamageInRound == mostDamageInRound.getMostDamageInRound()) {
                this.mostDamageInRound = 0;
            }
        }
        if (gamePlayer instanceof MostDamageInWave mostDamageInWave) {
            if (multiplier > 0) {
                this.mostDamageInWave = Math.max(this.mostDamageInWave, mostDamageInWave.getMostDamageInWave());
            } else if (this.mostDamageInWave == mostDamageInWave.getMostDamageInWave()) {
                this.mostDamageInWave = 0;
            }
        }

        if (databaseGame instanceof WavesCleared wavesCleared) {
            this.totalWavesCleared += wavesCleared.getWavesCleared() * multiplier;
        }
        if (databaseGame instanceof TimeElapsed timeElapsed) {
            this.totalTimePlayed += (long) timeElapsed.getTimeElapsed() * multiplier;
        }
    }

    public void merge(PvEDatabaseStatInformation other) {
        super.merge(other);
        this.experiencePvE += other.experiencePvE;
        this.totalWavesCleared += other.totalWavesCleared;
        this.totalTimePlayed += other.totalTimePlayed;
        this.highestWaveCleared = Math.max(this.highestWaveCleared, other.highestWaveCleared);
        this.mostDamageInRound = Math.max(this.mostDamageInRound, other.mostDamageInRound);
        this.mostDamageInWave = Math.max(this.mostDamageInWave, other.mostDamageInWave);
        this.fastestGameFinished = Math.min(this.fastestGameFinished, other.fastestGameFinished);
        other.mobKills.forEach((s, aLong) -> this.mobKills.merge(s, aLong, Long::sum));
        other.mobAssists.forEach((s, aLong) -> this.mobAssists.merge(s, aLong, Long::sum));
        other.mobDeaths.forEach((s, aLong) -> this.mobDeaths.merge(s, aLong, Long::sum));
    }

    public long getExperiencePvE() {
        return experiencePvE;
    }

    public long getTotalTimePlayed() {
        return totalTimePlayed;
    }

    public Map<String, Long> getMobKills() {
        return mobKills;
    }

    public Map<String, Long> getMobAssists() {
        return mobAssists;
    }

    public Map<String, Long> getMobDeaths() {
        return mobDeaths;
    }

    public int getHighestWaveCleared() {
        return highestWaveCleared;
    }

    public void setHighestWaveCleared(int highestWaveCleared) {
        this.highestWaveCleared = highestWaveCleared;
    }

    public long getMostDamageInRound() {
        return mostDamageInRound;
    }

    public void setMostDamageInRound(long mostDamageInRound) {
        this.mostDamageInRound = mostDamageInRound;
    }

    public long getMostDamageInWave() {
        return mostDamageInWave;
    }

    public void setMostDamageInWave(long mostDamageInWave) {
        this.mostDamageInWave = mostDamageInWave;
    }

    public int getTotalWavesCleared() {
        return totalWavesCleared;
    }

    public void addTimePlayed(long time) {
        this.totalTimePlayed += time;
    }

    public long getFastestGameFinished() {
        return fastestGameFinished;
    }

    public void setFastestGameFinished(long fastestGameFinished) {
        this.fastestGameFinished = fastestGameFinished;
    }

}
