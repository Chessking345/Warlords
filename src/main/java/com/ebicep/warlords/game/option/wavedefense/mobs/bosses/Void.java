package com.ebicep.warlords.game.option.wavedefense.mobs.bosses;

import com.ebicep.warlords.abilties.internal.DamageCheck;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.effects.FireWorkEffectPlayer;
import com.ebicep.warlords.effects.ParticleEffect;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.option.wavedefense.WaveDefenseOption;
import com.ebicep.warlords.game.option.wavedefense.mobs.MobTier;
import com.ebicep.warlords.game.option.wavedefense.mobs.irongolem.IronGolem;
import com.ebicep.warlords.game.option.wavedefense.mobs.mobtypes.BossMob;
import com.ebicep.warlords.game.option.wavedefense.mobs.skeleton.AbstractSkeleton;
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
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicInteger;

public class Void extends AbstractSkeleton implements BossMob {

    private boolean flamePhaseTrigger = false;
    private boolean flamePhaseTriggerTwo = false;
    private boolean timedDamageTrigger = false;
    private boolean preventArmageddon = false;
    private final int stormRadius = 10;
    private final int earthQuakeRadius = 10;

    AtomicInteger damageToDeal = new AtomicInteger(0);

    public Void(Location spawnLocation) {
        super(
                spawnLocation,
                "Void",
                MobTier.BOSS,
                new Utils.SimpleEntityEquipment(
                        SkullUtils.getSkullFrom(SkullID.END_MONSTER),
                        Utils.applyColorTo(Material.LEATHER_CHESTPLATE, 20, 20, 20),
                        Utils.applyColorTo(Material.LEATHER_LEGGINGS, 20, 20, 20),
                        Utils.applyColorTo(Material.LEATHER_BOOTS, 20, 20, 20),
                        Weapons.VOID_EDGE.getItem()
                ),
                200000,
                0.2f,
                20,
                3000,
                4000
        );
    }

    @Override
    public void onSpawn(WaveDefenseOption option) {
        for (WarlordsEntity we : PlayerFilter.playingGame(getWarlordsNPC().getGame())) {
            if (we.getEntity() instanceof Player) {
                PacketUtils.sendTitle(
                        (Player) we.getEntity(),
                        ChatColor.DARK_GRAY + getWarlordsNPC().getName(),
                        ChatColor.BLACK + "?????",
                        20, 30, 20
                );
            }
        }

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

            @Override
            public void multiplyKB(Vector currentVector) {
                // immune to KB
                currentVector.multiply(0.05);
            }
        });
    }

    @Override
    public void whileAlive(int ticksElapsed, WaveDefenseOption option) {
        Location loc = warlordsNPC.getLocation();
        long playerCount = option.getGame().warlordsPlayers().count();
        if (warlordsNPC.getHealth() < (warlordsNPC.getMaxHealth() * 0.75f) && !flamePhaseTrigger) {
            flamePhaseTrigger = true;
            immolation(option, loc);
        }

        if (warlordsNPC.getHealth() < (warlordsNPC.getMaxHealth() * 0.15f) && !flamePhaseTriggerTwo) {
            flamePhaseTriggerTwo = true;
            immolation(option, loc);
        }

        if (warlordsNPC.getHealth() < (warlordsNPC.getMaxHealth() * .5f) && !timedDamageTrigger) {
            timedDamageTrigger = true;
            preventArmageddon = true;
            timedDamage(option, playerCount, 15000, 13);
            for (int i = 0; i < (2 * playerCount); i++) {
                option.spawnNewMob(new IronGolem(loc));
            }
        }

        if (ticksElapsed % 160 == 0) {
            //Bukkit.broadcastMessage("earthquake");
            Utils.playGlobalSound(loc, Sound.ENDERDRAGON_GROWL, 2, 0.4f);
            EffectUtils.strikeLightning(loc, false);
            EffectUtils.playSphereAnimation(loc, earthQuakeRadius, ParticleEffect.SPELL_WITCH, 2);
            EffectUtils.playHelixAnimation(loc, earthQuakeRadius, ParticleEffect.FIREWORKS_SPARK, 2, 40);
            for (WarlordsEntity enemy : PlayerFilter
                    .entitiesAround(warlordsNPC, earthQuakeRadius, earthQuakeRadius, earthQuakeRadius)
                    .aliveEnemiesOf(warlordsNPC)
            ) {
                Utils.addKnockback(loc, enemy, -1.5, 0.25);
                enemy.addDamageInstance(
                        warlordsNPC,
                        "Ground Shred",
                        750,
                        900,
                        0,
                        100,
                        false
                );
            }
        }

        if (ticksElapsed % 300 == 0 && !preventArmageddon) {
            Utils.playGlobalSound(loc, "rogue.healingremedy.impact", 500, 0.6f);
            Utils.playGlobalSound(loc, "rogue.healingremedy.impact", 500, 0.6f);
            warlordsNPC.addSpeedModifier(warlordsNPC, "Armageddon Slowness", -99, 90);
            new GameRunnable(warlordsNPC.getGame()) {
                @Override
                public void run() {
                    if (warlordsNPC.isDead() || preventArmageddon) {
                        this.cancel();
                        return;
                    }

                    EffectUtils.strikeLightningInCylinder(loc, stormRadius, false, 10, warlordsNPC.getGame());
                    shockwave(loc, stormRadius, 10, playerCount, 1);
                    EffectUtils.strikeLightningInCylinder(loc, stormRadius + 5, false, 20, warlordsNPC.getGame());
                    shockwave(loc, stormRadius + 5, 20, playerCount, 1);
                    EffectUtils.strikeLightningInCylinder(loc, stormRadius + 10, false, 30, warlordsNPC.getGame());
                    shockwave(loc, stormRadius + 10, 30, playerCount, 1);
                    EffectUtils.strikeLightningInCylinder(loc, stormRadius + 20, false, 40, warlordsNPC.getGame());
                    shockwave(loc, stormRadius + 20, 40, playerCount, 1);
                    EffectUtils.strikeLightningInCylinder(loc, stormRadius + 25, false, 50, warlordsNPC.getGame());
                    shockwave(loc, stormRadius + 25, 50, playerCount, 1);
                    EffectUtils.strikeLightningInCylinder(loc, stormRadius + 30, false, 60, warlordsNPC.getGame());
                    shockwave(loc, stormRadius + 30, 60, playerCount, 1);
                }
            }.runTaskLater(60);
        }
    }

    @Override
    public void onAttack(WarlordsEntity mob, WarlordsEntity receiver, WarlordsDamageHealingEvent event) {

    }

    @Override
    public void onDamageTaken(WarlordsEntity self, WarlordsEntity attacker, WarlordsDamageHealingEvent event) {

    }

    private void immolation(WaveDefenseOption option, Location loc) {
        warlordsNPC.addSpeedModifier(warlordsNPC, "Void Slowness", -99, 250);
        for (int i = 0; i < 3; i++) {
            Utils.playGlobalSound(loc, Sound.ENDERDRAGON_GROWL, 500, 0.6f);
        }

        for (WarlordsEntity we : PlayerFilter.playingGame(getWarlordsNPC().getGame())) {
            if (we.getEntity() instanceof Player) {
                PacketUtils.sendTitle(
                        (Player) we.getEntity(),
                        ChatColor.RED + "PREPARE TO DIE",
                        ChatColor.LIGHT_PURPLE + "Augmented Immolation",
                        20, 60, 20
                );
            }
        }

        float damage;
        switch (option.getDifficulty()) {
            case HARD:
                damage = 300;
                break;
            case EASY:
                damage = 100;
                break;
            default:
                damage = 250;
                break;
        }
        new GameRunnable(warlordsNPC.getGame()) {
            int counter = 0;

            @Override
            public void run() {
                if (warlordsNPC.isDead()) {
                    this.cancel();
                    return;
                }

                counter++;
                double radius = (4 * counter);
                Utils.playGlobalSound(loc, Sound.ENDERDRAGON_GROWL, 500, 0.1f);
                Utils.playGlobalSound(loc, "warrior.laststand.activation", 500, 0.1f);
                EffectUtils.playHelixAnimation(warlordsNPC.getLocation(), radius, ParticleEffect.SMOKE_NORMAL, 2, counter);
                for (WarlordsEntity flameTarget : PlayerFilter
                        .entitiesAround(warlordsNPC, radius, radius, radius)
                        .aliveEnemiesOf(warlordsNPC)
                ) {
                    flameTarget.addDamageInstance(
                            warlordsNPC,
                            "Augmented Immolation",
                            damage,
                            damage,
                            0,
                            100,
                            false
                    );

                    warlordsNPC.addHealingInstance(
                            warlordsNPC,
                            "Augmented Immolation",
                            damage * 0.5f,
                            damage * 0.5f,
                            0,
                            100,
                            false,
                            false
                    );
                }

                if (counter == 50) {
                    this.cancel();
                    warlordsNPC.getSpeed().addBaseModifier(40);
                }
            }
        }.runTaskTimer(40, 3);
    }

    private void shockwave(Location loc, double radius, int tickDelay, long playerCount, float damageMultiplier) {
        new GameRunnable(warlordsNPC.getGame()) {
            @Override
            public void run() {
                if (warlordsNPC.isDead()) {
                    this.cancel();
                }

                Utils.playGlobalSound(loc, Sound.ENDERDRAGON_GROWL, 10, 0.4f);
                Utils.playGlobalSound(loc, "warrior.laststand.activation", 10, 0.4f);
                for (WarlordsEntity we : PlayerFilter
                        .entitiesAround(loc, radius, radius, radius)
                        .aliveEnemiesOf(warlordsNPC)
                ) {
                    if (!we.getCooldownManager().hasCooldownFromName("Cloaked")) {
                        we.addDamageInstance(warlordsNPC, "Augmented Armageddon", (550 * playerCount) * damageMultiplier, (700 * playerCount) * damageMultiplier, 0, 100, false);
                        Utils.addKnockback(warlordsNPC.getLocation(), we, -2, 0.2);
                    }
                }
            }
        }.runTaskLater(tickDelay);
    }

    private void timedDamage(WaveDefenseOption option, long playerCount, int damageValue, int timeToDealDamage) {
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
            Utils.addKnockback(warlordsNPC.getLocation(), we, -4, 0.3);
            Utils.playGlobalSound(warlordsNPC.getLocation(), Sound.WITHER_SPAWN, 500, 0.3f);
        }

        AtomicInteger countdown = new AtomicInteger(timeToDealDamage);
        new GameRunnable(warlordsNPC.getGame()) {
            int counter = 0;
            @Override
            public void run() {
                if (warlordsNPC.isDead()) {
                    this.cancel();
                    return;
                }

                if (damageToDeal.get() <= 0) {
                    FireWorkEffectPlayer.playFirework(warlordsNPC.getLocation(), FireworkEffect.builder()
                            .withColor(Color.WHITE)
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .build());
                    warlordsNPC.getSpec().getBlue().onActivate(warlordsNPC, null);
                    preventArmageddon = false;
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
                        EffectUtils.playParticleLinkAnimation(
                                we.getLocation(),
                                warlordsNPC.getLocation(),
                                255,
                                255,
                                255,
                                2
                        );

                        we.addDamageInstance(
                                warlordsNPC,
                                "Vampiric Leash",
                                500,
                                500,
                                -1,
                                100,
                                true
                        );
                    }
                }

                if (countdown.get() <= 0 && damageToDeal.get() > 0) {
                    for (int i = 0; i < (2 * option.getGame().warlordsPlayers().count()); i++) {
                        option.spawnNewMob(new IronGolem(spawnLocation));
                    }

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
                                we.getMaxHealth() * 0.3f,
                                we.getMaxHealth() * 0.3f,
                                -1,
                                100,
                                false,
                                false
                        );
                    }

                    preventArmageddon = false;
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