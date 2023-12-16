package com.ebicep.warlords.abilities;

import com.ebicep.warlords.abilities.internal.AbstractAbility;
import com.ebicep.warlords.abilities.internal.Duration;
import com.ebicep.warlords.abilities.internal.icon.BlueAbilityIcon;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.events.player.ingame.WarlordsAbilityTargetEvent;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.player.general.Specializations;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.LinkedCooldown;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.AbstractUpgradeBranch;
import com.ebicep.warlords.pve.upgrades.rogue.apothecary.RemedicChainsBranch;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class RemedicChains extends AbstractAbility implements BlueAbilityIcon, Duration {

    public int playersLinked = 0;
    public int numberOfBrokenLinks = 0;

    private float healingMultiplier = 12.5f; // %
    private float allyDamageIncrease = 12; // %
    private int tickDuration = 160;
    private int alliesAffected = 3;
    private int linkBreakRadius = 15;
    private int castRange = 10;

    public RemedicChains() {
        super("Remedic Chains", 728, 815, 16, 50, 20, 200);
    }

    @Override
    public void updateDescription(Player player) {
        description = Component.text("Bind yourself to ")
                               .append(Component.text(alliesAffected, NamedTextColor.YELLOW))
                               .append(Component.text(" allies near you, increasing the damage they deal by "))
                               .append(Component.text(format(allyDamageIncrease) + "%", NamedTextColor.RED))
                               .append(Component.text(" as long as the link is active. Lasts "))
                               .append(Component.text(format(tickDuration / 20f) + " ", NamedTextColor.GOLD))
                               .append(Component.text("seconds.\n\nWhen the link expires you and the allies are healed for "))
                               .append(formatRangeHealing(minDamageHeal, maxDamageHeal))
                               .append(Component.text(" health. Breaking the link early will only heal the allies for "))
                               .append(Component.text(format(healingMultiplier) + "%", NamedTextColor.GREEN))
                               .append(Component.text(" of the original amount for each second they have been linked.\n\nThe link will break if you are "))
                               .append(Component.text(linkBreakRadius + " ", NamedTextColor.YELLOW))
                               .append(Component.text("blocks apart."));
    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        List<Pair<String, String>> info = new ArrayList<>();
        info.add(new Pair<>("Times Used", "" + timesUsed));
        info.add(new Pair<>("Players Linked", "" + playersLinked));
        info.add(new Pair<>("Times Link Broke", "" + numberOfBrokenLinks));

        return info;
    }

    @Override
    public boolean onActivate(@Nonnull WarlordsEntity wp) {
        Set<WarlordsEntity> teammatesNear = PlayerFilter
                .entitiesAround(wp, castRange, castRange, castRange)
                .aliveTeammatesOfExcludingSelf(wp)
                .closestFirst(wp)
                .limit(alliesAffected)
                .stream()
                .collect(Collectors.toSet());

        if (teammatesNear.size() < 1) {
            wp.sendMessage(Component.text("There are no allies nearby to link!", NamedTextColor.RED));
            return false;
        }


        Utils.playGlobalSound(wp.getLocation(), "rogue.remedicchains.activation", 2, 0.2f);

        HashMap<WarlordsEntity, Float> healthBoosts = new HashMap<>();
        teammatesNear.forEach(warlordsEntity -> {
            wp.sendMessage(WarlordsEntity.GIVE_ARROW_GREEN
                    .append(Component.text(" Your Remedic Chains is now protecting ", NamedTextColor.GRAY))
                    .append(Component.text(warlordsEntity.getName(), NamedTextColor.YELLOW))
                    .append(Component.text("!", NamedTextColor.GRAY))
            );
            warlordsEntity.sendMessage(WarlordsEntity.RECEIVE_ARROW_GREEN
                    .append(Component.text(" " + wp.getName() + "'s", NamedTextColor.GRAY))
                    .append(Component.text(" Remedic Chains", NamedTextColor.YELLOW))
                    .append(Component.text(" is now increasing your ", NamedTextColor.GRAY))
                    .append(Component.text("damage", NamedTextColor.RED))
                    .append(Component.text(" for ", NamedTextColor.GRAY))
                    .append(Component.text(format(tickDuration / 20f), NamedTextColor.GOLD))
                    .append(Component.text(" seconds!", NamedTextColor.GRAY))
            );
            float healthIncrease = warlordsEntity.getMaxHealth() * .25f;
            healthBoosts.put(warlordsEntity, healthIncrease);
            if (pveMasterUpgrade) {
                warlordsEntity.setMaxHealth(warlordsEntity.getMaxHealth() + healthIncrease);
            }

        });

        if (pveMasterUpgrade) {
            float healthIncrease = wp.getMaxHealth() * .25f;
            healthBoosts.put(wp, healthIncrease);
            wp.setMaxHealth(wp.getMaxHealth() + healthIncrease);
        }

        RemedicChains tempRemedicChain = new RemedicChains();
        LinkedCooldown<RemedicChains> remedicChainsCooldown = new LinkedCooldown<>(
                name,
                "REMEDIC",
                RemedicChains.class,
                tempRemedicChain,
                wp,
                CooldownTypes.ABILITY,
                (cooldownManager, linkedCooldown) -> {
                    if (!Objects.equals(cooldownManager.getWarlordsEntity(), wp)) {
                        return;
                    }
                    if (wp.isDead()) {
                        return;
                    }
                    wp.addHealingInstance(
                            wp,
                            name,
                            minDamageHeal,
                            maxDamageHeal,
                            critChance,
                            critMultiplier
                    );
                    for (WarlordsEntity linkedEntity : linkedCooldown.getLinkedEntities()) {
                        linkedEntity.addHealingInstance(
                                wp,
                                name,
                                minDamageHeal,
                                maxDamageHeal,
                                critChance,
                                critMultiplier
                        );
                    }
                },
                (cooldownManager, linkedCooldown) -> {
                    if (!Objects.equals(cooldownManager.getWarlordsEntity(), wp)) {
                        return;
                    }
                    if (pveMasterUpgrade) {
                        healthBoosts.forEach((entity, aFloat) -> entity.setMaxHealth(entity.getMaxHealth() - aFloat));
                    }
                },
                tickDuration,
                Collections.singletonList((cooldown, ticksLeft, ticksElapsed) -> {
                    if (ticksElapsed % 8 != 0) {
                        return;
                    }
                    Set<WarlordsEntity> linkedEntities = cooldown.getLinkedEntities();
                    Set<WarlordsEntity> toRemove = new HashSet<>();
                    for (WarlordsEntity linked : linkedEntities) {
                        boolean outOfRange = wp.getLocation().distanceSquared(linked.getLocation()) > linkBreakRadius * linkBreakRadius;
                        if (outOfRange) {
                            linked.getCooldownManager().removeCooldownNoForce(cooldown);
                            Utils.playGlobalSound(linked.getLocation(), "rogue.remedicchains.impact", 0.1f, 1.4f);
                            linked.getWorld().spawnParticle(
                                    Particle.VILLAGER_HAPPY,
                                    linked.getLocation().add(0, 1, 0),
                                    10,
                                    0.5,
                                    0.5,
                                    0.5,
                                    1,
                                    null,
                                    true
                            );
                            // Ally is out of range, break link
                            numberOfBrokenLinks++;

                            float totalHealingMultiplier = ((healingMultiplier / 100f) * (ticksElapsed / 20f));
                            linked.addHealingInstance(
                                    wp,
                                    name,
                                    minDamageHeal * totalHealingMultiplier,
                                    maxDamageHeal * totalHealingMultiplier,
                                    0,
                                    100
                            );
                        }
                        EffectUtils.playParticleLinkAnimation(wp.getLocation(), linked.getLocation(), 250, 200, 250, 1);
                        if (outOfRange || linked.isDead()) {
                            toRemove.add(linked);
                            if (pveMasterUpgrade) {
                                linked.setMaxHealth(linked.getMaxHealth() - healthBoosts.get(linked));
                            }
                        }
                    }
                    linkedEntities.removeAll(toRemove);
                }),
                teammatesNear
        ) {
            private final ImpalingStrike impalingStrike = new ImpalingStrike();

            @Override
            public float modifyDamageBeforeInterveneFromAttacker(WarlordsDamageHealingEvent event, float currentDamageValue) {
                return currentDamageValue * (1 + allyDamageIncrease / 100f);
            }

            @Override
            public void onEndFromAttacker(WarlordsDamageHealingEvent event, float currentDamageValue, boolean isCrit) {
                if (!pveMasterUpgrade2) {
                    return;
                }
                if (!event.getAbility().contains("Strike")) {
                    return;
                }
                switch (Specializations.getClass(event.getAttacker().getSpecClass())) {
                    case WARRIOR, PALADIN, ROGUE -> ImpalingStrike.giveLeechCooldown(
                            event.getAttacker(),
                            event.getWarlordsEntity(),
                            impalingStrike.getLeechDuration(),
                            impalingStrike.getLeechSelfAmount() / 100f,
                            impalingStrike.getLeechAllyAmount() / 100f,
                            warlordsDamageHealingFinalEvent -> {
                            }
                    );
                    default -> {
                    }
                }
            }

            @Override
            public float addEnergyPerHit(WarlordsEntity we, float energyPerHit) {
                if (!pveMasterUpgrade2) {
                    return energyPerHit;
                }
                return switch (Specializations.getClass(we.getSpecClass())) {
                    case MAGE, SHAMAN, ARCANIST -> energyPerHit * 2;
                    default -> energyPerHit;
                };
            }
        };
        wp.getCooldownManager().removeCooldown(RemedicChains.class, false);
        wp.getCooldownManager().addCooldown(remedicChainsCooldown);
        teammatesNear.forEach(entity -> entity.getCooldownManager().removeCooldown(RemedicChains.class, false));
        teammatesNear.forEach(entity -> entity.getCooldownManager().addCooldown(remedicChainsCooldown));
        Bukkit.getPluginManager().callEvent(new WarlordsAbilityTargetEvent.WarlordsBlueAbilityTargetEvent(wp, name, teammatesNear));

        return true;
    }

    @Override
    public AbstractUpgradeBranch<?> getUpgradeBranch(AbilityTree abilityTree) {
        return new RemedicChainsBranch(abilityTree, this);
    }

    public int getLinkBreakRadius() {
        return linkBreakRadius;
    }

    public void setLinkBreakRadius(int linkBreakRadius) {
        this.linkBreakRadius = linkBreakRadius;
    }

    public int getCastRange() {
        return castRange;
    }

    public void setCastRange(int castRange) {
        this.castRange = castRange;
    }

    public int getAlliesAffected() {
        return alliesAffected;
    }

    public void setAlliesAffected(int alliesAffected) {
        this.alliesAffected = alliesAffected;
    }

    @Override
    public int getTickDuration() {
        return tickDuration;
    }

    @Override
    public void setTickDuration(int tickDuration) {
        this.tickDuration = tickDuration;
    }

    public float getHealingMultiplier() {
        return healingMultiplier;
    }

    public void setHealingMultiplier(float healingMultiplier) {
        this.healingMultiplier = healingMultiplier;
    }

    public float getAllyDamageIncrease() {
        return allyDamageIncrease;
    }

    public void setAllyDamageIncrease(float allyDamageIncrease) {
        this.allyDamageIncrease = allyDamageIncrease;
    }


}
