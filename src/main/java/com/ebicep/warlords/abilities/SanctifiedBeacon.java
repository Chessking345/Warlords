package com.ebicep.warlords.abilities;

import com.ebicep.warlords.abilities.internal.AbstractBeaconAbility;
import com.ebicep.warlords.abilities.internal.icon.BlueAbilityIcon;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.effects.circle.CircleEffect;
import com.ebicep.warlords.effects.circle.LineEffect;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.AbstractUpgradeBranch;
import com.ebicep.warlords.pve.upgrades.arcanist.luminary.SanctifiedBeaconBranch;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class SanctifiedBeacon extends AbstractBeaconAbility<SanctifiedBeacon> implements BlueAbilityIcon {

    private int critMultiplierReducedTo = 100;
    private ArmorStand crystal;
    private int hexIntervalTicks = 80;
    private float damageReductionPve = 30;
    private final int maxAllies = 2;

    public SanctifiedBeacon() {
        this(null, null);
    }

    public SanctifiedBeacon(Location location, CircleEffect effect) {
        super("Sanctified Beacon", 0, 0, 15, 40, 0, 0, location, 8, 20, effect);
    }

    @Override
    public Component getBonusDescription() {
        return Component.text("All enemies within a ")
                        .append(Component.text(format(radius), NamedTextColor.YELLOW))
                        .append(Component.text(" block radius have their Crit Multiplier reduced to "))
                        .append(Component.text(critMultiplierReducedTo + "%", NamedTextColor.RED))
                        .append(Component.text(". The beacon will emit a wave of energy that grants "))
                        .append(Component.text(maxAllies, NamedTextColor.YELLOW))
                        .append(Component.text(" nearby allies "))
                        .append(Component.text("1", NamedTextColor.BLUE))
                        .append(Component.text(" stack of Merciful Hex every "))
                        .append(Component.text(format(hexIntervalTicks / 20f), NamedTextColor.GOLD))
                        .append(Component.text(" seconds within the same radius.\n\nOnly one beacon can be present on the field at once."));
    }

    @Override
    public LineEffect getLineEffect(Location target) {
        return new LineEffect(target.clone().add(0, 0.5, 0), Particle.REDSTONE, new Particle.DustOptions(Color.fromRGB(150, 8, 80), 1));
    }

    @Override
    public String getAbbreviation() {
        return "BEACON";
    }

    @Override
    public Class<SanctifiedBeacon> getBeaconClass() {
        return SanctifiedBeacon.class;
    }

    @Override
    public SanctifiedBeacon getObject(Location groundLocation, CircleEffect effect) {
        crystal = Utils.spawnArmorStand(groundLocation, armorStand -> {
            armorStand.setGravity(true);
            armorStand.setMarker(true);
            armorStand.getEquipment().setHelmet(new ItemStack(Material.BROWN_STAINED_GLASS_PANE));
        });
        return new SanctifiedBeacon(groundLocation, effect);

    }

    @Override
    public void whileActive(@Nonnull WarlordsEntity wp, RegularCooldown<SanctifiedBeacon> cooldown, Integer ticksLeft, Integer ticksElapsed) {
        SanctifiedBeacon beacon = cooldown.getCooldownObject();
        float rad = beacon.getRadius();
        if (ticksElapsed % 5 == 0) {
            for (WarlordsEntity enemy : PlayerFilter
                    .entitiesAround(beacon.getGroundLocation(), rad, rad, rad)
                    .aliveEnemiesOf(wp)
            ) {
                enemy.getCooldownManager().removeCooldownByObject(this);
                enemy.getCooldownManager().addCooldown(new RegularCooldown<>(
                        name,
                        null,
                        SanctifiedBeacon.class,
                        beacon,
                        wp,
                        CooldownTypes.ABILITY,
                        cooldownManager -> {
                        },
                        6 // a little longer to make sure there's no gaps in the effect
                ) {
                    @Override
                    public float setCritMultiplierFromAttacker(WarlordsDamageHealingEvent event, float currentCritMultiplier) {
                        return critMultiplierReducedTo;
                    }

                    @Override
                    public float modifyDamageBeforeInterveneFromAttacker(WarlordsDamageHealingEvent event, float currentDamageValue) {
                        if (wp.isInPve()) {
                            return currentDamageValue * convertToDivisionDecimal(damageReductionPve);
                        }
                        return currentDamageValue;
                    }
                });
                if (pveMasterUpgrade) {
                    enemy.getSpeed().removeModifier(name);
                    enemy.addSpeedModifier(wp, name, -30, 6, "BASE");
                }
            }
        }

        int yawIncrease = ticksElapsed % hexIntervalTicks == 0 ? 120 : 10;
        if (ticksElapsed % 2 == 0) {
            Location crystalLocation = crystal.getLocation();
            crystalLocation.setYaw(crystalLocation.getYaw() + yawIncrease);
            crystal.teleport(crystalLocation);
        }

        if (ticksElapsed % hexIntervalTicks == 0 && ticksElapsed != 0) {
            for (WarlordsEntity ally : PlayerFilter
                    .entitiesAround(beacon.getGroundLocation(), rad, rad, rad)
                    .aliveTeammatesOf(wp)
                    .closestFirst(beacon.getGroundLocation())
                    .limit(maxAllies)
            ) {
                EffectUtils.playParticleLinkAnimation(
                        crystal.getLocation().clone().add(0, .5, 0),
                        ally.getLocation(),
                        20, 200, 20,
                        2
                );
                MercifulHex.giveMercifulHex(wp, ally);
            }

            Utils.playGlobalSound(crystal.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1, 2);
            EffectUtils.playCircularEffectAround(
                    wp.getGame(),
                    crystal.getLocation(),
                    Particle.TOTEM,
                    3,
                    1,
                    0.15,
                    4,
                    1,
                    4
            );
            EffectUtils.playCircularEffectAround(
                    wp.getGame(),
                    crystal.getLocation(),
                    Particle.VILLAGER_HAPPY,
                    1,
                    1,
                    0.1,
                    8,
                    1,
                    3
            );
        }
    }

    @Override
    public ArmorStand getCrystal() {
        return crystal;
    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        List<Pair<String, String>> info = new ArrayList<>();
        info.add(new Pair<>("Times Used", "" + timesUsed));
        return info;
    }

    @Override
    public AbstractUpgradeBranch<?> getUpgradeBranch(AbilityTree abilityTree) {
        return new SanctifiedBeaconBranch(abilityTree, this);
    }

    public int getCritMultiplierReducedTo() {
        return critMultiplierReducedTo;
    }

    public void setCritMultiplierReducedTo(int critMultiplierReducedTo) {
        this.critMultiplierReducedTo = critMultiplierReducedTo;
    }

    public int getHexIntervalTicks() {
        return hexIntervalTicks;
    }

    public void setHexIntervalTicks(int hexIntervalTicks) {
        this.hexIntervalTicks = hexIntervalTicks;
    }

    public float getDamageReductionPve() {
        return damageReductionPve;
    }

    public void setDamageReductionPve(float damageReductionPve) {
        this.damageReductionPve = damageReductionPve;
    }
}
