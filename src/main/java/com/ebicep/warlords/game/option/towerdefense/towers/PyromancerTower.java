package com.ebicep.warlords.game.option.towerdefense.towers;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.abilities.internal.DamageCheck;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.game.option.towerdefense.attributes.AttackSpeed;
import com.ebicep.warlords.game.option.towerdefense.attributes.Damage;
import com.ebicep.warlords.game.option.towerdefense.attributes.Range;
import com.ebicep.warlords.game.option.towerdefense.attributes.upgradeable.TowerUpgrade;
import com.ebicep.warlords.game.option.towerdefense.attributes.upgradeable.TowerUpgradeInstance;
import com.ebicep.warlords.game.option.towerdefense.attributes.upgradeable.Upgradeable;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.player.ingame.cooldowns.instances.InstanceFlags;
import com.ebicep.warlords.util.bukkit.LocationBuilder;
import com.ebicep.warlords.util.bukkit.Matrix4d;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.modifiablevalues.FloatModifiable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public class PyromancerTower extends AbstractTower implements Damage, Range, AttackSpeed, Upgradeable.Path2 {

    private final List<FloatModifiable> damages = new ArrayList<>();
    private final List<FloatModifiable> ranges = new ArrayList<>();
    private final List<FloatModifiable> attackSpeeds = new ArrayList<>();
    private final List<TowerUpgrade> upgrades = new ArrayList<>();

    private final FloatModifiable flameDamage = new FloatModifiable(500);
    private final FloatModifiable flameRange = new FloatModifiable(30);
    private final FloatModifiable flameAttackSpeed = new FloatModifiable(5 * 20); // 5 seconds

    public PyromancerTower(Game game, UUID owner, Location location) {
        super(game, owner, location);
        damages.add(flameDamage);
        ranges.add(flameRange);
        attackSpeeds.add(flameAttackSpeed);
        TowerUpgradeInstance.DamageUpgradeInstance upgradeDamage1 = new TowerUpgradeInstance.DamageUpgradeInstance(25);
        TowerUpgradeInstance.DamageUpgradeInstance upgradeDamage2 = new TowerUpgradeInstance.DamageUpgradeInstance(25);
        TowerUpgradeInstance.DamageUpgradeInstance upgradeDamage3 = new TowerUpgradeInstance.DamageUpgradeInstance(50);

        upgrades.add(new TowerUpgrade("Upgrade 1", upgradeDamage1) {
            @Override
            public void onUpgrade() {
                flameDamage.addAdditiveModifier(name, upgradeDamage1.getValue());
            }
        });
        upgrades.add(new TowerUpgrade("Upgrade 2", upgradeDamage2) {
            @Override
            public void onUpgrade() {
                flameDamage.addAdditiveModifier(name, upgradeDamage2.getValue());
            }
        });
        upgrades.add(new TowerUpgrade("Future Damage + Minor AOE", upgradeDamage3) {
            @Override
            public void onUpgrade() {
                flameDamage.addAdditiveModifier(name, upgradeDamage3.getValue());
            }
        });
        upgrades.add(new TowerUpgrade("Burn", upgradeDamage3) {});
    }

    @Override
    public TowerRegistry getTowerRegistry() {
        return TowerRegistry.PYRO_TOWER;
    }

    @Override
    public void whileActive(int ticksElapsed) {
        super.whileActive(ticksElapsed);
        if (ticksElapsed % 5 == 0) {
            EffectUtils.displayParticle(Particle.CRIMSON_SPORE, topCenterLocation, 5, .5, .1, .5, 2);
        }
        int attackSpeed = (int) flameAttackSpeed.getCalculatedValue();
        if (ticksElapsed % attackSpeed == 0) {
            flameAttack();
        }
    }

    private void flameAttack() {
        float rangeValue = flameRange.getCalculatedValue();
        float damageValue = flameDamage.getCalculatedValue();
        getEnemyMobs(EnemyTargetPriority.FIRST, rangeValue, 1).forEach(warlordsNPC -> {
            int teleportDuration = 5;
            Location targetLocation = new LocationBuilder(warlordsNPC.getLocation())
                    .addY(1);
            LocationBuilder startLocation = new LocationBuilder(warlordsTower.getLocation())
                    .addY(-.5)
                    .faceTowards(targetLocation);
//                EffectUtils.playParticleLinkAnimation(warlordsNPC.getLocation(), startLocation.clone().addY(-1), Particle.FLAME);
            playSpiralFacingEffect(startLocation, targetLocation, Particle.SMALL_FLAME);
            playSpiralFacingEffect(startLocation.clone().forward(1.1f), targetLocation, Particle.DRAGON_BREATH);

            ItemDisplay arrow = fireArrowTowards(teleportDuration, startLocation, targetLocation);
            new BukkitRunnable() {
                @Override
                public void run() {
                    warlordsNPC.addDamageInstance(warlordsTower, "Flame", damageValue, damageValue, 0, 100);
                    if (upgrades.get(2).isUnlocked()) {
                        PlayerFilter.entitiesAround(warlordsNPC, 2, 2, 2)
                                    .aliveEnemiesOf(warlordsTower)
                                    .excluding(warlordsNPC)
                                    .forEach(warlordsEntity -> warlordsEntity.addDamageInstance(warlordsTower, "Flame", damageValue, damageValue, 0, 100));
                    } else if (upgrades.get(3).isUnlocked()) {
                        warlordsNPC.getCooldownManager().addCooldown(new RegularCooldown<>(
                                "Pyromancer Tower Burn",
                                "BRN",
                                PyromancerTower.class,
                                null,
                                warlordsTower,
                                CooldownTypes.DEBUFF,
                                cooldownManager -> {
                                },
                                60,
                                Collections.singletonList((cooldown, ticksLeft, ticksElapsed) -> {
                                    if (ticksLeft % 20 == 0) {
                                        float healthDamage = warlordsNPC.getMaxHealth() * 0.005f;
                                        healthDamage = DamageCheck.clamp(healthDamage);
                                        warlordsNPC.addDamageInstance(
                                                warlordsTower,
                                                "Burn",
                                                healthDamage,
                                                healthDamage,
                                                0,
                                                100,
                                                EnumSet.of(InstanceFlags.RECURSIVE)
                                        );
                                    }
                                })
                        ) {
                            @Override
                            public float modifyDamageBeforeInterveneFromSelf(WarlordsDamageHealingEvent event, float currentDamageValue) {
                                return currentDamageValue * 1.2f;
                            }
                        });
                    }
                    EffectUtils.displayParticle(Particle.LAVA, warlordsNPC.getLocation().clone().add(0, 1, 0), 15, 0.5F, 0, 0.5F, 500);
                    arrow.remove();
                }
            }.runTaskLater(Warlords.getInstance(), teleportDuration);
        });
    }

    private static void playSpiralFacingEffect(Location startLocation, Location targetLocation, Particle particle) {
        double width = .6;

        LocationBuilder builder = new LocationBuilder(startLocation);
        for (int ticksLived = 0; ticksLived < 200; ticksLived++) {
            Matrix4d center = new Matrix4d(builder);
            for (float i = 0; i < 4; i++) {
                double angle = Math.toRadians(i * 90) + ticksLived * 0.45;
                EffectUtils.displayParticle(
                        particle,
                        center.translateVector(builder.getWorld(), 0, Math.sin(angle) * width, Math.cos(angle) * width),
                        3
                );
            }
            builder.forward(.4);
            if (builder.distanceSquared(targetLocation) < 2) {
                break;
            }
        }
    }

    private static ItemDisplay fireArrowTowards(int teleportDuration, Location startLocation, Location endLocation) {
        LocationBuilder start = new LocationBuilder(startLocation);
        float pitchTowards = start.getPitch();
        start.pitch(0); // make arrow straight
        start.yaw(start.getYaw() - 90); // rotate arrow to face the right direction
        LocationBuilder end = new LocationBuilder(endLocation).direction(start.getDirection());
        ItemStack item = new ItemStack(Material.TIPPED_ARROW);
        PotionMeta itemMeta = (PotionMeta) item.getItemMeta();
        itemMeta.setBasePotionType(PotionType.INSTANT_HEAL);
        item.setItemMeta(itemMeta);
        ItemDisplay arrow = startLocation.getWorld().spawn(
                start,
                ItemDisplay.class,
                itemDisplay -> {
                    itemDisplay.setItemStack(item);
                    itemDisplay.setTransformation(new Transformation(
                                    new Vector3f(),
                                    new AxisAngle4f((float) Math.toRadians(45 + pitchTowards), 0, 0, 1),
                                    new Vector3f(2f),
                                    new AxisAngle4f()
                            )
                    );
                    itemDisplay.setTeleportDuration(teleportDuration);
                }
        );
        arrow.teleport(end);
        return arrow;
    }

    @Override
    public List<TowerUpgrade> getUpgrades() {
        return upgrades;
    }

    @Override
    public List<FloatModifiable> getDamages() {
        return damages;
    }

    @Override
    public List<FloatModifiable> getRanges() {
        return ranges;
    }

    @Override
    public List<FloatModifiable> getAttackSpeeds() {
        return attackSpeeds;
    }
}
