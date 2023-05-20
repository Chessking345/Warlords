package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.mithra;

import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGameBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerResult;
import com.ebicep.warlords.database.repositories.games.pojos.pve.events.mithra.spidersdwelling.DatabaseGamePvEEventSpidersDwelling;
import com.ebicep.warlords.database.repositories.games.pojos.pve.wavedefense.DatabaseGamePlayerPvEWaveDefense;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.DatabaseWarlordsClasses;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.EventMode;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.mithra.classes.*;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.mithra.spidersdwelling.DatabasePlayerPvEEventSpidersDwellingDifficultyStats;
import com.ebicep.warlords.game.GameMode;
import com.ebicep.warlords.player.general.Classes;
import com.ebicep.warlords.player.general.Specializations;
import com.ebicep.warlords.util.chat.ChatUtils;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedHashMap;
import java.util.Map;

public class DatabasePlayerPvEEventMithraDifficultyStats extends PvEEventMithraDatabaseStatInformation implements DatabaseWarlordsClasses<PvEEventMithraDatabaseStatInformation>, EventMode {

    private DatabaseMagePvEEventMithra mage = new DatabaseMagePvEEventMithra();
    private DatabaseWarriorPvEEventMithra warrior = new DatabaseWarriorPvEEventMithra();
    private DatabasePaladinPvEEventMithra paladin = new DatabasePaladinPvEEventMithra();
    private DatabaseShamanPvEEventMithra shaman = new DatabaseShamanPvEEventMithra();
    private DatabaseRoguePvEEventMithra rogue = new DatabaseRoguePvEEventMithra();
    @Field("player_count_stats")
    private Map<Integer, DatabasePlayerPvEEventMithraPlayerCountStats> playerCountStats = new LinkedHashMap<>() {{
        put(1, new DatabasePlayerPvEEventMithraPlayerCountStats());
        put(2, new DatabasePlayerPvEEventMithraPlayerCountStats());
        put(3, new DatabasePlayerPvEEventMithraPlayerCountStats());
        put(4, new DatabasePlayerPvEEventMithraPlayerCountStats());
    }};
    @Field("event_points_spent")
    private long eventPointsSpent;
    @Field("rewards_purchased")
    private Map<String, Long> rewardsPurchased = new LinkedHashMap<>();
    @Field("spiders_dwelling_stats")
    private DatabasePlayerPvEEventSpidersDwellingDifficultyStats spidersDwellingStats = new DatabasePlayerPvEEventSpidersDwellingDifficultyStats();

    public DatabasePlayerPvEEventMithraDifficultyStats() {
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
        DatabasePlayerPvEEventMithraPlayerCountStats countStats = this.getPlayerCountStats(playerCount);
        if (countStats != null) {
            countStats.updateStats(databasePlayer, databaseGame, gamePlayer, multiplier, playersCollection);
        } else {
            ChatUtils.MessageType.GAME_SERVICE.sendErrorMessage("Invalid player count = " + playerCount);
        }

        //MODES
        if (databaseGame instanceof DatabaseGamePvEEventSpidersDwelling) {
            this.spidersDwellingStats.updateStats(databasePlayer, databaseGame, gamePlayer, multiplier, playersCollection);
        }
    }

    @Override
    public DatabaseBasePvEEventMithra getSpec(Specializations specializations) {
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
    public DatabaseBasePvEEventMithra getClass(Classes classes) {
        return switch (classes) {
            case MAGE -> mage;
            case WARRIOR -> warrior;
            case PALADIN -> paladin;
            case SHAMAN -> shaman;
            case ROGUE -> rogue;
        };
    }

    @Override
    public DatabaseBasePvEEventMithra[] getClasses() {
        return new DatabaseBasePvEEventMithra[]{mage, warrior, paladin, shaman, rogue};
    }

    @Override
    public PvEEventMithraDatabaseStatInformation getMage() {
        return mage;
    }

    @Override
    public PvEEventMithraDatabaseStatInformation getWarrior() {
        return warrior;
    }

    @Override
    public PvEEventMithraDatabaseStatInformation getPaladin() {
        return paladin;
    }

    @Override
    public PvEEventMithraDatabaseStatInformation getShaman() {
        return shaman;
    }

    @Override
    public PvEEventMithraDatabaseStatInformation getRogue() {
        return rogue;
    }

    public DatabasePlayerPvEEventMithraPlayerCountStats getPlayerCountStats(int playerCount) {
        if (playerCount < 1) {
            return null;
        }
        return playerCountStats.computeIfAbsent(playerCount, k -> new DatabasePlayerPvEEventMithraPlayerCountStats());
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

    public DatabasePlayerPvEEventSpidersDwellingDifficultyStats getSpidersDwellingStats() {
        return spidersDwellingStats;
    }
}
