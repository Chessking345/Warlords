package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.narmer.narmerstomb.classes;

import com.ebicep.warlords.database.repositories.player.pojos.DatabaseWarlordsSpecs;
import com.ebicep.warlords.database.repositories.player.pojos.Stats;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.narmer.narmerstomb.DatabaseBasePvEEventNarmersTomb;

public class DatabasePaladinPvEEventNarmersTomb extends DatabaseBasePvEEventNarmersTomb implements DatabaseWarlordsSpecs {

    private DatabaseBasePvEEventNarmersTomb avenger = new DatabaseBasePvEEventNarmersTomb();
    private DatabaseBasePvEEventNarmersTomb crusader = new DatabaseBasePvEEventNarmersTomb();
    private DatabaseBasePvEEventNarmersTomb protector = new DatabaseBasePvEEventNarmersTomb();

    public DatabasePaladinPvEEventNarmersTomb() {
        super();
    }

    @Override
    public Stats[] getSpecs() {
        return new DatabaseBasePvEEventNarmersTomb[]{avenger, crusader, protector};
    }

    public DatabaseBasePvEEventNarmersTomb getAvenger() {
        return avenger;
    }

    public DatabaseBasePvEEventNarmersTomb getCrusader() {
        return crusader;
    }

    public DatabaseBasePvEEventNarmersTomb getProtector() {
        return protector;
    }

}
