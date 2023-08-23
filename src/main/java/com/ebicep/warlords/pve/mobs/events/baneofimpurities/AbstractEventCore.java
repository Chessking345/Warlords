package com.ebicep.warlords.pve.mobs.events.baneofimpurities;

import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.option.pve.PveOption;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.PermanentCooldown;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.player.ingame.cooldowns.instances.InstanceFlags;
import com.ebicep.warlords.pve.mobs.MobTier;
import com.ebicep.warlords.pve.mobs.Mobs;
import com.ebicep.warlords.pve.mobs.mobflags.Unswappable;
import com.ebicep.warlords.pve.mobs.mobtypes.BossMob;
import com.ebicep.warlords.pve.mobs.zombie.AbstractZombie;
import com.ebicep.warlords.util.java.RandomCollection;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.EnumSet;

public abstract class AbstractEventCore extends AbstractZombie implements BossMob, Unswappable {

    private final int killTime;
    private final RandomCollection<Mobs> summonList;

    public AbstractEventCore(
            Location spawnLocation,
            String name,
            EntityEquipment ee,
            int maxHealth,
            int killTime,
            RandomCollection<Mobs> summonList
    ) {
        super(spawnLocation, name, MobTier.BOSS, ee, maxHealth, 0, 0, 0, 0);
        this.killTime = killTime;
        this.summonList = summonList;
        livingEntity.setGravity(false);
        entity.resetAI();
    }

    @Override
    public Component getDescription() {
        return Component.text("Bing Bang BOOM..", NamedTextColor.DARK_GRAY);
    }

    @Override
    public NamedTextColor getColor() {
        return NamedTextColor.GOLD;
    }

    @Override
    public void onSpawn(PveOption option) {
        super.onSpawn(option);
        int playerCount = option.playerCount();
        float scaledHealth = (float) (warlordsNPC.getMaxHealth() * (.0625 * Math.pow(Math.E, 0.69314718056 * playerCount))); // ln4/2 = 0.69314718056
        warlordsNPC.setMaxBaseHealth(scaledHealth);
        warlordsNPC.setMaxHealth(scaledHealth);
        warlordsNPC.setHealth(scaledHealth);

        warlordsNPC.setStunTicks(Integer.MAX_VALUE);
        warlordsNPC.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        warlordsNPC.getCooldownManager().addCooldown(new PermanentCooldown<>(
                "NO KB",
                null,
                null,
                null,
                warlordsNPC,
                CooldownTypes.ABILITY,
                cooldownManager -> {
                },
                false
        ) {
            @Override
            public void multiplyKB(Vector currentVector) {
                // immune to KB
                currentVector.multiply(0);
            }
        });
    }

    @Override
    public void whileAlive(int ticksElapsed, PveOption option) {
        int secondsElapsed = ticksElapsed / 20;
        if (ticksElapsed % 20 == 0) {
            if (secondsElapsed < killTime) {
                for (WarlordsEntity we : PlayerFilter
                        .playingGame(getWarlordsNPC().getGame())
                        .aliveEnemiesOf(warlordsNPC)
                ) {
                    if (secondsElapsed != 0) {
                        we.getEntity().showTitle(Title.title(
                                Component.text("", NamedTextColor.RED),
                                Component.text(killTime - secondsElapsed, NamedTextColor.RED),
                                Title.Times.times(Ticks.duration(10), Ticks.duration(35), Ticks.duration(0))
                        ));
                        Utils.playGlobalSound(warlordsNPC.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 500, 0.4f);
                    }
                    if (secondsElapsed % 15 == 0) {
                        we.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                        we.addSpeedModifier(warlordsNPC, "CHAOS", -20, 100, "BASE");
                        we.getCooldownManager().addCooldown(new RegularCooldown<>(
                                "Chaos",
                                "CHAOS",
                                AbstractEventCore.class,
                                null,
                                warlordsNPC,
                                CooldownTypes.DEBUFF,
                                cooldownManager -> {
                                },
                                100
                        ));
                    }
                }
            } else if (secondsElapsed == killTime) {
                playDeathAnimation(() -> {
                    warlordsNPC.die(warlordsNPC);
                    for (WarlordsEntity we : PlayerFilter
                            .playingGame(getWarlordsNPC().getGame())
                            .aliveEnemiesOf(warlordsNPC)
                    ) {
                        we.getEntity().clearTitle();
                        we.addDamageInstance(warlordsNPC, "Core Explosion", 25000, 25000, 0, 100, EnumSet.of(InstanceFlags.TRUE_DAMAGE));
                    }
                });
            }

            option.spawnNewMob(summonList.next().createMob.apply(pveOption.getRandomSpawnLocation(warlordsNPC)));
        }
    }

    @Override
    public void onAttack(WarlordsEntity attacker, WarlordsEntity receiver, WarlordsDamageHealingEvent event) {

    }

    @Override
    public void onDamageTaken(WarlordsEntity self, WarlordsEntity attacker, WarlordsDamageHealingEvent event) {

    }

    private void playDeathAnimation(Runnable afterAnimation) {
        // spin + elevate then explode
        new GameRunnable(warlordsNPC.getGame()) {

            float yaw = warlordsNPC.getEntity().getBodyYaw();

            @Override
            public void run() {
                //TODO swirl?
                warlordsNPC.getEntity().teleport(warlordsNPC.getLocation().add(0, 0.05, 0));
                warlordsNPC.getEntity().setRotation(yaw, 0);
                yaw += 10;

                if (yaw > 400) {
                    customDeathAnimation();
                    //TODO explosion particle + firework?
                    afterAnimation.run();
                    cancel();
                }
            }

        }.runTaskTimer(0, 0);
    }

    public abstract void customDeathAnimation();

}
