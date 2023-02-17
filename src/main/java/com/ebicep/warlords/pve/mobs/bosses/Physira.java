package com.ebicep.warlords.pve.mobs.bosses;

import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.option.PveOption;
import com.ebicep.warlords.player.general.Weapons;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.pve.mobs.MobTier;
import com.ebicep.warlords.pve.mobs.mobtypes.BossMob;
import com.ebicep.warlords.pve.mobs.zombie.AbstractZombie;
import com.ebicep.warlords.util.pve.SkullID;
import com.ebicep.warlords.util.pve.SkullUtils;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.Location;
import org.bukkit.Material;

public class Physira extends AbstractZombie implements BossMob {

    public Physira(Location spawnLocation) {
        super(spawnLocation,
                "Physira",
                MobTier.BOSS,
                new Utils.SimpleEntityEquipment(
                        SkullUtils.getSkullFrom(SkullID.DEMON_KING),
                        Utils.applyColorTo(Material.LEATHER_CHESTPLATE, 120, 60, 60),
                        Utils.applyColorTo(Material.LEATHER_CHESTPLATE, 120, 60, 60),
                        Utils.applyColorTo(Material.LEATHER_CHESTPLATE, 120, 60, 60),
                        Weapons.SILVER_PHANTASM_STAFF_2.getItem()
                ),
                32000,
                0.18f,
                20,
                2000,
                2600
        );
    }

    @Override
    public void onSpawn(PveOption option) {

    }

    @Override
    public void whileAlive(int ticksElapsed, PveOption option) {

    }

    @Override
    public void onAttack(WarlordsEntity attacker, WarlordsEntity receiver, WarlordsDamageHealingEvent event) {

    }

    @Override
    public void onDamageTaken(WarlordsEntity self, WarlordsEntity attacker, WarlordsDamageHealingEvent event) {

    }

}