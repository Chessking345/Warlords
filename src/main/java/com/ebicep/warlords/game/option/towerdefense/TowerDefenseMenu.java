package com.ebicep.warlords.game.option.towerdefense;

import com.ebicep.warlords.game.option.towerdefense.attributes.upgradeable.Upgradeable;
import com.ebicep.warlords.game.option.towerdefense.events.TowerSellEvent;
import com.ebicep.warlords.game.option.towerdefense.mobs.TowerDefenseMobInfo;
import com.ebicep.warlords.game.option.towerdefense.towers.AbstractTower;
import com.ebicep.warlords.game.option.towerdefense.waves.FixedPlayerWave;
import com.ebicep.warlords.game.option.towerdefense.waves.TowerDefenseSpawnWaveAction;
import com.ebicep.warlords.game.option.towerdefense.waves.WaveAction;
import com.ebicep.warlords.menu.Menu;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.pve.mobs.Mob;
import com.ebicep.warlords.util.bukkit.ComponentBuilder;
import com.ebicep.warlords.util.bukkit.ItemBuilder;
import com.ebicep.warlords.util.java.NumberFormat;
import com.ebicep.warlords.util.java.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TowerDefenseMenu {

    public static final String SUMMON_MENU_TITLE = "Summon Troops";

    public static final MobGroup[] TOWER_DEFENSE_MOB_GROUPS = {
            new MobGroup(new ItemBuilder(Material.ZOMBIE_HEAD)
                    .name(Component.text("Zombie", NamedTextColor.GREEN))
                    .get(),
                    TowerDefenseMobInfo.ZOMBIE
            ),
            new MobGroup(new ItemBuilder(Material.SKELETON_SKULL)
                    .name(Component.text("Skeleton", NamedTextColor.GREEN))
                    .get(),
                    TowerDefenseMobInfo.SKELETON
            ),
    };

    public static void openBuildMenu(Player player, AbstractTower tower) {
        Pair<Integer, Integer> ownerHeadPos = new Pair<>(2, 2);
        Pair<Integer, Integer> sellPos = new Pair<>(6, 3);

        Menu menu = new Menu(tower.getName(), 9 * 6);

        if (tower instanceof Upgradeable upgradeable) {
            ownerHeadPos.setA(7);
            ownerHeadPos.setB(1);
            sellPos.setA(7);
            sellPos.setB(3);
            upgradeable.addToMenu(menu, player, (AbstractTower & Upgradeable) tower);
        }

        menu.setItem(ownerHeadPos.getA(), ownerHeadPos.getB(),
                new ItemBuilder(Material.PLAYER_HEAD) //HeadUtils.getHead(tower.getOwner()) TODO
                                                      .get(),
                (m, e) -> {

                }
        );
        menu.setItem(sellPos.getA(), sellPos.getB(),
                new ItemBuilder(Material.RED_CONCRETE)
                        .name(Component.text("Sell", NamedTextColor.RED))
                        .get(),
                (m, e) -> {
                    Bukkit.getPluginManager().callEvent(new TowerSellEvent(tower));
                    player.closeInventory();
                }
        );
        menu.setItem(4, 5, Menu.MENU_CLOSE, Menu.ACTION_CLOSE_MENU);
        menu.openForPlayer(player);
    }

    public static void openMarket(Player player, WarlordsEntity warlordsEntity, TowerDefensePlayerInfo playerInfo) {
        Menu menu = new Menu("Market", 9 * 6);

        menu.setItem(4, 0,
                new ItemBuilder(Material.BOOK)
                        .name(Component.text("INFO HERE"))
                        .get(),
                (m, e) -> {

                }
        );

        // TODO buying special effects

        menu.setItem(4, 5, Menu.MENU_CLOSE, Menu.ACTION_CLOSE_MENU);
        menu.openForPlayer(player);
    }

    public static void openSummonTroopsMenu(Player player, WarlordsEntity warlordsEntity, TowerDefenseSpawner spawner, TowerDefensePlayerInfo playerInfo) {
        Menu menu = new Menu(SUMMON_MENU_TITLE, 9 * 6);

        FixedPlayerWave playerWave = playerInfo.getPlayerWave();
        List<WaveAction<TowerDefenseOption>> actions = playerWave.getActions();
        int lastSpawnIndex = playerWave.getWaveActionIndex();

        int x = 1;
        int y = 2;
        for (int i = 0; i < TOWER_DEFENSE_MOB_GROUPS.length; i++) {
            MobGroup mobGroup = TOWER_DEFENSE_MOB_GROUPS[i];
            TowerDefenseMobInfo mobInfos = mobGroup.mobsWithCost;
            Mob mob = mobInfos.getMob();
            int cost = mobInfos.getCost();
            int expReward = mobInfos.getIncomeModifier();
            int spawnDelay = mobInfos.getSpawnDelay();
            menu.setItem(x, y,
                    new ItemBuilder(mobGroup.head)
                            .lore(
                                    Component.text("MOB DESCRIPTION"),
                                    Component.empty(),
                                    ComponentBuilder.create("Summon Cost: ")
                                                    .text("❂ " + cost, NamedTextColor.GOLD)
                                                    .build(),
                                    ComponentBuilder.create("Health: ")
                                                    .text(NumberFormat.formatOptionalTenths(mob.maxHealth), NamedTextColor.GREEN)
                                                    .build(),
                                    ComponentBuilder.create("Damage: ")
                                                    .text(NumberFormat.formatOptionalTenths(mob.minMeleeDamage) + " - " + NumberFormat.formatOptionalTenths(mob.maxMeleeDamage),
                                                            NamedTextColor.RED
                                                    )
                                                    .build(),
                                    ComponentBuilder.create("Speed: ")
                                                    .text(NumberFormat.formatOptionalHundredths(mob.walkSpeed), NamedTextColor.WHITE)
                                                    .build(),
                                    ComponentBuilder.create("Income: ")
                                                    .text(NumberFormat.formatOptionalTenths(expReward), NamedTextColor.DARK_AQUA)
                                                    .build(),
                                    Component.empty(),
                                    ComponentBuilder.create()
                                                    .text("LEFT-CLICK ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                                                    .text("to add to queue")
                                                    .build(),
                                    ComponentBuilder.create()
                                                    .text("SHIFT-CLICK ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                                                    .text("to fill queue")
                                                    .build()
                            )
                            .get(),
                    (m, e) -> {
                        if (e.isLeftClick()) {
                            if (TowerDefenseSpawner.MAX_PLAYER_SPAWN_AMOUNT + 1 == actions.size() / 2 - lastSpawnIndex / 2) {
                                player.sendMessage(Component.text("You have reached the maximum amount of mobs you can spawn at a time!", NamedTextColor.RED));
                                return;
                            }
                            int amountSpawned = playerWave.add(mob, spawnDelay, e.isShiftClick(), warlordsEntity);
                            if (amountSpawned > 0) {
                                playerInfo.addIncomeRate(expReward * amountSpawned);
                                warlordsEntity.sendMessage(ComponentBuilder.create(amountSpawned + "x ", NamedTextColor.WHITE)
                                                                           .text(mob.name, NamedTextColor.GREEN)
                                                                           .text(" spawned (", NamedTextColor.GRAY)
                                                                           .text("+" + (expReward * amountSpawned) + " income", NamedTextColor.DARK_AQUA)
                                                                           .text(")", NamedTextColor.GRAY)
                                                                           .build()
                                );
                            }
                            openSummonTroopsMenu(player, warlordsEntity, spawner, playerInfo);
                        }
                    }
            );
            if (i == 4) {
                x = 1;
                y++;
            }
            x++;
        }

        x = 2;
        for (int i = lastSpawnIndex; i < actions.size(); i++) {
            WaveAction<TowerDefenseOption> waveAction = actions.get(i);
            if (!(waveAction instanceof TowerDefenseSpawnWaveAction spawnAction)) {
                continue;
            }
            Mob mob = spawnAction.getMob();
            menu.setItem(x, 1,
                    new ItemBuilder(mob.getHead())
                            .name(Component.text(mob.name, NamedTextColor.GREEN))
                            .get(),
                    (m, e) -> {
                    }
            );

            x++;
        }

        menu.setItem(4, 5, Menu.MENU_CLOSE, Menu.ACTION_CLOSE_MENU);
        menu.openForPlayer(player);
    }

    record MobGroup(ItemStack head, TowerDefenseMobInfo mobsWithCost) {
    }

}
