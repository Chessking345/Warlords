package com.ebicep.warlords.abilities;

import com.ebicep.warlords.abilities.internal.AbstractStrike;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingFinalEvent;
import com.ebicep.warlords.player.general.SpecType;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsNPC;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownFilter;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.LinkedCooldown;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.player.ingame.cooldowns.instances.InstanceFlags;
import com.ebicep.warlords.pve.mobs.flags.BossLike;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.AbstractUpgradeBranch;
import com.ebicep.warlords.pve.upgrades.warrior.defender.WoundingStrikeBranchDefender;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class WoundingStrikeDefender extends AbstractStrike {

    private int wounding = 25;

    public WoundingStrikeDefender() {
        super("Wounding Strike", 415.8f, 556.5f, 0, 100, 20, 200);
    }

    @Override
    public void updateDescription(Player player) {
        description = Component.text("Strike the targeted enemy player, causing")
                               .append(formatRangeDamage(minDamageHeal, maxDamageHeal))
                               .append(Component.text(" damage and "))
                               .append(Component.text("wounding", NamedTextColor.RED))
                               .append(Component.text(" them for "))
                               .append(Component.text("3", NamedTextColor.GOLD))
                               .append(Component.text(" seconds. A wounded player receives "))
                               .append(Component.text(wounding + "%", NamedTextColor.RED))
                               .append(Component.text(" less healing for the duration of the effect."));
    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        List<Pair<String, String>> info = new ArrayList<>();
        info.add(new Pair<>("Players Stuck", "" + timesUsed));

        return info;
    }

    @Override
    public AbstractUpgradeBranch<?> getUpgradeBranch(AbilityTree abilityTree) {
        return new WoundingStrikeBranchDefender(abilityTree, this);
    }

    @Override
    protected void playSoundAndEffect(Location location) {
        Utils.playGlobalSound(location, "warrior.mortalstrike.impact", 2, 1);
        randomHitEffect(location, 7, 255, 0, 0);
    }

    @Override
    protected boolean onHit(@Nonnull WarlordsEntity wp, @Nonnull WarlordsEntity nearPlayer) {
        nearPlayer.addDamageInstance(
                wp,
                name,
                minDamageHeal,
                maxDamageHeal,
                critChance,
                critMultiplier,
                wp.getCooldownManager().hasCooldown(Intervene.class) && nearPlayer instanceof WarlordsNPC warlordsNPC && !(warlordsNPC.getMob() instanceof BossLike)
                ? EnumSet.of(InstanceFlags.PIERCE) :
                EnumSet.noneOf(InstanceFlags.class)
        ).ifPresent(event -> onFinalEvent(wp, nearPlayer, event));

        if (pveMasterUpgrade2) {
            additionalHit(
                    1,
                    wp,
                    nearPlayer,
                    1,
                    warlordsEntity -> {
                        if (warlordsEntity instanceof WarlordsNPC warlordsNPC && !(warlordsNPC.getMob() instanceof BossLike)) {
                            return EnumSet.of(InstanceFlags.PIERCE);
                        }
                        return EnumSet.noneOf(InstanceFlags.class);
                    },
                    finalEvent -> finalEvent.ifPresent(event -> onFinalEvent(wp, event.getWarlordsEntity(), event))
            );
        }

        return true;
    }

    private void onFinalEvent(@Nonnull WarlordsEntity wp, @Nonnull WarlordsEntity nearPlayer, WarlordsDamageHealingFinalEvent event) {
        if (event.isDead()) {
            return;
        }
        if (event.isCrit() && pveMasterUpgrade) {
            damageReductionOnCrit(wp, nearPlayer);
        }
        if (!(nearPlayer.getCooldownManager().hasCooldownFromName("Wounding Strike"))) {
            nearPlayer.sendMessage(
                    Component.text("You are ", NamedTextColor.GRAY)
                             .append(Component.text("wounded", NamedTextColor.RED))
                             .append(Component.text(".", NamedTextColor.GRAY))
            );
        }
        if (!nearPlayer.getCooldownManager().hasCooldown(WoundingStrikeBerserker.class)) {
            nearPlayer.getCooldownManager().removeCooldownByName("Wounding Strike", true);
            nearPlayer.getCooldownManager().addCooldown(new RegularCooldown<>(
                    name,
                    "WND",
                    WoundingStrikeDefender.class,
                    new WoundingStrikeDefender(),
                    wp,
                    CooldownTypes.DEBUFF,
                    cooldownManager -> {
                    },
                    cooldownManager -> {
                        if (new CooldownFilter<>(cooldownManager, RegularCooldown.class).filterNameActionBar("WND").stream().count() == 1) {
                            nearPlayer.sendMessage(
                                    Component.text("You are no longer ", NamedTextColor.GRAY)
                                             .append(Component.text("wounded", NamedTextColor.RED))
                                             .append(Component.text(".", NamedTextColor.GRAY))
                            );
                        }
                    },
                    3 * 20
            ) {
                @Override
                public float modifyHealingFromSelf(WarlordsDamageHealingEvent event, float currentHealValue) {
                    return currentHealValue * (100 - wounding) / 100f;
                }

                @Override
                public PlayerNameData addSuffixFromOther() {
                    return new PlayerNameData(Component.text("WND", NamedTextColor.RED),
                            we -> we == wp || (we.isTeammate(nearPlayer) && we.getSpecClass().specType == SpecType.HEALER)
                    );
                }
            });
        }
    }

    private void damageReductionOnCrit(WarlordsEntity we, WarlordsEntity nearPlayer) {
        Set<WarlordsEntity> teammates = PlayerFilter
                .entitiesAround(nearPlayer, 10, 10, 10)
                .aliveTeammatesOfExcludingSelf(we)
                .stream()
                .collect(Collectors.toSet());
        LinkedCooldown<?> linkedCooldown = new LinkedCooldown<>(
                name + " Resistance",
                "STRIKE RES",
                WoundingStrikeDefender.class,
                new WoundingStrikeDefender(),
                we,
                CooldownTypes.BUFF,
                cooldownManager -> {
                },
                cooldownManager -> {
                },
                5 * 20,
                Collections.emptyList(),
                teammates
        ) {
            @Override
            public float modifyDamageAfterInterveneFromSelf(WarlordsDamageHealingEvent event, float currentDamageValue) {
                return currentDamageValue * 0.7f;
            }
        };

        we.getCooldownManager().removeCooldownByName(name + " Resistance");
        we.getCooldownManager().addCooldown(linkedCooldown);
        for (WarlordsEntity teammate : teammates) {
            teammate.getCooldownManager().removeCooldownByName(name + " Resistance");
            teammate.getCooldownManager().addCooldown(linkedCooldown);
        }
    }

    public int getWounding() {
        return wounding;
    }

    public void setWounding(int wounding) {
        this.wounding = wounding;
    }
}