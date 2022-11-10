package com.ebicep.warlords.pve.weapons.weapontypes.legendaries.titles;

import com.ebicep.warlords.player.ingame.WarlordsPlayer;
import com.ebicep.warlords.pve.weapons.weapontypes.legendaries.AbstractLegendaryWeapon;

import java.util.UUID;

public class LegendaryEnhanced extends AbstractLegendaryWeapon {
    public static final int MELEE_DAMAGE_MIN = 155;
    public static final int MELEE_DAMAGE_MAX = 180;
    public static final int CRIT_CHANCE = 20;
    public static final int CRIT_MULTIPLIER = 180;
    public static final int HEALTH_BONUS = 400;
    public static final int SPEED_BONUS = 8;

    public LegendaryEnhanced() {
    }

    public LegendaryEnhanced(UUID uuid) {
        super(uuid);
    }

    public LegendaryEnhanced(AbstractLegendaryWeapon legendaryWeapon) {
        super(legendaryWeapon);
    }

    @Override
    public String getTitle() {
        return "Enhanced";
    }

    @Override
    public void applyToWarlordsPlayer(WarlordsPlayer player) {
        super.applyToWarlordsPlayer(player);

        //TODO
    }

    @Override
    public String getPassiveEffect() {
        return "Increase the duration of effects caused by primary (right-click) abilities by 2s.";
    }

    @Override
    public void generateStats() {
        this.meleeDamage = MELEE_DAMAGE_MIN;
        this.critChance = CRIT_CHANCE;
        this.critMultiplier = CRIT_MULTIPLIER;
        this.healthBonus = HEALTH_BONUS;
        this.speedBonus = SPEED_BONUS;
    }

    @Override
    public int getMeleeDamageRange() {
        return MELEE_DAMAGE_MAX - MELEE_DAMAGE_MIN;
    }
}
