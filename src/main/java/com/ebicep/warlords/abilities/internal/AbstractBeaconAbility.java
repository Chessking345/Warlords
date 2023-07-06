package com.ebicep.warlords.abilities.internal;

import com.ebicep.warlords.abilities.BeaconOfShadow;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.effects.circle.CircleEffect;
import com.ebicep.warlords.effects.circle.CircumferenceEffect;
import com.ebicep.warlords.effects.circle.LineEffect;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.util.bukkit.LocationUtils;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.Collections;

public abstract class AbstractBeaconAbility<T extends AbstractBeaconAbility<T>> extends AbstractAbility implements Duration {

    protected Location groundLocation; // not static
    protected CircleEffect effect; // not static
    protected float radius; // not static
    protected int tickDuration;

    public AbstractBeaconAbility(
            String name,
            float minDamageHeal,
            float maxDamageHeal,
            float cooldown,
            float energyCost,
            float critChance,
            float critMultiplier,
            Location groundLocation,
            int radius,
            int secondDuration,
            CircleEffect effect
    ) {
        super(name, minDamageHeal, maxDamageHeal, cooldown, energyCost, critChance, critMultiplier);
        this.groundLocation = groundLocation;
        this.radius = radius;
        this.tickDuration = secondDuration * 20;
        this.effect = effect;
    }

    @Override
    public void updateDescription(Player player) {
        description = Component.text("Place a stationary beacon on the ground that lasts ")
                               .append(Component.text(format(tickDuration / 20f), NamedTextColor.GOLD))
                               .append(Component.text(" seconds. "))
                               .append(getBonusDescription());
    }

    public abstract Component getBonusDescription();

    @Override
    public boolean onActivate(@Nonnull WarlordsEntity wp, Player player) {
        wp.subtractEnergy(energyCost, false);
        wp.getCooldownManager().limitCooldowns(RegularCooldown.class, AbstractBeaconAbility.class, 1);
        Location groundLocation = LocationUtils.getGroundLocation(player);

        Utils.playGlobalSound(groundLocation, "arcanist.beacon.impact", 0.3f, 1);
        Utils.playGlobalSound(groundLocation, "arcanist.beaconshadow.activation", 2, 1);

        CircleEffect teamCircleEffect = new CircleEffect(
                wp.getGame(),
                wp.getTeam(),
                groundLocation,
                radius,
                new CircumferenceEffect(Particle.VILLAGER_HAPPY, Particle.REDSTONE),
                getLineEffect(groundLocation)
        );

        ArmorStand beacon = Utils.spawnArmorStand(
                groundLocation.clone().add(0, -1.425, 0),
                armorStand -> armorStand.getEquipment().setHelmet(new ItemStack(Material.BEACON))
        );

        new GameRunnable(wp.getGame()) {
            int interval = 4;
            @Override
            public void run() {
                interval--;
                EffectUtils.playSphereAnimation(
                        beacon.getLocation(),
                        2.5 + interval,
                        60,
                        0,
                        40
                );

                if (interval <= 0) {
                    this.cancel();
                }
            }
        }.runTaskTimer(0, 2);

        String soundString = getBeaconClass() == BeaconOfShadow.class ? "arcanist.beaconimpair.activation" : "arcanist.beaconlight.activation";
        Utils.playGlobalSound(beacon.getLocation(), soundString, 0.07f, 0.4f);

        wp.getCooldownManager().addCooldown(new RegularCooldown<>(
                name,
                getAbbreviation(),
                getBeaconClass(),
                getObject(groundLocation, teamCircleEffect),
                wp,
                CooldownTypes.ABILITY,
                cooldownManager -> {
                },
                cooldownManager -> {
                    beacon.remove();
                },
                false,
                tickDuration,
                Collections.singletonList((cooldown, ticksLeft, ticksElapsed) -> {
                    //particle effects
                    teamCircleEffect.playEffects();
                    whileActive(wp, cooldown, ticksLeft, ticksElapsed);
                })
        ));
        return true;
    }

    public abstract LineEffect getLineEffect(Location target);

    public abstract String getAbbreviation();

    public abstract Class<T> getBeaconClass();

    public abstract T getObject(Location groundLocation, CircleEffect effect);

    public abstract void whileActive(@Nonnull WarlordsEntity wp, RegularCooldown<T> cooldown, Integer ticksLeft, Integer ticksElapsed);

    public abstract Material getGlassMaterial();

    @Override
    public int getTickDuration() {
        return tickDuration;
    }

    @Override
    public void setTickDuration(int tickDuration) {
        this.tickDuration = tickDuration;
    }

    public Location getGroundLocation() {
        return groundLocation;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
        if (effect != null) {
            effect.setRadius(radius);
        }
    }
}
