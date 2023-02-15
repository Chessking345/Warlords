package com.ebicep.warlords.database.repositories.player.pojos.ctf;

import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGameBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerResult;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.ctf.classses.*;
import com.ebicep.warlords.game.GameMode;
import com.ebicep.warlords.player.general.Classes;
import com.ebicep.warlords.player.general.Specializations;

public class DatabasePlayerCTF extends CTFDatabaseStatInformation implements com.ebicep.warlords.database.repositories.player.pojos.DatabasePlayer {

    private DatabaseMageCTF mage = new DatabaseMageCTF();
    private DatabaseWarriorCTF warrior = new DatabaseWarriorCTF();
    private DatabasePaladinCTF paladin = new DatabasePaladinCTF();
    private DatabaseShamanCTF shaman = new DatabaseShamanCTF();
    private DatabaseRogueCTF rogue = new DatabaseRogueCTF();

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

        //UPDATE UNIVERSAL EXPERIENCE
        this.experience += gamePlayer.getExperienceEarnedUniversal() * multiplier;

        //UPDATE CLASS, SPEC
        this.getClass(Specializations.getClass(gamePlayer.getSpec())).updateStats(databaseGame, gamePlayer, multiplier, playersCollection);
        this.getSpec(gamePlayer.getSpec()).updateStats(databaseGame, gamePlayer, multiplier, playersCollection);
    }

    @Override
    public DatabaseBaseCTF getSpec(Specializations specializations) {
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
    public DatabaseBaseCTF getClass(Classes classes) {
        return switch (classes) {
            case MAGE -> mage;
            case WARRIOR -> warrior;
            case PALADIN -> paladin;
            case SHAMAN -> shaman;
            case ROGUE -> rogue;
        };
    }

    @Override
    public DatabaseBaseCTF[] getClasses() {
        return new DatabaseBaseCTF[]{mage, warrior, paladin, shaman, rogue};
    }

    public DatabaseMageCTF getMage() {
        return mage;
    }

    public DatabaseWarriorCTF getWarrior() {
        return warrior;
    }

    public DatabasePaladinCTF getPaladin() {
        return paladin;
    }

    public DatabaseShamanCTF getShaman() {
        return shaman;
    }

    public DatabaseRogueCTF getRogue() {
        return rogue;
    }
}
