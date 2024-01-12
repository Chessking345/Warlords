package com.ebicep.warlords.pve.mobs.events.libraryarchives;

import com.ebicep.warlords.abilities.*;
import com.ebicep.warlords.abilities.internal.AbstractAbility;
import com.ebicep.warlords.pve.mobs.Mob;
import org.bukkit.Location;

public class EventInquisiteurVPA extends EventInquisiteur {

    public EventInquisiteurVPA(Location spawnLocation) {
        this(
                spawnLocation,
                "Inquisiteur-VPA",
                135000,
                0.38f,
                25,
                0,
                0,
                new ImpalingStrike() {{
                    this.setLeechDuration(3);
                    this.setLeechAllyAmount(20);
                    this.setLeechSelfAmount(15);
                    this.setPveMasterUpgrade(true);
                }},
                new WaterBreath() {{
                    this.setPveMasterUpgrade2(true);
                }},
                new GroundSlamRevenant(),
                new SanctifiedBeacon() {{
                    this.setPveMasterUpgrade2(true);
                }},
                new HealingTotem() {{
                    this.setPveMasterUpgrade(true);
                }}
        );
    }

    public EventInquisiteurVPA(
            Location spawnLocation,
            String name,
            int maxHealth,
            float walkSpeed,
            int damageResistance,
            float minMeleeDamage,
            float maxMeleeDamage,
            AbstractAbility... abilities
    ) {
        super(spawnLocation, name, maxHealth, walkSpeed, damageResistance, minMeleeDamage, maxMeleeDamage, abilities);
    }

    @Override
    public float getCrackiness() {
        return .20f;
    }

    @Override
    public Mob getMobRegistry() {
        return Mob.EVENT_INQUISITEUR_VPA;
    }

}
