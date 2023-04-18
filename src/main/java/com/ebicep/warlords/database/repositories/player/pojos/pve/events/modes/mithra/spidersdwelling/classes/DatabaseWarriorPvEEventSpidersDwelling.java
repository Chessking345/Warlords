package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.mithra.spidersdwelling.classes;


import com.ebicep.warlords.database.repositories.player.pojos.AbstractDatabaseStatInformation;
import com.ebicep.warlords.database.repositories.player.pojos.DatabaseWarlordsSpecs;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.mithra.spidersdwelling.DatabaseBasePvEEventSpidersDwelling;

public class DatabaseWarriorPvEEventSpidersDwelling extends DatabaseBasePvEEventSpidersDwelling implements DatabaseWarlordsSpecs {

    private DatabaseBasePvEEventSpidersDwelling berserker = new DatabaseBasePvEEventSpidersDwelling();
    private DatabaseBasePvEEventSpidersDwelling defender = new DatabaseBasePvEEventSpidersDwelling();
    private DatabaseBasePvEEventSpidersDwelling revenant = new DatabaseBasePvEEventSpidersDwelling();

    public DatabaseWarriorPvEEventSpidersDwelling() {
        super();
    }

    @Override
    public AbstractDatabaseStatInformation[] getSpecs() {
        return new DatabaseBasePvEEventSpidersDwelling[]{berserker, defender, revenant};
    }


    public DatabaseBasePvEEventSpidersDwelling getBerserker() {
        return berserker;
    }

    public DatabaseBasePvEEventSpidersDwelling getDefender() {
        return defender;
    }

    public DatabaseBasePvEEventSpidersDwelling getRevenant() {
        return revenant;
    }

}
