package com.ebicep.warlords.abilities;

import com.ebicep.warlords.abilities.internal.AbstractConsecrate;
import com.ebicep.warlords.effects.circle.CircleEffect;
import com.ebicep.warlords.effects.circle.CircumferenceEffect;
import com.ebicep.warlords.effects.circle.DoubleLineEffect;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.player.ingame.cooldowns.instances.InstanceFlags;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.AbstractUpgradeBranch;
import com.ebicep.warlords.pve.upgrades.paladin.avenger.ConsecrateBranchAvenger;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;

public class ConsecrateAvenger extends AbstractConsecrate {

    public ConsecrateAvenger() {
        super(158.4f, 213.6f, 50, 20, 175, 20, 5);
    }

    public ConsecrateAvenger(
            float minDamageHeal,
            float maxDamageHeal,
            float energyCost,
            float critChance,
            float critMultiplier,
            int strikeDamageBoost,
            float radius,
            Location location
    ) {
        super(minDamageHeal, maxDamageHeal, energyCost, critChance, critMultiplier, strikeDamageBoost, radius, location);
    }

    @Override
    public boolean onActivate(@Nonnull WarlordsEntity wp) {
        if (!pveMasterUpgrade2) {
            return super.onActivate(wp);
        }
        

        Location location = wp.getLocation().clone();

        Utils.playGlobalSound(location, "paladin.consecrate.activation", 2, 1);
        float radius = hitBox.getCalculatedValue();
        CircleEffect circleEffect = new CircleEffect(
                wp.getGame(),
                wp.getTeam(),
                location,
                radius,
                new CircumferenceEffect(Particle.REDSTONE, new Particle.DustOptions(Color.fromRGB(210, 50, 50), 1))
                        .particlesPerCircumference(.25),
                new DoubleLineEffect(Particle.REDSTONE, new Particle.DustOptions(Color.fromRGB(255, 160, 160), 1))
        );

        HashSet<WarlordsEntity> hit = new HashSet<>();
        wp.getCooldownManager().addCooldown(new RegularCooldown<>(
                name,
                null,
                AbstractConsecrate.class,
                createConsecrate(),
                wp,
                CooldownTypes.ABILITY,
                cooldownManager -> {
                },
                cooldownManager -> {
                },
                false,
                tickDuration,
                Collections.singletonList((cooldown, ticksLeft, ticksElapsed) -> {
                    Location updatedLocation = wp.getLocation();
                    circleEffect.setCenter(updatedLocation);
                    circleEffect.playEffects();
                    if (ticksElapsed % 30 == 0) {
                        PlayerFilter.entitiesAround(updatedLocation, radius, 6, radius)
                                    .aliveEnemiesOf(wp)
                                    .forEach(enemy -> {
                                        hit.add(enemy);
                                        playersHit++;
                                        enemy.addDamageInstance(
                                                wp,
                                                name,
                                                minDamageHeal,
                                                maxDamageHeal,
                                                critChance,
                                                critMultiplier
                                        );
                                    });
                    }
                })
        ) {
            @Override
            public float modifyDamageBeforeInterveneFromAttacker(WarlordsDamageHealingEvent event, float currentDamageValue) {
                if (event.getFlags().contains(InstanceFlags.STRIKE_IN_CONS)) {
                    return currentDamageValue;
                }
                if (!hit.contains(event.getWarlordsEntity())) {
                    return currentDamageValue;
                }
                event.getFlags().add(InstanceFlags.STRIKE_IN_CONS);
                addStrikesBoosted();
                return currentDamageValue * convertToMultiplicationDecimal(strikeDamageBoost);
            }
        });
        return true;
    }

    @Nonnull
    @Override
    public AbstractConsecrate createConsecrate() {
        return new ConsecrateAvenger(minDamageHeal, maxDamageHeal, energyCost.getBaseValue(), critChance, critMultiplier, strikeDamageBoost, hitBox.getBaseValue(), location);
    }

    @Nonnull
    @Override
    public String getStrikeName() {
        return "Avenger's Strike";
    }

    @Override
    public AbstractUpgradeBranch<?> getUpgradeBranch(AbilityTree abilityTree) {
        return new ConsecrateBranchAvenger(abilityTree, this);
    }
}
