package com.ebicep.warlords.database.repositories.player.pojos.general.classescomppub;

import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGameBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerBase;
import com.ebicep.warlords.database.repositories.games.pojos.DatabaseGamePlayerResult;
import com.ebicep.warlords.database.repositories.player.PlayersCollections;
import com.ebicep.warlords.database.repositories.player.pojos.AbstractDatabaseStatInformation;
import com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer;
import com.ebicep.warlords.game.GameMode;

public class DatabaseMage extends AbstractDatabaseStatInformation {

    protected DatabaseBaseSpec pyromancer = new DatabaseBaseSpec();
    protected DatabaseBaseSpec cryomancer = new DatabaseBaseSpec();
    protected DatabaseBaseSpec aquamancer = new DatabaseBaseSpec();

    public DatabaseMage() {
    }

    @Override
    public void updateCustomStats(
            DatabasePlayer databasePlayer, DatabaseGameBase databaseGame,
            GameMode gameMode,
            DatabaseGamePlayerBase gamePlayer,
            DatabaseGamePlayerResult result,
            int multiplier,
            PlayersCollections playersCollection
    ) {
        //UPDATE SPEC EXPERIENCE
        this.experience += gamePlayer.getExperienceEarnedSpec() * multiplier;
    }

    public DatabaseBaseSpec getPyromancer() {
        return pyromancer;
    }

    public void setPyromancer(DatabaseBaseSpec pyromancer) {
        this.pyromancer = pyromancer;
    }

    public DatabaseBaseSpec getCryomancer() {
        return cryomancer;
    }

    public void setCryomancer(DatabaseBaseSpec cryomancer) {
        this.cryomancer = cryomancer;
    }

    public DatabaseBaseSpec getAquamancer() {
        return aquamancer;
    }

    public void setAquamancer(DatabaseBaseSpec aquamancer) {
        this.aquamancer = aquamancer;
    }
}
