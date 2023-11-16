package com.ebicep.warlords.pve.mobs.bosses.bossminions;

import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.option.pve.PveOption;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsNPC;
import com.ebicep.warlords.pve.mobs.AbstractMob;
import com.ebicep.warlords.pve.mobs.Mob;
import com.ebicep.warlords.pve.mobs.flags.NoTarget;
import com.ebicep.warlords.pve.mobs.tiers.BossMinionMob;
import com.ebicep.warlords.util.java.NumberFormat;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.TNTPrimed;

import java.util.List;

public class NarmersDeathCharge extends AbstractMob implements BossMinionMob, NoTarget {

    private WarlordsNPC.CustomHologramLine customHologramLine;

    public NarmersDeathCharge(Location spawnLocation) {
        this(spawnLocation, "Narmer's Death Charge", 2000, 0f, 0, 0, 0);
    }

    public NarmersDeathCharge(
            Location spawnLocation,
            String name,
            int maxHealth,
            float walkSpeed,
            int damageResistance,
            float minMeleeDamage,
            float maxMeleeDamage
    ) {
        super(spawnLocation,
                name,
                maxHealth,
                walkSpeed,
                damageResistance,
                minMeleeDamage,
                maxMeleeDamage
        );
    }

    @Override
    public Mob getMobRegistry() {
        return Mob.NARMERS_DEATH_CHARGE;
    }

    @Override
    public void giveGoals() {
    }

    @Override
    public void onSpawn(PveOption option) {
        super.onSpawn(option);
        if (npc.getEntity() instanceof TNTPrimed tntPrimed) {
            tntPrimed.setFuseTicks(Integer.MAX_VALUE);
        }
        customHologramLine = new WarlordsNPC.CustomHologramLine(Component.text(""));
        warlordsNPC.getCustomHologramLines().add(customHologramLine);
    }

    @Override
    public void whileAlive(int ticksElapsed, PveOption option) {
        if (ticksElapsed < 20 * 5) {
            TextColor color = ticksElapsed < 20 * 3 ? NamedTextColor.GREEN : ticksElapsed < 20 * 4 ? NamedTextColor.YELLOW : NamedTextColor.RED;
            customHologramLine.setText(Component.text(NumberFormat.formatTenths(5 - ticksElapsed / 20f), color));
            return;
        }
        if (ticksElapsed == 20 * 5) {
            customHologramLine.setText(Component.text("PRIMED", NamedTextColor.RED));
            return;
        }
        if (warlordsNPC.isDead()) {
            return;
        }
        if (ticksElapsed % 40 == 0) {
            Utils.playGlobalSound(warlordsNPC.getLocation(), Sound.ENTITY_CREEPER_HURT, 2, 1);
        }
        List<WarlordsEntity> warlordsEntities = PlayerFilter.entitiesAround(warlordsNPC, 2.5, 2, 2.5)
                                                            .excluding(warlordsNPC)
                                                            .filter(warlordsEntity -> {
                                                                if (!(warlordsEntity instanceof WarlordsNPC wNPC)) {
                                                                    return true;
                                                                }
                                                                return wNPC.getMob() instanceof NarmerAcolyte;
                                                            })
                                                            .toList();
        if (warlordsEntities.isEmpty()) {
            return;
        }
        warlordsEntities.forEach(warlordsEntity -> {
            if (warlordsEntity instanceof WarlordsNPC wNPC && !(wNPC.getMob() instanceof NarmerAcolyte)) {
                return;
            }
            int damage = warlordsEntity instanceof WarlordsNPC ? 5000 : 1500;
            warlordsEntity.addDamageInstance(warlordsNPC, "Explosion", damage, damage, 0, 100);
        });
        Utils.playGlobalSound(warlordsNPC.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 500, 1);
        EffectUtils.displayParticle(Particle.EXPLOSION_NORMAL, warlordsNPC.getLocation(), 1, 0, 0, 0, 0.5);
        warlordsNPC.die(warlordsNPC);
    }

    @Override
    public void onAttack(WarlordsEntity attacker, WarlordsEntity receiver, WarlordsDamageHealingEvent event) {

    }

    @Override
    public void onDamageTaken(WarlordsEntity self, WarlordsEntity attacker, WarlordsDamageHealingEvent event) {

    }
}
