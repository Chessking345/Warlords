package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.libraryarchives.grimoiresgraveyard.classes;

import com.ebicep.warlords.database.repositories.player.pojos.DatabaseWarlordsSpecs;
import com.ebicep.warlords.database.repositories.player.pojos.Stats;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.libraryarchives.grimoiresgraveyard.DatabaseBasePvEEventLibraryArchivesGrimoiresGraveyard;

public class DatabaseRoguePvEEventLibraryArchivesGrimoiresGraveyard extends DatabaseBasePvEEventLibraryArchivesGrimoiresGraveyard implements DatabaseWarlordsSpecs {

    private DatabaseBasePvEEventLibraryArchivesGrimoiresGraveyard assassin = new DatabaseBasePvEEventLibraryArchivesGrimoiresGraveyard();
    private DatabaseBasePvEEventLibraryArchivesGrimoiresGraveyard vindicator = new DatabaseBasePvEEventLibraryArchivesGrimoiresGraveyard();
    private DatabaseBasePvEEventLibraryArchivesGrimoiresGraveyard apothecary = new DatabaseBasePvEEventLibraryArchivesGrimoiresGraveyard();

    public DatabaseRoguePvEEventLibraryArchivesGrimoiresGraveyard() {
        super();
    }

    @Override
    public Stats[] getSpecs() {
        return new DatabaseBasePvEEventLibraryArchivesGrimoiresGraveyard[]{assassin, vindicator, apothecary};
    }


    public DatabaseBasePvEEventLibraryArchivesGrimoiresGraveyard getAssassin() {
        return assassin;
    }

    public DatabaseBasePvEEventLibraryArchivesGrimoiresGraveyard getVindicator() {
        return vindicator;
    }

    public DatabaseBasePvEEventLibraryArchivesGrimoiresGraveyard getApothecary() {
        return apothecary;
    }
}
