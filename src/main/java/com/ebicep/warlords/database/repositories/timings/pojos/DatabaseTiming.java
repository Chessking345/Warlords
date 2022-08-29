package com.ebicep.warlords.database.repositories.timings.pojos;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.database.leaderboards.guilds.GuildLeaderboardManager;
import com.ebicep.warlords.database.leaderboards.stats.StatsLeaderboard;
import com.ebicep.warlords.database.leaderboards.stats.StatsLeaderboardManager;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer;
import com.ebicep.warlords.guilds.Guild;
import com.ebicep.warlords.guilds.GuildManager;
import com.ebicep.warlords.player.general.ExperienceManager;
import com.ebicep.warlords.util.chat.ChatUtils;
import com.ebicep.warlords.util.java.DateUtil;
import com.mongodb.client.MongoCollection;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Document(collection = "Timings")
public class DatabaseTiming {

    private static final String[] WEEKLY_EXPERIENCE_LEADERBOARDS = new String[]{
            "Wins",
            "Losses",
            "Kills",
            "Assists",
            "Deaths",
            "DHP",
            "DHP Per Game",
            "Damage",
            "Healing",
            "Absorbed",
            "Flags Captured",
            "Flags Returned",
    };
    @Id
    protected String id;
    private String title;
    @Field("last_reset")
    private Instant lastReset = DateUtil.getResetDateToday();
    @Field("timing")
    private Timing timing;

    public DatabaseTiming() {
    }

    public DatabaseTiming(String title, Timing timing) {
        this.title = title;
        this.timing = timing;
    }

    public DatabaseTiming(String title, Instant lastReset, Timing timing) {
        this.title = title;
        this.lastReset = lastReset;
        this.timing = timing;
    }

    public static void checkStatsTimings() {
        Instant currentDate = Instant.now();
        //WEEKLY
        Warlords.newChain()
                .asyncFirst(() -> DatabaseManager.timingsService.findByTitle("Weekly Stats"))
                .async(timing -> {
                    if (timing == null) {
                        ChatUtils.MessageTypes.TIMINGS.sendMessage("Could not find Weekly Stats timing in database. Creating new timing.");
                        DatabaseManager.timingsService.create(new DatabaseTiming("Weekly Stats", DateUtil.getResetDateLatestMonday(), Timing.WEEKLY));
                    } else {
                        long minutesBetween = ChronoUnit.MINUTES.between(timing.getLastReset(), currentDate);
                        ChatUtils.MessageTypes.TIMINGS.sendMessage("Weekly Reset Time Minute: " + minutesBetween + " > " + (timing.getTiming().minuteDuration - 30));
                        //10 min buffer
                        if (minutesBetween > 0 && minutesBetween > timing.getTiming().minuteDuration - 30) {
                            try {
                                //adding new document with top weekly players
                                org.bson.Document topPlayers = getTopPlayersOnLeaderboard();
                                MongoCollection<org.bson.Document> weeklyLeaderboards = DatabaseManager.warlordsDatabase.getCollection("Weekly_Leaderboards");
                                weeklyLeaderboards.insertOne(topPlayers);

                                ExperienceManager.awardWeeklyExperience(topPlayers);
                                //clearing weekly
                                DatabaseManager.playerService.deleteAll(PlayersCollections.WEEKLY);
                            } catch (Exception e) {
                                ChatUtils.MessageTypes.TIMINGS.sendMessage("ERROR DOING WEEKLY EXP THINGY - COMPS DIDNT HAPPEN?");
                            }
                            //updating date to current
                            timing.setLastReset(DateUtil.getResetDateLatestMonday());
                            DatabaseManager.timingsService.update(timing);

                            ChatUtils.MessageTypes.TIMINGS.sendMessage("Weekly player information reset");
                            return true;
                        }
                    }
                    return false;
                })
                .syncLast((reset) -> {
                    if (reset) {
                        //reloading boards
                        StatsLeaderboardManager.CACHED_PLAYERS.get(PlayersCollections.WEEKLY).clear();
                        StatsLeaderboardManager.reloadLeaderboardsFromCache(PlayersCollections.WEEKLY, false);
                    }
                })
                .execute();
        //DAILY
        Warlords.newChain()
                .asyncFirst(() -> DatabaseManager.timingsService.findByTitle("Daily Stats"))
                .async(timing -> {
                    if (timing == null) {
                        ChatUtils.MessageTypes.TIMINGS.sendMessage("Could not find Daily Stats timing in database. Creating new timing.");
                        DatabaseManager.timingsService.create(new DatabaseTiming("Daily Stats", DateUtil.getResetDateToday(), Timing.DAILY));
                    } else {
                        long minutesBetween = ChronoUnit.MINUTES.between(timing.getLastReset(), currentDate);
                        ChatUtils.MessageTypes.TIMINGS.sendMessage("Daily Reset Time Minute: " + minutesBetween + " > " + (timing.getTiming().minuteDuration - 30));
                        //10 min buffer
                        if (minutesBetween > 0 && minutesBetween > timing.getTiming().minuteDuration - 10) {
                            //clearing daily
                            DatabaseManager.playerService.deleteAll(PlayersCollections.DAILY);
                            //updating date to current
                            timing.setLastReset(DateUtil.getResetDateToday());
                            DatabaseManager.timingsService.update(timing);
                            ChatUtils.MessageTypes.TIMINGS.sendMessage("Daily player information reset");
                            return true;
                        }
                    }
                    return false;
                })
                .syncLast((reset) -> {
                    if (reset) {
                        //reloading boards
                        StatsLeaderboardManager.CACHED_PLAYERS.get(PlayersCollections.DAILY).clear();
                        StatsLeaderboardManager.reloadLeaderboardsFromCache(PlayersCollections.DAILY, false);
                    }
                })
                .execute();
    }

    public static void checkGuildsTimings() {
        Instant now = Instant.now();
        Warlords.newChain()
                .asyncFirst(() -> DatabaseManager.timingsService.findByTitle("Guilds"))
                .async(timing -> {
                    if (timing == null) {
                        ChatUtils.MessageTypes.TIMINGS.sendMessage("Could not find Guilds timing in database. Creating new timing.");
                        DatabaseManager.timingsService.create(new DatabaseTiming("Guilds", DateUtil.getResetDateToday(), Timing.DAILY));
                    } else {
                        long minutesBetween = ChronoUnit.MINUTES.between(timing.getLastReset(), now);
                        ChatUtils.MessageTypes.TIMINGS.sendMessage("Daily Reset Time Minute: " + minutesBetween + " > " + (timing.getTiming().minuteDuration - 30));
                        //10 min buffer
                        if (minutesBetween > 0 && minutesBetween > timing.getTiming().minuteDuration - 10) {
                            //updating date to current
                            timing.setLastReset(DateUtil.getResetDateToday());
                            DatabaseManager.timingsService.update(timing);
                            ChatUtils.MessageTypes.TIMINGS.sendMessage("Guilds daily counters reset");
                            return true;
                        }
                    }
                    return false;
                })
                .syncLast((reset) -> {
                    if (reset) {
                        for (Guild guild : GuildManager.GUILDS) {
                            guild.setDailyExperience(0);
                            GuildManager.queueUpdateGuild(guild);
                        }
                        GuildLeaderboardManager.recalculateLeaderboards();
                    }
                })
                .execute();
    }

    public static org.bson.Document getTopPlayersOnLeaderboard() {
        List<StatsLeaderboard> statsLeaderboards = StatsLeaderboardManager.LEADERBOARD_CTF.getComps().getLeaderboards();
        org.bson.Document document = new org.bson.Document("date", Instant.now()).append("total_players", statsLeaderboards.get(0).getSortedWeekly().size());
        for (String title : WEEKLY_EXPERIENCE_LEADERBOARDS) {
            statsLeaderboards.stream().filter(leaderboard -> leaderboard.getTitle().equals(title)).findFirst().ifPresent(leaderboard -> {
                Number[] numbers = leaderboard.getTopThreeValues();
                String[] names = leaderboard.getTopThreePlayerNames(numbers, DatabasePlayer::getName);
                String[] uuids = leaderboard.getTopThreePlayerNames(numbers, databasePlayer -> databasePlayer.getUuid().toString());
                List<org.bson.Document> topList = new ArrayList<>();
                for (int i = 0; i < numbers.length; i++) {
                    topList.add(new org.bson.Document("names", names[i]).append("uuids", uuids[i]).append("amount", numbers[i]));
                }
                org.bson.Document totalDocument = new org.bson.Document();
                if (numbers[0] instanceof Integer) {
                    totalDocument = new org.bson.Document("total", Arrays.stream(numbers).mapToInt(Number::intValue).sum());
                } else if (numbers[0] instanceof Long) {
                    totalDocument = new org.bson.Document("total", Arrays.stream(numbers).mapToLong(Number::longValue).sum());
                }
                document.append(title.toLowerCase().replace(" ", "_"), totalDocument.append("name", title).append("top", topList));
            });
        }
        return document;
    }

    public String getTitle() {
        return title;
    }

    public Instant getLastReset() {
        return lastReset;
    }

    public void setLastReset(Instant lastReset) {
        this.lastReset = lastReset;
    }

    public Timing getTiming() {
        return timing;
    }

    @Override
    public String toString() {
        return "DatabaseTiming{" +
                "title='" + title + '\'' +
                ", timing=" + timing +
                '}';
    }
}