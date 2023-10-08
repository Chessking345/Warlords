package com.ebicep.warlords.database.repositories.games.pojos.pve.events.boltaro.boltaroslair;

import com.ebicep.warlords.database.repositories.games.pojos.pve.events.DatabaseGamePlayerPvEEvent;
import com.ebicep.warlords.events.game.WarlordsGameTriggerWinEvent;
import com.ebicep.warlords.game.option.pve.wavedefense.WaveDefenseOption;
import com.ebicep.warlords.game.option.pve.wavedefense.events.EventPointsOption;
import com.ebicep.warlords.player.ingame.WarlordsPlayer;

public class DatabaseGamePlayerPvEEventBoltarosLair extends DatabaseGamePlayerPvEEvent {

    public DatabaseGamePlayerPvEEventBoltarosLair() {
    }

    public DatabaseGamePlayerPvEEventBoltarosLair(
            WarlordsPlayer warlordsPlayer,
            WarlordsGameTriggerWinEvent gameWinEvent, WaveDefenseOption waveDefenseOption,
            EventPointsOption eventPointsOption
    ) {
        super(warlordsPlayer, gameWinEvent, waveDefenseOption, eventPointsOption);
    }
}
