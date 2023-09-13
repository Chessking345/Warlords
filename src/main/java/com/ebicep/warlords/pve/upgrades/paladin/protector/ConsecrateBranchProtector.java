package com.ebicep.warlords.pve.upgrades.paladin.protector;

import com.ebicep.warlords.abilities.ConsecrateProtector;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.Upgrade;
import com.ebicep.warlords.pve.upgrades.paladin.AbstractConsecrateBranch;

public class ConsecrateBranchProtector extends AbstractConsecrateBranch<ConsecrateProtector> {

    public ConsecrateBranchProtector(AbilityTree abilityTree, ConsecrateProtector ability) {
        super(abilityTree, ability);

        masterUpgrade2 = new Upgrade(
                "Sanctifying Ring",
                "Consecrate - Master Upgrade",
                """
                        Transform consecrate into a mobile ring that travels with you, damaging nearby enemies every 1.5s in a 4 block radius.
                        Enemies hit by Sanctifying Ring will heal the you for 15% of the damage dealt.
                        """,
                50000,
                () -> {

                }
        );
    }

}
