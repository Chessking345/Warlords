package com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.narmer.narmerstomb;


import com.ebicep.warlords.database.repositories.games.pojos.pve.events.narmer.narmerstomb.DatabaseGamePlayerPvEEventNarmersTomb;
import com.ebicep.warlords.database.repositories.games.pojos.pve.events.narmer.narmerstomb.DatabaseGamePvEEventNarmersTomb;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.narmer.PvEEventNarmerStats;

public interface PvEEventNarmerNarmersTombStats extends PvEEventNarmerStats<
        DatabaseGamePvEEventNarmersTomb,
        DatabaseGamePlayerPvEEventNarmersTomb> {

    int getHighestWaveCleared();

    int getTotalWavesCleared();

}
