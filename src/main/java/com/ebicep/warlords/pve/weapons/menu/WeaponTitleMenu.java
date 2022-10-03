package com.ebicep.warlords.pve.weapons.menu;

import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer;
import com.ebicep.warlords.database.repositories.player.pojos.pve.DatabasePlayerPvE;
import com.ebicep.warlords.menu.Menu;
import com.ebicep.warlords.pve.Currencies;
import com.ebicep.warlords.pve.weapons.AbstractWeapon;
import com.ebicep.warlords.pve.weapons.weapontypes.legendaries.AbstractLegendaryWeapon;
import com.ebicep.warlords.pve.weapons.weapontypes.legendaries.LegendaryTitles;
import com.ebicep.warlords.util.bukkit.ItemBuilder;
import com.ebicep.warlords.util.bukkit.TextComponentBuilder;
import com.ebicep.warlords.util.java.NumberFormat;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ebicep.warlords.menu.Menu.MENU_BACK;
import static com.ebicep.warlords.pve.weapons.menu.WeaponManagerMenu.openWeaponEditor;

public class WeaponTitleMenu {

    public static void openWeaponTitleMenu(Player player, DatabasePlayer databasePlayer, AbstractLegendaryWeapon weapon, int page) {
        Menu menu = new Menu("Apply Title to Weapon", 9 * 5);

        menu.setItem(
                4,
                0,
                weapon.generateItemStack(),
                (m, e) -> {
                }
        );

        for (int i = 0; i < 3; i++) {
            int titleIndex = ((page - 1) * 3) + i;
            if (titleIndex < LegendaryTitles.VALUES.length) {
                LegendaryTitles title = LegendaryTitles.VALUES[titleIndex];
                AbstractLegendaryWeapon titledWeapon = title.titleWeapon.apply(weapon);
                Set<Map.Entry<Currencies, Long>> cost = title.getCost().entrySet();

                ItemBuilder itemBuilder = new ItemBuilder(titledWeapon.generateItemStack())
                        .addLore("", ChatColor.AQUA + "Title Cost: ");
                for (Map.Entry<Currencies, Long> currenciesLongEntry : cost) {
                    itemBuilder.addLore(ChatColor.GRAY + " - " + ChatColor.GREEN + NumberFormat.addCommas(currenciesLongEntry.getValue()) + " " +
                            currenciesLongEntry.getKey().getColoredName() + "s");
                }
                boolean equals = weapon.getClass().equals(title.clazz);
                if (equals) {
                    itemBuilder.enchant(Enchantment.OXYGEN, 1);
                    itemBuilder.flags(ItemFlag.HIDE_ENCHANTS);
                }
                menu.setItem((i % 3) * 3 + 1, 2,
                        itemBuilder.get(),
                        (m, e) -> {
                            if (equals) {
                                player.sendMessage(ChatColor.RED + "You already have this title on your weapon!");
                                return;
                            }
                            DatabasePlayerPvE pveStats = databasePlayer.getPveStats();
                            for (Map.Entry<Currencies, Long> currenciesLongEntry : cost) {
                                if (pveStats.getCurrencyValue(currenciesLongEntry.getKey()) < currenciesLongEntry.getValue()) {
                                    player.sendMessage(ChatColor.RED + "You do not have enough " + currenciesLongEntry.getKey()
                                            .getColoredName() + "s" + ChatColor.RED + " to apply this title!");
                                    return;
                                }
                            }
                            Menu.openConfirmationMenu(
                                    player,
                                    "Apply Title",
                                    3,
                                    Collections.singletonList(ChatColor.GRAY + "Apply " + ChatColor.GREEN + title.title + ChatColor.GRAY + " title"),
                                    Collections.singletonList(ChatColor.GRAY + "Go back"),
                                    (m2, e2) -> {
                                        AbstractLegendaryWeapon newTitledWeapon = titleWeapon(player, databasePlayer, weapon, title);
                                        openWeaponTitleMenu(player, databasePlayer, newTitledWeapon, page);
                                    },
                                    (m2, e2) -> openWeaponTitleMenu(player, databasePlayer, weapon, page),
                                    (m2) -> {
                                    }
                            );
                        }
                );
            }
        }

        if (page - 1 > 0) {
            menu.setItem(0, 4,
                    new ItemBuilder(Material.ARROW)
                            .name(ChatColor.GREEN + "Previous Page")
                            .lore(ChatColor.YELLOW + "Page " + (page - 1))
                            .get(),
                    (m, e) -> openWeaponTitleMenu(player, databasePlayer, weapon, page - 1)
            );
        }
        if (LegendaryTitles.VALUES.length > (page * 3)) {
            menu.setItem(8, 4,
                    new ItemBuilder(Material.ARROW)
                            .name(ChatColor.GREEN + "Next Page")
                            .lore(ChatColor.YELLOW + "Page " + (page + 1))
                            .get(),
                    (m, e) -> openWeaponTitleMenu(player, databasePlayer, weapon, page + 1)
            );
        }

        menu.setItem(4, 4, MENU_BACK, (m, e) -> openWeaponEditor(player, databasePlayer, weapon));
        menu.openForPlayer(player);
    }

    public static AbstractLegendaryWeapon titleWeapon(Player player, DatabasePlayer databasePlayer, AbstractLegendaryWeapon weapon, LegendaryTitles title) {
        AbstractLegendaryWeapon titledWeapon = title.titleWeapon.apply(weapon);
        List<AbstractWeapon> weaponInventory = databasePlayer.getPveStats().getWeaponInventory();
        weaponInventory.remove(weapon);
        weaponInventory.add(titledWeapon);
        DatabaseManager.queueUpdatePlayerAsync(databasePlayer);

        player.spigot().sendMessage(
                new TextComponent(ChatColor.GRAY + "Titled Weapon: "),
                new TextComponentBuilder(weapon.getName())
                        .setHoverItem(weapon.generateItemStack())
                        .getTextComponent(),
                new TextComponent(ChatColor.GRAY + " and it became "),
                new TextComponentBuilder(titledWeapon.getName())
                        .setHoverItem(titledWeapon.generateItemStack())
                        .getTextComponent(),
                new TextComponent(ChatColor.GRAY + "!")
        );

        return titledWeapon;
    }

}
