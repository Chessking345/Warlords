package com.ebicep.warlords.pve.upgrades.rogue.vindicator;

import com.ebicep.warlords.abilties.RighteousStrike;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.AbstractUpgradeBranch;
import com.ebicep.warlords.pve.upgrades.Upgrade;

public class RighteousStrikeBranch extends AbstractUpgradeBranch<RighteousStrike> {

    float minDamage = ability.getMinDamageHeal();
    float maxDamage = ability.getMaxDamageHeal();
    float energyCost = ability.getEnergyCost();

    public RighteousStrikeBranch(AbilityTree abilityTree, RighteousStrike ability) {
        super(abilityTree, ability);

        treeA.add(new Upgrade(
                "Impair - Tier I",
                "+7.5% Damage",
                5000,
                () -> {
                    ability.setMinDamageHeal(minDamage * 1.075f);
                    ability.setMaxDamageHeal(maxDamage * 1.075f);
                }
        ));
        treeA.add(new Upgrade(
                "Impair - Tier II",
                "+15% Damage",
                10000,
                () -> {
                    ability.setMinDamageHeal(minDamage * 1.15f);
                    ability.setMaxDamageHeal(maxDamage * 1.15f);
                }
        ));
        treeA.add(new Upgrade(
                "Impair - Tier III",
                "+22.5% Damage",
                15000,
                () -> {
                    ability.setMinDamageHeal(minDamage * 1.225f);
                    ability.setMaxDamageHeal(maxDamage * 1.225f);
                }
        ));
        treeA.add(new Upgrade(
                "Impair - Tier IV",
                "+30% Damage",
                20000,
                () -> {
                    ability.setMinDamageHeal(minDamage * 1.3f);
                    ability.setMaxDamageHeal(maxDamage * 1.3f);
                }
        ));

        treeB.add(new Upgrade(
                "Spark - Tier I",
                "-2.5 Energy cost",
                5000,
                () -> {
                    ability.setEnergyCost(energyCost - 2.5f);
                }
        ));
        treeB.add(new Upgrade(
                "Spark - Tier II",
                "-5 Energy cost",
                10000,
                () -> {
                    ability.setEnergyCost(energyCost - 5);
                }
        ));
        treeB.add(new Upgrade(
                "Spark - Tier III",
                "-7.5 Energy cost",
                15000,
                () -> {
                    ability.setEnergyCost(energyCost - 7.5f);
                }
        ));
        treeB.add(new Upgrade(
                "Spark - Tier IV",
                "-10 Energy cost",
                20000,
                () -> {
                    ability.setEnergyCost(energyCost - 10);
                }
        ));


        masterUpgrade = new Upgrade(
                "Righteous Slash",
                "Righteous Strike - Master Upgrade",
                "Righteous Strike hits 2 additional enemies\nand knocks all hit targets back slightly.",
                50000,
                () -> {
                    ability.setPveUpgrade(true);
                }
        );
    }
}