package com.ebicep.warlords.abilties;

import com.ebicep.warlords.abilties.internal.AbstractLightInfusionBase;
import com.ebicep.warlords.effects.ParticleEffect;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Collections;

public class LightInfusionCrusader extends AbstractLightInfusionBase {

    private int speedBuff = 40;
    private int energyGiven = 120;

    public LightInfusionCrusader(float cooldown) {
        super(cooldown);
    }

    @Override
    public boolean onActivate(@Nonnull WarlordsEntity wp, @Nonnull Player player) {
        wp.addEnergy(wp, name, energyGiven);
        Utils.playGlobalSound(player.getLocation(), "paladin.infusionoflight.activation", 2, 1);

        Runnable cancelSpeed = wp.addSpeedModifier(wp, "Infusion", speedBuff, tickDuration, "BASE");

        LightInfusionCrusader tempLightInfusion = new LightInfusionCrusader(cooldown);
        wp.getCooldownManager().addRegularCooldown(
                name,
                "INF",
                LightInfusionCrusader.class,
                tempLightInfusion,
                wp,
                CooldownTypes.ABILITY,
                cooldownManager -> {
                },
                cooldownManager -> {
                    cancelSpeed.run();
                },
                tickDuration,
                Collections.singletonList((cooldown, ticksLeft, ticksElapsed) -> {
                    if (ticksElapsed % 4 == 0) {
                        ParticleEffect.SPELL.display(
                                0.3f,
                                0.1f,
                                0.3f,
                                0.2f,
                                2,
                                wp.getLocation().add(0, 1.2, 0),
                                500
                        );
                    }
                })
        );

        if (pveUpgrade) {
            for (WarlordsEntity infusionTarget : PlayerFilter
                    .entitiesAround(wp, 6, 6, 6)
                    .aliveTeammatesOfExcludingSelf(wp)
            ) {
                infusionTarget.addEnergy(wp, name, energyGiven / 2f);
                infusionTarget.getCooldownManager().addRegularCooldown(
                        name,
                        "INF",
                        LightInfusionCrusader.class,
                        tempLightInfusion,
                        wp,
                        CooldownTypes.ABILITY,
                        cooldownManager -> {
                        },
                        cooldownManager -> {
                            cancelSpeed.run();
                        },
                        tickDuration,
                        Collections.singletonList((cooldown, ticksLeft, ticksElapsed) -> {
                            if (ticksElapsed % 4 == 0) {
                                ParticleEffect.SPELL.display(
                                        0.3f,
                                        0.1f,
                                        0.3f,
                                        0.2f,
                                        2,
                                        wp.getLocation().add(0, 1.2, 0),
                                        500
                                );
                            }
                        })
                );
            }
        }

        for (int i = 0; i < 10; i++) {
            ParticleEffect.SPELL.display(
                    1,
                    0,
                    1,
                    0.3f,
                    3,
                    wp.getLocation().add(0, 1.5, 0),
                    500
            );
        }

        return true;
    }

    public int getSpeedBuff() {
        return speedBuff;
    }

    public void setSpeedBuff(int speedBuff) {
        this.speedBuff = speedBuff;
    }

    public int getEnergyGiven() {
        return energyGiven;
    }

    public void setEnergyGiven(int energyGiven) {
        this.energyGiven = energyGiven;
    }


}
