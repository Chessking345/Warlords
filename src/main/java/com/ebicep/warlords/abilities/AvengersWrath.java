package com.ebicep.warlords.abilities;

import com.ebicep.warlords.abilities.internal.AbstractAbility;
import com.ebicep.warlords.abilities.internal.AbstractStrike;
import com.ebicep.warlords.abilities.internal.Duration;
import com.ebicep.warlords.abilities.internal.icon.OrangeAbilityIcon;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.events.player.ingame.WarlordsStrikeEvent;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.player.ingame.cooldowns.instances.InstanceFlags;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.AbstractUpgradeBranch;
import com.ebicep.warlords.pve.upgrades.paladin.avenger.AvengersWrathBranch;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.*;

public class AvengersWrath extends AbstractAbility implements OrangeAbilityIcon, Duration {

    public int extraPlayersStruck = 0;
    public int playersStruckDuringWrath = 0;
    public int playersKilledDuringWrath = 0;

    private int tickDuration = 240;
    private float energyPerSecond = 20;
    private int maxTargets = 2;
    private int hitRadius = 5;

    public AvengersWrath() {
        super("Avenger's Wrath", 0, 0, 52.85f, 0);
    }

    @Override
    public void updateDescription(Player player) {
        description = Component.text("Burst with incredible holy power, causing your Avenger's Strikes to hit up to ")
                               .append(Component.text(maxTargets, NamedTextColor.YELLOW))
                               .append(Component.text(" additional enemies that are within "))
                               .append(Component.text("5", NamedTextColor.YELLOW))
                               .append(Component.text(" blocks of your target. Your energy per second is increased by "))
                               .append(Component.text(format(energyPerSecond), NamedTextColor.GOLD))
                               .append(Component.text(" for the duration of the effect. Lasts "))
                               .append(Component.text(format(tickDuration / 20f), NamedTextColor.GOLD))
                               .append(Component.text(" seconds."));
    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        List<Pair<String, String>> info = new ArrayList<>();
        info.add(new Pair<>("Times Used", "" + timesUsed));
        info.add(new Pair<>("Extra Players Struck", "" + extraPlayersStruck));

        return info;
    }

    @Override
    public boolean onActivate(@Nonnull WarlordsEntity wp, @Nonnull Player player) {
        Utils.playGlobalSound(wp.getLocation(), "paladin.avengerswrath.activation", 2, 1);

        AvengersWrath tempAvengersWrath = new AvengersWrath();
        wp.getCooldownManager().removeCooldown(AvengersWrath.class, false);
        wp.getCooldownManager().addCooldown(new RegularCooldown<>(
                name,
                "WRATH",
                AvengersWrath.class,
                tempAvengersWrath,
                wp,
                CooldownTypes.ABILITY,
                cooldownManager -> {
                },
                tickDuration,
                Collections.singletonList((cooldown, ticksLeft, ticksElapsed) -> {
                    if (ticksElapsed % 4 == 0) {
                        wp.getLocation().getWorld().spawnParticle(
                                Particle.SPELL,
                                wp.getLocation().add(0, 1.2, 0),
                                6,
                                0.3F,
                                0.1F,
                                0.3F,
                                0.2F,
                                null,
                                true
                        );
                    }
                })
        ) {
            @Override
            public void onDamageFromAttacker(WarlordsDamageHealingEvent event, float currentDamageValue, boolean isCrit) {
                if (event.getAbility().equals("Avenger's Strike") && !event.getFlags().contains(InstanceFlags.AVENGER_WRATH_STRIKE)) {
                    tempAvengersWrath.addPlayersStruckDuringWrath();
                    for (WarlordsEntity wrathTarget : PlayerFilter
                            .entitiesAround(event.getWarlordsEntity(), hitRadius, hitRadius, hitRadius)
                            .aliveEnemiesOf(wp)
                            .closestFirst(event.getWarlordsEntity())
                            .excluding(event.getWarlordsEntity())
                            .limit(maxTargets)
                    ) {
                        wp.doOnStaticAbility(AvengersWrath.class, AvengersWrath::addExtraPlayersStruck);
                        tempAvengersWrath.addPlayersStruckDuringWrath();

                        Optional<Consecrate> standingOnConsecrate = AbstractStrike.getStandingOnConsecrate(wp, wrathTarget);
                        if (standingOnConsecrate.isPresent() && !event.getFlags().contains(InstanceFlags.STRIKE_IN_CONS)) {
                            wp.doOnStaticAbility(Consecrate.class, Consecrate::addStrikesBoosted);
                            wrathTarget.addDamageInstance(
                                    wp,
                                    "Avenger's Strike",
                                    event.getMin() * (1 + standingOnConsecrate.get().getStrikeDamageBoost() / 100f),
                                    event.getMax() * (1 + standingOnConsecrate.get().getStrikeDamageBoost() / 100f),
                                    event.getCritChance(),
                                    event.getCritMultiplier(),
                                    EnumSet.of(InstanceFlags.AVENGER_WRATH_STRIKE)
                            );
                        } else {
                            wrathTarget.addDamageInstance(
                                    wp,
                                    "Avenger's Strike",
                                    event.getMin(),
                                    event.getMax(),
                                    event.getCritChance(),
                                    event.getCritMultiplier(),
                                    EnumSet.of(InstanceFlags.AVENGER_WRATH_STRIKE)
                            );
                        }
                        Bukkit.getPluginManager().callEvent(new WarlordsStrikeEvent(wp, AvengersWrath.this, wrathTarget));
                        wrathTarget.subtractEnergy(10, true);
                    }
                }
            }

            @Override
            public void onDeathFromEnemies(WarlordsDamageHealingEvent event, float currentDamageValue, boolean isCrit, boolean isKiller) {
                if (isKiller) {
                    tempAvengersWrath.addPlayersKilledDuringWrath();
                }
            }

            @Override
            public float addEnergyGainPerTick(float energyGainPerTick) {
                return energyGainPerTick + energyPerSecond / 20f;
            }
        });

        return true;
    }

    @Override
    public AbstractUpgradeBranch<?> getUpgradeBranch(AbilityTree abilityTree) {
        return new AvengersWrathBranch(abilityTree, this);
    }

    public void addPlayersStruckDuringWrath() {
        playersStruckDuringWrath++;
    }

    public void addExtraPlayersStruck() {
        extraPlayersStruck++;
    }

    public void addPlayersKilledDuringWrath() {
        playersKilledDuringWrath++;
    }

    public int getPlayersStruckDuringWrath() {
        return playersStruckDuringWrath;
    }

    public int getPlayersKilledDuringWrath() {
        return playersKilledDuringWrath;
    }

    public float getEnergyPerSecond() {
        return energyPerSecond;
    }

    public void setEnergyPerSecond(float energyPerSecond) {
        this.energyPerSecond = energyPerSecond;
    }


    public int getMaxTargets() {
        return maxTargets;
    }

    public void setMaxTargets(int maxTargets) {
        this.maxTargets = maxTargets;
    }

    public int getHitRadius() {
        return hitRadius;
    }

    public void setHitRadius(int hitRadius) {
        this.hitRadius = hitRadius;
    }

    @Override
    public int getTickDuration() {
        return tickDuration;
    }

    @Override
    public void setTickDuration(int tickDuration) {
        this.tickDuration = tickDuration;
    }
}
