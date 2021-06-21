package com.ebicep.warlords.classes.abilties;

import com.ebicep.warlords.WarlordsPlayer;
import com.ebicep.warlords.classes.AbstractAbility;
import com.ebicep.warlords.util.PlayerFilter;
import org.bukkit.entity.Player;



public class InspiringPresence extends AbstractAbility {

    public InspiringPresence() {
        super("Inspiring Presence", 0, 0, 60 + 10, 0, 0, 0,
                "§7Your presence on the battlefield\n" +
                        "§7inspires your allies, increasing\n" +
                        "§7their energy regeneration by §e10\n" +
                        "§7per second and their movement\n" +
                        "§7by §e30% §7for §612 §7seconds.");
    }

    @Override
    public void onActivate(WarlordsPlayer warlordsPlayer, Player player) {
        PlayerFilter.entitiesAround(warlordsPlayer, 6, 2, 6)
            .aliveTeammatesOf(warlordsPlayer)
            .concat(warlordsPlayer)
            .forEach((nearPlayer) -> {
                nearPlayer.getSpeed().addSpeedModifier("Inspiring Presence", 30, 12 * 20, "BASE");
                nearPlayer.setPresence(12 * 20);
            });

        for (Player player1 : player.getWorld().getPlayers()) {
            player1.playSound(player.getLocation(), "paladin.inspiringpresence.activation", 2, 1);
        }
    }
}