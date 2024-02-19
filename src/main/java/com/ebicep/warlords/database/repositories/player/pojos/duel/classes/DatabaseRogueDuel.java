package com.ebicep.warlords.database.repositories.player.pojos.duel.classes;

import com.ebicep.warlords.database.repositories.player.pojos.StatsWarlordsSpecs;
import com.ebicep.warlords.database.repositories.player.pojos.duel.DatabaseBaseDuel;

public class DatabaseRogueDuel implements StatsWarlordsSpecs<DatabaseBaseDuel> {

    private DatabaseBaseDuel assassin = new DatabaseBaseDuel();
    private DatabaseBaseDuel vindicator = new DatabaseBaseDuel();
    private DatabaseBaseDuel apothecary = new DatabaseBaseDuel();

    public DatabaseRogueDuel() {
        super();
    }

    @Override
    public DatabaseBaseDuel[] getSpecs() {
        return new DatabaseBaseDuel[]{assassin, vindicator, apothecary};
    }


    public DatabaseBaseDuel getAssassin() {
        return assassin;
    }

    public DatabaseBaseDuel getVindicator() {
        return vindicator;
    }

    public DatabaseBaseDuel getApothecary() {
        return apothecary;
    }
}
