package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.narmer;

import com.ebicep.warlords.database.repositories.events.pojos.DatabaseGameEvent;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGameBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerResult;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.game.GameMode;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedHashMap;
import java.util.Map;

public class DatabasePlayerPvEEventNarmerStats extends DatabasePlayerPvEEventNarmerDifficultyStats {

    @Field("events")
    private Map<Long, DatabasePlayerPvEEventNarmerDifficultyStats> eventStats = new LinkedHashMap<>();

    @Override
    public void updateCustomStats(
            DatabaseGameBase databaseGame,
            GameMode gameMode,
            DatabaseGamePlayerBase gamePlayer,
            DatabaseGamePlayerResult result,
            int multiplier,
            PlayersCollections playersCollection
    ) {
        super.updateCustomStats(databaseGame, gameMode, gamePlayer, result, multiplier, playersCollection);

        getEvent(DatabaseGameEvent.currentGameEvent.getStartDateSecond()).updateStats(databaseGame, gamePlayer, multiplier, playersCollection);
    }

    public Map<Long, DatabasePlayerPvEEventNarmerDifficultyStats> getEventStats() {
        return eventStats;
    }

    public DatabasePlayerPvEEventNarmerDifficultyStats getEvent(long epochSecond) {
        return eventStats.computeIfAbsent(epochSecond, k -> new DatabasePlayerPvEEventNarmerDifficultyStats());
    }
}
