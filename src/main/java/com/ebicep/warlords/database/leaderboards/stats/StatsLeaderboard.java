package com.ebicep.warlords.database.leaderboards.stats;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.filoghost.holographicdisplays.api.hologram.HologramLines;
import me.filoghost.holographicdisplays.api.hologram.VisibilitySettings;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StatsLeaderboard {

    public static final int MAX_PAGES = 5;
    public static final int PLAYERS_PER_PAGE = 10;
    private final String title;
    private final Location location;
    private final HashMap<PlayersCollections, List<DatabasePlayer>> sortedTimedPlayers = new HashMap<>();
    private final HashMap<PlayersCollections, List<List<Hologram>>> sortedTimedHolograms = new HashMap<>() {{
        for (PlayersCollections value : PlayersCollections.values()) {
            put(value, new ArrayList<>());
        }
    }};
    private final Function<DatabasePlayer, Number> valueFunction;
    private final Function<DatabasePlayer, String> stringFunction;
    private final Comparator<DatabasePlayer> comparator;

    public StatsLeaderboard(String title, Location location, Function<DatabasePlayer, Number> valueFunction, Function<DatabasePlayer, String> stringFunction) {
        this.title = title;
        this.location = location;
        this.valueFunction = valueFunction;
        this.stringFunction = stringFunction;
        this.comparator = (o1, o2) -> {
            //if (o1.getUuid().equals(o2.getUuid())) return 0;
            BigDecimal value1 = new BigDecimal(valueFunction.apply(o1).toString());
            BigDecimal value2 = new BigDecimal(valueFunction.apply(o2).toString());
            return value2.compareTo(value1);
        };
        for (PlayersCollections value : PlayersCollections.values()) {
            sortedTimedPlayers.put(value, new ArrayList<>());
        }
    }

    public static int compare(Number a, Number b) {
        return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatsLeaderboard that = (StatsLeaderboard) o;
        return Objects.equals(title, that.title) && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, location);
    }

    public void resetHolograms(PlayersCollections collection, Set<DatabasePlayer> databasePlayers, String categoryName, String subTitle) {
        //resetting sort then adding new sorted values
        resetSortedPlayers(databasePlayers, collection);
        //creating leaderboard
        List<Hologram> holograms = new ArrayList<>();
        for (int i = 0; i < StatsLeaderboard.MAX_PAGES; i++) {
            holograms.add(createHologram(collection, i, subTitle + " - " + (categoryName.isEmpty() ? "" : categoryName + " - ") + collection.name));
        }
        getSortedHolograms(collection).clear();
        getSortedHolograms(collection).add(holograms);
    }

    public Hologram createHologram(PlayersCollections collection, int page, String subTitle) {
        List<DatabasePlayer> databasePlayers = getSortedPlayers(collection);

        Hologram hologram = HolographicDisplaysAPI.get(Warlords.getInstance()).createHologram(location);
        HologramLines hologramLines = hologram.getLines();
        hologramLines.appendText(ChatColor.AQUA + ChatColor.BOLD.toString() + collection.name + " " + title);
        hologramLines.appendText(ChatColor.GRAY + subTitle);
        for (int i = page * PLAYERS_PER_PAGE; i < (page + 1) * PLAYERS_PER_PAGE && i < databasePlayers.size(); i++) {
            DatabasePlayer databasePlayer = databasePlayers.get(i);
            hologramLines.appendText(ChatColor.YELLOW.toString() + (i + 1) + ". " + ChatColor.AQUA + databasePlayer.getName() + ChatColor.GRAY + " - " + ChatColor.YELLOW + stringFunction.apply(databasePlayer));
        }
        hologram.getVisibilitySettings().setGlobalVisibility(VisibilitySettings.Visibility.HIDDEN);

        return hologram;
    }

    public List<DatabasePlayer> getSortedPlayers(PlayersCollections collections) {
        return sortedTimedPlayers.get(collections);
    }

    public List<List<Hologram>> getSortedHolograms(PlayersCollections collections) {
        return sortedTimedHolograms.get(collections);
    }

    public void resetSortedPlayers(Set<DatabasePlayer> newSortedPlayers, PlayersCollections collections) {
        List<DatabasePlayer> databasePlayers = new ArrayList<>(newSortedPlayers);
        databasePlayers.sort(comparator);
        sortedTimedPlayers.put(collections, databasePlayers);
    }

    public <T extends Number> T[] getTopThreeValues() {
        //current top value to compare to
        List<DatabasePlayer> sortedWeekly = sortedTimedPlayers.get(PlayersCollections.WEEKLY);
        Number topValue = valueFunction.apply(sortedWeekly.get(0));

        Class<T> clazz = (Class<T>) topValue.getClass();
        //ouput array of type clazz
        T[] output = (T[]) Array.newInstance(clazz, 3);
        //first top number is current top
        output[0] = (T) topValue;

        List<Number> topThree = new ArrayList<>();
        int counter = 0;
        //looping to get the next top two numbers
        //filtering out all players with 3 or less games from leaderboards if the top player has 10 or more (no one game olivers)
        boolean filter = sortedWeekly.get(0).getPlays() >= 10;
        List<DatabasePlayer> databasePlayers;
        if (filter) {
            databasePlayers = sortedWeekly.stream().filter(databasePlayer -> databasePlayer.getPlays() > 3).collect(Collectors.toList());
        } else {
            databasePlayers = new ArrayList<>(sortedWeekly);
        }

        for (DatabasePlayer databasePlayer : databasePlayers) {
            //must have more than 3 plays to get awarded
            if (databasePlayer.getPlays() <= 3) continue;

            Number currentTopValue = valueFunction.apply(databasePlayer);
            if (counter < 2) {
                if (compare(topValue, currentTopValue) > 0) {
                    topThree.add(currentTopValue);
                    topValue = currentTopValue;
                    counter++;
                }
            } else {
                break;
            }
        }

        //adding last two top numbers
        for (int i = 0; i < topThree.size(); i++) {
            output[i + 1] = (T) topThree.get(i);
        }

        return output;
    }

    public String[] getTopThreePlayerNames(Number[] numbers, Function<DatabasePlayer, String> function) {
        String[] topThreePlayers = new String[3];
        Arrays.fill(topThreePlayers, "");

        //matching top value with players
        for (int i = 0; i < numbers.length; i++) {
            Number topValue = numbers[i];
            for (DatabasePlayer databasePlayer : sortedTimedPlayers.get(PlayersCollections.WEEKLY)) {
                if (Objects.equals(valueFunction.apply(databasePlayer), topValue)) {
                    topThreePlayers[i] = topThreePlayers[i] + function.apply(databasePlayer) + ",";
                }
            }
            if (i == 2) {
                break;
            }
        }

        //removing end comma
        for (int i = 0; i < topThreePlayers.length; i++) {
            if (topThreePlayers[i].length() > 0) {
                topThreePlayers[i] = topThreePlayers[i].substring(0, topThreePlayers[i].length() - 1);
            }
        }
        return topThreePlayers;
    }

    public String getTitle() {
        return title;
    }

    public Location getLocation() {
        return location;
    }

    public HashMap<PlayersCollections, List<List<Hologram>>> getSortedTimedHolograms() {
        return sortedTimedHolograms;
    }

    public Function<DatabasePlayer, String> getStringFunction() {
        return stringFunction;
    }
}
