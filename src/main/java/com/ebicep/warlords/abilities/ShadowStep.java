package com.ebicep.warlords.abilities;

import com.ebicep.warlords.abilities.internal.AbstractAbility;
import com.ebicep.warlords.abilities.internal.icon.PurpleAbilityIcon;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.effects.FireWorkEffectPlayer;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.AbstractUpgradeBranch;
import com.ebicep.warlords.pve.upgrades.rogue.assassin.ShadowStepBranch;
import com.ebicep.warlords.util.bukkit.LocationBuilder;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShadowStep extends AbstractAbility implements PurpleAbilityIcon {

    public int totalPlayersHit = 0;

    private int fallDamageNegation = 10;

    public ShadowStep() {
        super("Shadow Step", 466, 598, 12, 20, 15, 175);
    }

    @Override
    public void updateDescription(Player player) {
        description = Component.text("Leap forward, dealing ")
                               .append(formatRangeDamage(minDamageHeal, maxDamageHeal))
                               .append(Component.text(" damage to all enemies close on cast or when landing on the ground. You take reduced fall damage while leaping."))
                               .append(Component.newline())
                               .append(Component.newline())
                               .append(Component.text("Shadow Step has reduced range when holding a Flag."));
    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        List<Pair<String, String>> info = new ArrayList<>();
        info.add(new Pair<>("Times Used", "" + timesUsed));
        info.add(new Pair<>("Players Hit", "" + totalPlayersHit));

        return info;
    }

    @Override
    public boolean onActivate(@Nonnull WarlordsEntity wp, @Nullable Player player) {
        Location playerLoc = wp.getLocation();
        wp.subtractEnergy(name, energyCost, false);
        Utils.playGlobalSound(playerLoc, "rogue.drainingmiasma.activation", 1, 2);
        Utils.playGlobalSound(playerLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2, 2);

        wp.setFlagPickCooldown(2);

        EffectUtils.playFirework(
                wp.getLocation().add(0, pveMasterUpgrade2 ? 1 : 0, 0),
                FireworkEffect.builder()
                              .withColor(Color.BLACK)
                              .with(FireworkEffect.Type.BALL)
                              .build()
        );

        if (wp.onHorse()) {
            wp.removeHorse();
        }

        if (pveMasterUpgrade2) {
            doShadowDash(wp);
        } else {
            if (wp.getCarriedFlag() != null) {
                player.setVelocity(playerLoc.getDirection().multiply(1).setY(0.35));
                player.setFallDistance(-fallDamageNegation);
            } else {
                player.setVelocity(playerLoc.getDirection().multiply(1.5).setY(0.7));
                player.setFallDistance(-fallDamageNegation);
            }

            doShadowStep(wp, playerLoc);
        }

        return true;
    }

    @Override
    public AbstractUpgradeBranch<?> getUpgradeBranch(AbilityTree abilityTree) {
        return new ShadowStepBranch(abilityTree, this);
    }

    private void doShadowDash(@Nonnull WarlordsEntity wp) {
        Set<WarlordsEntity> hit = new HashSet<>();
        LocationBuilder locationBuilder = new LocationBuilder(wp.getEyeLocation());
        for (Block ignored : Utils.getTargetBlockInBetween(wp.getEyeLocation(), 8)) {
            if (!Utils.getTargetBlock(locationBuilder, 1).getType().isAir() ||
                    !locationBuilder.getBlock().getType().isAir() ||
                    !locationBuilder.clone()
                                    .addY(1)
                                    .getBlock()
                                    .getType()
                                    .isAir()
            ) {
                locationBuilder.centerXZBlock();
                boolean isSlab = locationBuilder.clone().addY(-1).getBlock().getBlockData() instanceof Slab;
                locationBuilder.addY(isSlab ? -0.5 : 0);
                break;
            }
            PlayerFilter.entitiesAround(locationBuilder.clone().addY(-1), 2, 2, 2)
                        .aliveEnemiesOf(wp)
                        .excluding(hit)
                        .forEach(warlordsEntity -> {
                            hit.add(warlordsEntity);
                            warlordsEntity.addDamageInstance(
                                    wp,
                                    "Shadow Dash",
                                    minDamageHeal,
                                    maxDamageHeal,
                                    critChance,
                                    critMultiplier
                            );
                        });
            locationBuilder = locationBuilder.forward(1);
            EffectUtils.displayParticle(
                    Particle.SMOKE_NORMAL,
                    locationBuilder.clone().addY(-.5),
                    10,
                    .1,
                    .1,
                    .1,
                    0
            );
        }
        Utils.playGlobalSound(wp.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 2, 1.5f);
        wp.teleportLocationOnly(locationBuilder);
    }


    private void doShadowStep(@Nonnull WarlordsEntity wp, Location playerLoc) {
        List<WarlordsEntity> playersHit = new ArrayList<>();
        for (WarlordsEntity assaultTarget : PlayerFilter
                .entitiesAround(wp, 5, 5, 5)
                .aliveEnemiesOf(wp)
        ) {
            totalPlayersHit++;
            assaultTarget.addDamageInstance(wp, name, minDamageHeal, maxDamageHeal, critChance, critMultiplier);
            Utils.playGlobalSound(playerLoc, "warrior.revenant.orbsoflife", 2, 1.9f);
            playersHit.add(assaultTarget);
        }

        new GameRunnable(wp.getGame()) {
            double y = playerLoc.getY();
            boolean wasOnGround = true;
            int counter = 0;

            @Override
            public void run() {
                counter++;
                // if player never lands in the span of 10 seconds, remove damage.
                if (counter == 200 || wp.isDead()) {
                    this.cancel();
                }

                wp.getLocation(playerLoc);
                boolean hitGround = wp.getEntity().isOnGround() || wp.onHorse();
                y = playerLoc.getY();

                if (wasOnGround && !hitGround) {
                    wasOnGround = false;
                }

                if (!wasOnGround && hitGround) {
                    wasOnGround = true;

                    for (WarlordsEntity landingTarget : PlayerFilter
                            .entitiesAround(wp, 5, 5, 5)
                            .aliveEnemiesOf(wp)
                            .excluding(playersHit)
                    ) {
                        totalPlayersHit++;
                        landingTarget.addDamageInstance(wp, name, minDamageHeal, maxDamageHeal, critChance, critMultiplier);
                        Utils.playGlobalSound(playerLoc, "warrior.revenant.orbsoflife", 2, 1.9f);
                    }

                    if (pveMasterUpgrade) {
                        buffOnLanding(wp);
                    }

                    FireWorkEffectPlayer.playFirework(wp.getLocation(), FireworkEffect.builder()
                                                                                      .withColor(Color.BLACK)
                                                                                      .with(FireworkEffect.Type.BALL)
                                                                                      .build());

                    this.cancel();
                }
            }
        }.runTaskTimer(0, 0);
    }

    private void buffOnLanding(WarlordsEntity we) {
        we.addSpeedModifier(we, name, 80, 5 * 20);
        we.getCooldownManager().removeCooldown(ShadowStep.class, false);
        we.getCooldownManager().addCooldown(new RegularCooldown<>(
                "STEP KB",
                "STEP KB",
                ShadowStep.class,
                new ShadowStep(),
                we,
                CooldownTypes.ABILITY,
                cooldownManager -> {
                },
                5 * 20
        ) {
            @Override
            public void multiplyKB(Vector currentVector) {
                currentVector.multiply(0.2);
            }
        });
    }

    public void setFallDamageNegation(int fallDamageNegation) {
        this.fallDamageNegation = fallDamageNegation;
    }


}
