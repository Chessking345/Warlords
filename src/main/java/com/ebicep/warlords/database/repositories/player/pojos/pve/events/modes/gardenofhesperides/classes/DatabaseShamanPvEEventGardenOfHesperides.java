package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.gardenofhesperides.classes;

import com.ebicep.warlords.database.repositories.player.pojos.AbstractDatabaseStatInformation;
import com.ebicep.warlords.database.repositories.player.pojos.DatabaseWarlordsSpecs;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.gardenofhesperides.DatabaseBasePvEEventGardenOfHesperides;

public class DatabaseShamanPvEEventGardenOfHesperides extends DatabaseBasePvEEventGardenOfHesperides implements DatabaseWarlordsSpecs {

    private DatabaseBasePvEEventGardenOfHesperides thunderlord = new DatabaseBasePvEEventGardenOfHesperides();
    private DatabaseBasePvEEventGardenOfHesperides spiritguard = new DatabaseBasePvEEventGardenOfHesperides();
    private DatabaseBasePvEEventGardenOfHesperides earthwarden = new DatabaseBasePvEEventGardenOfHesperides();

    public DatabaseShamanPvEEventGardenOfHesperides() {
        super();
    }

    @Override
    public AbstractDatabaseStatInformation[] getSpecs() {
        return new DatabaseBasePvEEventGardenOfHesperides[]{thunderlord, spiritguard, earthwarden};
    }

    public DatabaseBasePvEEventGardenOfHesperides getThunderlord() {
        return thunderlord;
    }

    public DatabaseBasePvEEventGardenOfHesperides getSpiritguard() {
        return spiritguard;
    }

    public DatabaseBasePvEEventGardenOfHesperides getEarthwarden() {
        return earthwarden;
    }

}
