package com.ebicep.warlords.pve.upgrades.rogue.assassin;

import com.ebicep.warlords.abilities.IncendiaryCurse;
import com.ebicep.warlords.pve.upgrades.*;

public class IncendiaryCurseBranch extends AbstractUpgradeBranch<IncendiaryCurse> {

    float minDamage = ability.getMinDamageHeal();
    float maxDamage = ability.getMaxDamageHeal();
    float hitbox = ability.getHitbox();

    public IncendiaryCurseBranch(AbilityTree abilityTree, IncendiaryCurse ability) {
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
                }, 7.5f)
                .addTo(treeA);

        UpgradeTreeBuilder
                .create(abilityTree, this)
                .addUpgradeCooldown(ability)
                .addUpgrade(
                        new UpgradeTypes.UpgradeType() {
                            @Override
                            public String getDescription0(String value) {
                                return "+" + value + " Blocks hit radius";
                            }

                            @Override
                            public void run(float value) {
                                ability.setHitbox(hitbox + value);
                            }
                        },
                        1
                )
                .addTo(treeB);

        masterUpgrade = new Upgrade(
                "Blazing Curse",
                "Incendiary Curse - Master Upgrade",
                "All enemies hit become disoriented. Increase the damage they take by 50% for 5 seconds.",
                50000,
                () -> {

                }
        );
        masterUpgrade2 = new Upgrade(
                "Unforseen Curse",
                "Incendiary Curse - Master Upgrade",
                """
                        Increase the Blindness duration by 2s. Additionally, every enemy blinded by Incendiary Curse gives 5 energy.
                        """,
                50000,
                () -> {
                    ability.setBlindDurationInTicks(ability.getBlindDurationInTicks() + 40);
                }
        );
    }
}
