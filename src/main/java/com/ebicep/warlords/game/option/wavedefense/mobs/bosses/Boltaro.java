package com.ebicep.warlords.game.option.wavedefense.mobs.bosses;

import com.ebicep.warlords.game.option.wavedefense.mobs.MobTier;
import com.ebicep.warlords.game.option.wavedefense.mobs.mobtypes.BossMob;
import com.ebicep.warlords.game.option.wavedefense.mobs.zombie.AbstractZombie;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.util.bukkit.PacketUtils;
import com.ebicep.warlords.util.pve.SkullID;
import com.ebicep.warlords.util.pve.SkullUtils;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Boltaro extends AbstractZombie implements BossMob {

    public Boltaro(Location spawnLocation) {
        super(spawnLocation,
                "Boltaro",
                MobTier.BOSS,
                new Utils.SimpleEntityEquipment(
                        SkullUtils.getSkullFrom(SkullID.END_MONSTER),
                        new ItemStack(Material.DIAMOND_CHESTPLATE),
                        new ItemStack(Material.CHAINMAIL_LEGGINGS),
                        new ItemStack(Material.CHAINMAIL_BOOTS),
                        new ItemStack(Material.COOKED_FISH)
                ),
                15000,
                0.6f,
                10,
                300,
                400
        );
    }

    @Override
    public void onSpawn() {
        for (WarlordsEntity we : PlayerFilter.playingGame(getWarlordsNPC().getGame())) {
            if (we.getEntity() instanceof Player) {
                PacketUtils.sendTitle(
                        (Player) we.getEntity(),
                        ChatColor.RED + getWarlordsNPC().getName(),
                        ChatColor.GOLD + "Right Hand of the Illusion Vanguard",
                        20, 40, 20
                );
            }
        }
    }

    @Override
    public void whileAlive(int ticksElapsed) {

    }

    @Override
    public void onAttack(WarlordsEntity attacker, WarlordsEntity receiver) {
    }

    @Override
    public void onDamageTaken(WarlordsEntity mob, WarlordsEntity attacker) {

    }


}
