package com.ebicep.warlords.pve.weapons.weapontypes.legendaries.titles;

import com.ebicep.warlords.abilties.UndyingArmy;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.effects.ParticleEffect;
import com.ebicep.warlords.events.player.ingame.WarlordsAddCooldownEvent;
import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.game.Team;
import com.ebicep.warlords.game.option.pve.PveOption;
import com.ebicep.warlords.player.ingame.WarlordsPlayer;
import com.ebicep.warlords.player.ingame.cooldowns.AbstractCooldown;
import com.ebicep.warlords.pve.Spendable;
import com.ebicep.warlords.pve.mobs.mobtypes.BossMob;
import com.ebicep.warlords.pve.mobs.zombie.BasicZombie;
import com.ebicep.warlords.pve.weapons.weapontypes.legendaries.AbstractLegendaryWeapon;
import com.ebicep.warlords.pve.weapons.weapontypes.legendaries.LegendaryTitles;
import com.ebicep.warlords.util.bukkit.HeadUtils;
import com.ebicep.warlords.util.java.Pair;
import com.ebicep.warlords.util.java.RandomCollection;
import com.ebicep.warlords.util.java.Utils;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.PlayerFilterGeneric;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class LegendaryRequiem extends AbstractLegendaryWeapon {

    public static final RandomCollection<Integer> SPAWN_AMOUNT = new RandomCollection<Integer>()
            .add(2, 2)
            .add(2, 3)
            .add(1, 4)
            .add(1, 5);
    public static final int SPAWN_LIMIT = 20;
    public static final int COOLDOWN = 30;

    public LegendaryRequiem() {
    }

    public LegendaryRequiem(UUID uuid) {
        super(uuid);
    }

    public LegendaryRequiem(AbstractLegendaryWeapon legendaryWeapon) {
        super(legendaryWeapon);
    }

    @Override
    public void applyToWarlordsPlayer(WarlordsPlayer player, PveOption pveOption) {
        super.applyToWarlordsPlayer(player, pveOption);


        Game game = player.getGame();
        game.registerEvents(new Listener() {

            @EventHandler
            public void onAddCooldown(WarlordsAddCooldownEvent event) {
                AbstractCooldown<?> cooldown = event.getAbstractCooldown();
                if (!(cooldown.getCooldownObject() instanceof UndyingArmy)) {
                    return;
                }
                if (!Objects.equals(event.getPlayer(), player)) {
                    return;
                }
                PlayerFilterGeneric.playingGameWarlordsNPCs(game)
                                   .aliveEnemiesOf(player)
                                   .filter(warlordsNPC -> !(warlordsNPC.getMob() instanceof BossMob))
                                   .limit(Utils.generateRandomValueBetweenInclusive(2, 5))
                                   .forEach(convertedEnemy -> {
                                       EffectUtils.playCylinderAnimation(convertedEnemy.getLocation(), 1.05, ParticleEffect.VILLAGER_HAPPY, 1);
                                       convertedEnemy.setTeam(Team.BLUE);
                                       //removing teammate mobs that are agroed on converted target
                                       PlayerFilterGeneric.playingGameWarlordsNPCs(game)
                                                          .aliveTeammatesOf(player)
                                                          .filter(teammate -> Objects.equals(teammate.getMob().getTarget(), convertedEnemy.getEntity()))
                                                          .forEach(teammate -> teammate.getMob().removeTarget());
                                       convertedEnemy.getMob().removeTarget();
                                   });
            }

        });

        new GameRunnable(game) {

            @Override
            public void run() {
                if (player.isDead()) {
                    return;
                }
                int spawnAmount = SPAWN_AMOUNT.next();
                int alliedNPCs = (int) game.warlordsNPCs()
                                           .filter(warlordsNPC -> warlordsNPC.isTeammate(player))
                                           .count();
                if (alliedNPCs + spawnAmount > SPAWN_LIMIT) {
                    spawnAmount = SPAWN_LIMIT - alliedNPCs;
                }
                for (int i = 0; i < spawnAmount; i++) {
                    BasicZombie mob = new BasicZombie(player.getLocation());
                    EntityEquipment equipment = mob.getEe();
                    equipment.setHelmet(HeadUtils.getHead(player.getUuid()));
                    mob.updateEquipment();
                    pveOption.spawnNewMob(mob, Team.BLUE);
                }
            }
        }.runTaskTimer(100, COOLDOWN * 20);

    }

    @Override
    public String getPassiveEffect() {
        return "Every " + COOLDOWN + " seconds summon a random assortment of mobs to fight for you. Using Undying Army has additional effect of converting enemy mobs to allies.";
    }

    @Override
    public LegendaryTitles getTitle() {
        return LegendaryTitles.REQUIEM;
    }

    @Override
    protected float getMeleeDamageMinValue() {
        return 160;
    }

    @Override
    protected float getHealthBonusValue() {
        return 800;
    }

    @Override
    protected float getSpeedBonusValue() {
        return 8;
    }

    @Override
    protected float getMeleeDamageMaxValue() {
        return 180;
    }

    @Override
    protected float getCritChanceValue() {
        return 20;
    }

    @Override
    protected float getCritMultiplierValue() {
        return 175;
    }

    @Override
    public List<Pair<String, String>> getPassiveEffectUpgrade() {
        return null;
    }

    @Override
    public LinkedHashMap<Spendable, Long> getTitleUpgradeCost(int tier) {
        return null;
    }
}
