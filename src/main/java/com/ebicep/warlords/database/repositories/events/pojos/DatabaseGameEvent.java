package com.ebicep.warlords.database.repositories.events.pojos;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.database.leaderboards.events.EventsLeaderboardManager;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer;
import com.ebicep.warlords.database.repositories.player.pojos.general.FutureMessage;
import com.ebicep.warlords.guilds.Guild;
import com.ebicep.warlords.guilds.GuildManager;
import com.ebicep.warlords.guilds.GuildPlayer;
import com.ebicep.warlords.guilds.logs.types.general.GuildLogGameEventReward;
import com.ebicep.warlords.pve.Currencies;
import com.ebicep.warlords.util.chat.ChatUtils;
import com.ebicep.warlords.util.java.NumberFormat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Document(collection = "Game_Events")
public class DatabaseGameEvent {

    public static final HashMap<GameEvents, DatabaseGameEvent> PREVIOUS_GAME_EVENTS = new HashMap<>();
    public static final HashMap<GameEvents, List<Long>> ALL_GAME_EVENT_TIMES = new HashMap<>();
    public static DatabaseGameEvent currentGameEvent = null;

    public static void sendGameEventMessage(Player player, String message) {
        player.sendMessage(ChatColor.GOLD + "Game Events" + ChatColor.DARK_GRAY + " > " + message);
    }

    public static void startGameEvent() {
        long start = System.nanoTime();
        ChatUtils.MessageTypes.GAME_EVENTS.sendMessage("Scanning for game events...");
        Warlords.newChain()
                .asyncFirst(() -> DatabaseManager.gameEventsService.findAll())
                .syncLast(gameEvents -> {
                    Instant now = Instant.now();
                    for (DatabaseGameEvent gameEvent : gameEvents) {
                        ALL_GAME_EVENT_TIMES.computeIfAbsent(gameEvent.getEvent(), k -> new ArrayList<>()).add(gameEvent.getStartDateSecond());
                        if (gameEvent.getStartDate().isAfter(now)) {
                            continue;
                        }
                        if (gameEvent.getEndDate().isBefore(now)) {
                            PREVIOUS_GAME_EVENTS.put(gameEvent.getEvent(), gameEvent);
                        } else if (gameEvent.getEndDate().isAfter(now)) {
                            ChatUtils.MessageTypes.GAME_EVENTS.sendMessage("Found active game event: " + gameEvent.getEvent().name + " in " + (System.nanoTime() - start) / 1000000 + "ms");
                            ChatUtils.MessageTypes.GAME_EVENTS.sendMessage("Start: " + gameEvent.getStartDate());
                            ChatUtils.MessageTypes.GAME_EVENTS.sendMessage("End: " + gameEvent.getEndDate());
                            currentGameEvent = gameEvent;
                            if (!gameEvent.getStarted()) {
                                ChatUtils.MessageTypes.GAME_EVENTS.sendMessage("New Event Detected, clearing player currencies...");
                                Currencies currency = gameEvent.getEvent().currency;
                                for (DatabasePlayer databasePlayer : DatabaseManager.CACHED_PLAYERS.get(PlayersCollections.LIFETIME).values()) {
                                    Long currencyValue = databasePlayer.getPveStats().getCurrencyValue(currency);
                                    if (currencyValue > 0) {
                                        databasePlayer.getPveStats().subtractCurrency(currency, currencyValue);
                                        DatabaseManager.queueUpdatePlayerAsync(databasePlayer);
                                    }
                                }
                                gameEvent.setStarted(true);
                                Warlords.newChain()
                                        .async(() -> DatabaseManager.gameEventsService.update(gameEvent)).execute();
                            }
                            gameEvent.start();
                            break;
                        }
                    }
                    if (currentGameEvent == null && !PREVIOUS_GAME_EVENTS.isEmpty()) {
                        DatabaseGameEvent gameEvent = PREVIOUS_GAME_EVENTS
                                .values()
                                .stream()
                                .min((o1, o2) -> o2.getEndDate().compareTo(o1.getEndDate()))
                                .get();
                        ChatUtils.MessageTypes.GAME_EVENTS.sendMessage("Days from last game event: " + gameEvent.getEndDate()
                                                                                                                .until(Instant.now(), ChronoUnit.DAYS));
                        if (gameEvent.getEndDate().isAfter(Instant.now().minus(7, ChronoUnit.DAYS))) {
                            currentGameEvent = gameEvent;
                            currentGameEvent.start();
                        } else {
                            ChatUtils.MessageTypes.GAME_EVENTS.sendMessage("Last game event was over 7 days ago, not starting");
                        }
                    }
                    EventsLeaderboardManager.create();
                })
                .execute();
    }

    public GameEvents getEvent() {
        return event;
    }

    public long getStartDateSecond() {
        return startDate.getEpochSecond();
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public Boolean getStarted() {
        return started;
    }

    public void setStarted(Boolean started) {
        this.started = started;
    }

    public void start() {
        if (GameEvents.npc != null) {
            GameEvents.npc.destroy();
        }
        if (!gaveRewards) {
            getEvent().initialize();
        }
        ChatUtils.MessageTypes.GAME_EVENTS.sendMessage("Creating new Game Event NPC...");
        getEvent().createNPC();
    }

    @Id
    protected String id;
    private GameEvents event;
    @Field("start_date")
    private Instant startDate;
    @Field("end_date")
    private Instant endDate;
    private boolean started;
    @Field("gave_rewards")
    private boolean gaveRewards;

    public void giveRewards() {
        //player rewards
        List<DatabasePlayer> databasePlayers = DatabaseManager.CACHED_PLAYERS
                .get(PlayersCollections.LIFETIME)
                .values()
                .stream()
                .filter(databasePlayer -> event.eventsStatsFunction.apply(databasePlayer.getPveStats().getEventStats())
                                                                   .getOrDefault(getStartDateSecond(), null) != null &&
                        event.eventsStatsFunction.apply(databasePlayer.getPveStats().getEventStats())
                                                 .get(getStartDateSecond())
                                                 .getEventPointsCumulative() > 0)
                .sorted((o1, o2) -> Long.compare(
                        event.eventsStatsFunction.apply(o2.getPveStats().getEventStats()).get(getStartDateSecond()).getEventPointsCumulative(),
                        event.eventsStatsFunction.apply(o1.getPveStats().getEventStats()).get(getStartDateSecond()).getEventPointsCumulative()
                ))
                .collect(Collectors.toList());
        ChatUtils.MessageTypes.GAME_EVENTS.sendMessage("Giving rewards for " + event.name + " (" + getStartDateSecond() + ") (" + databasePlayers.size() + " players)");
        HashMap<DatabasePlayer, List<String>> playerMessages = new HashMap<>();
        for (int i = 0; i < databasePlayers.size(); i++) {
            int position = i + 1;
            DatabasePlayer databasePlayer = databasePlayers.get(i);
            databasePlayer.getPveStats()
                          .getGameEventRewards()
                          .add(new GameEventReward(event.getRewards(position), event.name + " Event", getStartDateSecond()));

            List<String> messages = new ArrayList<>();
            messages.add(ChatColor.GOLD + "------------------------------------------------");
            messages.add(ChatColor.RED + event.name + " Event has Ended!");
            messages.add("");
            messages.add(ChatColor.YELLOW + "#" + position + ". " +
                    ChatColor.AQUA + databasePlayer.getName() +
                    ChatColor.GRAY + " - " +
                    ChatColor.YELLOW + NumberFormat.addCommas(event.eventsStatsFunction.apply(databasePlayer.getPveStats().getEventStats())
                                                                                       .get(getStartDateSecond())
                                                                                       .getEventPointsCumulative()) + " Points");
            playerMessages.put(databasePlayer, messages);
        }
        //guild rewards
        List<Guild> guilds = GuildManager.GUILDS
                .stream()
                .filter(guild -> guild.getEventPoints(event, getStartDateSecond()) > 0)
                .sorted((o1, o2) -> Long.compare(
                        o2.getEventPoints(event, getStartDateSecond()),
                        o1.getEventPoints(event, getStartDateSecond())
                ))
                .collect(Collectors.toList());
        for (int i = 0; i < guilds.size(); i++) {
            int position = i + 1;
            Guild guild = guilds.get(i);
            LinkedHashMap<String, Long> guildRewards = event.getGuildRewards(position);
            guild.addCurrentCoins(guildRewards.getOrDefault("Coins", 0L));
            guild.addExperience(guildRewards.getOrDefault("Experience", 0L));
            guild.log(new GuildLogGameEventReward(event, getStartDateSecond(), position, guildRewards));

            for (GuildPlayer player : guild.getPlayers()) {
                for (DatabasePlayer databasePlayer : playerMessages.keySet()) {
                    if (databasePlayer.getUuid().equals(player.getUUID())) {
                        playerMessages.get(databasePlayer).add(ChatColor.YELLOW + "#" + position + ". " +
                                ChatColor.GOLD + guild.getName() +
                                ChatColor.GRAY + " - " +
                                ChatColor.YELLOW + NumberFormat.addCommas(guild.getEventPoints(event, getStartDateSecond())) + " Points");
                        break;
                    }
                }
            }

            guild.queueUpdate();
        }
        //player reward message
        playerMessages.forEach((databasePlayer, messages) -> {
            messages.add("");
            messages.add(ChatColor.GREEN + "Claim your rewards through your");
            messages.add(ChatColor.GREEN + "Reward Inventory!");
            messages.add(ChatColor.GOLD + "------------------------------------------------");
            databasePlayer.addFutureMessage(new FutureMessage(messages, true));
            DatabaseManager.queueUpdatePlayerAsync(databasePlayer);
        });
    }

    public boolean isGaveRewards() {
        return gaveRewards;
    }

    public void setGaveRewards(boolean gaveRewards) {
        this.gaveRewards = gaveRewards;
    }

    @Override
    public String toString() {
        return "DatabaseGameEvent{" +
                "event=" + event +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", started=" + started +
                '}';
    }
}
