package com.ebicep.warlords.abilities;

import com.ebicep.warlords.abilities.internal.AbstractAbility;
import com.ebicep.warlords.abilities.internal.Duration;
import com.ebicep.warlords.abilities.internal.icon.OrangeAbilityIcon;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.events.player.ingame.WarlordsAddCooldownEvent;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.cooldowns.AbstractCooldown;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownFilter;
import com.ebicep.warlords.player.ingame.cooldowns.CooldownTypes;
import com.ebicep.warlords.player.ingame.cooldowns.cooldowns.RegularCooldown;
import com.ebicep.warlords.player.ingame.cooldowns.instances.InstanceFlags;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.pve.upgrades.AbstractUpgradeBranch;
import com.ebicep.warlords.pve.upgrades.arcanist.conjurer.AstralPlagueBranch;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

public class AstralPlague extends AbstractAbility implements OrangeAbilityIcon, Duration {

    private int tickDuration = 240;
    private int hexTickDurationIncrease = 40;

    public AstralPlague() {
        super("Astral Plague", 0, 0, 50, 10, 0, 0);
    }

    @Override
    public void updateDescription(Player player) {
        description = Component.text("Grant yourself Astral Energy, increasing Poisonous Hex duration by ")
                               .append(Component.text(format(hexTickDurationIncrease / 20f), NamedTextColor.GOLD))
                               .append(Component.text(" seconds and causing Soulfire Beam to not consume Poisonous Hex stacks. " +
                                       "\n\nYour attacks pierces shields and defenses of enemies with "))
                               .append(Component.text("3", NamedTextColor.RED))
                               .append(Component.text(" stacks of Poisonous Hex. Lasts"))
                               .append(Component.text(format(tickDuration / 20f), NamedTextColor.GOLD))
                               .append(Component.text(" seconds. "));
    }

    @Override
    public List<Pair<String, String>> getAbilityInfo() {
        return null;
    }

    @Override
    public boolean onActivate(@Nonnull WarlordsEntity wp, Player player) {
        wp.subtractEnergy(energyCost, false);

        Utils.playGlobalSound(wp.getLocation(), "arcanist.astralplague.activation", 2, 1.1f);
        Utils.playGlobalSound(wp.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2, 0.7f);
        EffectUtils.playCircularShieldAnimation(wp.getLocation(), Particle.SOUL, 8, 3, 1);
        EffectUtils.playCircularEffectAround(wp.getGame(), wp.getLocation(), Particle.FLAME, 1, 1, 0.25, 1, 1, 2);

        wp.getCooldownManager().addCooldown(new RegularCooldown<>(
                name,
                "ASTRAL",
                AstralPlague.class,
                new AstralPlague(),
                wp,
                CooldownTypes.ABILITY,
                cooldownManager -> {
                },
                tickDuration
        ) {

            @Override
            public float addCritMultiplierFromAttacker(WarlordsDamageHealingEvent event, float currentCritMultiplier) {
                if (pveMasterUpgrade) {
                    return currentCritMultiplier + 40;
                }
                return currentCritMultiplier;
            }

            @Override
            protected Listener getListener() {
                return new Listener() {

                    @EventHandler(priority = EventPriority.LOWEST)
                    private void onAddCooldown(WarlordsAddCooldownEvent event) {
                        AbstractCooldown<?> cooldown = event.getAbstractCooldown();
                        if (Objects.equals(cooldown.getFrom(), wp) &&
                                cooldown instanceof RegularCooldown<?> regularCooldown &&
                                cooldown.getCooldownObject() instanceof PoisonousHex
                        ) {
                            regularCooldown.setTicksLeft(regularCooldown.getTicksLeft() + hexTickDurationIncrease);
                        }
                    }

                    @EventHandler
                    public void onDamageHeal(WarlordsDamageHealingEvent event) {
                        if (event.isHealingInstance()) {
                            return;
                        }
                        WarlordsEntity victim = event.getWarlordsEntity();
                        if (victim.equals(wp)) {
                            return;
                        }
                        if (!event.getAttacker().equals(wp)) {
                            return;
                        }
                        PoisonousHex fromHex = PoisonousHex.getFromHex(wp);
                        if (new CooldownFilter<>(victim, RegularCooldown.class)
                                .filterCooldownClass(PoisonousHex.class)
                                .stream()
                                .count() < fromHex.getMaxStacks()
                        ) {
                            return;
                        }
                        event.getFlags().add(InstanceFlags.PIERCE_DAMAGE);
                        if (inPve) {
                            event.getFlags().add(InstanceFlags.IGNORE_SELF_RES);
                        }
                        if (pveMasterUpgrade && Objects.equals(event.getAbility(), "Soulfire Beam")) {
                            event.setCritChance(100);
                        }
                    }

                };
            }
        });
        PlayerFilter.playingGame(wp.getGame())
                    .enemiesOf(wp)
                    .forEach(enemy -> {
                        new CooldownFilter<>(enemy, RegularCooldown.class)
                                .filterCooldownClass(PoisonousHex.class)
                                .filterCooldownFrom(wp)
                                .forEach(cd -> cd.setTicksLeft(cd.getTicksLeft() + hexTickDurationIncrease));
                    });
        return true;
    }

    @Override
    public AbstractUpgradeBranch<?> getUpgradeBranch(AbilityTree abilityTree) {
        return new AstralPlagueBranch(abilityTree, this);
    }

    @Override
    public int getTickDuration() {
        return tickDuration;
    }

    @Override
    public void setTickDuration(int tickDuration) {
        this.tickDuration = tickDuration;
    }
}
