package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.boltaro;

import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGameBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerResult;
import com.ebicep.warlords.database.repositories.games.pojos.pve.events.DatabaseGamePlayerPvEEvent;
import com.ebicep.warlords.database.repositories.games.pojos.pve.events.DatabaseGamePvEEvent;
import com.ebicep.warlords.database.repositories.games.pojos.pve.events.boltaro.boltaroslair.DatabaseGamePvEEventBoltaroLair;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.AbstractDatabaseStatInformation;
import com.ebicep.warlords.game.GameMode;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedHashMap;
import java.util.Map;

public class PvEEventBoltaroDatabaseStatInformation extends AbstractDatabaseStatInformation {

    @Field("experience_pve")
    protected long experiencePvE;
    @Field("total_time_played")
    protected long totalTimePlayed = 0;
    @Field("mob_kills")
    protected Map<String, Long> mobKills = new LinkedHashMap<>();
    @Field("mob_assists")
    protected Map<String, Long> mobAssists = new LinkedHashMap<>();
    @Field("mob_deaths")
    protected Map<String, Long> mobDeaths = new LinkedHashMap<>();

    @Field("event_points_cum")
    private long eventPointsCumulative;
    @Field("highest_event_points_game")
    private long highestEventPointsGame;

    @Override
    public void updateCustomStats(
            DatabaseGameBase databaseGame,
            GameMode gameMode,
            DatabaseGamePlayerBase gamePlayer,
            DatabaseGamePlayerResult result,
            int multiplier,
            PlayersCollections playersCollection
    ) {
        assert databaseGame instanceof DatabaseGamePvEEvent;
        assert gamePlayer instanceof DatabaseGamePlayerPvEEvent;

        DatabaseGamePvEEvent databaseGamePvEEvent = (DatabaseGamePvEEvent) databaseGame;
        DatabaseGamePlayerPvEEvent gamePlayerPvEEvent = (DatabaseGamePlayerPvEEvent) gamePlayer;

        this.totalTimePlayed += (long) databaseGamePvEEvent.getTimeElapsed() * multiplier;
        gamePlayerPvEEvent.getMobKills().forEach((s, aLong) -> this.mobKills.merge(s, aLong * multiplier, Long::sum));
        gamePlayerPvEEvent.getMobAssists().forEach((s, aLong) -> this.mobAssists.merge(s, aLong * multiplier, Long::sum));
        gamePlayerPvEEvent.getMobDeaths().forEach((s, aLong) -> this.mobDeaths.merge(s, aLong * multiplier, Long::sum));

        if (databaseGame instanceof DatabaseGamePvEEventBoltaroLair) {
            this.eventPointsCumulative += Math.min(gamePlayerPvEEvent.getPoints(), 50_000) * multiplier;
        } else {
            this.eventPointsCumulative += Math.min(gamePlayerPvEEvent.getPoints(), 15_000) * multiplier;
        }
        if (multiplier > 0) {
            this.highestEventPointsGame = Math.max(this.highestEventPointsGame, gamePlayerPvEEvent.getPoints());
        }
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

    public long getEventPointsCumulative() {
        return eventPointsCumulative;
    }

    public void setEventPointsCumulative(long eventPointsCumulative) {
        this.eventPointsCumulative = eventPointsCumulative;
    }

    public long getHighestEventPointsGame() {
        return highestEventPointsGame;
    }
}
