package com.ebicep.warlords.database.repositories.player.pojos.pve.events;

import com.ebicep.warlords.database.repositories.events.pojos.DatabaseGameEvent;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGameBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerResult;
import com.ebicep.warlords.database.repositories.games.pojos.pve.events.DatabaseGamePlayerPvEEvent;
import com.ebicep.warlords.database.repositories.games.pojos.pve.events.boltaro.boltarobonanza.DatabaseGamePvEEventBoltaroBonanza;
import com.ebicep.warlords.database.repositories.games.pojos.pve.events.boltaro.boltaroslair.DatabaseGamePvEEventBoltaroLair;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.boltaro.DatabasePlayerPvEEventBoltaroDifficultyStats;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.boltaro.DatabasePlayerPvEEventBoltaroStats;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.mithra.DatabasePlayerPvEEventMithraDifficultyStats;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.mithra.DatabasePlayerPvEEventMithraStats;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.narmer.DatabasePlayerPvEEventNarmerDifficultyStats;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.narmer.DatabasePlayerPvEEventNarmerStats;
import com.ebicep.warlords.game.GameMode;
import com.ebicep.warlords.guilds.Guild;
import com.ebicep.warlords.guilds.GuildManager;
import com.ebicep.warlords.guilds.GuildPlayer;
import com.ebicep.warlords.util.java.Pair;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

public class DatabasePlayerPvEEventStats extends DatabasePlayerPvEEventDifficultyStats {

    @Field("boltaro")
    private DatabasePlayerPvEEventBoltaroStats boltaroStats = new DatabasePlayerPvEEventBoltaroStats();
    @Field("narmer")
    private DatabasePlayerPvEEventNarmerStats narmerStats = new DatabasePlayerPvEEventNarmerStats();
    @Field("mithra")
    private DatabasePlayerPvEEventMithraStats mithraStats = new DatabasePlayerPvEEventMithraStats();

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

        DatabaseGameEvent currentGameEvent = DatabaseGameEvent.currentGameEvent;
        if (currentGameEvent != null) {
            currentGameEvent.getEvent().updateStatsFunction.apply(this).updateStats(databaseGame, gamePlayer, multiplier, playersCollection);

            //GUILDS
            Pair<Guild, GuildPlayer> guildGuildPlayerPair = GuildManager.getGuildAndGuildPlayerFromPlayer(gamePlayer.getUuid());
            if (playersCollection == PlayersCollections.LIFETIME && guildGuildPlayerPair != null) {
                Guild guild = guildGuildPlayerPair.getA();
                GuildPlayer guildPlayer = guildGuildPlayerPair.getB();

                long points;
                if (databaseGame instanceof DatabaseGamePvEEventBoltaroLair) {
                    points = Math.min(((DatabaseGamePlayerPvEEvent) gamePlayer).getPoints(), 50_000) * multiplier;
                } else if (databaseGame instanceof DatabaseGamePvEEventBoltaroBonanza) {
                    points = Math.min(((DatabaseGamePlayerPvEEvent) gamePlayer).getPoints(), 15_000) * multiplier;
                } else {
                    points = Math.min(((DatabaseGamePlayerPvEEvent) gamePlayer).getPoints(), 100_000) * multiplier;
                }
                guild.addEventPoints(currentGameEvent.getEvent(), currentGameEvent.getStartDateSecond(), points * multiplier);
                guildPlayer.addEventPoints(currentGameEvent.getEvent(),
                        currentGameEvent.getStartDateSecond(),
                        points * multiplier
                );
                guild.queueUpdate();
            }
        }
    }

    public DatabasePlayerPvEEventBoltaroStats getBoltaroStats() {
        return boltaroStats;
    }

    public Map<Long, DatabasePlayerPvEEventBoltaroDifficultyStats> getBoltaroEventStats() {
        return boltaroStats.getEventStats();
    }

    public DatabasePlayerPvEEventNarmerStats getNarmerStats() {
        return narmerStats;
    }

    public Map<Long, DatabasePlayerPvEEventNarmerDifficultyStats> getNarmerEventStats() {
        return narmerStats.getEventStats();
    }

    public DatabasePlayerPvEEventMithraStats getMithraStats() {
        return mithraStats;
    }

    public Map<Long, DatabasePlayerPvEEventMithraDifficultyStats> getMithraEventStats() {
        return mithraStats.getEventStats();
    }


}
