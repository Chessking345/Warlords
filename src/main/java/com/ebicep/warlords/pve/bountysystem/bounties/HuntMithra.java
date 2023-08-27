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
import com.ebicep.warlords.pve.mobs.bosses.Mithra;
import org.springframework.data.annotation.Transient;

import java.util.UUID;

public class HuntMithra extends AbstractBounty implements TracksDuringGame, DailyRewardSpendable2 {

    @Transient
    private int newKills = 0;

    @Override
    public int getTarget() {
        return 1;
    }

    @Override
    public String getName() {
        return "Hunt-Mithra";
    }

    @Override
    public String getDescription() {
        return "Defeat Mithra in Normal Mode.";
    }

    @Override
    public Bounty getBounty() {
        return Bounty.HUNT_MITHRA;
    }

    @Override
    public void onKill(UUID uuid, WarlordsDeathEvent event) {
        if (event.getWarlordsEntity() instanceof WarlordsNPC warlordsNPC && warlordsNPC.getMob() instanceof Mithra) {
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
