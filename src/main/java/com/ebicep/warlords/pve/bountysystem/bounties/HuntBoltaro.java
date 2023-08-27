package com.ebicep.warlords.pve.bountysystem.bounties;

import com.ebicep.warlords.events.player.ingame.WarlordsDeathEvent;
import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.player.ingame.WarlordsNPC;
import com.ebicep.warlords.pve.DifficultyIndex;
import com.ebicep.warlords.pve.bountysystem.AbstractBounty;
import com.ebicep.warlords.pve.bountysystem.Bounty;
import com.ebicep.warlords.pve.bountysystem.BountyUtils;
import com.ebicep.warlords.pve.bountysystem.rewards.DailyRewardSpendable2;
import com.ebicep.warlords.pve.bountysystem.trackers.TracksDuringGame;
import com.ebicep.warlords.pve.mobs.bosses.Boltaro;
import org.springframework.data.annotation.Transient;

import java.util.UUID;

public class HuntBoltaro extends AbstractBounty implements TracksDuringGame, DailyRewardSpendable2 {

    @Transient
    private int newKills = 0;

    @Override
    public int getTarget() {
        return 1;
    }

    @Override
    public String getName() {
        return "Hunt-Boltaro";
    }

    @Override
    public String getDescription() {
        return "Defeat Boltaro in Normal Mode.";
    }

    @Override
    public Bounty getBounty() {
        return Bounty.HUNT_BOLTARO;
    }

    @Override
    public void onKill(UUID uuid, WarlordsDeathEvent event) {
        if (event.getWarlordsEntity() instanceof WarlordsNPC warlordsNPC && warlordsNPC.getMob() instanceof Boltaro) {
            newKills++;
        }
    }

    @Override
    public boolean trackGame(Game game) {
        return BountyUtils.waveDefenseMatchesDifficulty(game, DifficultyIndex.NORMAL);
    }

    @Override
    public long getNewValue() {
        return newKills;
    }

    @Override
    public void reset() {
        newKills = 0;
    }
}
