package com.ebicep.warlords.game.option.towerdefense;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Subcommand;
import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.game.option.Option;
import com.ebicep.warlords.game.option.towerdefense.towers.AbstractTower;
import com.ebicep.warlords.game.option.towerdefense.towers.TowerRegistry;
import com.ebicep.warlords.util.chat.ChatChannels;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

@CommandAlias("towerdefense|td")
@CommandPermission("group.administrator")
public class TowerDefenseCommand extends BaseCommand {

    @Subcommand("build")
    public void build(@Conditions("requireGame:gamemode=TOWER_DEFENSE") Player player, TowerRegistry tower) {
        Game game = Warlords.getGameManager().getPlayerGame(player.getUniqueId()).get();
        Location location = player.getLocation();
        location.setYaw(0);
        tower.create.apply(game, location).build();
    }

    @Subcommand("debug")
    public void debug(@Conditions("requireGame:gamemode=TOWER_DEFENSE") Player player) {
        Game game = Warlords.getGameManager().getPlayerGame(player.getUniqueId()).get();
        for (Option option : game.getOptions()) {
            if (option instanceof TowerBuildOption towerBuildOption) {
                towerBuildOption.toggleDebug();
                ChatChannels.sendDebugMessage(player, Component.text("Debug: " + towerBuildOption.isDebug(), NamedTextColor.GREEN));
            }
        }
    }

    @Subcommand("removeall")
    public void removeAll(@Conditions("requireGame:gamemode=TOWER_DEFENSE") Player player) {
        Game game = Warlords.getGameManager().getPlayerGame(player.getUniqueId()).get();
        for (Option option : game.getOptions()) {
            if (option instanceof TowerBuildOption towerBuildOption) {
                Map<AbstractTower, Integer> builtTowers = towerBuildOption.getBuiltTowers();
                for (AbstractTower builtTower : builtTowers.keySet()) {
                    builtTower.remove();
                }
                builtTowers.clear();
            }
        }
        ChatChannels.sendDebugMessage(player, Component.text("Removed all towers", NamedTextColor.GREEN));
    }

    @Subcommand("reloadtowers")
    public void reloadTowers(CommandIssuer issuer) {
        EnumSet<TowerRegistry> updated = TowerRegistry.updateCaches();
        List<TowerRegistry> notUpdated = new ArrayList<>();
        for (TowerRegistry value : TowerRegistry.VALUES) {
            if (!updated.contains(value)) {
                notUpdated.add(value);
            }
        }
        ChatChannels.sendDebugMessage(issuer, Component.text("Updated: ", NamedTextColor.GREEN)
                                                       .append(updated.stream()
                                                                      .sorted(Comparator.comparing(Enum::ordinal))
                                                                      .map(tower -> Component.text(tower.name(), NamedTextColor.YELLOW))
                                                                      .collect(Component.toComponent(Component.text(", ", NamedTextColor.GRAY))))
        );
        if (!notUpdated.isEmpty()) {
            ChatChannels.sendDebugMessage(issuer, Component.text("Not Updated: ", NamedTextColor.RED)
                                                           .append(notUpdated.stream()
                                                                             .map(tower -> Component.text(tower.name(), NamedTextColor.YELLOW))
                                                                             .collect(Component.toComponent(Component.text(", ", NamedTextColor.GRAY))))
            );
        }
    }

}