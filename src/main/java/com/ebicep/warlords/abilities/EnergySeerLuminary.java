package com.ebicep.warlords.abilities;

import com.ebicep.warlords.abilities.internal.AbstractEnergySeer;
import com.ebicep.warlords.abilities.internal.icon.PurpleAbilityIcon;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.AbstractUpgradeBranch;
import com.ebicep.warlords.pve.upgrades.arcanist.luminary.EnergySeerBranchLuminary;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class EnergySeerLuminary extends AbstractEnergySeer<EnergySeerLuminary> implements PurpleAbilityIcon {

    private int healingIncrease = 20;

    @Override
    public Component getBonus() {
        return Component.text("increase your healing by ")
                        .append(Component.text(healingIncrease + "%", NamedTextColor.GREEN));
    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        List<Pair<String, String>> info = new ArrayList<>();
        info.add(new Pair<>("Times Used", "" + timesUsed));
        return info;
    }

    @Override
    public Class<EnergySeerLuminary> getEnergySeerClass() {
        return EnergySeerLuminary.class;
    }

    @Override
    public EnergySeerLuminary getObject() {
        return new EnergySeerLuminary();
    }

    @Override
    public RegularCooldown<EnergySeerLuminary> getBonusCooldown(@Nonnull WarlordsEntity wp) {
        return new RegularCooldown<>(
                name,
                "SEER",
                getEnergySeerClass(),
                getObject(),
                wp,
                CooldownTypes.ABILITY,
                cooldownManager -> {

                },
                bonusDuration
        ) {
            @Override
            public float modifyHealingFromSelf(WarlordsDamageHealingEvent event, float currentHealValue) {
                return healingIncrease * convertToMultiplicationDecimal(healingIncrease);
            }
        };
    }

    @Override
    protected void onEnd(WarlordsEntity wp, EnergySeerLuminary cooldownObject) {
        if (pveMasterUpgrade2) {
            PlayerFilter.entitiesAround(wp, 10, 10, 10)
                        .aliveTeammatesOfExcludingSelf(wp)
                        .forEach(warlordsEntity -> {
                            MercifulHex.giveMercifulHex(wp, warlordsEntity);
                            EffectUtils.playParticleLinkAnimation(warlordsEntity.getLocation(), wp.getLocation(), Particle.VILLAGER_HAPPY, 1, 1.25);
                        });
        }
    }

    public int getTickDuration() {
        return tickDuration;
    }

    public void setTickDuration(int tickDuration) {
        this.tickDuration = tickDuration;
    }

    public int getEnergyRestore() {
        return energyRestore;
    }

    public void setEnergyRestore(int energyRestore) {
        this.energyRestore = energyRestore;
    }

    @Override
    public AbstractUpgradeBranch<?> getUpgradeBranch(AbilityTree abilityTree) {
        return new EnergySeerBranchLuminary(abilityTree, this);
    }

    public int getHealingIncrease() {
        return healingIncrease;
    }

    public void setHealingIncrease(int healingIncrease) {
        this.healingIncrease = healingIncrease;
    }
}
