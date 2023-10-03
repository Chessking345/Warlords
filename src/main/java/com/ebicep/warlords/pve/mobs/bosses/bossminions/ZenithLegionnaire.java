package com.ebicep.warlords.pve.mobs.bosses.bossminions;

import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.effects.FireWorkEffectPlayer;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.option.pve.PveOption;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsNPC;
import com.ebicep.warlords.pve.mobs.Mob;
import com.ebicep.warlords.pve.mobs.abilities.AbstractPveAbility;
import com.ebicep.warlords.pve.mobs.tiers.BossMinionMob;
import com.ebicep.warlords.pve.mobs.zombie.AbstractZombie;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.*;

import javax.annotation.Nonnull;

public class ZenithLegionnaire extends AbstractZombie implements BossMinionMob {

    public ZenithLegionnaire(Location spawnLocation) {
        super(spawnLocation,
                "Zenith Legionnaire",
                4400,
                0.32f,
                10,
                1000,
                1500,
                new Remedy()
        );
    }

    public ZenithLegionnaire(
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
                maxHealth,
                walkSpeed,
                damageResistance,
                minMeleeDamage,
                maxMeleeDamage,
                new Remedy()
        );
    }

    @Override
    public Mob getMobRegistry() {
        return Mob.ZENITH_LEGIONNAIRE;
    }

    @Override
    public void onSpawn(PveOption option) {
        super.onSpawn(option);
        EffectUtils.strikeLightning(warlordsNPC.getLocation(), false);
    }

    @Override
    public void whileAlive(int ticksElapsed, PveOption option) {

    }

    @Override
    public void onAttack(WarlordsEntity attacker, WarlordsEntity receiver, WarlordsDamageHealingEvent event) {
        Utils.addKnockback(name, attacker.getLocation(), receiver, -1.1, 0.3);
        Utils.playGlobalSound(attacker.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2, 0.2f);
    }

    @Override
    public void onDamageTaken(WarlordsEntity self, WarlordsEntity attacker, WarlordsDamageHealingEvent event) {
    }

    @Override
    public void onDeath(WarlordsEntity killer, Location deathLocation, PveOption option) {
        super.onDeath(killer, deathLocation, option);
        FireWorkEffectPlayer.playFirework(deathLocation, FireworkEffect.builder()
                                                                       .withColor(Color.ORANGE)
                                                                       .with(FireworkEffect.Type.BALL)
                                                                       .withTrail()
                                                                       .build());
    }

    private static class Remedy extends AbstractPveAbility {

        public Remedy() {
            super(
                    "Remedy",
                    500,
                    500,
                    10,
                    100
            );
        }

        @Override
        public boolean onPveActivate(@Nonnull WarlordsEntity wp, PveOption pveOption) {
            wp.subtractEnergy(name, energyCost, false);

            PlayerFilter.playingGame(wp.getGame())
                        .filter(we -> we.getName().equals("Zenith"))
                        .forEach(zenith -> {
                            zenith.addHealingInstance(
                                    wp,
                                    name,
                                    minDamageHeal,
                                    maxDamageHeal,
                                    critChance,
                                    critMultiplier
                            );

                            Utils.playGlobalSound(zenith.getLocation(), "shaman.earthlivingweapon.impact", 3, 1.5f);
                            EffectUtils.playParticleLinkAnimation(zenith.getLocation(), wp.getLocation(), Particle.VILLAGER_HAPPY);
                        });
            if (wp instanceof WarlordsNPC warlordsNPC) {
                warlordsNPC.getMob().removeTarget();
            }

            return true;
        }
    }
}