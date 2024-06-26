package com.ebicep.warlords.pve.upgrades.rogue.assassin;

import com.ebicep.warlords.abilities.JudgementStrike;
import com.ebicep.warlords.pve.upgrades.*;

public class JudgementStrikeBranch extends AbstractUpgradeBranch<JudgementStrike> {

    float minDamage;
    float maxDamage;
    float strikeHeal = ability.getStrikeHeal();

    @Override
    public void runOnce() {
        ability.setMinDamageHeal(ability.getMinDamageHeal() * 1.3f);
        ability.setMaxDamageHeal(ability.getMaxDamageHeal() * 1.3f);
    }

    public JudgementStrikeBranch(AbilityTree abilityTree, JudgementStrike ability) {
        super(abilityTree, ability);
        minDamage = ability.getMinDamageHeal();
        maxDamage = ability.getMaxDamageHeal();
        UpgradeTreeBuilder
                .create(abilityTree, this)
                .addUpgrade(new UpgradeTypes.DamageUpgradeType() {
                    @Override
                    public void run(float value) {
                        float v = 1 + value / 100;
                        ability.setMinDamageHeal(minDamage * v);
                        ability.setMaxDamageHeal(maxDamage * v);
                    }
                }, 15f)
                .addTo(treeA);

        UpgradeTreeBuilder
                .create(abilityTree, this)
                .addUpgrade(new UpgradeTypes.HealingUpgradeType() {

                    @Override
                    public String getDescription0(String value) {
                        return "+" + value + " Healing on Strike Kill";
                    }

                    @Override
                    public void run(float value) {
                        ability.setStrikeHeal(strikeHeal + value);
                    }
                }, 100f)
                .addUpgradeEnergy(ability, 5f)
                .addTo(treeB);

        masterUpgrade = new Upgrade(
                "Death Strike",
                "Judgement Strike - Master Upgrade",
                """
                        +100 Healing on Strike Kill
                        -10 Energy cost
                                                
                        Each strike deals 1% of the target's max health as bonus damage.""",
                50000,
                () -> {
                    ability.setStrikeHeal(ability.getStrikeHeal() + 100);
                    ability.getEnergyCost().addAdditiveModifier("Master Upgrade Branch", -10);
                }
        );
        masterUpgrade2 = new Upgrade(
                "Judgemental Fury",
                "Judgement Strike - Master Upgrade",
                """
                        +45% Crit multiplier
                                                
                        Judgement Strike will now hit twice in one use, the second strike is counted as an additional strike for a guarantee crit"
                        """,
                50000,
                () -> {
                    ability.setCritMultiplier(ability.getCritMultiplier() + 45);
                }
        );
    }
}
