package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.mithra.spidersdwelling;

import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGameBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerResult;
import com.ebicep.warlords.database.repositories.games.pojos.pve.wavedefense.DatabaseGamePlayerPvEWaveDefense;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.DatabaseWarlordsClasses;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.mithra.spidersdwelling.classes.*;
import com.ebicep.warlords.game.GameMode;
import com.ebicep.warlords.player.general.Classes;
import com.ebicep.warlords.player.general.Specializations;
import com.ebicep.warlords.util.chat.ChatUtils;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedHashMap;
import java.util.Map;

public class DatabasePlayerPvEEventSpidersDwellingDifficultyStats extends PvEEventSpidersDwellingDatabaseStatInformation implements DatabaseWarlordsClasses<PvEEventSpidersDwellingDatabaseStatInformation> {

    private DatabaseMagePvEEventSpidersDwelling mage = new DatabaseMagePvEEventSpidersDwelling();
    private DatabaseWarriorPvEEventSpidersDwelling warrior = new DatabaseWarriorPvEEventSpidersDwelling();
    private DatabasePaladinPvEEventSpidersDwelling paladin = new DatabasePaladinPvEEventSpidersDwelling();
    private DatabaseShamanPvEEventSpidersDwelling shaman = new DatabaseShamanPvEEventSpidersDwelling();
    private DatabaseRoguePvEEventSpidersDwelling rogue = new DatabaseRoguePvEEventSpidersDwelling();
    private DatabaseArcanistPvEEventSpidersDwelling arcanist = new DatabaseArcanistPvEEventSpidersDwelling();
    @Field("player_count_stats")
    private Map<Integer, DatabasePlayerPvEEventSpidersDwellingPlayerCountStats> playerCountStats = new LinkedHashMap<>() {{
        put(1, new DatabasePlayerPvEEventSpidersDwellingPlayerCountStats());
        put(2, new DatabasePlayerPvEEventSpidersDwellingPlayerCountStats());
        put(3, new DatabasePlayerPvEEventSpidersDwellingPlayerCountStats());
        put(4, new DatabasePlayerPvEEventSpidersDwellingPlayerCountStats());
    }};

    public DatabasePlayerPvEEventSpidersDwellingDifficultyStats() {
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
        DatabasePlayerPvEEventSpidersDwellingPlayerCountStats countStats = this.getPlayerCountStats(playerCount);
        if (countStats != null) {
            countStats.updateStats(databasePlayer, databaseGame, gamePlayer, multiplier, playersCollection);
        } else {
            ChatUtils.MessageType.GAME_SERVICE.sendErrorMessage("Invalid player count = " + playerCount);
        }

    }

    @Override
    public DatabaseBasePvEEventSpidersDwelling getSpec(Specializations specializations) {
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
            case CONJURER -> arcanist.getConjurer();
            case SENTINEL -> arcanist.getSentinel();
            case CLERIC -> arcanist.getCleric();
        };
    }

    @Override
    public DatabaseBasePvEEventSpidersDwelling getClass(Classes classes) {
        return switch (classes) {
            case MAGE -> mage;
            case WARRIOR -> warrior;
            case PALADIN -> paladin;
            case SHAMAN -> shaman;
            case ROGUE -> rogue;
            case ARCANIST -> arcanist;
        };
    }

    @Override
    public DatabaseBasePvEEventSpidersDwelling[] getClasses() {
        return new DatabaseBasePvEEventSpidersDwelling[]{mage, warrior, paladin, shaman, rogue};
    }

    @Override
    public PvEEventSpidersDwellingDatabaseStatInformation getMage() {
        return mage;
    }

    @Override
    public PvEEventSpidersDwellingDatabaseStatInformation getWarrior() {
        return warrior;
    }

    @Override
    public PvEEventSpidersDwellingDatabaseStatInformation getPaladin() {
        return paladin;
    }

    @Override
    public PvEEventSpidersDwellingDatabaseStatInformation getShaman() {
        return shaman;
    }

    @Override
    public PvEEventSpidersDwellingDatabaseStatInformation getRogue() {
        return rogue;
    }

    public DatabasePlayerPvEEventSpidersDwellingPlayerCountStats getPlayerCountStats(int playerCount) {
        if (playerCount < 1) {
            return null;
        }
        return playerCountStats.computeIfAbsent(playerCount, k -> new DatabasePlayerPvEEventSpidersDwellingPlayerCountStats());
    }

}
