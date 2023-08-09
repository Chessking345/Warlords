package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.illumina.theborderlineofillusion;

import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGameBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerResult;
import com.ebicep.warlords.database.repositories.games.pojos.pve.wavedefense.DatabaseGamePlayerPvEWaveDefense;
import com.ebicep.warlords.database.repositories.games.pojos.pve.wavedefense.DatabaseGamePvEWaveDefense;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.DatabaseWarlordsClasses;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.illumina.theborderlineofillusion.classes.*;
import com.ebicep.warlords.game.GameMode;
import com.ebicep.warlords.player.general.Classes;
import com.ebicep.warlords.player.general.Specializations;

public class DatabasePlayerPvEEventTheBorderLineOfIllusionPlayerCountStats extends PvEEventTheBorderLineOfIllusionDatabaseStatInformation implements DatabaseWarlordsClasses<PvEEventTheBorderLineOfIllusionDatabaseStatInformation> {

    private DatabaseMagePvEEventTheBorderLineOfIllusion mage = new DatabaseMagePvEEventTheBorderLineOfIllusion();
    private DatabaseWarriorPvEEventTheBorderLineOfIllusion warrior = new DatabaseWarriorPvEEventTheBorderLineOfIllusion();
    private DatabasePaladinPvEEventTheBorderLineOfIllusion paladin = new DatabasePaladinPvEEventTheBorderLineOfIllusion();
    private DatabaseShamanPvEEventTheBorderLineOfIllusion shaman = new DatabaseShamanPvEEventTheBorderLineOfIllusion();
    private DatabaseRoguePvEEventTheBorderLineOfIllusion rogue = new DatabaseRoguePvEEventTheBorderLineOfIllusion();
    private DatabaseArcanistPvEEventTheBorderLineOfIllusion arcanist = new DatabaseArcanistPvEEventTheBorderLineOfIllusion();

    public DatabasePlayerPvEEventTheBorderLineOfIllusionPlayerCountStats() {
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
        assert databaseGame instanceof DatabaseGamePvEWaveDefense;
        assert gamePlayer instanceof DatabaseGamePlayerPvEWaveDefense;

        super.updateCustomStats(databasePlayer, databaseGame, gameMode, gamePlayer, result, multiplier, playersCollection);

        //UPDATE UNIVERSAL EXPERIENCE
        this.experience += gamePlayer.getExperienceEarnedUniversal() * multiplier;
        this.experiencePvE += gamePlayer.getExperienceEarnedUniversal() * multiplier;

        //UPDATE CLASS, SPEC
        this.getClass(Specializations.getClass(gamePlayer.getSpec())).updateStats(databasePlayer, databaseGame, gamePlayer, multiplier, playersCollection);
        this.getSpec(gamePlayer.getSpec()).updateStats(databasePlayer, databaseGame, gamePlayer, multiplier, playersCollection);
    }

    @Override
    public DatabaseBasePvEEventTheBorderLineOfIllusion getSpec(Specializations specializations) {
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
            case LUMINARY -> arcanist.getLuminary();
        };
    }

    @Override
    public DatabaseBasePvEEventTheBorderLineOfIllusion getClass(Classes classes) {
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
    public DatabaseBasePvEEventTheBorderLineOfIllusion[] getClasses() {
        return new DatabaseBasePvEEventTheBorderLineOfIllusion[]{mage, warrior, paladin, shaman, rogue};
    }

    @Override
    public PvEEventTheBorderLineOfIllusionDatabaseStatInformation getMage() {
        return mage;
    }

    @Override
    public PvEEventTheBorderLineOfIllusionDatabaseStatInformation getWarrior() {
        return warrior;
    }

    @Override
    public PvEEventTheBorderLineOfIllusionDatabaseStatInformation getPaladin() {
        return paladin;
    }

    @Override
    public PvEEventTheBorderLineOfIllusionDatabaseStatInformation getShaman() {
        return shaman;
    }

    @Override
    public PvEEventTheBorderLineOfIllusionDatabaseStatInformation getRogue() {
        return rogue;
    }

    public void merge(DatabasePlayerPvEEventTheBorderLineOfIllusionPlayerCountStats other) {
        super.merge(other);
        mage.merge(other.mage);
        warrior.merge(other.warrior);
        paladin.merge(other.paladin);
        shaman.merge(other.shaman);
        rogue.merge(other.rogue);
        for (Classes value : Classes.VALUES) {
            this.getClass(value).merge(other.getClass(value));
        }
        for (Specializations value : Specializations.VALUES) {
            this.getSpec(value).merge(other.getSpec(value));
        }
    }

}
