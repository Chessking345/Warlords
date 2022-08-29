package com.ebicep.warlords.guilds.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.HelpEntry;
import co.aikar.commands.annotation.*;
import com.ebicep.warlords.util.chat.ChatChannels;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Comparator;

@CommandAlias("gdebug")
@CommandPermission("group.adminisrator")
public class GuildDebugCommand extends BaseCommand {

    @Subcommand("experience")
    @Description("Sets the experience of the guild")
    public void setExperience(@Conditions("guild:true") Player player, GuildPlayerWrapper guildPlayerWrapper, Integer amount) {
        guildPlayerWrapper.getGuild().setExperience(amount);
        ChatChannels.sendDebugMessage(player, ChatColor.GREEN + "Set guild " + guildPlayerWrapper.getGuild().getName() + " experience to " + ChatColor.YELLOW + amount, true);
    }

    @Subcommand("coins")
    @Description("Sets the coins of the guild")
    public void setCoins(@Conditions("guild:true") Player player, GuildPlayerWrapper guildPlayerWrapper, Integer amount) {
        guildPlayerWrapper.getGuild().setCoins(amount);
        ChatChannels.sendDebugMessage(player, ChatColor.GREEN + "Set guild " + guildPlayerWrapper.getGuild().getName() + " coins to " + ChatColor.YELLOW + amount, true);
    }

    @HelpCommand
    public void help(CommandIssuer issuer, CommandHelp help) {
        help.getHelpEntries().sort(Comparator.comparing(HelpEntry::getCommand));
        help.showHelp();
    }

}