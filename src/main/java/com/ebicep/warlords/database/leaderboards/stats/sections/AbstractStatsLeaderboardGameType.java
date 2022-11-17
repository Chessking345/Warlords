package com.ebicep.warlords.database.leaderboards.stats.sections;

import com.ebicep.warlords.database.leaderboards.stats.StatsLeaderboard;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.AbstractDatabaseStatInformation;
import com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer;
import com.ebicep.warlords.util.java.NumberFormat;
import me.filoghost.holographicdisplays.api.hologram.Hologram;

import java.util.List;
import java.util.Set;

import static com.ebicep.warlords.database.leaderboards.stats.StatsLeaderboardLocations.*;

/**
 * Different gamemodes
 * <p>ALL
 * <p>CTF
 */
public abstract class AbstractStatsLeaderboardGameType<T extends AbstractDatabaseStatInformation> {

    protected final List<StatsLeaderboardCategory<T>> gameTypeCategories;

    protected AbstractStatsLeaderboardGameType(List<StatsLeaderboardCategory<T>> gameTypeCategories) {
        this.gameTypeCategories = gameTypeCategories;
    }

    public void addLeaderboards() {
        for (StatsLeaderboardCategory<T> category : gameTypeCategories) {
            addBaseLeaderboards(category);
        }
    }

    public abstract String getSubTitle();

    public abstract void addExtraLeaderboards(StatsLeaderboardCategory<T> statsLeaderboardCategory);

    public void resetLeaderboards(PlayersCollections collection, Set<DatabasePlayer> databasePlayers) {
        switch (collection) {
            case LIFETIME:
                databasePlayers.removeIf(databasePlayer -> databasePlayer.getPlays() + databasePlayer.getPveStats().getPlays() < 50);
                break;
            case WEEKLY:
                databasePlayers.removeIf(databasePlayer -> databasePlayer.getPlays() + databasePlayer.getPveStats().getPlays() < 10);
                break;
        }
        String subTitle = getSubTitle();
        for (StatsLeaderboardCategory<T> category : gameTypeCategories) {
            category.resetLeaderboards(collection, databasePlayers, subTitle);
        }
    }

    public void addBaseLeaderboards(StatsLeaderboardCategory<T> statsLeaderboardCategory) {
        statsLeaderboardCategory.getAllHolograms().forEach(Hologram::delete);

        List<StatsLeaderboard> statsLeaderboards = statsLeaderboardCategory.getLeaderboards();
        statsLeaderboards.clear();

        statsLeaderboards.add(new StatsLeaderboard("Wins", LEAD_2, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getWins(), databasePlayer -> NumberFormat.addCommaAndRound(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getWins())));
        statsLeaderboards.add(new StatsLeaderboard("Losses", CIRCULAR_1_CENTER, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getLosses(), databasePlayer -> NumberFormat.addCommaAndRound(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getLosses())));
        statsLeaderboards.add(new StatsLeaderboard("Plays", LEAD_1, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getPlays(), databasePlayer -> NumberFormat.addCommaAndRound(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getPlays())));
        statsLeaderboards.add(new StatsLeaderboard("Kills", LEAD_3, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getKills(), databasePlayer -> NumberFormat.addCommaAndRound(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getKills())));
        statsLeaderboards.add(new StatsLeaderboard("Assists", CIRCULAR_1_OUTER_3, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getAssists(), databasePlayer -> NumberFormat.addCommaAndRound(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getAssists())));
        statsLeaderboards.add(new StatsLeaderboard("Deaths", CIRCULAR_1_OUTER_4, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getDeaths(), databasePlayer -> NumberFormat.addCommaAndRound(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getDeaths())));
        statsLeaderboards.add(new StatsLeaderboard("Damage", CIRCULAR_1_OUTER_6, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getDamage(), databasePlayer -> NumberFormat.addCommaAndRound(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getDamage())));
        statsLeaderboards.add(new StatsLeaderboard("Healing", CIRCULAR_1_OUTER_5, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getHealing(), databasePlayer -> NumberFormat.addCommaAndRound(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getHealing())));
        statsLeaderboards.add(new StatsLeaderboard("Absorbed", CIRCULAR_1_OUTER_1, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getAbsorbed(), databasePlayer -> NumberFormat.addCommaAndRound(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getAbsorbed())));


        statsLeaderboards.add(new StatsLeaderboard("DHP", CIRCULAR_2_OUTER_3, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getDHP(), databasePlayer -> NumberFormat.addCommaAndRound(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getDHP())));
        statsLeaderboards.add(new StatsLeaderboard("DHP Per Game", LEAD_4, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getDHPPerGame(), databasePlayer -> NumberFormat.addCommaAndRound(Math.round((double) (statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getDHPPerGame()) * 10) / 10d)));
        statsLeaderboards.add(new StatsLeaderboard("Kills Per Game", CIRCULAR_2_OUTER_2, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getKillsPerGame(), databasePlayer -> String.valueOf(Math.round(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getKillsPerGame() * 10) / 10d)));
        statsLeaderboards.add(new StatsLeaderboard("Deaths Per Game", CIRCULAR_2_OUTER_1, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getDeathsPerGame(), databasePlayer -> String.valueOf(Math.round(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getDeathsPerGame() * 10) / 10d)));
        statsLeaderboards.add(new StatsLeaderboard("Kills/Assists Per Game", CIRCULAR_2_OUTER_4, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getKillsAssistsPerGame(), databasePlayer -> String.valueOf(Math.round(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getKillsAssistsPerGame() * 10) / 10d)));

        statsLeaderboards.add(new StatsLeaderboard("Experience", CENTER_BOARD, databasePlayer -> statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getExperience(), databasePlayer -> NumberFormat.addCommaAndRound(statsLeaderboardCategory.getStatFunction().apply(databasePlayer).getExperience())));


        this.addExtraLeaderboards(statsLeaderboardCategory);
    }


    public List<StatsLeaderboardCategory<T>> getCategories() {
        return gameTypeCategories;
    }

}
