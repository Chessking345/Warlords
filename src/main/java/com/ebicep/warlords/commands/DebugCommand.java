package com.ebicep.warlords.commands;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.maps.Game;
import com.ebicep.warlords.maps.state.TimerDebugAble;
import com.ebicep.warlords.menu.DebugMenu;
import com.ebicep.warlords.player.WarlordsPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to do that.");
            return true;
        }

        Game game = Warlords.game; // In the future allow the user to select a game player
        if (args.length < 1) {
            DebugMenu.openDebugMenu((Player) sender);
            //sender.sendMessage("§cYou need to pass an argument, valid arguments: [timer, energy, cooldown, cooldownmode, takedamage]");
            return true;
        }
        WarlordsPlayer player = BaseCommand.requireWarlordsPlayer(sender);
        if (args[0].equals("energy") || args[0].equals("cooldown") || args[0].equals("damage") || args[0].equals("takedamage")) {
            if (args.length == 3 && args[2] != null) {
                player = Warlords.getPlayer(Bukkit.getPlayer(args[2]).getUniqueId());
            }
            if (player == null) { // We only have a warlords player if the game is running
                return true;
            }
        }
        switch (args[0]) {
            case "timer":
                if (!(game.getState() instanceof TimerDebugAble)) {
                    sender.sendMessage("§cThis gamestate cannot be manipulated by the timer debug option");
                    return true;
                }
                TimerDebugAble timerDebugAble = (TimerDebugAble) game.getState();
                if (args.length < 2) {
                    sender.sendMessage("§cTimer requires 2 or more arguments, valid arguments: [skip, reset]");
                    return true;
                }
                switch (args[1]) {
                    case "reset":
                        timerDebugAble.resetTimer();
                        sender.sendMessage(ChatColor.RED + "DEV: §aTimer has been reset!");
                        return true;
                    case "skip":
                        timerDebugAble.skipTimer();
                        sender.sendMessage(ChatColor.RED + "DEV: §aTimer has been skipped!");
                        return true;
                    default:
                        sender.sendMessage("§cInvalid option!");
                        return true;
                }
            case "energy": {
                if (args.length < 2) {
                    sender.sendMessage("§cEnergy requires 2 or more arguments, valid arguments: [disable, enable]");
                    return true;
                }
                switch (args[1]) {
                    case "disable":
                        player.setInfiniteEnergy(true);
                        sender.sendMessage(ChatColor.RED + "DEV: " + player.getColoredName() + "'s §aEnergy consumption has been disabled!");
                        return true;
                    case "enable":
                        player.setInfiniteEnergy(false);
                        sender.sendMessage(ChatColor.RED + "DEV: " + player.getColoredName() + "'s §aEnergy consumption has been enabled!");
                        return true;
                    default:
                        sender.sendMessage("§cInvalid option!");
                        return false;
                }
            }

            case "cooldown": {
                if (args.length < 2) {
                    sender.sendMessage("§cCooldown requires 2 or more arguments, valid arguments: [disable, enable]");
                    return true;
                }
                switch (args[1]) {
                    case "disable":
                        player.setDisableCooldowns(true);
                        sender.sendMessage(ChatColor.RED + "DEV: " + player.getColoredName() + "'s §aCooldown timers have been disabled!");
                        return true;
                    case "enable":
                        player.setDisableCooldowns(false);
                        sender.sendMessage(ChatColor.RED + "DEV: " + player.getColoredName() + "'s §aCooldown timers have been enabled!");
                        return true;
                    default:
                        sender.sendMessage("§cInvalid option!");
                        return false;
                }
            }

            case "damage": {
                if (args.length < 2) {
                    sender.sendMessage("§cDamage requires 2 or more arguments, valid arguments: [disable, enable]");
                    return true;
                }
                switch (args[1]) {
                    case "disable":
                        player.setTakeDamage(false);
                        sender.sendMessage(ChatColor.RED + "§cDEV: " + player.getColoredName() + "'s §aTaking damage has been disabled!");
                        return true;
                    case "enable":
                        player.setTakeDamage(true);
                        sender.sendMessage(ChatColor.RED + "§cDEV: " + player.getColoredName() + "'s §aTaking damage has been enabled!");
                        return true;
                    default:
                        sender.sendMessage("§cInvalid option!");
                        return false;
                }
            }

            case "takedamage": {
                if (args.length < 2) {
                    sender.sendMessage("§cTake Damage requires more arguments, valid arguments: [1000, 2000, 3000, 4000]");
                    return true;
                }

                switch (args[1]) {
                    case "1000":
                        player.addHealth(player, "debug", -1000, -1000, -1, 100);
                        sender.sendMessage(ChatColor.RED + "§cDEV: " + player.getColoredName() + " §atook 1000 damage!");
                        return true;
                    case "2000":
                        player.addHealth(player, "debug", -2000, -2000, -1, 100);
                        sender.sendMessage(ChatColor.RED + "§cDEV: " + player.getColoredName() + " §atook 2000 damage!");
                        return true;
                    case "3000":
                        player.addHealth(player, "debug", -3000, -3000, -1, 100);
                        sender.sendMessage(ChatColor.RED + "§cDEV: " + player.getColoredName() + " §atook 3000 damage!");
                        return true;
                    case "4000":
                        player.addHealth(player, "debug", -4000, -4000, -1, 100);
                        sender.sendMessage(ChatColor.RED + "§cDEV: " + player.getColoredName() + " §atook 4000 damage!");
                        return true;
                    case "5000":
                        player.addHealth(player, "debug", -5000, -5000, -1, 100);
                        sender.sendMessage(ChatColor.RED + "§cDEV: " + player.getColoredName() + " §atook 5000 damage!");
                        return true;
                    default:
                        sender.sendMessage("§cInvalid option! [Options: 1000, 2000, 3000, 4000]");
                        return false;
                }
            }

            case "cooldownmode": {
                if (args.length < 2) {
                    sender.sendMessage("§cCooldown Mode requires 2 or more arguments, valid arguments: [disable, enable]");
                    return true;
                }

                switch (args[1]) {
                    case "disable":
                        sender.sendMessage(ChatColor.RED + "Cooldown Mode can't be disabled in game!");
                        return true;
                    case "enable":
                        game.setCooldownMode(true);
                        Bukkit.broadcastMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "-------------------------------");
                        Bukkit.broadcastMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "Cooldown Mode has been enabled by §c§l" + sender.getName() + "§6!");
                        Bukkit.broadcastMessage(ChatColor.GREEN + ChatColor.BOLD.toString() + "+50% Cooldown Reduction");
                        Bukkit.broadcastMessage(ChatColor.YELLOW + ChatColor.BOLD.toString() + "-50% Energy Costs");
                        Bukkit.broadcastMessage(ChatColor.GREEN + ChatColor.BOLD.toString() + "+50% Max Health");
                        Bukkit.broadcastMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "-------------------------------");
                        return true;
                    default:
                        sender.sendMessage("§cInvalid option!");
                        return false;
                }
            }

            default:
                sender.sendMessage("§cInvalid option! valid args: [cooldownmode, cooldown, energy, damage, takedamage");
                return true;
        }
    }

    public void register(Warlords instance) {
        instance.getCommand("wl").setExecutor(this);
        //instance.getCommand("wl").setTabCompleter(this);
    }
}
