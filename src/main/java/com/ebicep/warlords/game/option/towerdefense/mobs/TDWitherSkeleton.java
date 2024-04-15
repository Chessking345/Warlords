package com.ebicep.warlords.game.option.towerdefense.mobs;

import com.ebicep.warlords.abilities.internal.AbstractAbility;
import com.ebicep.warlords.pve.mobs.Mob;
import com.ebicep.warlords.pve.mobs.tiers.BasicMob;
import org.bukkit.Location;

public class TDWitherSkeleton extends TowerDefenseMob implements BasicMob {

    public TDWitherSkeleton(Location spawnLocation) {
        this(
                spawnLocation,
                "Wither Skeleton",
                1000,
                .4f,
                0,
                100,
                100
        );
    }

    public TDWitherSkeleton(
            Location spawnLocation,
            String name,
            int maxHealth,
            float walkSpeed,
            float damageResistance,
            float minMeleeDamage,
            float maxMeleeDamage,
            AbstractAbility... abilities
    ) {
        super(spawnLocation, name, maxHealth, walkSpeed, damageResistance, minMeleeDamage, maxMeleeDamage, abilities);
    }

    @Override
    public Mob getMobRegistry() {
        return Mob.TD_WITHER_SKELETON;
    }

}