package com.ebicep.warlords.pve.mobs.events.pharaohsrevenge;

import com.ebicep.warlords.abilities.CripplingStrike;
import com.ebicep.warlords.abilities.FlameBurst;
import com.ebicep.warlords.abilities.SoulShackle;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.option.pve.PveOption;
import com.ebicep.warlords.player.general.ArmorManager;
import com.ebicep.warlords.player.general.Weapons;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsPlayer;
import com.ebicep.warlords.pve.mobs.abilities.AbstractPveAbility;
import com.ebicep.warlords.pve.mobs.tiers.BossMinionMob;
import com.ebicep.warlords.pve.mobs.zombie.AbstractZombie;
import com.ebicep.warlords.util.pve.SkullID;
import com.ebicep.warlords.util.pve.SkullUtils;
import com.ebicep.warlords.util.warlords.PlayerFilterGeneric;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.Location;
import org.bukkit.Material;

import javax.annotation.Nonnull;

public class EventDjet extends AbstractZombie implements BossMinionMob {

    private boolean wentBelowHealthThreshold = false;

    public EventDjet(Location spawnLocation) {
        super(spawnLocation,
                "Djet",
                new Utils.SimpleEntityEquipment(
                        SkullUtils.getSkullFrom(SkullID.ETHEREAL_WITHER_SKULL),
                        Utils.applyColorTo(Material.LEATHER_CHESTPLATE, 255, 160, 160),
                        ArmorManager.ArmorSets.GREATER_LEGGINGS.itemRed,
                        Utils.applyColorTo(Material.LEATHER_BOOTS, 255, 160, 160),
                        Weapons.WALKING_STICK.getItem()
                ),
                9000,
                0.32f,
                10,
                930,
                1210,
                new FlameBurst(1200, 1380),
                new SilenceCrippleAll()
        );
    }

    public EventDjet(
            Location spawnLocation,
            String name,
            int maxHealth,
            float walkSpeed,
            int damageResistance,
            float minMeleeDamage,
            float maxMeleeDamage
    ) {
        super(spawnLocation,
                name,
                new Utils.SimpleEntityEquipment(
                        SkullUtils.getSkullFrom(SkullID.ETHEREAL_WITHER_SKULL),
                        Utils.applyColorTo(Material.LEATHER_CHESTPLATE, 255, 160, 160),
                        ArmorManager.ArmorSets.GREATER_LEGGINGS.itemRed,
                        Utils.applyColorTo(Material.LEATHER_BOOTS, 255, 160, 160),
                        Weapons.WALKING_STICK.getItem()
                ),
                maxHealth,
                walkSpeed,
                damageResistance,
                minMeleeDamage,
                maxMeleeDamage,
                new FlameBurst(1200, 1380),
                new SilenceCrippleAll()
        );
    }

    @Override
    public void onSpawn(PveOption option) {
        super.onSpawn(option);
        int currentWave = option.getWaveCounter();
        if (currentWave % 5 == 0 && currentWave > 5) {
            float additionalHealthMultiplier = 1 + .15f * (currentWave / 5f - 1);
            warlordsNPC.setMaxBaseHealth(warlordsNPC.getMaxBaseHealth() * additionalHealthMultiplier);
            warlordsNPC.heal();
        }
    }

    @Override
    public void whileAlive(int ticksElapsed, PveOption option) {
        if (!aboveHealthThreshold() && !wentBelowHealthThreshold) {
            wentBelowHealthThreshold = true;
            playerClass.getAbilities().get(0).setCooldown(Float.MAX_VALUE);
        }
    }

    @Override
    public void onAttack(WarlordsEntity attacker, WarlordsEntity receiver, WarlordsDamageHealingEvent event) {

    }

    @Override
    public void onDamageTaken(WarlordsEntity self, WarlordsEntity attacker, WarlordsDamageHealingEvent event) {
        if (aboveHealthThreshold()) {
            warlordsNPC.setDamageResistance(10);
        } else {
            warlordsNPC.setDamageResistance(30);
        }
    }

    private boolean aboveHealthThreshold() {
        return warlordsNPC.getHealth() > warlordsNPC.getMaxBaseHealth() * .75;
    }

    private static class SilenceCrippleAll extends AbstractPveAbility {

        public SilenceCrippleAll() {
            super("Djet", 5, 50);
        }

        @Override
        public boolean onPveActivate(@Nonnull WarlordsEntity wp, PveOption pveOption) {
            for (WarlordsPlayer warlordsPlayer : PlayerFilterGeneric
                    .playingGameWarlordsPlayers(wp.getGame())
                    .aliveEnemiesOf(wp)
            ) {
                SoulShackle.shacklePlayer(warlordsPlayer, warlordsPlayer, 60);
                CripplingStrike.cripple(wp, warlordsPlayer, name, 3 * 20);
            }
            return true;
        }

    }
}
