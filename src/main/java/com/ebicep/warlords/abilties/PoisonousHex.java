package com.ebicep.warlords.abilties;

import com.ebicep.warlords.abilties.internal.AbstractPiercingProjectile;
import com.ebicep.warlords.abilties.internal.Duration;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownFilter;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.util.bukkit.LocationBuilder;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PoisonousHex extends AbstractPiercingProjectile implements Duration {

    @Nonnull
    public static PoisonousHex getFromHex(WarlordsEntity from) {
        return Arrays.stream(from.getSpec().getAbilities()).filter(PoisonousHex.class::isInstance)
                     .map(PoisonousHex.class::cast)
                     .findFirst()
                     .orElse(new PoisonousHex());
    }

    private int maxFullDistance = 30;
    private int hexStacksPerHit = 1;
    private float dotMinDamage = 34;
    private float dotMaxDamage = 46;
    private int maxStacks = 3;
    private double hitBox = 3.5;
    private int tickDuration = 80;

    public PoisonousHex() {
        super("Poisonous Hex", 351, 474, 0, 80, 20, 175, 2, 300, false);
        this.shotsFiredAtATime = 2;
        this.maxAngleOfShots = 30;
        this.forwardTeleportAmount = 1.6f;
    }

    @Override
    public void updateDescription(Player player) {
        description = Component.text("Throw Hex Fangs in front of you, dealing ")
                               .append(formatRangeDamage(minDamageHeal, maxDamageHeal))
                               .append(Component.text(" damage to up to 2 enemies. Additionally, hit targets receive "))
                               .append(Component.text(hexStacksPerHit, NamedTextColor.BLUE))
                               .append(Component.text(" stack" + (hexStacksPerHit != 1 ? "s" : "") + " of Poisonous Hex. Dealing "))
                               .append(formatRangeDamage(dotMinDamage, dotMaxDamage))
                               .append(Component.text(" damage every "))
                               .append(Component.text("2", NamedTextColor.GOLD))
                               .append(Component.text(" seconds. for "))
                               .append(Component.text("2", NamedTextColor.RED))
                               .append(Component.text(" times. Stacks up to "))
                               .append(Component.text(maxStacks, NamedTextColor.RED))
                               .append(Component.text(" times."))
                               .append(Component.text("\n\nHas an optimal range of "))
                               .append(Component.text(maxDistance, NamedTextColor.YELLOW))
                               .append(Component.text("blocks."));
    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        return null;
    }

    @Override
    protected void playEffect(@Nonnull Location currentLocation, int ticksLived) {

    }

    @Override
    protected int onHit(@Nonnull InternalProjectile projectile, @Nullable WarlordsEntity hit) {
        if (projectile.getHit().size() >= 2) {
            return 0;
        }
        WarlordsEntity wp = projectile.getShooter();
        Location currentLocation = projectile.getCurrentLocation();
        Location startingLocation = projectile.getStartingLocation();

        Utils.playGlobalSound(currentLocation, "shaman.lightningbolt.impact", 2, 1);

        double distanceSquared = startingLocation.distanceSquared(currentLocation);
        double toReduceBy = maxFullDistance * maxFullDistance > distanceSquared ? 1 :
                            1 - (Math.sqrt(distanceSquared) - maxFullDistance) / 75;
        if (toReduceBy < .2) {
            toReduceBy = .2;
        }
        int playersHit = 0;
        for (WarlordsEntity enemy : PlayerFilter
                .entitiesAround(currentLocation, hitBox, hitBox, hitBox)
                .aliveEnemiesOf(wp)
                .excluding(projectile.getHit())
        ) {
            getProjectiles(projectile).forEach(p -> p.getHit().add(enemy));
            playersHit++;
            if (enemy.onHorse()) {
                numberOfDismounts++;
            }
            boolean trueDamage = wp.getCooldownManager().hasCooldown(AstralPlague.class) && new CooldownFilter<>(enemy, RegularCooldown.class)
                    .filterCooldownClass(PoisonousHex.class)
                    .stream()
                    .count() == 3;
            enemy.addDamageInstance(
                    wp,
                    name,
                    (float) (minDamageHeal * toReduceBy),
                    (float) (maxDamageHeal * toReduceBy),
                    critChance,
                    critMultiplier,
                    trueDamage
            );
            givePoisonousHex(wp, enemy);
            if (projectile.getHit().size() >= 2) {
                getProjectiles(projectile).forEach(InternalProjectile::cancel);
                break;
            }
        }

        return playersHit;
    }

    private void givePoisonousHex(WarlordsEntity from, WarlordsEntity to) {
        boolean trueDamage = from.getCooldownManager().hasCooldown(AstralPlague.class) && new CooldownFilter<>(to, RegularCooldown.class)
                .filterCooldownClass(AstralPlague.class)
                .stream()
                .count() == 3;
        to.getCooldownManager().limitCooldowns(RegularCooldown.class, PoisonousHex.class, 3);
        to.getCooldownManager().addCooldown(new RegularCooldown<>(
                "Poisonous Hex",
                "PHEX",
                PoisonousHex.class,
                new PoisonousHex(),
                from,
                CooldownTypes.DEBUFF,
                cooldownManager -> {

                },
                tickDuration + (from.getCooldownManager().hasCooldown(AstralPlague.class) ? 80 : 40), // base add 20 to delay damage by a second
                Collections.singletonList((cooldown, ticksLeft, ticksElapsed) -> {
                    if (ticksElapsed % 40 == 0 && ticksElapsed != 0) {
                        to.addDamageInstance(
                                from,
                                name,
                                dotMinDamage,
                                dotMaxDamage,
                                0,
                                100,
                                trueDamage
                        );
                    }
                })
        ));
    }

    @Override
    protected boolean shouldEndProjectileOnHit(@Nonnull InternalProjectile projectile, WarlordsEntity wp) {
        return false;
    }

    @Override
    protected boolean shouldEndProjectileOnHit(@Nonnull InternalProjectile projectile, Block block) {
        return true;
    }

    @Override
    protected void onNonCancellingHit(@Nonnull InternalProjectile projectile, @Nonnull WarlordsEntity hit, @Nonnull Location impactLocation) {
        if (projectile.getHit().size() >= 2) {
            return;
        }
        WarlordsEntity wp = projectile.getShooter();
        Location currentLocation = projectile.getCurrentLocation();
        Location startingLocation = projectile.getStartingLocation();

        Utils.playGlobalSound(currentLocation, "shaman.lightningbolt.impact", 2, 1);

        double distanceSquared = startingLocation.distanceSquared(currentLocation);
        double toReduceBy = maxFullDistance * maxFullDistance > distanceSquared ? 1 :
                            1 - (Math.sqrt(distanceSquared) - maxFullDistance) / 75;
        if (toReduceBy < .2) {
            toReduceBy = .2;
        }
        for (WarlordsEntity enemy : PlayerFilter
                .entitiesAround(currentLocation, hitBox, hitBox, hitBox)
                .aliveEnemiesOf(wp)
                .excluding(projectile.getHit())
        ) {
            getProjectiles(projectile).forEach(p -> p.getHit().add(enemy));
            if (enemy.onHorse()) {
                numberOfDismounts++;
            }

            enemy.addDamageInstance(
                    wp,
                    name,
                    (float) (minDamageHeal * toReduceBy),
                    (float) (maxDamageHeal * toReduceBy),
                    critChance,
                    critMultiplier,
                    wp.getCooldownManager().hasCooldown(AstralPlague.class) && new CooldownFilter<>(enemy, RegularCooldown.class)
                            .filterCooldownClass(AstralPlague.class)
                            .stream()
                            .count() == 3
            );
            givePoisonousHex(wp, enemy);
            if (projectile.getHit().size() >= 2) {
                getProjectiles(projectile).forEach(InternalProjectile::cancel);
                break;
            }
        }
    }

    @Override
    protected Location getProjectileStartingLocation(WarlordsEntity shooter, Location startingLocation) {
        return new LocationBuilder(startingLocation.clone()).addY(-.5).backward(0f);
    }

    @Override
    protected void onSpawn(@Nonnull InternalProjectile projectile) {
        super.onSpawn(projectile);
        ArmorStand fallenSoul = Utils.spawnArmorStand(projectile.getStartingLocation().clone().add(0, -1.7, 0), armorStand -> {
            armorStand.setMarker(true);
            armorStand.getEquipment().setHelmet(new ItemStack(Material.CRIMSON_FENCE));
            armorStand.setHeadPose(new EulerAngle(-Math.atan2(
                    projectile.getSpeed().getY(),
                    Math.sqrt(
                            Math.pow(projectile.getSpeed().getX(), 2) +
                                    Math.pow(projectile.getSpeed().getZ(), 2)
                    )
            ), 0, 0));
        });

        projectile.addTask(new InternalProjectileTask() {
            @Override
            public void run(InternalProjectile projectile) {
                fallenSoul.teleport(projectile.getCurrentLocation().clone().add(0, -1.7, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);
                projectile.getCurrentLocation().getWorld().spawnParticle(
                        Particle.SPELL_WITCH,
                        projectile.getCurrentLocation().clone().add(0, 0, 0),
                        1,
                        0,
                        0,
                        0,
                        0,
                        null,
                        true
                );
            }

            @Override
            public void onDestroy(InternalProjectile projectile) {
                fallenSoul.remove();
                projectile.getCurrentLocation().getWorld().spawnParticle(
                        Particle.SPELL_WITCH,
                        projectile.getCurrentLocation(),
                        1,
                        0,
                        0,
                        0,
                        0.7f,
                        null,
                        true
                );
            }
        });
    }

    @Nullable
    @Override
    protected String getActivationSound() {
        return "shaman.lightningbolt.impact";
    }

    @Override
    protected float getSoundVolume() {
        return 2;
    }

    @Override
    protected float getSoundPitch() {
        return 1.5f;
    }

    @Override
    public int getTickDuration() {
        return tickDuration;
    }

    @Override
    public void setTickDuration(int tickDuration) {
        this.tickDuration = tickDuration;
    }

    public float getDotMinDamage() {
        return dotMinDamage;
    }

    public void setDotMinDamage(float dotMinDamage) {
        this.dotMinDamage = dotMinDamage;
    }

    public float getDotMaxDamage() {
        return dotMaxDamage;
    }

    public void setDotMaxDamage(float dotMaxDamage) {
        this.dotMaxDamage = dotMaxDamage;
    }

    public int getMaxStacks() {
        return maxStacks;
    }
}
