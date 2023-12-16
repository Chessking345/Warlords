package com.ebicep.warlords.pve.upgrades.warrior;

import com.ebicep.warlords.abilities.internal.AbstractSeismicWave;
import com.ebicep.warlords.pve.upgrades.*;

public class SeismicWaveBranch extends AbstractUpgradeBranch<AbstractSeismicWave> {

    float minDamage = ability.getMinDamageHeal();
    float maxDamage = ability.getMaxDamageHeal();

    public SeismicWaveBranch(AbilityTree abilityTree, AbstractSeismicWave ability) {
        super(abilityTree, ability);

        UpgradeTreeBuilder
                .create(abilityTree, this)
                .addUpgrade(new UpgradeTypes.DamageUpgradeType() {
                    @Override
                    public void run(float value) {
                        float v = 1 + value / 100;
                        ability.setMinDamageHeal(minDamage * v);
                        ability.setMaxDamageHeal(maxDamage * v);
                    }
                }, 5f)
                .addTo(treeA);

        UpgradeTreeBuilder
                .create(abilityTree, this)
                .addUpgradeCooldown(ability, 0.0375f)
                .addTo(treeB);

        masterUpgrade = new Upgrade(
                "Seismic Smash",
                "Seismic Wave - Master Upgrade",
                "Increase the size of Seismic Wave by 150% and deal increased damage the further away the enemy is. (Max 1.5x at 15 blocks).",
                50000,
                () -> {

                    ability.setWaveLength((int) (ability.getWaveLength() * 2.5f));
                    ability.setWaveWidth((int) (ability.getWaveWidth() * 2.5f));
                }
        );
        masterUpgrade2 = new Upgrade(
                "Wild Wave",
                "Seismic Wave - Master Upgrade",
                """
                        Increase the size of Seismic Wave by 150%. Enemies hit will be stunned for 1s. Enemies that are WOUNDED will take 30% more damage.
                        """,
                50000,
                () -> {
                    ability.setWaveLength((int) (ability.getWaveLength() * 2.5f));
                    ability.setWaveWidth((int) (ability.getWaveWidth() * 2.5f));
                }
        );
    }
}
