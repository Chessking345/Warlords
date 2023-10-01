package com.ebicep.warlords.abilities;

import com.ebicep.warlords.abilities.internal.AbstractAbility;
import com.ebicep.warlords.abilities.internal.CanReduceCooldowns;
import com.ebicep.warlords.abilities.internal.Overheal;
import com.ebicep.warlords.abilities.internal.icon.RedAbilityIcon;
import com.ebicep.warlords.achievements.types.ChallengeAchievements;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.events.player.ingame.WarlordsAbilityActivateEvent;
import com.ebicep.warlords.events.player.ingame.WarlordsAddCooldownEvent;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownManager;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.player.ingame.cooldowns.instances.InstanceFlags;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.AbstractUpgradeBranch;
import com.ebicep.warlords.pve.upgrades.mage.aquamancer.WaterBreathBranch;
import com.ebicep.warlords.util.bukkit.LocationBuilder;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class WaterBreath extends AbstractAbility implements RedAbilityIcon, CanReduceCooldowns {

    public int playersHealed = 0;
    public int debuffsRemoved = 0;

    private int maxAnimationTime = 12;
    private int maxAnimationEffects = 4;
    private float hitbox = 10;
    private double velocity = 1.1;

    public WaterBreath() {
        super("Water Breath", 528, 723, 6.3f, 60, 25, 175);
    }

    @Override
    public void updateDescription(Player player) {
        description = Component.text("Breathe water in a cone in front of you, knocking back enemies, cleansing all ")
                               .append(Component.text("de-buffs", NamedTextColor.YELLOW))
                               .append(Component.text(" and restoring "))
                               .append(formatRangeHealing(minDamageHeal, maxDamageHeal))
                               .append(Component.text(" health to yourself and all allies hit."))
                               .append(Component.text("\n\nWater Breath can overheal allies for up to "))
                               .append(Component.text("10%", NamedTextColor.GREEN))
                               .append(Component.text(" of their max health as bonus health for "))
                               .append(Component.text(Overheal.OVERHEAL_DURATION, NamedTextColor.GOLD))
                               .append(Component.text(" seconds."));
    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        List<Pair<String, String>> info = new ArrayList<>();
        info.add(new Pair<>("Times Used", "" + timesUsed));
        info.add(new Pair<>("Players Healed", "" + playersHealed));
        info.add(new Pair<>("Debuffs Removed", "" + debuffsRemoved));

        return info;
    }

    @Override
    public boolean onActivate(@Nonnull WarlordsEntity wp, @Nonnull Player player) {
        wp.subtractEnergy(name, energyCost, false);
        Utils.playGlobalSound(player.getLocation(), "mage.waterbreath.activation", 2, 1);
        player.getWorld().spawnParticle(
                Particle.HEART,
                player.getLocation().add(0, 0.7, 0),
                2,
                0.6,
                0.6,
                0.6,
                1,
                null,
                true
        );

        Location playerLoc = new LocationBuilder(player.getLocation())
                .pitch(0)
                .add(0, 1.7, 0);

        EffectUtils.playSpiralAnimation(
                wp,
                playerLoc,
                maxAnimationEffects,
                maxAnimationTime,
                (center, animationTimer) -> {},
                Particle.DRIP_WATER, Particle.ENCHANTMENT_TABLE, Particle.VILLAGER_HAPPY
        );

        int previousDebuffsRemoved = debuffsRemoved;
        debuffsRemoved += wp.getCooldownManager().removeDebuffCooldowns();
        wp.getSpeed().removeSlownessModifiers();
        wp.addHealingInstance(wp, name, minDamageHeal, maxDamageHeal, critChance, critMultiplier);

        Location playerEyeLoc = new LocationBuilder(player.getLocation())
                .pitch(0)
                .backward(1);
        Vector viewDirection = playerLoc.getDirection();
        for (WarlordsEntity breathTarget : PlayerFilter
                .entitiesAroundRectangle(playerLoc, hitbox - 2.5, hitbox, hitbox - 2.5)
                .excluding(wp)
                .isAlive()
        ) {
            Vector direction = breathTarget.getLocation().subtract(playerEyeLoc).toVector().normalize();
            if (!(viewDirection.dot(direction) > .68)) {
                continue;
            }
            CooldownManager breathTargetCooldownManager = breathTarget.getCooldownManager();
            if (wp.isTeammate(breathTarget)) {
                playersHealed++;
                debuffsRemoved += breathTargetCooldownManager.removeDebuffCooldowns();
                breathTarget.getSpeed().removeSlownessModifiers();
                breathTarget.addHealingInstance(wp, name, minDamageHeal, maxDamageHeal, critChance, critMultiplier, EnumSet.of(InstanceFlags.CAN_OVERHEAL_OTHERS));
                Overheal.giveOverHeal(wp, breathTarget);
                if (pveMasterUpgrade || pveMasterUpgrade2) {
                    regenOnHit(wp, breathTarget);
                }
            } else {
                final Location loc = breathTarget.getLocation();
                final Vector v = player.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(-velocity).setY(0.2);
                breathTarget.setVelocity(name, v, false);

                if (pveMasterUpgrade2) {
                    giveMaliciousMist(wp, breathTarget);
                }
            }
        }
        int totalDebuffsRemoved = debuffsRemoved - previousDebuffsRemoved;
        if (totalDebuffsRemoved >= 7) {
            ChallengeAchievements.checkForAchievement(wp, ChallengeAchievements.CLEANSING_RITUAL);
        }

        return true;
    }

    private void regenOnHit(WarlordsEntity giver, WarlordsEntity hit) {
        boolean hasPreviousCooldown = hit.getCooldownManager().hasCooldown(WaterBreath.class);
        hit.getCooldownManager().removeCooldown(WaterBreath.class, false);
        hit.getCooldownManager().addRegularCooldown(
                name,
                "BREATH RGN",
                WaterBreath.class,
                new WaterBreath(),
                giver,
                CooldownTypes.ABILITY,
                cooldownManager -> {
                },
                5 * 20,
                Collections.singletonList((cooldown, ticksLeft, ticksElapsed) -> {
                    if (ticksLeft % 20 == 0) {
                        float healing = hit.getMaxHealth() * 0.02f;
                        hit.addHealingInstance(
                                giver,
                                name,
                                healing,
                                healing,
                                0,
                                100,
                                EnumSet.of(InstanceFlags.CAN_OVERHEAL_OTHERS)
                        );
                    }
                })
        );
        if (!hasPreviousCooldown) {
            hit.getSpec().decreaseAllCooldownTimersBy(1.5f);
            hit.updateItems();
        }
    }

    private static void giveMaliciousMist(@Nonnull WarlordsEntity wp, WarlordsEntity breathTarget) {
        CooldownManager breathTargetCooldownManager = breathTarget.getCooldownManager();
        breathTargetCooldownManager.removeBuffCooldowns();
        breathTargetCooldownManager.removeCooldownByName("Malicious Mist");
        breathTargetCooldownManager.addCooldown(new RegularCooldown<>(
                "Malicious Mist",
                "MIST",
                WaterBreath.class,
                new WaterBreath(),
                wp,
                CooldownTypes.ABILITY,
                cooldownManager -> {
                },
                3 * 20,
                Collections.singletonList((cooldown, ticksLeft, ticksElapsed) -> {
                    if (ticksLeft % 5 == 0) {
                        EffectUtils.displayParticle(
                                Particle.WATER_SPLASH,
                                breathTarget.getLocation().add(0, 1.25, 0),
                                10,
                                0.4f,
                                0.4f,
                                0.4f,
                                0
                        );
                    }
                })
        ) {
            @Override
            protected Listener getListener() {
                return new Listener() {
                    @EventHandler
                    public void onAbilityActivate(WarlordsAbilityActivateEvent event) {
                        if (event.getWarlordsEntity().equals(breathTarget)) {
                            event.setCancelled(true);
                        }
                    }

                    @EventHandler
                    public void onBuffAdd(WarlordsAddCooldownEvent event) {
                        if (event.getWarlordsEntity().equals(breathTarget) && event.getAbstractCooldown().getCooldownType() == CooldownTypes.BUFF) {
                            event.setCancelled(true);
                        }
                    }
                };
            }
        });
    }

    @Override
    public AbstractUpgradeBranch<?> getUpgradeBranch(AbilityTree abilityTree) {
        return new WaterBreathBranch(abilityTree, this);
    }

    @Override
    public boolean canReduceCooldowns() {
        return pveMasterUpgrade;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public int getMaxAnimationTime() {
        return maxAnimationTime;
    }

    public void setMaxAnimationTime(int maxAnimationTime) {
        this.maxAnimationTime = maxAnimationTime;
    }

    public float getHitbox() {
        return hitbox;
    }

    public void setHitbox(float hitbox) {
        this.hitbox = hitbox;
    }

    public int getMaxAnimationEffects() {
        return maxAnimationEffects;
    }

    public void setMaxAnimationEffects(int maxAnimationEffects) {
        this.maxAnimationEffects = maxAnimationEffects;
    }
}
