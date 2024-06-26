package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.gardenofhesperides.theacropolis.classes;

import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.gardenofhesperides.theacropolis.PvEEventGardenOfHesperidesTheAcropolisStatsWarlordsSpecs;

public class DatabaseShamanPvEEventGardenOfHesperidesAcropolis implements PvEEventGardenOfHesperidesTheAcropolisStatsWarlordsSpecs {

    private DatabaseBasePvEEventGardenOfHesperidesAcropolis thunderlord = new DatabaseBasePvEEventGardenOfHesperidesAcropolis();
    private DatabaseBasePvEEventGardenOfHesperidesAcropolis spiritguard = new DatabaseBasePvEEventGardenOfHesperidesAcropolis();
    private DatabaseBasePvEEventGardenOfHesperidesAcropolis earthwarden = new DatabaseBasePvEEventGardenOfHesperidesAcropolis();

    public DatabaseShamanPvEEventGardenOfHesperidesAcropolis() {
        super();
    }

    @Override
    public DatabaseBasePvEEventGardenOfHesperidesAcropolis[] getSpecs() {
        return new DatabaseBasePvEEventGardenOfHesperidesAcropolis[]{thunderlord, spiritguard, earthwarden};
    }

    public DatabaseBasePvEEventGardenOfHesperidesAcropolis getThunderlord() {
        return thunderlord;
    }

    public DatabaseBasePvEEventGardenOfHesperidesAcropolis getSpiritguard() {
        return spiritguard;
    }

    public DatabaseBasePvEEventGardenOfHesperidesAcropolis getEarthwarden() {
        return earthwarden;
    }

}
