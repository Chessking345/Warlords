package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.boltaro.boltarobonanza;

import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGameBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerResult;
import com.ebicep.warlords.database.repositories.games.pojos.pve.wavedefense.DatabaseGamePlayerPvEWaveDefense;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.DatabaseWarlordsClasses;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.boltaro.boltarobonanza.classes.*;
import com.ebicep.warlords.game.GameMode;
import com.ebicep.warlords.player.general.Classes;
import com.ebicep.warlords.player.general.Specializations;
import com.ebicep.warlords.util.chat.ChatUtils;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedHashMap;
import java.util.Map;

public class DatabasePlayerPvEEventBoltaroBonanzaDifficultyStats extends PvEEventBoltaroBonanzaDatabaseStatInformation implements DatabaseWarlordsClasses<PvEEventBoltaroBonanzaDatabaseStatInformation> {

    private DatabaseMagePvEEventBoltaroBonanza mage = new DatabaseMagePvEEventBoltaroBonanza();
    private DatabaseWarriorPvEEventBoltaroBonanza warrior = new DatabaseWarriorPvEEventBoltaroBonanza();
    private DatabasePaladinPvEEventBoltaroBonanza paladin = new DatabasePaladinPvEEventBoltaroBonanza();
    private DatabaseShamanPvEEventBoltaroBonanza shaman = new DatabaseShamanPvEEventBoltaroBonanza();
    private DatabaseRoguePvEEventBoltaroBonanza rogue = new DatabaseRoguePvEEventBoltaroBonanza();
    @Field("player_count_stats")
    private Map<Integer, DatabasePlayerPvEEventBoltaroBonanzaPlayerCountStats> playerCountStats = new LinkedHashMap<>() {{
        put(1, new DatabasePlayerPvEEventBoltaroBonanzaPlayerCountStats());
        put(2, new DatabasePlayerPvEEventBoltaroBonanzaPlayerCountStats());
        put(3, new DatabasePlayerPvEEventBoltaroBonanzaPlayerCountStats());
        put(4, new DatabasePlayerPvEEventBoltaroBonanzaPlayerCountStats());
    }};

    public DatabasePlayerPvEEventBoltaroBonanzaDifficultyStats() {
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
        DatabasePlayerPvEEventBoltaroBonanzaPlayerCountStats countStats = this.getPlayerCountStats(playerCount);
        if (countStats != null) {
            countStats.updateStats(databasePlayer, databaseGame, gamePlayer, multiplier, playersCollection);
        } else {
            ChatUtils.MessageTypes.GAME_SERVICE.sendErrorMessage("Invalid player count = " + playerCount);
        }

    }

    @Override
    public DatabaseBasePvEEventBoltaroBonanza getSpec(Specializations specializations) {
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
    public DatabaseBasePvEEventBoltaroBonanza getClass(Classes classes) {
        return switch (classes) {
            case MAGE -> mage;
            case WARRIOR -> warrior;
            case PALADIN -> paladin;
            case SHAMAN -> shaman;
            case ROGUE -> rogue;
        };
    }

    @Override
    public DatabaseBasePvEEventBoltaroBonanza[] getClasses() {
        return new DatabaseBasePvEEventBoltaroBonanza[]{mage, warrior, paladin, shaman, rogue};
    }

    @Override
    public PvEEventBoltaroBonanzaDatabaseStatInformation getMage() {
        return mage;
    }

    @Override
    public PvEEventBoltaroBonanzaDatabaseStatInformation getWarrior() {
        return warrior;
    }

    @Override
    public PvEEventBoltaroBonanzaDatabaseStatInformation getPaladin() {
        return paladin;
    }

    @Override
    public PvEEventBoltaroBonanzaDatabaseStatInformation getShaman() {
        return shaman;
    }

    @Override
    public PvEEventBoltaroBonanzaDatabaseStatInformation getRogue() {
        return rogue;
    }

    public DatabasePlayerPvEEventBoltaroBonanzaPlayerCountStats getPlayerCountStats(int playerCount) {
        if (playerCount < 1) {
            return null;
        }
        return playerCountStats.computeIfAbsent(playerCount, k -> new DatabasePlayerPvEEventBoltaroBonanzaPlayerCountStats());
    }

}
