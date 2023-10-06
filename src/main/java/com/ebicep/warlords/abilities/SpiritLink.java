package com.ebicep.warlords.abilities;

import com.ebicep.warlords.abilities.internal.AbstractChain;
import com.ebicep.warlords.abilities.internal.icon.RedAbilityIcon;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsNPC;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownFilter;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownManager;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.PersistentCooldown;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.AbstractUpgradeBranch;
import com.ebicep.warlords.pve.upgrades.shaman.spiritguard.SpiritLinkBranch;
import com.ebicep.warlords.util.bukkit.LocationUtils;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;


public class SpiritLink extends AbstractChain implements RedAbilityIcon {

    public int numberOfDismounts = 0;
    private double speedDuration = 1.5;
    private double damageReductionDuration = 4.5;

    public SpiritLink() {
        super("Spirit Link", 290, 392, 8.61f, 40, 20, 175, 20, 10, 2);
    }

    @Override
    public void updateDescription(Player player) {
        description = Component.text("Links your spirit with up to ")
                               .append(Component.text("3", NamedTextColor.RED))
                               .append(Component.text(" enemy players, dealing "))
                               .append(formatRangeDamage(minDamageHeal, maxDamageHeal))
                               .append(Component.text(" damage to the first target hit. Each additional hit deals "))
                               .append(Component.text("20%", NamedTextColor.RED))
                               .append(Component.text(" reduced damage. You gain "))
                               .append(Component.text("40%", NamedTextColor.YELLOW))
                               .append(Component.text(" speed for "))
                               .append(Component.text(speedDuration, NamedTextColor.GOLD))
                               .append(Component.text(" seconds, and take "))
                               .append(Component.text("15%", NamedTextColor.RED))
                               .append(Component.text(" reduced damage for "))
                               .append(Component.text(damageReductionDuration, NamedTextColor.GOLD))
                               .append(Component.text(" seconds.\n\nHas an initial cast range of "))
                               .append(Component.text(radius, NamedTextColor.YELLOW))
                               .append(Component.text(" blocks."));
    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        List<Pair<String, String>> info = new ArrayList<>();
        info.add(new Pair<>("Times Used", "" + timesUsed));
        info.add(new Pair<>("Players Hit", "" + playersHit));
        info.add(new Pair<>("Dismounts", "" + numberOfDismounts));

        return info;
    }

    @Override
    public AbstractUpgradeBranch<?> getUpgradeBranch(AbilityTree abilityTree) {
        return new SpiritLinkBranch(abilityTree, this);
    }

    @Override
    protected Set<WarlordsEntity> getEntitiesHitAndActivate(WarlordsEntity wp) {
        Set<WarlordsEntity> hitCounter = new HashSet<>();
        for (WarlordsEntity nearPlayer : PlayerFilter
                .entitiesAround(wp, radius, radius - 2, radius)
                .aliveEnemiesOf(wp)
                .lookingAtFirst(wp)
                .soulBindedFirst(wp)
        ) {
            if (LocationUtils.isLookingAtChain(wp.getEntity(), nearPlayer.getEntity()) && LocationUtils.hasLineOfSight(wp.getEntity(), nearPlayer.getEntity())) {
                playersHit++;
                if (nearPlayer.onHorse()) {
                    numberOfDismounts++;
                }
                chain(wp.getLocation(), nearPlayer.getLocation());
                nearPlayer.addDamageInstance(wp, name, minDamageHeal, maxDamageHeal, critChance, critMultiplier);
                hitCounter.add(nearPlayer);

                List<CooldownManager.LinkInformation> linkInformation = wp.getCooldownManager().getNumberOfBoundPlayersLink(nearPlayer);
                for (CooldownManager.LinkInformation information : linkInformation) {
                    healNearPlayers(wp, nearPlayer, information);
                }

                additionalBounce(wp, hitCounter, nearPlayer, new ArrayList<>(Arrays.asList(wp, nearPlayer)), pveMasterUpgrade2 && linkInformation.size() > 0 ? -1 : 0);

                if (pveMasterUpgrade2 && nearPlayer instanceof WarlordsNPC warlordsNPC) {
                    warlordsNPC.getMob().setTarget(wp);
                    EffectUtils.displayParticle(
                            Particle.SPELL_INSTANT,
                            warlordsNPC.getLocation().add(0, 1.2, 0),
                            5,
                            .25,
                            .25,
                            .25,
                            0
                    );
                }

                break;
            }
        }
        return hitCounter;
    }

    @Override
    protected void onHit(WarlordsEntity we, int hitCounter) {
        we.playSound(we.getLocation(), "mage.firebreath.activation", 1, 1);
        if (we.isInPve()) {
            we.getCooldownManager().limitCooldowns(RegularCooldown.class, SpiritLink.class, 4);
        }
        // speed buff
        we.addSpeedModifier(we, "Spirit Link", 40, (int) (speedDuration * 20)); // 30 is ticks
        we.getCooldownManager().addCooldown(new RegularCooldown<>(
                name,
                "LINK",
                SpiritLink.class,
                new SpiritLink(),
                we,
                CooldownTypes.BUFF,
                cooldownManager -> {
                },
                (int) (damageReductionDuration * 20)
        ) {
            @Override
            public float modifyDamageAfterInterveneFromSelf(WarlordsDamageHealingEvent event, float currentDamageValue) {
                float newDamageValue = currentDamageValue * .85f;
                event.getWarlordsEntity().addAbsorbed(Math.abs(currentDamageValue - newDamageValue));
                return newDamageValue;
            }
        });
    }

    @Override
    protected ItemStack getChainItem() {
        return new ItemStack(Material.SPRUCE_FENCE_GATE);
    }

    private void additionalBounce(WarlordsEntity wp, Set<WarlordsEntity> hitCounter, WarlordsEntity chainTarget, List<WarlordsEntity> toExclude, int bounceCount) {
        float bounceDamageReduction = Math.max(0, 1 - (bounceCount + 1) * .2f);
        if (bounceCount >= additionalBounces || bounceDamageReduction == 0) {
            return;
        }
        for (WarlordsEntity bounceTarget : PlayerFilter
                .entitiesAround(chainTarget, bounceRange, bounceRange, bounceRange)
                .aliveEnemiesOf(wp)
                .excluding(toExclude)
                .soulBindedFirst(wp)
        ) {
            playersHit++;
            if (bounceTarget.onHorse()) {
                numberOfDismounts++;
            }
            chain(chainTarget.getLocation(), bounceTarget.getLocation());
            bounceTarget.addDamageInstance(wp, name, minDamageHeal * bounceDamageReduction, maxDamageHeal * bounceDamageReduction, critChance, critMultiplier);
            hitCounter.add(bounceTarget);

            List<CooldownManager.LinkInformation> linkInformation = wp.getCooldownManager().getNumberOfBoundPlayersLink(bounceTarget);
            for (CooldownManager.LinkInformation information : linkInformation) {
                healNearPlayers(wp, bounceTarget, information);
            }

            toExclude.add(bounceTarget);
            additionalBounce(wp, hitCounter, bounceTarget, toExclude, bounceCount + (pveMasterUpgrade2 && linkInformation.size() > 0 ? 0 : 1));

            if (pveMasterUpgrade2 && bounceTarget instanceof WarlordsNPC warlordsNPC) {
                warlordsNPC.getMob().setTarget(wp);
                EffectUtils.displayParticle(
                        Particle.SPELL_INSTANT,
                        warlordsNPC.getLocation().add(0, 1.2, 0),
                        5,
                        .25,
                        .25,
                        .25,
                        0
                );
            }

            break;
        }
    }

    private void healNearPlayers(WarlordsEntity warlordsPlayer, WarlordsEntity hitPlayer, CooldownManager.LinkInformation linkInformation) {
        float radius = linkInformation.radius();
        int limit = linkInformation.limit();
        //adding .25 to totem, cap 6 sec
//        new CooldownFilter<>(warlordsPlayer, RegularCooldown.class)
//                .filterName("Spirits' Respite")
//                .findFirst()
//                .ifPresent(regularCooldown -> {
//                    regularCooldown.setTicksLeft(Math.min(regularCooldown.getTicksLeft() + 10, 6 * 20));
//                });
        warlordsPlayer.addHealingInstance(warlordsPlayer, "Soulbinding Weapon", 400, 400, 0, 100);
        for (WarlordsEntity nearPlayer : PlayerFilter
                .entitiesAround(warlordsPlayer, radius, radius, radius)
                .aliveTeammatesOfExcludingSelf(warlordsPlayer)
                .closestWarlordPlayersFirst(warlordsPlayer.getLocation())
                .limit(limit)
        ) {
            warlordsPlayer.doOnStaticAbility(Soulbinding.class, Soulbinding::addLinkTeammatesHealed);
            nearPlayer.addHealingInstance(warlordsPlayer, "Soulbinding Weapon", 200, 200, 0, 100);
        }
        new CooldownFilter<>(warlordsPlayer, PersistentCooldown.class)
                .filterCooldownClassAndMapToObjectsOfClass(Soulbinding.class)
                .filter(soulbinding -> soulbinding.hasBoundPlayerSoul(hitPlayer))
                .forEach(soulbinding -> {
                    if (soulbinding.isPveMasterUpgrade()) {
                        warlordsPlayer.addEnergy(warlordsPlayer, "Soulbinding Weapon", 1);
                    }
                });
    }

    public double getSpeedDuration() {
        return speedDuration;
    }

    public void setSpeedDuration(double speedDuration) {
        this.speedDuration = speedDuration;
    }

    public double getDamageReductionDuration() {
        return damageReductionDuration;
    }

    public void setDamageReductionDuration(double damageReductionDuration) {
        this.damageReductionDuration = damageReductionDuration;
    }
}
