package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.narmer;

import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGameBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerResult;
import com.ebicep.warlords.database.repositories.games.pojos.pve.events.narmer.narmerstomb.DatabaseGamePvEEventNarmersTomb;
import com.ebicep.warlords.database.repositories.games.pojos.pve.wavedefense.DatabaseGamePlayerPvEWaveDefense;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.DatabaseWarlordsClasses;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.EventMode;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.narmer.classes.*;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.narmer.narmerstomb.DatabasePlayerPvEEventNarmersTombDifficultyStats;
import com.ebicep.warlords.game.GameMode;
import com.ebicep.warlords.player.general.Classes;
import com.ebicep.warlords.player.general.Specializations;
import com.ebicep.warlords.util.chat.ChatUtils;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedHashMap;
import java.util.Map;

public class DatabasePlayerPvEEventNarmerDifficultyStats extends PvEEventNarmerDatabaseStatInformation implements DatabaseWarlordsClasses<PvEEventNarmerDatabaseStatInformation>, EventMode {

    private DatabaseMagePvEEventNarmer mage = new DatabaseMagePvEEventNarmer();
    private DatabaseWarriorPvEEventNarmer warrior = new DatabaseWarriorPvEEventNarmer();
    private DatabasePaladinPvEEventNarmer paladin = new DatabasePaladinPvEEventNarmer();
    private DatabaseShamanPvEEventNarmer shaman = new DatabaseShamanPvEEventNarmer();
    private DatabaseRoguePvEEventNarmer rogue = new DatabaseRoguePvEEventNarmer();
    @Field("player_count_stats")
    private Map<Integer, DatabasePlayerPvEEventNarmerPlayerCountStats> playerCountStats = new LinkedHashMap<>() {{
        put(1, new DatabasePlayerPvEEventNarmerPlayerCountStats());
        put(2, new DatabasePlayerPvEEventNarmerPlayerCountStats());
        put(3, new DatabasePlayerPvEEventNarmerPlayerCountStats());
        put(4, new DatabasePlayerPvEEventNarmerPlayerCountStats());
    }};
    @Field("event_points_spent")
    private long eventPointsSpent;
    @Field("rewards_purchased")
    private Map<String, Long> rewardsPurchased = new LinkedHashMap<>();
    @Field("tomb_stats")
    private DatabasePlayerPvEEventNarmersTombDifficultyStats tombStats = new DatabasePlayerPvEEventNarmersTombDifficultyStats();

    public DatabasePlayerPvEEventNarmerDifficultyStats() {
    }

    @Override
    public void updateCustomStats(
            com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer databasePlayer, DatabaseGameBase databaseGame,
            GameMode gameMode,
            DatabaseGamePlayerBase gamePlayer,
            DatabaseGamePlayerResult result,
            int multiplier,
            PlayersCollections playersCollection
    ) {
        assert gamePlayer instanceof DatabaseGamePlayerPvEWaveDefense;

        super.updateCustomStats(databasePlayer, databaseGame, gameMode, gamePlayer, result, multiplier, playersCollection);

        //UPDATE UNIVERSAL EXPERIENCE
        this.experience += gamePlayer.getExperienceEarnedUniversal() * multiplier;
        this.experiencePvE += gamePlayer.getExperienceEarnedUniversal() * multiplier;

        //UPDATE CLASS, SPEC
        this.getClass(Specializations.getClass(gamePlayer.getSpec())).updateStats(databasePlayer, databaseGame, gamePlayer, multiplier, playersCollection);
        this.getSpec(gamePlayer.getSpec()).updateStats(databasePlayer, databaseGame, gamePlayer, multiplier, playersCollection);

        //UPDATE PLAYER COUNT STATS
        int playerCount = databaseGame.getBasePlayers().size();
        DatabasePlayerPvEEventNarmerPlayerCountStats countStats = this.getPlayerCountStats(playerCount);
        if (countStats != null) {
            countStats.updateStats(databasePlayer, databaseGame, gamePlayer, multiplier, playersCollection);
        } else {
            ChatUtils.MessageType.GAME_SERVICE.sendErrorMessage("Invalid player count = " + playerCount);
        }

        //MODES
        if (databaseGame instanceof DatabaseGamePvEEventNarmersTomb) {
            this.tombStats.updateStats(databasePlayer, databaseGame, gamePlayer, multiplier, playersCollection);
        }
    }

    @Override
    public DatabaseBasePvEEventNarmer getSpec(Specializations specializations) {
        return switch (specializations) {
            case PYROMANCER -> mage.getPyromancer();
            case CRYOMANCER -> mage.getCryomancer();
            case AQUAMANCER -> mage.getAquamancer();
            case BERSERKER -> warrior.getBerserker();
            case DEFENDER -> warrior.getDefender();
            case REVENANT -> warrior.getRevenant();
            case AVENGER -> paladin.getAvenger();
            case CRUSADER -> paladin.getCrusader();
            case PROTECTOR -> paladin.getProtector();
            case THUNDERLORD -> shaman.getThunderlord();
            case SPIRITGUARD -> shaman.getSpiritguard();
            case EARTHWARDEN -> shaman.getEarthwarden();
            case ASSASSIN -> rogue.getAssassin();
            case VINDICATOR -> rogue.getVindicator();
            case APOTHECARY -> rogue.getApothecary();
        };
    }

    @Override
    public DatabaseBasePvEEventNarmer getClass(Classes classes) {
        return switch (classes) {
            case MAGE -> mage;
            case WARRIOR -> warrior;
            case PALADIN -> paladin;
            case SHAMAN -> shaman;
            case ROGUE -> rogue;
        };
    }

    @Override
    public DatabaseBasePvEEventNarmer[] getClasses() {
        return new DatabaseBasePvEEventNarmer[]{mage, warrior, paladin, shaman, rogue};
    }

    @Override
    public PvEEventNarmerDatabaseStatInformation getMage() {
        return mage;
    }

    @Override
    public PvEEventNarmerDatabaseStatInformation getWarrior() {
        return warrior;
    }

    @Override
    public PvEEventNarmerDatabaseStatInformation getPaladin() {
        return paladin;
    }

    @Override
    public PvEEventNarmerDatabaseStatInformation getShaman() {
        return shaman;
    }

    @Override
    public PvEEventNarmerDatabaseStatInformation getRogue() {
        return rogue;
    }

    public DatabasePlayerPvEEventNarmerPlayerCountStats getPlayerCountStats(int playerCount) {
        if (playerCount < 1) {
            return null;
        }
        return playerCountStats.computeIfAbsent(playerCount, k -> new DatabasePlayerPvEEventNarmerPlayerCountStats());
    }

    @Override
    public long getEventPointsSpent() {
        return eventPointsSpent;
    }

    @Override
    public void addEventPointsSpent(long eventPointsSpent) {
        this.eventPointsSpent += eventPointsSpent;
    }

    @Override
    public Map<String, Long> getRewardsPurchased() {
        return rewardsPurchased;
    }

    public DatabasePlayerPvEEventNarmersTombDifficultyStats getTombStats() {
        return tombStats;
    }
}
