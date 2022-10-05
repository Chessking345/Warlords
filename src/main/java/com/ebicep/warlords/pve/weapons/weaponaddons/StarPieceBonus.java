package com.ebicep.warlords.pve.weapons.weaponaddons;

import com.ebicep.warlords.pve.Currencies;
import com.ebicep.warlords.pve.weapons.WeaponStats;
import com.ebicep.warlords.util.java.NumberFormat;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.ebicep.warlords.util.java.Utils.generateRandomIndexFromListSize;

public interface StarPieceBonus {

    default WeaponStats generateRandomStatBonus() {
        List<WeaponStats> randomStatBonus = getRandomStatBonus();
        return randomStatBonus.get(generateRandomIndexFromListSize(randomStatBonus.size()));
    }

    default List<WeaponStats> getRandomStatBonus() {
        return Arrays.asList(
                WeaponStats.MELEE_DAMAGE,
                WeaponStats.CRIT_CHANCE,
                WeaponStats.CRIT_MULTIPLIER,
                WeaponStats.HEALTH_BONUS
        );
    }

    default String getStarPieceBonusString() {
        return ChatColor.WHITE + " (+" + getStarPieceBonusValue() + "%)";
    }

    int getStarPieceBonusValue();

    default String getStarPieceBonusMultiplicativeString(double stat) {
        //return NumberFormat.formatOptionalHundredths(stat) + " > " +  NumberFormat.formatOptionalHundredths(stat * getStarPieceBonusMultiplicativeValue());
        return NumberFormat.formatOptionalHundredths(stat * getStarPieceBonusMultiplicativeValue());
    }

    default float getStarPieceBonusMultiplicativeValue() {
        return 1 + getStarPieceBonusValue() / 100.0f;
    }

    default List<String> getStarPieceCostLore(Currencies starPieceCurrency) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.AQUA + "Cost: ");
        lore.add(ChatColor.GRAY + " - " + Currencies.COIN.getCostColoredName(getStarPieceBonusCost()));
        lore.add(ChatColor.GRAY + " - " + starPieceCurrency.getCostColoredName(1));
        return lore;
    }

    default int getStarPieceBonusCost() {
        return 10000;
    }

}
