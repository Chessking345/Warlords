package com.ebicep.warlords.pve.items.types.specialitems.buckler.delta;

import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.option.pve.PveOption;
import com.ebicep.warlords.player.ingame.WarlordsPlayer;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.PermanentCooldown;
import com.ebicep.warlords.pve.items.statpool.BasicStatPool;
import com.ebicep.warlords.pve.items.types.AbstractItem;
import com.ebicep.warlords.pve.items.types.specialitems.CraftsInto;
import com.ebicep.warlords.pve.items.types.specialitems.buckler.omega.ElementalShield;
import com.ebicep.warlords.util.warlords.PlayerFilter;

import java.util.Set;

public class BucklerPiece extends SpecialDeltaBuckler implements CraftsInto {

    public BucklerPiece() {
    }

    public BucklerPiece(Set<BasicStatPool> statPool) {
        super(statPool);
    }

    @Override
    public String getDescription() {
        return "Half magma, half ice.";
    }

    @Override
    public String getBonus() {
        return "Healing done to a target will damage nearby enemies around that target for 5% of the healing done.";
    }

    @Override
    public String getName() {
        return "Hazardous Buckler";
    }

    @Override
    public void applyToWarlordsPlayer(WarlordsPlayer warlordsPlayer, PveOption pveOption) {
        warlordsPlayer.getCooldownManager().addCooldown(new PermanentCooldown<>(
                getName(),
                null,
                BucklerPiece.class,
                null,
                warlordsPlayer,
                CooldownTypes.ITEM,
                cooldownManager -> {
                },
                false
        ) {

            @Override
            public void onHealFromAttacker(WarlordsDamageHealingEvent event, float currentDamageValue, boolean isCrit) {
                float damageAmount = currentDamageValue * .1f;
                PlayerFilter.entitiesAround(event.getWarlordsEntity(), 3, 3, 3)
                            .aliveEnemiesOf(warlordsPlayer)
                            .forEach(warlordsEntity -> {
                                warlordsEntity.addDamageInstance(
                                        warlordsEntity,
                                        BucklerPiece.this.getName(),
                                        damageAmount,
                                        damageAmount,
                                        isCrit ? 100 : 0,
                                        100
                                );
                            });
            }
        });
    }


    @Override
    public AbstractItem getCraftsInto(Set<BasicStatPool> statPool) {
        return new ElementalShield(statPool);
    }
}
