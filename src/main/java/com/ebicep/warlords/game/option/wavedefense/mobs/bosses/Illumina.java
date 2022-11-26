package com.ebicep.warlords.game.option.wavedefense.mobs.bosses;

import com.ebicep.warlords.abilties.internal.DamageCheck;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.effects.FallingBlockWaveEffect;
import com.ebicep.warlords.effects.FireWorkEffectPlayer;
import com.ebicep.warlords.effects.ParticleEffect;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.option.wavedefense.WaveDefenseOption;
import com.ebicep.warlords.game.option.wavedefense.mobs.MobTier;
import com.ebicep.warlords.game.option.wavedefense.mobs.irongolem.IronGolem;
import com.ebicep.warlords.game.option.wavedefense.mobs.mobtypes.BossMob;
import com.ebicep.warlords.game.option.wavedefense.mobs.zombie.AbstractZombie;
import com.ebicep.warlords.player.general.Weapons;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.PermanentCooldown;
import com.ebicep.warlords.util.bukkit.PacketUtils;
import com.ebicep.warlords.util.pve.SkullID;
import com.ebicep.warlords.util.pve.SkullUtils;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

public class Illumina extends AbstractZombie implements BossMob {

    private boolean phaseOneTriggered = false;
    private boolean phaseTwoTriggered = false;
    private boolean phaseThreeTriggered = false;
    private boolean phaseFourTriggered = false;

    private AtomicInteger damageToDeal = new AtomicInteger(0);

    public Illumina(Location spawnLocation) {
        super(spawnLocation,
                "Illumina",
                MobTier.BOSS,
                new Utils.SimpleEntityEquipment(
                        SkullUtils.getSkullFrom(SkullID.DEEP_DARK_WORM),
                        Utils.applyColorTo(Material.LEATHER_CHESTPLATE, 120, 120, 200),
                        Utils.applyColorTo(Material.LEATHER_LEGGINGS, 120, 120, 200),
                        Utils.applyColorTo(Material.LEATHER_BOOTS, 120, 120, 200),
                        Weapons.SILVER_PHANTASM_SWORD_3.getItem()
                ),
                40000,
                0.15f,
                20,
                2000,
                3000
        );
    }

    @Override
    public void onSpawn(WaveDefenseOption option) {
        for (WarlordsEntity we : PlayerFilter.playingGame(getWarlordsNPC().getGame())) {
            if (we.getEntity() instanceof Player) {
                PacketUtils.sendTitle(
                        (Player) we.getEntity(),
                        ChatColor.BLUE + "Illumina",
                        ChatColor.DARK_GRAY + "General of the Illusion Legion",
                        20, 30, 20
                );
            }
        }

        for (int i = 0; i < (2 * option.getGame().warlordsPlayers().count()); i++) {
            option.spawnNewMob(new IronGolem(spawnLocation));
        }
    }

    @Override
    public void whileAlive(int ticksElapsed, WaveDefenseOption option) {
        long playerCount = option.getGame().warlordsPlayers().count();

        if (ticksElapsed % 100 == 0) {
            new FallingBlockWaveEffect(warlordsNPC.getLocation().add(0, 1, 0), 7, 1.2, Material.LEAVES, (byte) 0).play();
            for (WarlordsEntity we : PlayerFilter
                    .entitiesAround(warlordsNPC, 10, 10, 10)
                    .aliveEnemiesOf(warlordsNPC)
            ) {
                we.getSpeed().addSpeedModifier(warlordsNPC, "Bramble Slowness", -99, 30);
                we.addDamageInstance(
                        warlordsNPC,
                        "Bramble",
                        700,
                        1000,
                        -1,
                        100,
                        true
                );
            }
        }

        if (warlordsNPC.getHealth() < (warlordsNPC.getMaxHealth() * .9f) && !phaseOneTriggered) {
            phaseOneTriggered = true;
            timedDamage(playerCount, 8000, 11);
        }

        if (warlordsNPC.getHealth() < (warlordsNPC.getMaxHealth() * .6f) && !phaseTwoTriggered) {
            phaseTwoTriggered = true;
            timedDamage(playerCount, 10000, 11);
        }

        if (warlordsNPC.getHealth() < (warlordsNPC.getMaxHealth() * .3f) && !phaseThreeTriggered) {
            phaseThreeTriggered = true;
            timedDamage(playerCount, 12000, 11);
        }

        if (warlordsNPC.getHealth() < (warlordsNPC.getMaxHealth() * .05f) && !phaseFourTriggered) {
            phaseFourTriggered = true;
            timedDamage(playerCount, (int) warlordsNPC.getHealth(), 11);
            for (int i = 0; i < (2 * option.getGame().warlordsPlayers().count()); i++) {
                option.spawnNewMob(new IronGolem(spawnLocation));
            }
        }
    }

    @Override
    public void onAttack(WarlordsEntity attacker, WarlordsEntity receiver, WarlordsDamageHealingEvent event) {

    }

    @Override
    public void onDamageTaken(WarlordsEntity self, WarlordsEntity attacker, WarlordsDamageHealingEvent event) {

    }

    @Override
    public void onDeath(WarlordsEntity killer, Location deathLocation, WaveDefenseOption option) {
        FireWorkEffectPlayer.playFirework(deathLocation, FireworkEffect.builder()
                .withColor(Color.BLACK)
                .withColor(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .build());
        EffectUtils.strikeLightning(deathLocation, false, 2);
    }

    private void timedDamage(long playerCount, int damageValue, int timeToDealDamage) {
        damageToDeal.set((int) (damageValue * playerCount));

        for (WarlordsEntity we : PlayerFilter
                .playingGame(getWarlordsNPC().getGame())
                .aliveEnemiesOf(warlordsNPC)
        ) {
            if (we.getEntity() instanceof Player) {
                PacketUtils.sendTitle(
                        (Player) we.getEntity(),
                        "",
                        ChatColor.RED + "Keep attacking Illumina to stop the draining!",
                        10, 35, 0
                );
            }
            Utils.addKnockback(warlordsNPC.getLocation(), we, -2.5, 0.4);
            Utils.playGlobalSound(warlordsNPC.getLocation(), Sound.WITHER_SPAWN, 500, 0.5f);
        }

        warlordsNPC.getCooldownManager().removeCooldown(DamageCheck.class);
        warlordsNPC.getCooldownManager().addCooldown(new PermanentCooldown<>(
                "Damage Check",
                null,
                DamageCheck.class,
                DamageCheck.DAMAGE_CHECK,
                warlordsNPC,
                CooldownTypes.ABILITY,
                cooldownManager -> {
                },
                true
        ) {
            @Override
            public float modifyDamageAfterInterveneFromSelf(WarlordsDamageHealingEvent event, float currentDamageValue) {
                damageToDeal.set((int) (damageToDeal.get() - currentDamageValue));
                return currentDamageValue;
            }
        });

        AtomicInteger countdown = new AtomicInteger(timeToDealDamage);
        new GameRunnable(warlordsNPC.getGame()) {
            int counter = 0;
            @Override
            public void run() {
                if (warlordsNPC.isDead() || damageToDeal.get() <= 0) {
                    FireWorkEffectPlayer.playFirework(warlordsNPC.getLocation(), FireworkEffect.builder()
                            .withColor(Color.WHITE)
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .build());
                    this.cancel();
                    return;
                }

                if (counter++ % 20 == 0) {
                    countdown.getAndDecrement();
                    Utils.playGlobalSound(warlordsNPC.getLocation(), Sound.NOTE_STICKS, 500, 0.4f);
                    Utils.playGlobalSound(warlordsNPC.getLocation(), Sound.NOTE_STICKS, 500, 0.4f);
                    for (WarlordsEntity we : PlayerFilter
                            .entitiesAround(warlordsNPC, 100, 100, 100)
                            .aliveEnemiesOf(warlordsNPC)
                    ) {
                        EffectUtils.playParticleLinkAnimation(we.getLocation(), warlordsNPC.getLocation(), 255, 255, 255, 2);
                        we.addDamageInstance(
                                warlordsNPC,
                                "Vampiric Leash",
                                300,
                                300,
                                -1,
                                100,
                                true
                        );
                    }
                }

                if (countdown.get() <= 0 && damageToDeal.get() > 0) {
                    FireWorkEffectPlayer.playFirework(warlordsNPC.getLocation(), FireworkEffect.builder()
                            .withColor(Color.WHITE)
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .build());
                    EffectUtils.strikeLightning(warlordsNPC.getLocation(), false, 10);
                    Utils.playGlobalSound(warlordsNPC.getLocation(), "shaman.earthlivingweapon.impact", 500, 0.5f);

                    for (WarlordsEntity we : PlayerFilter
                            .entitiesAround(warlordsNPC, 100, 100, 100)
                            .aliveEnemiesOf(warlordsNPC)
                    ) {
                        Utils.addKnockback(warlordsNPC.getLocation(), we, -2, 0.4);
                        EffectUtils.playParticleLinkAnimation(we.getLocation(), warlordsNPC.getLocation(), ParticleEffect.VILLAGER_HAPPY);
                        we.addDamageInstance(
                                warlordsNPC,
                                "Death Ray",
                                we.getMaxHealth() * 0.9f,
                                we.getMaxHealth() * 0.9f,
                                -1,
                                100,
                                true
                        );

                        warlordsNPC.addHealingInstance(
                                warlordsNPC,
                                "Death Ray Healing",
                                we.getMaxHealth() * 0.2f,
                                we.getMaxHealth() * 0.2f,
                                -1,
                                100,
                                false,
                                false
                        );
                    }

                    this.cancel();
                }

                for (WarlordsEntity we : PlayerFilter.playingGame(getWarlordsNPC().getGame())) {
                    if (we.getEntity() instanceof Player) {
                        PacketUtils.sendTitle(
                                (Player) we.getEntity(),
                                ChatColor.YELLOW.toString() + countdown.get(),
                                ChatColor.RED.toString() + damageToDeal.get(),
                                0, 4, 0
                        );
                    }
                }
            }
        }.runTaskTimer(40, 0);
    }
}
