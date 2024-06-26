package com.ebicep.warlords.pve.mobs.zombie;

import com.ebicep.warlords.abilities.internal.AbstractAbility;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.effects.circle.CircleEffect;
import com.ebicep.warlords.effects.circle.CircumferenceEffect;
import com.ebicep.warlords.effects.circle.DoubleLineEffect;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.option.pve.PveOption;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.pve.mobs.AbstractMob;
import com.ebicep.warlords.pve.mobs.Mob;
import com.ebicep.warlords.pve.mobs.abilities.AdvancedVoidShred;
import com.ebicep.warlords.pve.mobs.tiers.AdvancedMob;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.List;

public class VoidZombie extends AbstractMob implements AdvancedMob {

    private static final int voidRadius = 4;

    public VoidZombie(Location spawnLocation) {
        super(
                spawnLocation,
                "Zombie Singularity",
                11000,
                0.1f,
                0,
                1500,
                2000,
                new VoidShred(),
                new AdvancedVoidShred(200, 300, .5f, -70, voidRadius, 20)
        );
    }

    public VoidZombie(
            Location spawnLocation,
            String name,
            int maxHealth,
            float walkSpeed,
            int damageResistance,
            float minMeleeDamage,
            float maxMeleeDamage
    ) {
        super(
                spawnLocation,
                name,
                maxHealth,
                walkSpeed,
                damageResistance,
                minMeleeDamage,
                maxMeleeDamage,
                new VoidShred(),
                new AdvancedVoidShred(200, 300, .5f, -70, voidRadius, 20)
        );
    }

    @Override
    public Mob getMobRegistry() {
        return Mob.VOID_ZOMBIE;
    }

    @Override
    public void onSpawn(PveOption option) {
        super.onSpawn(option);
        EffectUtils.strikeLightning(warlordsNPC.getLocation(), false, 2);
    }

    @Override
    public void whileAlive(int ticksElapsed, PveOption option) {
        if (ticksElapsed % 8 == 0) {
            new CircleEffect(
                    warlordsNPC.getGame(),
                    warlordsNPC.getTeam(),
                    warlordsNPC.getLocation(),
                    voidRadius,
                    new CircumferenceEffect(Particle.FIREWORKS_SPARK, Particle.FIREWORKS_SPARK).particlesPerCircumference(0.6),
                    new DoubleLineEffect(Particle.SPELL)
            ).playEffects();
        }
    }

    @Override
    public void onAttack(WarlordsEntity attacker, WarlordsEntity receiver, WarlordsDamageHealingEvent event) {

    }

    @Override
    public void onDamageTaken(WarlordsEntity self, WarlordsEntity attacker, WarlordsDamageHealingEvent event) {

    }

    @Override
    public void onDeath(WarlordsEntity killer, Location deathLocation, PveOption option) {
        super.onDeath(killer, deathLocation, option);
        EffectUtils.playFirework(
                deathLocation,
                FireworkEffect.builder()
                              .withColor(Color.WHITE)
                              .with(FireworkEffect.Type.BURST)
                              .withTrail()
                              .build(),
                1
        );
        Utils.playGlobalSound(deathLocation, Sound.ENTITY_ZOMBIE_DEATH, 2, 0.4f);
    }

    private static class VoidShred extends AbstractAbility {

        public VoidShred() {
            super("Void Shred", 2, 100);
        }

        @Override
        public void updateDescription(Player player) {

        }

        @Override
        public List<Pair<String, String>> getAbilityInfo() {
            return null;
        }

        @Override
        public boolean onActivate(@Nonnull WarlordsEntity wp) {


            float healthDamage = wp.getMaxHealth() * 0.01f;
            wp.addDamageInstance(wp, "Void Shred", healthDamage, healthDamage, critChance, critMultiplier);
            return true;
        }
    }
}
