package com.ebicep.warlords.pve.items.menu;

import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer;
import com.ebicep.warlords.database.repositories.player.pojos.pve.DatabasePlayerPvE;
import com.ebicep.warlords.menu.Menu;
import com.ebicep.warlords.pve.Currencies;
import com.ebicep.warlords.pve.PvEUtils;
import com.ebicep.warlords.pve.Spendable;
import com.ebicep.warlords.pve.items.ItemTier;
import com.ebicep.warlords.pve.items.ItemsManager;
import com.ebicep.warlords.pve.items.menu.util.ItemMenuUtil;
import com.ebicep.warlords.pve.items.menu.util.ItemSearchMenu;
import com.ebicep.warlords.pve.items.types.AbstractItem;
import com.ebicep.warlords.pve.mobs.MobDrops;
import com.ebicep.warlords.util.bukkit.ComponentBuilder;
import com.ebicep.warlords.util.bukkit.ItemBuilder;
import com.ebicep.warlords.util.bukkit.WordWrap;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.java.TriConsumer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.ebicep.warlords.menu.Menu.MENU_BACK;

public class ItemCraftingMenu {

    private static final HashMap<ItemTier, TierCostInfo> TIER_COST_INFO = new HashMap<>() {{
        put(ItemTier.DELTA, new TierCostInfo(
                new LinkedHashMap<>() {{
                    put(Currencies.SYNTHETIC_SHARD, 10_000L);
                    put(MobDrops.ZENITH_STAR, 2L);
                }},
                new Pair<>(1, 4),
                new ArrayList<>() {{
                    add(new TierRequirement(ItemTier.ALPHA, 1, 1));
                    add(new TierRequirement(ItemTier.BETA, 1, 2));
                    add(new TierRequirement(ItemTier.GAMMA, 1, 3));
                }}
        ));
        put(ItemTier.OMEGA, new TierCostInfo(
                new LinkedHashMap<>() {{
                    put(Currencies.SYNTHETIC_SHARD, 25_000L);
                    put(Currencies.LEGEND_FRAGMENTS, 5_000L);
                    put(MobDrops.ZENITH_STAR, 10L);
                    put(Currencies.CELESTIAL_BRONZE, 5L);
                }},
                new Pair<>(1, 2),
                new ArrayList<>() {{
                    add(new TierRequirement(ItemTier.DELTA, 1, 1));
                }}
        ));
    }};
    private static final LinkedHashMap<Spendable, Long> CELESTIAL_SMELTERY_COST = new LinkedHashMap<>() {{
        put(Currencies.LEGEND_FRAGMENTS, 5000L);
        put(Currencies.SCRAP_METAL, 100L);
        put(MobDrops.ZENITH_STAR, 3L);
    }};

    public static void openItemCraftingMenu(Player player, DatabasePlayer databasePlayer) {
        Menu menu = new Menu("Ethical Enya", 9 * 4);

        menu.setItem(1, 1,
                new ItemBuilder(org.bukkit.Material.STAINED_CLAY, 1, (short) 4)
                        .name(ChatColor.GREEN + "Delta Forging")
                        .lore(ChatColor.GRAY + "Craft a Delta Tiered Item")
                        .get(),
                (m, e) -> openForgingMenu(player, databasePlayer, ItemTier.DELTA, new HashMap<>())
        );

        menu.setItem(4, 1,
                new ItemBuilder(org.bukkit.Material.STAINED_CLAY, 1, (short) 1)
                        .name(ChatColor.GREEN + "Omega Forging")
                        .lore(ChatColor.GRAY + "Craft an Omega Tiered Item")
                        .get(),
                (m, e) -> openForgingMenu(player, databasePlayer, ItemTier.OMEGA, new HashMap<>())
        );
        menu.setItem(7, 1,
                new ItemBuilder(Material.ANVIL)
                        .name(ChatColor.GREEN + "Celestial Smeltery")
                        .lore(ChatColor.GRAY + "Smelt Celestial Bronze")
                        .get(),
                (m, e) -> openCelestialSmelteryMenu(player, databasePlayer, null)
        );

        menu.setItem(4, 3, Menu.MENU_CLOSE, Menu.ACTION_CLOSE_MENU);
        menu.openForPlayer(player);
    }

    private static void openForgingMenu(Player player, DatabasePlayer databasePlayer, ItemTier itemTier, HashMap<ItemTier, AbstractItem<?, ?, ?>> items) {
        Menu menu = new Menu(itemTier.name + " Forging", 9 * 6);

        TierCostInfo tierCostInfo = TIER_COST_INFO.get(itemTier);
        List<TierRequirement> requirements = tierCostInfo.getRequirements();
        for (TierRequirement requirement : requirements) {
            ItemTier tier = requirement.getTier();
            ItemMenuUtil.addItemTierRequirement(menu, tier, items.get(tier), requirement.getX(), requirement.getY(), (m, e) -> {
                openItemSelectMenu(
                        player,
                        databasePlayer,
                        tier,
                        (m2, e2) -> openForgingMenu(player, databasePlayer, itemTier, items),
                        (i2, m2, e2) -> {
                            items.put(tier, i2);
                            openForgingMenu(player, databasePlayer, itemTier, items);
                        }
                );
            });
        }

        Pair<Integer, Integer> costLocation = tierCostInfo.getCostLocation();
        DatabasePlayerPvE pveStats = databasePlayer.getPveStats();

        ItemMenuUtil.addSpendableCostRequirement(databasePlayer, menu, tierCostInfo.getCost(), costLocation.getA(), costLocation.getB());
        ItemMenuUtil.addItemConfirmation(menu, () -> {
            addCraftItemConfirmation(player, databasePlayer, items, menu, requirements, pveStats, itemTier);
        });

        menu.setItem(4, 5, Menu.MENU_BACK, (m, e) -> openItemCraftingMenu(player, databasePlayer));
        menu.openForPlayer(player);
    }

    private static void openItemSelectMenu(
            Player player,
            DatabasePlayer databasePlayer,
            ItemTier tier,
            BiConsumer<Menu, InventoryClickEvent> back,
            TriConsumer<AbstractItem<?, ?, ?>, Menu, InventoryClickEvent> onClick
    ) {
        ItemSearchMenu menu = new ItemSearchMenu(
                player,
                "Select an Item",
                onClick,
                itemBuilder -> itemBuilder.addLore(
                        "",
                        ChatColor.YELLOW.toString() + ChatColor.BOLD + "CLICK" + ChatColor.GREEN + " to select"
                ),
                new ItemSearchMenu.PlayerItemMenuSettings(databasePlayer)
                        .setItemInventory(databasePlayer.getPveStats()
                                                        .getItemsManager()
                                                        .getItemInventory()
                                                        .stream()
                                                        .filter(item -> item.getTier() == tier)
                                                        .collect(Collectors.toList())),
                databasePlayer,
                m -> {
                    m.setItem(4, 5, Menu.MENU_BACK, back);
                }
        );
        menu.open();
    }

    private static void addCraftItemConfirmation(
            Player player,
            DatabasePlayer databasePlayer,
            HashMap<ItemTier, AbstractItem<?, ?, ?>> items,
            Menu menu,
            List<TierRequirement> requirements,
            DatabasePlayerPvE pveStats,
            ItemTier tier
    ) {
        boolean requirementsMet = requirements.stream().allMatch(requirement -> items.get(requirement.getTier()) != null);
        boolean enoughMobDrops = TIER_COST_INFO.get(tier)
                                               .getCost()
                                               .entrySet()
                                               .stream()
                                               .allMatch(entry -> entry.getKey().getFromPlayer(databasePlayer) >= entry.getValue());
        menu.setItem(6, 2,
                new ItemBuilder(requirementsMet && enoughMobDrops ? tier.clayBlock : new ItemStack(Material.BARRIER))
                        .name(ChatColor.GREEN + "Click to Craft Item")
                        .lore(
                                ItemMenuUtil.getRequirementMetString(requirementsMet, "Required Item" + (requirements.size() != 1 ? "s" : "") + " Selected"),
                                ItemMenuUtil.getRequirementMetString(enoughMobDrops, "Enough Mob Drops"),
                                "",
                                WordWrap.wrapWithNewline(ChatColor.GRAY + "Crafted Item will inherit the type and blessing of the highest tiered selected item.",
                                        160
                                )
                        )
                        .get(),
                (m, e) -> {
                    if (!requirementsMet) {
                        player.sendMessage(ChatColor.RED + "You do not have all the required items to craft this item!");
                        return;
                    }
                    TierCostInfo tierCostInfo = TIER_COST_INFO.get(tier);
                    for (Map.Entry<Spendable, Long> currenciesLongEntry : tierCostInfo.getCost().entrySet()) {
                        Spendable spendable = currenciesLongEntry.getKey();
                        Long cost = currenciesLongEntry.getValue();
                        if (spendable.getFromPlayer(databasePlayer) < cost) {
                            player.sendMessage(ChatColor.RED + "You need " + spendable.getCostColoredName(cost) + ChatColor.RED + " to craft this item!");
                            return;
                        }
                    }

                    Menu.openConfirmationMenu(player,
                            "Confirm Item Craft",
                            3,
                            Collections.singletonList(ChatColor.GRAY + "Craft " + tier.getColoredName() + ChatColor.GRAY + " Item"),
                            Collections.singletonList(ChatColor.GRAY + "Go back"),
                            (m2, e2) -> {
                                for (TierRequirement requirement : requirements) {
                                    pveStats.getItemsManager().removeItem(items.get(requirement.getTier()));
                                }
                                for (Map.Entry<Spendable, Long> currenciesLongEntry : tierCostInfo.getCost().entrySet()) {
                                    currenciesLongEntry.getKey().subtractFromPlayer(databasePlayer, currenciesLongEntry.getValue());
                                }

                                AbstractItem<?, ?, ?> inheritedItem = null;
                                if (tier == ItemTier.DELTA) {
                                    inheritedItem = items.get(ItemTier.GAMMA);
                                } else if (tier == ItemTier.OMEGA) {
                                    inheritedItem = items.get(ItemTier.DELTA);
                                }
                                if (inheritedItem == null) {
                                    return;
                                }
                                AbstractItem<?, ?, ?> craftedItem = inheritedItem.getType().create.apply(tier);
                                craftedItem.setModifier(inheritedItem.getModifier());
                                craftedItem.bless(null);
                                pveStats.getItemsManager().addItem(craftedItem);
                                AbstractItem.sendItemMessage(player,
                                        new ComponentBuilder(ChatColor.GRAY + "You crafted ")
                                                .appendHoverItem(craftedItem.getName(), craftedItem.generateItemStack())
                                );
                                player.closeInventory();
                            },
                            (m2, e2) -> openForgingMenu(player, databasePlayer, tier, items),
                            (m2) -> {
                            }
                    );
                }
        );
    }

    private static void openCelestialSmelteryMenu(Player player, DatabasePlayer databasePlayer, Integer boughtBlessing) {
        Menu menu = new Menu("Celestial Smeltery", 9 * 6);

        ItemBuilder itemBuilder = new ItemBuilder(Material.PAPER)
                .name(ChatColor.YELLOW.toString() + ChatColor.BOLD + "CLICK" +
                        ChatColor.GREEN + " to select a" + (boughtBlessing != null ? " different " : " ") + "blessing")
                .enchant(Enchantment.OXYGEN, 1)
                .flags(ItemFlag.HIDE_ENCHANTS);
        if (boughtBlessing == null) {
            itemBuilder.name(ChatColor.YELLOW.toString() + ChatColor.BOLD + "CLICK" + ChatColor.GREEN + " to select a blessing");
        } else {
            itemBuilder.name(ChatColor.GREEN + "Tier " + (boughtBlessing) + " Bought Blessing")
                       .addLore(
                               "",
                               ChatColor.YELLOW.toString() + ChatColor.BOLD + "CLICK" + ChatColor.GREEN + " to select a different blessing"
                       );
        }

        menu.setItem(1, 1,
                itemBuilder
                        .get(),
                (m, e) -> openBlessingSelectMenu(player, databasePlayer, boughtBlessing)
        );
        ItemMenuUtil.addPaneRequirement(menu, 2, 1, boughtBlessing != null);

        ItemMenuUtil.addSpendableCostRequirement(databasePlayer, menu, CELESTIAL_SMELTERY_COST, 1, 2);

        ItemMenuUtil.addItemConfirmation(menu, () -> {
            addCelestialSmelteryConfirmationMenu(player, databasePlayer, boughtBlessing, menu);
        });

        menu.setItem(4, 5, MENU_BACK, (m, e) -> openItemCraftingMenu(player, databasePlayer));
        menu.openForPlayer(player);
    }

    private static void openBlessingSelectMenu(Player player, DatabasePlayer databasePlayer, Integer boughtBlessing) {
        Menu menu = new Menu("Select a Blessing Tier", 9 * 4);

        ItemsManager itemsManager = databasePlayer.getPveStats().getItemsManager();

        for (int tier = 1; tier <= 5; tier++) {
            Integer blessingBoughtAmount = itemsManager.getBlessingBoughtAmount(tier);
            int finalTier = tier;
            menu.setItem(tier + 1, 1,
                    new ItemBuilder(Material.PAPER)
                            .name(ChatColor.GREEN + "Tier " + tier + " Bought Blessings")
                            .lore(ChatColor.GRAY + "Amount: " + ChatColor.YELLOW + blessingBoughtAmount)
                            .amount(blessingBoughtAmount)
                            .enchant(Enchantment.OXYGEN, 1)
                            .flags(ItemFlag.HIDE_ENCHANTS)
                            .get(),
                    (m, e) -> {
                        if (blessingBoughtAmount > 0) {
                            openCelestialSmelteryMenu(player, databasePlayer, finalTier);
                        }
                    }
            );
        }

        menu.setItem(4, 3, Menu.MENU_BACK, (m, e) -> openCelestialSmelteryMenu(player, databasePlayer, boughtBlessing));
        menu.openForPlayer(player);
    }

    private static void addCelestialSmelteryConfirmationMenu(Player player, DatabasePlayer databasePlayer, Integer boughtBlessing, Menu menu) {
        DatabasePlayerPvE pveStats = databasePlayer.getPveStats();

        boolean hasBoughtBlessing = boughtBlessing != null;
        boolean enoughCost = CELESTIAL_SMELTERY_COST.entrySet()
                                                    .stream()
                                                    .allMatch(entry -> entry.getKey().getFromPlayer(databasePlayer) >= entry.getValue());
        ItemBuilder itemBuilder = new ItemBuilder(hasBoughtBlessing && enoughCost ? Material.ANVIL : Material.BARRIER)
                .name(ChatColor.GREEN + "Click to Smelt a Celestial Bronze")
                .lore(
                        ItemMenuUtil.getRequirementMetString(hasBoughtBlessing, "Blessing Selected"),
                        ItemMenuUtil.getRequirementMetString(enoughCost, "Enough Loot")
                );

        menu.setItem(6, 2,
                itemBuilder.get(),
                (m, e) -> {
                    if (!hasBoughtBlessing || !enoughCost) {
                        return;
                    }

                    Menu.openConfirmationMenu(player,
                            "Confirm Smelt",
                            3,
                            new ArrayList<>() {{
                                add(ChatColor.GRAY + "Smelt a Celestial Bronze");
                                addAll(PvEUtils.getCostLore(CELESTIAL_SMELTERY_COST, "Smelt Cost"));
                                add(ChatColor.GRAY + " - " + ChatColor.GREEN + "Tier " + boughtBlessing + " Bought Blessing");
                            }},
                            Collections.singletonList(ChatColor.GRAY + "Go back"),
                            (m2, e2) -> {
                                for (Map.Entry<Spendable, Long> spendableLongEntry : CELESTIAL_SMELTERY_COST.entrySet()) {
                                    spendableLongEntry.getKey().subtractFromPlayer(databasePlayer, spendableLongEntry.getValue());
                                }
                                pveStats.getItemsManager().subtractBlessingBought(boughtBlessing);
                                pveStats.addCurrency(Currencies.CELESTIAL_BRONZE, 1);

                                DatabaseManager.queueUpdatePlayerAsync(databasePlayer);
                                player.closeInventory();

                                AbstractItem.sendItemMessage(player, ChatColor.GREEN + "You smelted a Celestial Bronze");
                            },
                            (m2, e2) -> openCelestialSmelteryMenu(player, databasePlayer, boughtBlessing),
                            (m2) -> {
                            }
                    );
                }
        );

    }

    static class TierRequirement {
        private final ItemTier tier;
        private final int x;
        private final int y;

        TierRequirement(ItemTier tier, int x, int y) {
            this.tier = tier;
            this.x = x;
            this.y = y;
        }

        public ItemTier getTier() {
            return tier;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    static class TierCostInfo {
        private final LinkedHashMap<Spendable, Long> cost;
        private final Pair<Integer, Integer> costLocation;
        private final List<TierRequirement> requirements;

        TierCostInfo(LinkedHashMap<Spendable, Long> cost, Pair<Integer, Integer> costLocation, List<TierRequirement> requirements) {
            this.cost = cost;
            this.costLocation = costLocation;
            this.requirements = requirements;
        }

        public LinkedHashMap<Spendable, Long> getCost() {
            return cost;
        }

        public Pair<Integer, Integer> getCostLocation() {
            return costLocation;
        }

        public List<TierRequirement> getRequirements() {
            return requirements;
        }

    }

}
