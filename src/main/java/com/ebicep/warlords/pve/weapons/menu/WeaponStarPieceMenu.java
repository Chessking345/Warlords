package com.ebicep.warlords.pve.weapons.menu;

import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer;
import com.ebicep.warlords.database.repositories.player.pojos.pve.DatabasePlayerPvE;
import com.ebicep.warlords.menu.Menu;
import com.ebicep.warlords.pve.rewards.Currencies;
import com.ebicep.warlords.pve.weapons.AbstractTierOneWeapon;
import com.ebicep.warlords.pve.weapons.WeaponsPvE;
import com.ebicep.warlords.util.bukkit.ItemBuilder;
import com.ebicep.warlords.util.bukkit.TextComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class WeaponStarPieceMenu {

    public static void openWeaponStarPieceMenu(Player player, AbstractTierOneWeapon weapon) {
        DatabasePlayer databasePlayer = DatabaseManager.playerService.findByUUID(player.getUniqueId());
        DatabasePlayerPvE databasePlayerPvE = databasePlayer.getPveStats();

        Menu menu = new Menu("Confirm Star Piece Application", 9 * 3);

        menu.setItem(2, 1,
                new ItemBuilder(Material.STAINED_CLAY, 1, (short) 13)
                        .name(ChatColor.GREEN + "Confirm")
                        .lore(
                                ChatColor.GRAY + "Apply a star piece to your weapon.",
                                ChatColor.GRAY + "This will override any previous star piece.",
                                "",
                                ChatColor.GRAY + "Cost: " + ChatColor.WHITE + weapon.getStarPieceBonusCost() + " synthetic shards",
                                "",
                                ChatColor.RED + "WARNING: " + ChatColor.GRAY + "This action cannot be undone."
                        )
                        .get(),
                (m, e) -> {
                    if (databasePlayerPvE.getCurrencyValue(Currencies.SYNTHETIC_SHARD) < weapon.getStarPieceBonusCost()) {
                        player.sendMessage(ChatColor.RED + "You do not have enough synthetic shards to apply this star piece.");
                    } else {
                        TextComponent weaponBefore = new TextComponentBuilder(weapon.getName())
                                .setHoverItem(weapon.generateItemStack())
                                .getTextComponent();
                        databasePlayerPvE.subtractOneCurrency(WeaponsPvE.getWeapon(weapon).starPieceCurrency);
                        databasePlayerPvE.subtractCurrency(Currencies.SYNTHETIC_SHARD, weapon.getStarPieceBonusCost());
                        weapon.setStarPieceBonus();

                        TextComponent weaponAfter = new TextComponentBuilder(weapon.getName())
                                .setHoverItem(weapon.generateItemStack())
                                .getTextComponent();

                        player.spigot().sendMessage(
                                new TextComponent(ChatColor.GREEN + "You applied a star piece onto "),
                                weaponBefore,
                                new TextComponent(ChatColor.GREEN + " and it became "),
                                weaponAfter,
                                new TextComponent(ChatColor.GREEN + "!")
                        );

                        DatabaseManager.queueUpdatePlayerAsync(databasePlayer);
                        WeaponManagerMenu.openWeaponEditor(player, weapon);
                    }
                }
        );

        menu.setItem(4, 1,
                weapon.generateItemStack(),
                (m, e) -> {
                }
        );

        menu.setItem(6, 1,
                new ItemBuilder(Material.STAINED_CLAY, 1, (short) 14)
                        .name(ChatColor.RED + "Deny")
                        .lore(ChatColor.GRAY + "Go back.")
                        .get(),
                (m, e) -> WeaponManagerMenu.openWeaponEditor(player, weapon)
        );

        menu.openForPlayer(player);
    }
}