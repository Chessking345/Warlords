package com.ebicep.warlords.abilties;

import com.ebicep.warlords.abilties.internal.AbstractAbility;
import com.ebicep.warlords.effects.FireWorkEffectPlayer;
import com.ebicep.warlords.effects.ParticleEffect;
import com.ebicep.warlords.events.player.WarlordsDamageHealingEvent;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsNPC;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class IncendiaryCurse extends AbstractAbility {
    private boolean pveUpgrade = false;

    protected int playersHit = 0;

    private static final double SPEED = 0.250;
    private static final double GRAVITY = -0.008;
    private float hitbox = 5;

    private int blindDurationInTicks = 40;

    public IncendiaryCurse() {
        super("Incendiary Curse", 408, 552, 8, 60, 25, 175);
    }

    @Override
    public void updateDescription(Player player) {
        description = "§7Ignite the targeted area with a cross flame,\n" +
                "§7dealing §c" + format(minDamageHeal) + " §7- §c" + format(maxDamageHeal) + " §7damage. Enemies\n" +
                "§7hit are blinded for §6" + format(blindDurationInTicks / 20f) + " §7seconds.";
    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        List<Pair<String, String>> info = new ArrayList<>();
        info.add(new Pair<>("Times Used", "" + timesUsed));
        info.add(new Pair<>("Players Hit", "" + playersHit));

        return info;
    }

    @Override
    public boolean onActivate(@Nonnull WarlordsEntity wp, @Nonnull Player player) {
        wp.subtractEnergy(energyCost, false);
        Utils.playGlobalSound(player.getLocation(), "mage.frostbolt.activation", 2, 0.7f);

        Location location = player.getLocation();
        Vector speed = player.getLocation().getDirection().multiply(SPEED);
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setHelmet(new ItemStack(Material.FIREBALL));
        stand.setGravity(false);
        stand.setVisible(false);
        new GameRunnable(wp.getGame()) {
            @Override
            public void run() {
                quarterStep(false);
                quarterStep(false);
                quarterStep(false);
                quarterStep(false);
                quarterStep(false);
                quarterStep(false);
                quarterStep(true);
            }

            private void quarterStep(boolean last) {

                if (!stand.isValid()) {
                    this.cancel();
                    return;
                }

                speed.add(new Vector(0, GRAVITY * SPEED, 0));
                Location newLoc = stand.getLocation();
                newLoc.add(speed);
                stand.teleport(newLoc);
                newLoc.add(0, 1.75, 0);

                stand.setHeadPose(new EulerAngle(-speed.getY() * 3, 0, 0));

                boolean shouldExplode;

                if (last) {
                    ParticleEffect.FIREWORKS_SPARK.display(
                            0.1f,
                            0.1f,
                            0.1f,
                            0.1f,
                            4,
                            newLoc.clone().add(0, -1, 0),
                            500
                    );
                }

                WarlordsEntity directHit;
                if (
                    !newLoc.getBlock().isEmpty()
                    && newLoc.getBlock().getType() != Material.GRASS
                    && newLoc.getBlock().getType() != Material.BARRIER
                    && newLoc.getBlock().getType() != Material.VINE
                ) {
                    // Explode based on collision
                    shouldExplode = true;
                } else {
                    directHit = PlayerFilter
                            .entitiesAroundRectangle(newLoc, 1, 2, 1)
                            .aliveEnemiesOf(wp)
                            .findFirstOrNull();
                    shouldExplode = directHit != null;
                }

                if (shouldExplode) {
                    stand.remove();

                    Utils.playGlobalSound(newLoc, Sound.FIRE_IGNITE, 2, 0.1f);

                    FireWorkEffectPlayer.playFirework(newLoc, FireworkEffect.builder()
                            .withColor(Color.ORANGE)
                            .withColor(Color.RED)
                            .with(FireworkEffect.Type.BURST)
                            .build());

                    ParticleEffect.SMOKE_NORMAL.display(0.4f, 0.05f, 0.4f, 0.2f, 100, newLoc, 500);

                    for (WarlordsEntity nearEntity : PlayerFilter
                            .entitiesAround(newLoc, hitbox, hitbox, hitbox)
                            .aliveEnemiesOf(wp)
                    ) {
                        playersHit++;

                        nearEntity.addDamageInstance(
                                wp,
                                name,
                                minDamageHeal,
                                maxDamageHeal,
                                critChance,
                                critMultiplier,
                                false
                        );
                        nearEntity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindDurationInTicks, 0, true, false));
                        nearEntity.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, blindDurationInTicks, 0, true, false));

                        if (pveUpgrade && nearEntity instanceof WarlordsNPC) {
                            FireWorkEffectPlayer.playFirework(newLoc, FireworkEffect.builder()
                                    .withColor(Color.RED)
                                    .withColor(Color.BLACK)
                                    .with(FireworkEffect.Type.BALL_LARGE)
                                    .build());

                            nearEntity.getCooldownManager().removeCooldown(IncendiaryCurse.class);
                            nearEntity.getCooldownManager().addCooldown(new RegularCooldown<>(
                                    name,
                                    "INCEN",
                                    IncendiaryCurse.class,
                                    new IncendiaryCurse(),
                                    wp,
                                    CooldownTypes.DEBUFF,
                                    cooldownManager -> {
                                    },
                                    2 * 20
                            ) {
                                @Override
                                public float modifyDamageBeforeInterveneFromSelf(WarlordsDamageHealingEvent event, float currentDamageValue) {
                                    return currentDamageValue * 1.5f;
                                }
                            });
                        }
                    }

                    this.cancel();
                }
            }

        }.runTaskTimer(0, 1);

        return true;
    }

    public int getBlindDurationInTicks() {
        return blindDurationInTicks;
    }

    public void setBlindDurationInTicks(int blindDurationInTicks) {
        this.blindDurationInTicks = blindDurationInTicks;
    }

    public float getHitbox() {
        return hitbox;
    }

    public void setHitbox(float hitbox) {
        this.hitbox = hitbox;
    }

    public boolean isPveUpgrade() {
        return pveUpgrade;
    }

    public void setPveUpgrade(boolean pveUpgrade) {
        this.pveUpgrade = pveUpgrade;
    }
}
