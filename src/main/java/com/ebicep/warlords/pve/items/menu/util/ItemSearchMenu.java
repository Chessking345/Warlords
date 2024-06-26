package com.ebicep.warlords.pve.items.menu.util;

import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer;
import com.ebicep.warlords.menu.Menu;
import com.ebicep.warlords.player.general.Specializations;
import com.ebicep.warlords.pve.Currencies;
import com.ebicep.warlords.pve.items.ItemLoadout;
import com.ebicep.warlords.pve.items.ItemTier;
import com.ebicep.warlords.pve.items.ItemsManager;
import com.ebicep.warlords.pve.items.addons.ItemAddonSpecBonus;
import com.ebicep.warlords.pve.items.statpool.BasicStatPool;
import com.ebicep.warlords.pve.items.types.AbstractItem;
import com.ebicep.warlords.pve.items.types.ItemType;
import com.ebicep.warlords.pve.mobs.Aspect;
import com.ebicep.warlords.pve.mobs.MobDrop;
import com.ebicep.warlords.util.bukkit.ItemBuilder;
import com.ebicep.warlords.util.java.TriConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ItemSearchMenu extends Menu {
    private final Player player;
    private final TriConsumer<AbstractItem, Menu, InventoryClickEvent> itemClickAction;
    private final UnaryOperator<ItemBuilder> editItem;
    private final PlayerItemMenuSettings menuSettings;
    private final DatabasePlayer databasePlayer;
    private Consumer<Menu> menu;

    public ItemSearchMenu(
            Player player,
            String name,
            TriConsumer<AbstractItem, Menu, InventoryClickEvent> itemClickAction,
            UnaryOperator<ItemBuilder> editItem,
            PlayerItemMenuSettings menuSettings,
            DatabasePlayer databasePlayer,
            Consumer<Menu> menu
    ) {
        this(player, name, itemClickAction, editItem, menuSettings, databasePlayer);
        this.menu = menu;
        menu.accept(this);
    }

    public ItemSearchMenu(
            Player player,
            String name,
            TriConsumer<AbstractItem, Menu, InventoryClickEvent> itemClickAction,
            UnaryOperator<ItemBuilder> editItem,
            PlayerItemMenuSettings menuSettings,
            DatabasePlayer databasePlayer
    ) {
        super(name, 9 * 6);
        this.player = player;
        this.itemClickAction = itemClickAction;
        this.editItem = editItem;
        this.menuSettings = menuSettings;
        this.databasePlayer = databasePlayer;
    }

    public void open() {
        super.clear();
        menuSettings.sort();
        addItems();
        addMobDrops();
        addResetSetting();
        addFilterBySetting();
        addSortBySetting();
        addSortOrderSetting();
        addPageArrows();
        if (menu != null) {
            menu.accept(this);
        }
        super.openForPlayer(player);
    }

    private void addItems() {
        List<UUID> equippedItems = databasePlayer.getPveStats()
                                                 .getItemsManager()
                                                 .getLoadouts()
                                                 .stream()
                                                 .map(ItemLoadout::getItems)
                                                 .flatMap(Collection::stream)
                                                 .toList();
        int page = menuSettings.getPage();
        List<AbstractItem> itemInventory = new ArrayList<>(menuSettings.getSortedItemInventory());
        int x = 0;
        int y = 0;
        for (int i = 0; i < 45; i++) {
            int itemNumber = ((page - 1) * 45) + i;
            if (itemNumber < itemInventory.size()) {
                AbstractItem item = itemInventory.get(itemNumber);
                ItemBuilder itemBuilder = item.generateItemBuilder();
                if (equippedItems.contains(item.getUUID())) {
                    if (!item.isFavorite()) {
                        itemBuilder.addLore(Component.empty());
                    }
                    itemBuilder.addLore(Component.text("EQUIPPED", NamedTextColor.AQUA));
                }
                itemBuilder = editItem.apply(itemBuilder);
                setItem(x, y,
                        itemBuilder.get(),
                        (m, e) -> itemClickAction.accept(item, m, e)
                );
                x++;
                if (x == 9) {
                    x = 0;
                    y++;
                }
            }
        }
    }

    private void addMobDrops() {
        setItem(2, 5,
                new ItemBuilder(Material.ZOMBIE_HEAD)
                        .name(Component.text("Your Drops", NamedTextColor.GREEN))
                        .lore(
                                MobDrop.ZENITH_STAR.getCostColoredName(MobDrop.ZENITH_STAR.getFromPlayer(databasePlayer)),
                                Currencies.SCRAP_METAL.getCostColoredName(Currencies.SCRAP_METAL.getFromPlayer(databasePlayer))
                        )
                        .get(),
                (m, e) -> {}
        );
    }

    private void addResetSetting() {
        setItem(3, 5,
                new ItemBuilder(Material.MILK_BUCKET)
                        .name(Component.text("Reset Settings", NamedTextColor.GREEN))
                        .lore(Component.text("Reset the filter, sort, and order of weapons", NamedTextColor.GRAY))
                        .get(),
                (m, e) -> {
                    menuSettings.reset();
                    DatabaseManager.queueUpdatePlayerAsync(databasePlayer);
                    open();
                }
        );
    }

    private void addFilterBySetting() {
        PlayerItemMenuSettings.PlayerItemMenuFilterSettings filterSettings = menuSettings.getFilterSettings();
        List<Component> filterLore = new ArrayList<>();
        TextComponent grayDash = Component.text("- ", NamedTextColor.GRAY);
        if (filterSettings.getTypeFilter() != ItemType.NONE) {
            filterLore.add(grayDash.append(Component.text(filterSettings.getTypeFilter().name, NamedTextColor.GRAY)));
        }
        if (filterSettings.getTierFilter() != ItemTier.NONE) {
            filterLore.add(grayDash.append(Component.text(filterSettings.getTierFilter().name, NamedTextColor.GRAY)));
        }
        if (filterSettings.getAspectFilter() != null) {
            filterLore.add(grayDash.append(Component.text(filterSettings.getAspectFilter().name, NamedTextColor.GRAY)));
        }
        if (filterSettings.getAddonFilter()) {
            filterLore.add(grayDash.append(Component.text("Selected Spec Bonus", NamedTextColor.GRAY)));
        }
        if (filterSettings.getFavoriteFilter()) {
            filterLore.add(grayDash.append(Component.text("Only Favorites", NamedTextColor.GRAY)));
        }
        if (filterLore.isEmpty()) {
            filterLore.add(Component.text("No filters selected", NamedTextColor.GRAY));
        }
        filterLore.add(Component.empty());
        filterLore.add(Component.textOfChildren(
                Component.text("CLICK", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" to change", NamedTextColor.GRAY)
        ));
        setItem(5, 5,
                new ItemBuilder(Material.HOPPER)
                        .name(Component.text("Filter Settings", NamedTextColor.GREEN))
                        .lore(filterLore)
                        .get(),
                (m, e) -> {
                    ItemFilterMenu.openItemFilterMenu(player, databasePlayer, (m2, e2) -> open());
                }
        );
    }

    private void addSortBySetting() {
        SortOptions sortedBy = menuSettings.getSortOption();
        setItem(6, 5,
                new ItemBuilder(Material.COMPARATOR)
                        .name(Component.text("Sort By", NamedTextColor.GREEN))
                        .lore(Arrays.stream(SortOptions.VALUES)
                                    .map(value -> Component.text(value.name, (sortedBy == value ? NamedTextColor.AQUA : NamedTextColor.GRAY)))
                                    .collect(Collectors.toList())
                        )
                        .get(),
                (m, e) -> {
                    menuSettings.setSortOption(sortedBy.next());
                    open();
                }
        );
    }

    private void addSortOrderSetting() {
        setItem(7, 5,
                new ItemBuilder(Material.LEVER)
                        .name(Component.text("Sort Order", NamedTextColor.GREEN))
                        .lore(
                                Component.text("Ascending", menuSettings.isAscending() ? NamedTextColor.AQUA : NamedTextColor.GRAY),
                                Component.text("Descending", menuSettings.isAscending() ? NamedTextColor.GRAY : NamedTextColor.AQUA)
                        )
                        .get(),
                (m, e) -> {
                    menuSettings.setAscending(!menuSettings.isAscending());
                    open();
                }
        );
    }

    private void addPageArrows() {
        int page = menuSettings.getPage();
        List<AbstractItem> itemInventory = new ArrayList<>(menuSettings.getSortedItemInventory());
        if (page - 1 > 0) {
            setItem(0, 5,
                    new ItemBuilder(Material.ARROW)
                            .name(Component.text("Previous Page", NamedTextColor.GREEN))
                            .lore(Component.text("Page " + (page - 1), NamedTextColor.YELLOW))
                            .get(),
                    (m, e) -> {
                        menuSettings.setPage(page - 1);
                        open();
                    }
            );
        }
        if (itemInventory.size() > (page * 45)) {
            setItem(8, 5,
                    new ItemBuilder(Material.ARROW)
                            .name(Component.text("Next Page", NamedTextColor.GREEN))
                            .lore(Component.text("Page " + (page + 1), NamedTextColor.YELLOW))
                            .get(),
                    (m, e) -> {
                        menuSettings.setPage(page + 1);
                        open();
                    }
            );
        }
    }

    public enum SortOptions {
        DATE("Date", Comparator.comparing(AbstractItem::getObtainedDate)),
        TIER("Tier", Comparator.comparing(AbstractItem::getTier)),
        TYPE("Type", Comparator.comparing(AbstractItem::getType)),
        ITEM_SCORE("Item Score", Comparator.comparing(AbstractItem::getItemScore)),
//        WEIGHT("Weight", Comparator.comparing(AbstractItem::getWeight)),

        ;

        private static final SortOptions[] VALUES = values();
        public final String name;
        public final Comparator<AbstractItem> comparator;

        SortOptions(String name, Comparator<AbstractItem> comparator) {
            this.name = name;
            this.comparator = comparator;
        }

        public SortOptions next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }

    public enum ModifierFilter {
        NONE("None",
                new ItemStack(Material.BARRIER),
                item -> true
        ),
        NORMAL("Normal",
                new ItemStack(Material.PAPER),
                item -> item.getModifier() == 0
        ),
        BLESSED("Blessed",
                new ItemBuilder(Material.PAPER)
                        .enchant(Enchantment.OXYGEN, 1)
                        .get(),
                item -> item.getModifier() > 0
        ),
        CURSED("Cursed",
                new ItemStack(Material.MAP),
                item -> item.getModifier() < 0
        ),

        ;

        public static final ModifierFilter[] VALUES = values();
        public final String name;
        public final ItemStack itemStack;
        public final Predicate<AbstractItem> filter;

        ModifierFilter(String name, ItemStack itemStack, Predicate<AbstractItem> filter) {
            this.name = name;
            this.itemStack = itemStack;
            this.filter = filter;
        }

        public ModifierFilter next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }

    public static class PlayerItemMenuSettings {
        private final Specializations selectedSpec;
        private int page = 1;
        private List<AbstractItem> itemInventory = new ArrayList<>();
        private List<AbstractItem> sortedItemInventory = new ArrayList<>();
        private PlayerItemMenuFilterSettings filterSettings;
        private SortOptions sortOption = SortOptions.DATE;
        private boolean ascending = true; //ascending = smallest -> largest/recent

        public PlayerItemMenuSettings(DatabasePlayer databasePlayer) {
            this(databasePlayer, databasePlayer.getLastSpec());
        }

        public PlayerItemMenuSettings(DatabasePlayer databasePlayer, Specializations classes) {
            this.selectedSpec = classes;
            ItemsManager itemsManager = databasePlayer.getPveStats().getItemsManager();
            PlayerItemMenuFilterSettings playerItemMenuFilterSettings = itemsManager.getMenuFilterSettings();
            if (playerItemMenuFilterSettings == null) {
                playerItemMenuFilterSettings = new PlayerItemMenuFilterSettings();
                itemsManager.setMenuFilterSettings(playerItemMenuFilterSettings);
                DatabaseManager.queueUpdatePlayerAsync(databasePlayer);
            }
            this.filterSettings = playerItemMenuFilterSettings;
            setItemInventory(new ArrayList<>(databasePlayer.getPveStats().getItemsManager().getItemInventory()));
        }

        public PlayerItemMenuSettings setItemInventory(List<AbstractItem> itemInventory) {
            this.itemInventory = itemInventory;
            this.sortedItemInventory = new ArrayList<>(itemInventory);
            return this;
        }

        public void reset() {
            this.page = 1;
            this.filterSettings.statPoolFilter = EnumSet.noneOf(BasicStatPool.class);
            this.filterSettings.tierFilter = ItemTier.NONE;
            this.filterSettings.typeFilter = ItemType.NONE;
            this.filterSettings.aspectFilter = null;
            this.filterSettings.addonFilter = false;
            this.filterSettings.favoriteFilter = false;
            this.sortOption = SortOptions.DATE;
            this.ascending = true;
        }

        public void sort() {
            sortedItemInventory = new ArrayList<>(itemInventory);
            if (!filterSettings.statPoolFilter.isEmpty()) {
                sortedItemInventory.removeIf(item -> {
                    for (BasicStatPool statPool : filterSettings.statPoolFilter) {
                        if (!item.getStatPool().containsKey(statPool)) {
                            return true;
                        }
                    }
                    return false;
                });
            }
            if (filterSettings.tierFilter != ItemTier.NONE) {
                sortedItemInventory.removeIf(item -> item.getTier() != filterSettings.tierFilter);
            }
            if (filterSettings.typeFilter != ItemType.NONE) {
                sortedItemInventory.removeIf(item -> item.getType() != filterSettings.typeFilter);
            }
            Aspect aspectFilter = filterSettings.aspectFilter;
            if (aspectFilter != null) {
                sortedItemInventory.removeIf(item -> item.getAspectModifier1() != aspectFilter && item.getAspectModifier2() != aspectFilter);
            }
            if (filterSettings.addonFilter) {
                sortedItemInventory.removeIf(item -> !(item instanceof ItemAddonSpecBonus itemAddonSpecBonus && itemAddonSpecBonus.getSpec() == selectedSpec));
            }
            if (filterSettings.favoriteFilter) {
                sortedItemInventory.removeIf(item -> !item.isFavorite());
            }
            sortedItemInventory.sort(sortOption.comparator);
            if (!ascending) {
                Collections.reverse(sortedItemInventory);
            }
        }

        public PlayerItemMenuFilterSettings getFilterSettings() {
            return filterSettings;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public List<AbstractItem> getSortedItemInventory() {
            return sortedItemInventory;
        }

        public SortOptions getSortOption() {
            return sortOption;
        }

        public void setSortOption(SortOptions sortOption) {
            this.sortOption = sortOption;
        }

        public boolean isAscending() {
            return ascending;
        }

        public void setAscending(boolean ascending) {
            this.ascending = ascending;
        }

        public static class PlayerItemMenuFilterSettings {
            public EnumSet<BasicStatPool> statPoolFilter = EnumSet.noneOf(BasicStatPool.class);
            public ItemTier tierFilter = ItemTier.NONE;
            public ItemType typeFilter = ItemType.NONE;
            public Aspect aspectFilter = null;
            public boolean addonFilter = false; // false = none, true = class
            private boolean favoriteFilter = false;

            public PlayerItemMenuFilterSettings() {
            }

            public EnumSet<BasicStatPool> getStatPoolFilter() {
                return statPoolFilter;
            }

            public ItemTier getTierFilter() {
                return tierFilter;
            }

            public void setTierFilter(ItemTier tierFilter) {
                this.tierFilter = tierFilter;
            }

            public ItemType getTypeFilter() {
                return typeFilter;
            }

            public void setTypeFilter(ItemType typeFilter) {
                this.typeFilter = typeFilter;
            }

            public Aspect getAspectFilter() {
                return aspectFilter;
            }

            public void setAspectFilter(Aspect aspectFilter) {
                this.aspectFilter = aspectFilter;
            }

            public boolean getAddonFilter() {
                return addonFilter;
            }

            public void nextAddonFilter() {
                addonFilter = !addonFilter;
            }

            public boolean getFavoriteFilter() {
                return favoriteFilter;
            }

            public void nextFavoriteFilter() {
                favoriteFilter = !favoriteFilter;
            }
        }
    }
}
