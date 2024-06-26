package com.ebicep.warlords.pve.mobs.pigzombie;

import com.ebicep.warlords.abilities.internal.AbstractAbility;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.List;

public class PigZombieHealing extends AbstractAbility {

    private final float hitbox;

    public PigZombieHealing(float heal, float hitbox) {
        super("Zombifaction", heal, heal, 3, 100);
        this.hitbox = hitbox;
    }

    @Override
    public void updateDescription(Player player) {

    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        return null;
    }

    @Override
    public boolean onActivate(@Nonnull WarlordsEntity wp) {


        Location location = wp.getLocation();
        Utils.playGlobalSound(location, Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1, 0.5f);
        Utils.playGlobalSound(location, "paladin.holyradiance.activation", 0.8f, 0.6f);
        EffectUtils.playCylinderAnimation(location, 6, Particle.FIREWORKS_SPARK, 1);
        for (WarlordsEntity ally : PlayerFilter
                .entitiesAround(wp, hitbox, hitbox, hitbox)
                .aliveTeammatesOfExcludingSelf(wp)
        ) {
            ally.addHealingInstance(
                    wp,
                    name,
                    minDamageHeal,
                    maxDamageHeal,
                    critChance,
                    critMultiplier
            );
        }
        return true;
    }
}
