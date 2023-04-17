package com.ebicep.warlords.pve.weapons.weapontypes.legendaries.titles;

import com.ebicep.warlords.abilties.Earthliving;
import com.ebicep.warlords.abilties.Windfury;
import com.ebicep.warlords.abilties.internal.AbstractAbility;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.option.pve.PveOption;
import com.ebicep.warlords.player.ingame.WarlordsPlayer;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownManager;
import com.ebicep.warlords.pve.weapons.weapontypes.legendaries.AbstractLegendaryWeapon;
import com.ebicep.warlords.pve.weapons.weapontypes.legendaries.LegendaryTitles;
import com.ebicep.warlords.pve.weapons.weapontypes.legendaries.PassiveCounter;
import com.ebicep.warlords.util.java.Pair;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.springframework.data.annotation.Transient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LegendaryVorpal extends AbstractLegendaryWeapon implements PassiveCounter {

    private static final int MELEE_DAMAGE_BOOST = 20;
    private static final float MELEE_DAMAGE_BOOST_PER_UPGRADE = 7.5f;
    private static final int PROC_CHANCE_INCREASE = 5;
    private static final float PROC_CHANCE_INCREASE_PER_UPGRADE = 2.5f;

    @Transient
    private int meleeCounter = 0;


    public LegendaryVorpal() {
    }

    public LegendaryVorpal(UUID uuid) {
        super(uuid);
    }

    public LegendaryVorpal(AbstractLegendaryWeapon legendaryWeapon) {
        super(legendaryWeapon);
    }

    @Override
    public String getPassiveEffect() {
        return "Every 5th melee hit deals 7x damage, bypassing damage reduction. When any of Windfury, Earthliving, and Soulbinding Weapon are active, increase the player’s melee damage by " +
                formatTitleUpgrade(MELEE_DAMAGE_BOOST + MELEE_DAMAGE_BOOST_PER_UPGRADE * getTitleLevel(), "%") + " and proc chance by " +
                formatTitleUpgrade(PROC_CHANCE_INCREASE + PROC_CHANCE_INCREASE_PER_UPGRADE * getTitleLevel(), "%") + ".";
    }

    @Override
    public List<Pair<String, String>> getPassiveEffectUpgrade() {
        return Arrays.asList(new Pair<>(
                        formatTitleUpgrade(MELEE_DAMAGE_BOOST + MELEE_DAMAGE_BOOST_PER_UPGRADE * getTitleLevel(), "%"),
                        formatTitleUpgrade(MELEE_DAMAGE_BOOST + MELEE_DAMAGE_BOOST_PER_UPGRADE * getTitleLevelUpgraded(), "%")
                ),
                new Pair<>(
                        formatTitleUpgrade(PROC_CHANCE_INCREASE + PROC_CHANCE_INCREASE_PER_UPGRADE * getTitleLevel(), "%"),
                        formatTitleUpgrade(PROC_CHANCE_INCREASE + PROC_CHANCE_INCREASE_PER_UPGRADE * getTitleLevelUpgraded(), "%")
                )
        );
    }

    @Override
    protected float getMeleeDamageMaxValue() {
        return 220;
    }

    @Override
    public void applyToWarlordsPlayer(WarlordsPlayer player, PveOption pveOption) {
        super.applyToWarlordsPlayer(player, pveOption);
        this.meleeCounter = 0;

        float meleeDamageBoost = 1 + (MELEE_DAMAGE_BOOST + MELEE_DAMAGE_BOOST_PER_UPGRADE * getTitleLevel()) / 100;
        float procChanceIncrease = PROC_CHANCE_INCREASE + PROC_CHANCE_INCREASE_PER_UPGRADE * getTitleLevel();

        for (AbstractAbility ability : player.getSpec().getAbilities()) {
            if (ability instanceof Windfury) {
                Windfury windfury = (Windfury) ability;
                windfury.setProcChance(windfury.getProcChance() + procChanceIncrease);
            } else if (ability instanceof Earthliving) {
                Earthliving earthliving = (Earthliving) ability;
                earthliving.setProcChance(earthliving.getProcChance() + procChanceIncrease);
            }
        }

        player.getGame().registerEvents(new Listener() {

            @EventHandler
            public void onEvent(WarlordsDamageHealingEvent event) {
                if (event.getAttacker() != player || event.getWarlordsEntity() == player) {
                    return;
                }

                String ability = event.getAbility();
                CooldownManager cooldownManager = player.getCooldownManager();
                if (cooldownManager.hasCooldownFromName("Windfury Weapon") && ability.equals("Windfury Weapon")) {
                    event.setMin(event.getMin() * meleeDamageBoost);
                    event.setMax(event.getMax() * meleeDamageBoost);
                    return;
                }
                if (cooldownManager.hasCooldownFromName("Earthliving Weapon") && ability.equals("Earthliving Weapon")) {
                    event.setMin(event.getMin() * meleeDamageBoost);
                    event.setMax(event.getMax() * meleeDamageBoost);
                    return;
                }
                if (!ability.isEmpty()) {
                    return;
                }
                meleeCounter++;
                updateItemCounter(player);
                if (cooldownManager.hasCooldownFromName("Windfury Weapon") ||
                        cooldownManager.hasCooldownFromName("Earthliving Weapon") ||
                        cooldownManager.hasCooldownFromName("Soulbinding Weapon")
                ) {
                    event.setMin(event.getMin() * meleeDamageBoost);
                    event.setMax(event.getMax() * meleeDamageBoost);
                }
                if (meleeCounter % 5 == 0) {
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 1, 2);
                    event.setMin(event.getMin() * 7);
                    event.setMax(event.getMax() * 7);
                    event.setIgnoreReduction(true);
                }
            }
        });
    }

    @Override
    public LegendaryTitles getTitle() {
        return LegendaryTitles.VORPAL;
    }

    @Override
    protected float getMeleeDamageMinValue() {
        return 200;
    }

    @Override
    protected float getCritChanceValue() {
        return 35;
    }

    @Override
    protected float getCritMultiplierValue() {
        return 200;
    }

    @Override
    protected float getHealthBonusValue() {
        return 300;
    }

    @Override
    protected float getSpeedBonusValue() {
        return 14;
    }

    @Override
    protected float getEnergyPerSecondBonusValue() {
        return -3;
    }

    @Override
    protected float getEnergyPerHitBonusValue() {
        return 4;
    }

    @Override
    public int getCounter() {
        return (meleeCounter % 5);
    }

    @Override
    public boolean constantlyUpdate() {
        return false;
    }
}
