package com.ebicep.warlords.abilities;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.abilities.internal.AbstractHolyRadiance;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.AbstractUpgradeBranch;
import com.ebicep.warlords.pve.upgrades.paladin.protector.HolyRadianceBranchProtector;
import com.ebicep.warlords.util.bukkit.LocationUtils;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HolyRadianceProtector extends AbstractHolyRadiance {

    private final int markRadius = 15;

    private int markDuration = 6;
    private float markHealing = 50;

    public HolyRadianceProtector(float minDamageHeal, float maxDamageHeal, float cooldown, float energyCost, float critChance, float critMultiplier) {
        super("Holy Radiance", minDamageHeal, maxDamageHeal, cooldown, energyCost, critChance, critMultiplier, 6);
    }

    @Override
    public void updateDescription(Player player) {
        description = Component.text("Radiate with holy energy, healing yourself and all nearby allies for ")
                               .append(formatRangeHealing(minDamageHeal, maxDamageHeal))
                               .append(Component.text(" health."))
                               .append(Component.text("\n\nYou may look at an ally to mark them for "))
                               .append(Component.text(markDuration, NamedTextColor.GOLD))
                               .append(Component.text("seconds. Your marked ally will emit a second Holy Radiance for "))
                               .append(Component.text(markHealing + "%", NamedTextColor.GREEN))
                               .append(Component.text(" of the original healing amount after the mark ends."))
                               .append(Component.text("\n\nMark has an optimal range of "))
                               .append(Component.text(markRadius, NamedTextColor.YELLOW))
                               .append(Component.text(" blocks."));
    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        List<Pair<String, String>> info = new ArrayList<>();
        info.add(new Pair<>("Times Used", "" + timesUsed));
        info.add(new Pair<>("Players Healed", "" + playersHealed));
        info.add(new Pair<>("Players Marked", "" + playersMarked));

        return info;
    }

    @Override
    public AbstractUpgradeBranch<?> getUpgradeBranch(AbilityTree abilityTree) {
        return new HolyRadianceBranchProtector(abilityTree, this);
    }

    @Override
    public boolean chain(WarlordsEntity wp, Player player) {
        if (pveMasterUpgrade) {
            for (WarlordsEntity circleTarget : PlayerFilter
                    .entitiesAround(wp, 15, 15, 15)
                    .aliveTeammatesOfExcludingSelf(wp)
            ) {
                emitMarkRadiance(wp, circleTarget);
            }

            return true;
        }

        for (WarlordsEntity markTarget : PlayerFilter
                .entitiesAround(player, markRadius, markRadius, markRadius)
                .aliveTeammatesOfExcludingSelf(wp)
                .lookingAtFirst(wp)
                .limit(1)
        ) {
            if (!LocationUtils.isLookingAtMark(player, markTarget.getEntity()) || !LocationUtils.hasLineOfSight(player, markTarget.getEntity())) {
                player.sendMessage(Component.text("Your mark was out of range or you did not target a player!", NamedTextColor.RED));
                continue;
            }
            Utils.playGlobalSound(wp.getLocation(), "paladin.consecrate.activation", 2, 0.65f);
            // chain particles
            EffectUtils.playParticleLinkAnimation(player.getLocation(), markTarget.getLocation(), 0, 255, 70, 1);
            EffectUtils.playChainAnimation(wp.getLocation(), markTarget.getLocation(), new ItemStack(Material.POPPY), 8);
            emitMarkRadiance(wp, markTarget);

            wp.sendMessage(WarlordsEntity.GIVE_ARROW_GREEN
                    .append(Component.text(" You have marked ", NamedTextColor.GRAY))
                    .append(Component.text(markTarget.getName(), NamedTextColor.GREEN))
                    .append(Component.text("!", NamedTextColor.GRAY))
            );

            markTarget.sendMessage(WarlordsEntity.RECEIVE_ARROW_RED
                    .append(Component.text(" You have been granted ", NamedTextColor.GRAY))
                    .append(Component.text("Protector's Mark", NamedTextColor.GREEN))
                    .append(Component.text(" by " + wp.getName() + "!", NamedTextColor.GRAY))
            );

            return true;
        }

        if (pveMasterUpgrade2) {
            wp.getCooldownManager().addCooldown(new RegularCooldown<>(
                    "Unrivalled Radiance",
                    "RAD",
                    HolyRadianceProtector.class,
                    null,
                    wp,
                    CooldownTypes.ABILITY,
                    cooldownManager -> {
                    },
                    61,
                    Collections.singletonList((cooldown, ticksLeft, ticksElapsed) -> {
                        if (ticksElapsed % 20 == 0 && ticksElapsed != 0) {
                            PlayerFilter.entitiesAround(wp, 10, 10, 10)
                                        .aliveTeammatesOf(wp)
                                        .forEach(warlordsEntity -> {
                                            warlordsEntity.addHealingInstance(
                                                    wp,
                                                    "Unrivalled Radiance",
                                                    150,
                                                    350,
                                                    0,
                                                    100
                                            );
                                        });
                        }
                    })
            ));
        }

        return false;
    }

    private void emitMarkRadiance(WarlordsEntity giver, WarlordsEntity target) {
        HolyRadianceProtector tempMark = new HolyRadianceProtector(
                minDamageHeal,
                maxDamageHeal,
                cooldown.getCurrentValue(),
                energyCost.getCurrentValue(),
                critChance,
                critMultiplier
        );
        target.getCooldownManager().addRegularCooldown(
                name,
                "PROT MARK",
                HolyRadianceProtector.class,
                tempMark,
                giver,
                CooldownTypes.BUFF,
                cooldownManager -> {
                    if (target.isDead()) {
                        return;
                    }

                    EffectUtils.displayParticle(
                            Particle.SPELL,
                            target.getLocation(),
                            12,
                            1,
                            1,
                            1,
                            0.06
                    );

                    Utils.playGlobalSound(target.getLocation(), "paladin.holyradiance.activation", 2, 0.95f);
                    for (WarlordsEntity waveTarget : PlayerFilter
                            .entitiesAround(target, 6, 6, 6)
                            .aliveTeammatesOf(target)
                    ) {
                        target.getGame().registerGameTask(
                                new FlyingArmorStand(
                                        target.getLocation(),
                                        waveTarget,
                                        giver,
                                        1.1,
                                        minDamageHeal * (markHealing / 100f),
                                        maxDamageHeal * (markHealing / 100f)
                                ).runTaskTimer(Warlords.getInstance(), 1, 1)
                        );
                    }
                },
                markDuration * 20,
                Collections.singletonList((cooldown, ticksLeft, ticksElapsed) -> {
                    if (ticksElapsed % 10 == 0) {
                        Location playerLoc = target.getLocation();
                        Location particleLoc = playerLoc.clone();
                        for (int i = 0; i < 4; i++) {
                            for (int j = 0; j < 10; j++) {
                                double angle = j / 9D * Math.PI * 2;
                                double width = 1;
                                particleLoc.setX(playerLoc.getX() + Math.sin(angle) * width);
                                particleLoc.setY(playerLoc.getY() + i / 6D);
                                particleLoc.setZ(playerLoc.getZ() + Math.cos(angle) * width);

                                EffectUtils.displayParticle(
                                        Particle.REDSTONE,
                                        particleLoc,
                                        1,
                                        new Particle.DustOptions(Color.fromRGB(0, 255, 70), 1)
                                );
                            }
                        }
                    }
                })
        );
    }

    public float getMarkHealing() {
        return markHealing;
    }

    public void setMarkHealing(float markHealing) {
        this.markHealing = markHealing;
    }

    public int getMarkDuration() {
        return markDuration;
    }

    public void setMarkDuration(int markDuration) {
        this.markDuration = markDuration;
    }
}