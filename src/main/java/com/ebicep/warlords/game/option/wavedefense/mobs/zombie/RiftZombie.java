package com.ebicep.warlords.game.option.wavedefense.mobs.zombie;

import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.effects.FireWorkEffectPlayer;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.option.wavedefense.WaveDefenseOption;
import com.ebicep.warlords.game.option.wavedefense.mobs.MobTier;
import com.ebicep.warlords.game.option.wavedefense.mobs.mobtypes.EliteMob;
import com.ebicep.warlords.player.general.Weapons;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.util.pve.SkullID;
import com.ebicep.warlords.util.pve.SkullUtils;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.*;

public class RiftZombie extends AbstractZombie implements EliteMob {

    public RiftZombie(Location spawnLocation) {
        super(
                spawnLocation,
                "Exiled Rift Walker",
                MobTier.ELITE,
                new Utils.SimpleEntityEquipment(
                        SkullUtils.getSkullFrom(SkullID.PURPLE_RIFT),
                        Utils.applyColorTo(Material.LEATHER_CHESTPLATE, 229, 69, 176),
                        Utils.applyColorTo(Material.LEATHER_LEGGINGS, 229, 69, 176),
                        Utils.applyColorTo(Material.LEATHER_BOOTS, 229, 69, 176),
                        Weapons.VORPAL_SWORD.getItem()
                ),
                10000,
                0.33f,
                10,
                800,
                1000
        );
    }

    @Override
    public void onSpawn(WaveDefenseOption option) {
        EffectUtils.strikeLightning(warlordsNPC.getLocation(), true);
    }

    @Override
    public void whileAlive(int ticksElapsed, WaveDefenseOption option) {
        if (ticksElapsed % 40 == 0) {
            Utils.playGlobalSound(warlordsNPC.getLocation(), Sound.CREEPER_HISS, 2, 0.2f);
        }

        if (ticksElapsed % 200 == 0) {
            Utils.playGlobalSound(warlordsNPC.getLocation(), Sound.ENDERDRAGON_GROWL, 2, 0.2f);
            warlordsNPC.getSpeed().addSpeedModifier("Rift Speed", 100, 2 * 20);
        }
    }

    @Override
    public void onAttack(WarlordsEntity attacker, WarlordsEntity receiver, WarlordsDamageHealingEvent event) {
        Utils.addKnockback(attacker.getLocation(), receiver, 1, 0.15);
    }

    @Override
    public void onDamageTaken(WarlordsEntity self, WarlordsEntity attacker, WarlordsDamageHealingEvent event) {
        self.getSpeed().addSpeedModifier("Rift Speed On Damage", 40, 5);
    }

    @Override
    public void onDeath(WarlordsEntity killer, Location deathLocation, WaveDefenseOption option) {
        super.onDeath(killer, deathLocation, option);
        FireWorkEffectPlayer.playFirework(deathLocation, FireworkEffect.builder()
                .withColor(Color.PURPLE)
                .with(FireworkEffect.Type.BURST)
                .withTrail()
                .build());
        Utils.playGlobalSound(deathLocation, Sound.ZOMBIE_DEATH, 2, 0.4f);
    }
}