package com.ebicep.warlords.player;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.classes.AbstractAbility;
import com.ebicep.warlords.classes.AbstractPlayerClass;
import com.ebicep.warlords.classes.abilties.*;
import com.ebicep.warlords.classes.shaman.specs.spiritguard.Spiritguard;
import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.database.repositories.player.pojos.general.DatabasePlayer;
import com.ebicep.warlords.events.WarlordsDeathEvent;
import com.ebicep.warlords.maps.Game;
import com.ebicep.warlords.maps.Team;
import com.ebicep.warlords.maps.flags.FlagLocation;
import com.ebicep.warlords.maps.flags.FlagManager;
import com.ebicep.warlords.maps.flags.GroundFlagLocation;
import com.ebicep.warlords.maps.flags.PlayerFlagLocation;
import com.ebicep.warlords.maps.state.PlayingState;
import com.ebicep.warlords.util.*;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.GenericAttributes;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class WarlordsPlayer {

    private final String name;
    private final UUID uuid;
    private final PlayingState gameState;
    private Team team;
    private AbstractPlayerClass spec;
    private Classes specClass;
    private Weapons weapon;
    private int health;
    private int maxHealth;
    private int regenTimer;
    private int timeInCombat = 0;
    private BigDecimal respawnTimer;
    private float respawnTimeSpent = 0;
    private boolean dead = false;
    private float energy;
    private float maxEnergy;
    private float horseCooldown;
    private int healPowerupDuration = 4;
    private float currentHealthModifier = 1;
    private int flagCooldown;
    private int hitCooldown;
    private UUID markedTarget;
    private int spawnProtection;
    private int spawnDamage = 0;
    private int flagsCaptured = 0;
    private int flagsReturned = 0;
    // We have to store these in here as the new player might logout midgame
    private float walkspeed = 1;
    private int blocksTravelledCM = 0;
    private boolean infiniteEnergy;
    private boolean disableCooldowns;
    private double energyModifier;
    private double cooldownModifier;
    private boolean takeDamage = true;
    private boolean canCrit = true;

    private final List<Float> recordDamage = new ArrayList<>();

    private final int[] kills = new int[Warlords.game.getMap().getGameTimerInTicks() / 20 / 60];
    private final int[] assists = new int[Warlords.game.getMap().getGameTimerInTicks() / 20 / 60];
    //assists = player - timeLeft(10 seconds)
    private final LinkedHashMap<WarlordsPlayer, Integer> hitBy = new LinkedHashMap<>();
    private final LinkedHashMap<WarlordsPlayer, Integer> healedBy = new LinkedHashMap<>();
    private final int[] deaths = new int[Warlords.game.getMap().getGameTimerInTicks() / 20 / 60];
    private final long[] damage = new long[Warlords.game.getMap().getGameTimerInTicks() / 20 / 60];
    private final long[] healing = new long[Warlords.game.getMap().getGameTimerInTicks() / 20 / 60];
    private final long[] absorbed = new long[Warlords.game.getMap().getGameTimerInTicks() / 20 / 60];

    private final long[] damageOnCarrier = new long[Warlords.game.getMap().getGameTimerInTicks() / 20 / 60];
    private final long[] healingOnCarrier = new long[Warlords.game.getMap().getGameTimerInTicks() / 20 / 60];

    private final List<Location> locations = new ArrayList<>();
    public List<Location> getLocations() {
        return locations;
    }

    private final CalculateSpeed speed;
    private boolean teamFlagCompass = true;
    private boolean powerUpHeal = false;

    private Location deathLocation = null;
    private ArmorStand deathStand = null;
    private LivingEntity entity = null;

    private double flagDamageMultiplier = 0;

    private CooldownManager cooldownManager = new CooldownManager(this);

    /**
     * @param player is the assigned player as WarlordsPlayer.
     * @param gameState what gamestate should the WarlordsPlayer be assigned to.
     * @param team optional team parameter to assign the WarlordsPlayer to a team.
     * @param settings what settings profile does the WarlordsPlayer use.
     */
    public WarlordsPlayer(
            @Nonnull OfflinePlayer player,
            @Nonnull PlayingState gameState,
            @Nonnull Team team,
            @Nonnull PlayerSettings settings
    ) {
        this.name = player.getName();
        this.uuid = player.getUniqueId();
        this.gameState = gameState;
        this.team = team;
        this.specClass = settings.getSelectedClass();
        this.spec = specClass.create.get();
        this.maxHealth = (int) (this.spec.getMaxHealth() * (gameState.getGame().getCooldownMode() ? 1.5 : 1));
        this.health = this.maxHealth;
        this.respawnTimer = BigDecimal.valueOf(-1);
        this.energy = 0;
        this.energyModifier = gameState.getGame().getCooldownMode() ? 0.5 : 1;
        this.maxEnergy = this.spec.getMaxEnergy();
        this.horseCooldown = 0;
        this.flagCooldown = 0;
        this.cooldownModifier = gameState.getGame().getCooldownMode() ? 0.5 : 1;
        this.hitCooldown = 20;
        this.spawnProtection = 0;
        this.speed = new CalculateSpeed(this::setWalkSpeed, 13);
        Player p = player.getPlayer();
        this.entity = spawnJimmy(p == null ? Warlords.getRejoinPoint(uuid) : p.getLocation(), null);
        this.weapon = Weapons.getSelected(player, settings.getSelectedClass());
        updatePlayerReference(p);
    }

    @Override
    public String toString() {
        return "WarlordsPlayer{" +
                "name='" + name + '\'' +
                ", uuid=" + uuid +
                '}';
    }

    public static final String GIVE_ARROW = ChatColor.RED + "\u00AB";
    public static final String RECEIVE_ARROW = ChatColor.GREEN + "\u00BB";

    /**
     * Adds a damage instance to an ability or a player.
     *
     * @param attacker Assigns the damage value to the original caster.
     * @param ability Name of the ability.
     * @param min The minimum damage amount.
     * @param max The maximum damage amount.
     * @param critChance The critical chance of the damage instance.
     * @param critMultiplier The critical multiplier of the damage instance.
     * @param ignoreReduction Whether the instance has to ignore damage reductions.
     */
    public void addDamageInstance(
            WarlordsPlayer attacker,
            String ability,
            float min,
            float max,
            int critChance,
            int critMultiplier,
            boolean ignoreReduction
    ) {
        boolean isMeleeHit = ability.isEmpty();
        boolean isFallDamage = ability.equals("Fall");

        // Spawn Protection / Undying Army / Game State
        if (spawnProtection != 0 || (dead && !cooldownManager.checkUndyingArmy(false)) || getGameState() != getGame().getState()) {
            if (spawnProtection != 0) {
                removeHorse();
            }
            return;
        }

        // Inferno
        if (!attacker.getCooldownManager().getCooldown(Inferno.class).isEmpty() && (!isMeleeHit && !ability.equals("Time Warp"))) {
            critChance += attacker.getSpec().getOrange().getCritChance();
            critMultiplier += attacker.getSpec().getOrange().getCritMultiplier();
        }

        // Assassin Mark crit chance increase
        if (!attacker.getCooldownManager().getCooldown(OrderOfEviscerate.class).isEmpty()) {
            if (!Utils.isLineOfSightAssassin(getEntity(), attacker.getEntity())) {
                critChance = 100;
            }
        }

        // Assasin takes damage, remove ability.
        if (getCooldownManager().hasCooldownFromName("Cloaked")) {
            getCooldownManager().removeCooldownByName("Cloaked");
            this.getEntity().removePotionEffect(PotionEffectType.INVISIBILITY);
            updateArmor();
        }
        //crit
        float damageValue = (int) ((Math.random() * (max - min)) + min);
        int crit = (int) ((Math.random() * (100)));
        boolean isCrit = false;
        if (crit <= critChance && attacker.canCrit) {
            isCrit = true;
            damageValue *= critMultiplier / 100f;
        }
        final float damageHealValueBeforeReduction = damageValue;
        addAbsorbed(Math.abs(damageValue - (damageValue *= 1 - spec.getDamageResistance() / 100f)));

        if (attacker == this && (isFallDamage || isMeleeHit)) {

            if (isMeleeHit) {
                // True damage
                sendMessage(GIVE_ARROW + ChatColor.GRAY + " You took " + ChatColor.RED + Math.round(min) + ChatColor.GRAY + " melee damage.");
                regenTimer = 10;
                if (health - min <= 0 && !cooldownManager.checkUndyingArmy(false)) {
                    die(attacker);
                    gameState.addKill(team, false);
                    if (entity instanceof Player) {
                        PacketUtils.sendTitle((Player) entity, ChatColor.RED + "YOU DIED!", ChatColor.GRAY + "You took " + ChatColor.RED + Math.round(min) + ChatColor.GRAY + " melee damage and died.", 0, 40, 0);
                    }

                    health = 0;
                } else {
                    health -= min;
                }

                playHurtAnimation(this.entity, attacker);

            } else {

                // Fall Damage
                sendMessage(GIVE_ARROW + ChatColor.GRAY + " You took " + ChatColor.RED + Math.round(damageValue) + ChatColor.GRAY + " fall damage.");
                regenTimer = 10;
                if (health - damageValue < 0 && !cooldownManager.checkUndyingArmy(false)) {
                    die(attacker);
                    gameState.addKill(team, false); // TODO, fall damage is only a suicide if it happens more than 5 seconds after the last damage
                    // Title card "YOU DIED!"
                    if (entity instanceof Player) {
                        PacketUtils.sendTitle((Player) entity, ChatColor.RED + "YOU DIED!", ChatColor.GRAY + "You took " + ChatColor.RED + Math.round(damageValue) + ChatColor.GRAY + " fall damage and died.", 0, 40, 0);
                    }

                    health = 0;
                } else {
                    health -= damageValue;
                }

                playHurtAnimation(entity, attacker);

                addAbsorbed(Math.abs(damageValue * spec.getDamageResistance() / 100));
            }
            cancelHealingPowerUp();
            return;
        }

        // Reduction before Intervene.
        if (!ignoreReduction) {

            // Flag carrier multiplier.
            damageValue *= flagDamageMultiplier == 0 ? 1 : flagDamageMultiplier;

            // Checks whether the player is standing in a Hammer of Light.
            if (!HammerOfLight.standingInHammer(attacker, entity)) {

                // Damage Increase
                // Example: 1.1 = 10% increase.

                // Checks whether the player has Berserk active.
                for (Cooldown cooldown : attacker.getCooldownManager().getCooldown(Berserk.class)) {
                    damageValue *= 1.3;
                }

                // Checks whether the player has Berserk active for self damage.
                for (Cooldown cooldown : cooldownManager.getCooldown(Berserk.class)) {
                    damageValue *= 1.1;
                }

                // Checks whether the player has been crippled by Healing Totem.
                if (!attacker.getCooldownManager().getCooldownFromName("Totem Crippling").isEmpty()) {
                    damageValue *= .75;
                }

                // Checks whether the player has been crippled by Crippling Strike.
                if (!attacker.getCooldownManager().getCooldown(CripplingStrike.class).isEmpty()) {
                    CripplingStrike cripplingStrike = (CripplingStrike) attacker.getCooldownManager().getCooldown(CripplingStrike.class).get(0).getCooldownObject();
                    damageValue *= .85 - (cripplingStrike.getConsecutiveStrikeCounter() * .05);
                }

                if (attacker.getMarkedTarget() == uuid) {
                    damageValue *= 1.25;
                }
            }

        }

        // Intervene
        if (!cooldownManager.getCooldown(Intervene.class).isEmpty() && cooldownManager.getCooldown(Intervene.class).get(0).getFrom() != this && !HammerOfLight.standingInHammer(attacker, entity) && isEnemy(attacker)) {
            Cooldown interveneCooldown = cooldownManager.getCooldown(Intervene.class).get(0);
            Intervene intervene = (Intervene) interveneCooldown.getCooldownObject();
            WarlordsPlayer intervenedBy = interveneCooldown.getFrom();

            damageValue *= .5;
            intervenedBy.addAbsorbed(damageValue);
            intervenedBy.setRegenTimer(10);
            intervene.addDamagePrevented(damageValue);
            intervenedBy.addDamageInstance(attacker, "Intervene", damageValue, damageValue, isCrit ? 100 : -1, 100, false);
            Location loc = getLocation();
            //EFFECTS + SOUNDS
            gameState.getGame().forEachOnlinePlayer((p, t) -> p.playSound(loc, "warrior.intervene.block", 2, 1));
            playHitSound(attacker);
            entity.playEffect(EntityEffect.HURT);
            intervenedBy.getEntity().playEffect(EntityEffect.HURT);

            // Red line particle if the player gets hit
            Location lineLoc = getLocation().add(0, 1, 0);
            lineLoc.setDirection(lineLoc.toVector().subtract(intervenedBy.getLocation().add(0, 1, 0).toVector()).multiply(-1));
            for (int i = 0; i < Math.floor(getLocation().distance(intervenedBy.getLocation())) * 2; i++) {
                ParticleEffect.REDSTONE.display(new ParticleEffect.OrdinaryColor(255, 0, 0), lineLoc, 500);
                ParticleEffect.REDSTONE.display(new ParticleEffect.OrdinaryColor(255, 0, 0), lineLoc, 500);
                lineLoc.add(lineLoc.getDirection().multiply(.5));
            }

            // Remove horses.
            removeHorse();
            intervenedBy.removeHorse();

            // Orbs of Life
            spawnOrbs(ability, attacker);

        } else {

            // Damage reduction after Intervene
            if (!ignoreReduction) {
                if (!HammerOfLight.standingInHammer(attacker, entity)) {

                    // Damage Reduction
                    // Example: .8 = 20% reduction.

                    // Checks whether the player has Ice Barrier Active.
                    for (Cooldown cooldown : cooldownManager.getCooldown(IceBarrier.class)) {
                        IceBarrier iceBarrier = (IceBarrier) cooldown.getCooldownObject();
                        addAbsorbed(Math.abs(damageValue - (damageValue *= iceBarrier.getDamageReduction())));
                    }

                    // Checks whether the player has Chain Lightning Active.
                    if (!cooldownManager.getCooldown(ChainLightning.class).isEmpty()) {
                        addAbsorbed(Math.abs(damageValue - (damageValue *= 1 - (Collections.max(cooldownManager.getCooldown(ChainLightning.class).stream()
                                .map(cd -> ((ChainLightning) cd.getCooldownObject()).getDamageReduction())
                                .collect(Collectors.toList())) * .1))));
                    }

                    // Checks whether the player has Spirit Link Active.
                    for (Cooldown cooldown : cooldownManager.getCooldown(SpiritLink.class)) {
                        addAbsorbed(Math.abs(damageValue - (damageValue *= .8)));
                    }

                    // Checks whether the player has Ice Barrier Active.
                    for (Cooldown cooldown : cooldownManager.getCooldown(LastStand.class)) {
                        WarlordsPlayer lastStandedBy = cooldown.getFrom();
                        LastStand lastStand = (LastStand) cooldown.getCooldownObject();
                        if (lastStandedBy == this) {
                            damageValue *= lastStand.getSelfDamageReduction();
                        } else {
                            damageValue *= lastStand.getTeammateDamageReduction();
                        }
                    }

                    // Checks whether the player has a Flag.
                    if (!cooldownManager.getCooldownFromName("Flag Damage Reduction").isEmpty()) {
                        damageValue *= .9;
                    }
                }
            }

            // Arcane Shield
            if (!cooldownManager.getCooldown(ArcaneShield.class).isEmpty() && isEnemy(attacker) && !HammerOfLight.standingInHammer(attacker, entity)) {
                ArcaneShield arcaneShield = (ArcaneShield) spec.getBlue();
                //adding dmg to shield
                arcaneShield.addShieldHealth(-damageValue);
                //check if broken
                if (arcaneShield.getShieldHealth() < 0) {
                    if (entity instanceof Player) {
                        ((EntityLiving) ((CraftPlayer) entity).getHandle()).setAbsorptionHearts(0);
                    }

                    cooldownManager.removeCooldown(ArcaneShield.class);
                    addDamageInstance(attacker, ability, -arcaneShield.getShieldHealth(), -arcaneShield.getShieldHealth(), isCrit ? 100 : -1, 100, true);

                    addAbsorbed(-(((ArcaneShield) spec.getBlue()).getShieldHealth()));

                    return;
                } else {
                    if (entity instanceof Player) {
                        ((EntityLiving) ((CraftPlayer) entity).getHandle()).setAbsorptionHearts((float) (arcaneShield.getShieldHealth() / (maxHealth * .5) * 20));
                    }

                    if (isMeleeHit) {
                        sendMessage(GIVE_ARROW + ChatColor.GRAY + " You absorbed " + attacker.getName() + "'s melee " + ChatColor.GRAY + "hit.");
                        attacker.sendMessage(RECEIVE_ARROW + ChatColor.GRAY + " Your melee hit was absorbed by " + name);
                    } else {
                        sendMessage(GIVE_ARROW + ChatColor.GRAY + " You absorbed " + attacker.getName() + "'s " + ability + " " + ChatColor.GRAY + "hit.");
                        attacker.sendMessage(RECEIVE_ARROW + ChatColor.GRAY + " Your " + ability + " was absorbed by " + name + ChatColor.GRAY + ".");
                    }

                    addAbsorbed(Math.abs(damageHealValueBeforeReduction));
                }

                //ORBS
                spawnOrbs(ability, attacker);

                playHurtAnimation(this.entity, attacker);

                if (!isMeleeHit) {
                    playHitSound(attacker);
                }
                removeHorse();

            } else {

                boolean debt = false;

                if (isEnemy(attacker)) {
                    hitBy.put(attacker, 10);

                    cancelHealingPowerUp();

                    removeHorse();
                    regenTimer = 10;

                    //LAST STAND HEALING
                    if (!cooldownManager.getCooldown(LastStand.class).isEmpty() && !HammerOfLight.standingInHammer(attacker, entity)) {
                        for (Cooldown cooldown : cooldownManager.getCooldown(LastStand.class)) {
                            WarlordsPlayer lastStandedBy = cooldown.getFrom();
                            lastStandedBy.addAbsorbed(damageValue);
                            //HEALING FROM LASTSTAND
                            if (lastStandedBy != this) {
                                float finalDamageHealValue = damageValue;
                                boolean finalIsCrit = isCrit;
                                //healing if multiple last stands
                                lastStandedBy.getCooldownManager().getCooldown(LastStand.class).stream()
                                        .filter(cd -> cd.getCooldownObject() == cooldown.getCooldownObject() && cd.getTimeLeft() > 0)
                                        .forEach(ls -> lastStandedBy.addHealingInstance(lastStandedBy, "Last Stand", finalDamageHealValue, finalDamageHealValue, finalIsCrit ? 100 : -1, 100, false));
                            }
                        }
                    }

                    // Death's Debt
                    List<Cooldown> debtsCooldown = cooldownManager.getCooldownFromName("Spirits Respite");
                    if (!debtsCooldown.isEmpty()) {
                        ((DeathsDebt) debtsCooldown.get(0).getCooldownObject()).addDelayedDamage(damageValue);
                        debt = true;
                    }

                    if (isCrit) {
                        if (isMeleeHit) {
                            sendMessage(GIVE_ARROW + ChatColor.GRAY + " " + attacker.getName() + " hit you for " + ChatColor.RED + "§l" + Math.round(damageValue) + "! " + ChatColor.GRAY + "critical melee damage.");
                            attacker.sendMessage(RECEIVE_ARROW + ChatColor.GRAY + " " + "You hit " + name + " for " + ChatColor.RED + "§l" + Math.round(damageValue) + "! " + ChatColor.GRAY + "critical melee damage.");
                        } else {
                            sendMessage(GIVE_ARROW + ChatColor.GRAY + " " + attacker.getName() + "'s " + ability + " hit you for " + ChatColor.RED + "§l" + Math.round(damageValue) + "! " + ChatColor.GRAY + "critical damage.");
                            attacker.sendMessage(RECEIVE_ARROW + ChatColor.GRAY + " " + "Your " + ability + " hit " + name + " for " + ChatColor.RED + "§l" + Math.round(damageValue) + "! " + ChatColor.GRAY + "critical damage.");
                        }
                    } else {
                        if (isMeleeHit) {
                            sendMessage(GIVE_ARROW + ChatColor.GRAY + " " + attacker.getName() + " hit you for " + ChatColor.RED + Math.round(damageValue) + " " + ChatColor.GRAY + "melee damage.");
                            attacker.sendMessage(RECEIVE_ARROW + ChatColor.GRAY + " " + "You hit " + name + " for " + ChatColor.RED + Math.round(damageValue) + " " + ChatColor.GRAY + "melee damage.");
                        } else {
                            sendMessage(GIVE_ARROW + ChatColor.GRAY + " " + attacker.getName() + "'s " + ability + " hit you for " + ChatColor.RED + Math.round(damageValue) + " " + ChatColor.GRAY + "damage.");
                            attacker.sendMessage(RECEIVE_ARROW + ChatColor.GRAY + " " + "Your " + ability + " hit " + name + " for " + ChatColor.RED + Math.round(damageValue) + " " + ChatColor.GRAY + "damage.");
                        }
                    }

                    // Repentance
                    if (spec instanceof Spiritguard) {
                        ((Repentance) spec.getBlue()).addToPool(damageValue);
                    }
                    if (attacker.getSpec() instanceof Spiritguard) {
                        if (!attacker.getCooldownManager().getCooldown(Repentance.class).isEmpty()) {
                            Repentance repentance = (Repentance) attacker.getSpec().getBlue();
                            int healthToAdd = (int) (repentance.getPool() * (repentance.getDamageConvertPercent() / 100f)) + 10;
                            attacker.addHealingInstance(attacker, "Repentance", healthToAdd, healthToAdd, -1, 100, false);
                            repentance.setPool(repentance.getPool() * .5f);
                            attacker.addEnergy(attacker, "Repentance", (float) (healthToAdd * .035));
                        }
                    }

                    // Orbs of Life + Spawns additional orb if the ability is Crippling Strike
                    spawnOrbs(ability, attacker);
                    if (ability.equals("Crippling Strike")) {
                        spawnOrbs(ability, attacker);
                    }

                    // Protector's Strike
                    if (ability.equals("Protector's Strike")) {

                        // Self Heal
                        if (Warlords.getPlayerSettings(attacker.uuid).getSkillBoostForClass() == ClassesSkillBoosts.PROTECTOR_STRIKE) {
                            attacker.addHealingInstance(attacker, ability, damageValue / 1.67f, damageValue / 1.67f, isCrit ? 100 : -1, 100, false);
                        } else {
                            attacker.addHealingInstance(attacker, ability, damageValue / 2, damageValue / 2, isCrit ? 100 : -1, 100, false);
                        }

                        // Ally Heal
                        for (WarlordsPlayer nearTeamPlayer : PlayerFilter
                                .entitiesAround(attacker, 10, 10, 10)
                                .aliveTeammatesOfExcludingSelf(attacker)
                                .sorted(Comparator.comparing((WarlordsPlayer p) -> p.getCooldownManager().hasCooldown(HolyRadianceProtector.class) ? 0 : 1)
                                .thenComparing(Utils.sortClosestBy(WarlordsPlayer::getLocation, attacker.getLocation())))
                                .limit(2)
                        ) {
                            if (Warlords.getPlayerSettings(attacker.uuid).getSkillBoostForClass() == ClassesSkillBoosts.PROTECTOR_STRIKE) {
                                nearTeamPlayer.addHealingInstance(attacker, ability, damageValue * 1.2f, damageValue * 1.2f, isCrit ? 100 : -1, 100, false);
                            } else {
                                nearTeamPlayer.addHealingInstance(attacker, ability, damageValue, damageValue, isCrit ? 100 : -1, 100, false);
                            }
                        }
                    }
                }

                // Blood Lust
                if (!attacker.getCooldownManager().getCooldown(BloodLust.class).isEmpty()) {
                    BloodLust bloodLust = (BloodLust) attacker.getSpec().getBlue();
                    attacker.addHealingInstance(attacker, "Blood Lust", damageValue * (bloodLust.getDamageConvertPercent() / 100f), damageValue * (bloodLust.getDamageConvertPercent() / 100f), -1, 100, false);
                }

                if (ability.equals("Judgement Strike")) {
                    if (isCrit) {
                        attacker.getSpeed().addSpeedModifier("Judgement Speed", 20, 2 * 20, "BASE");
                    }
                }

                if (!attacker.getCooldownManager().getCooldown(OrderOfEviscerate.class).isEmpty()) {
                    if (attacker.getMarkedTarget() != uuid) {
                        attacker.sendMessage("You have marked " + getName());
                    }
                    attacker.setMarkedTarget(uuid);
                }

                updateJimmyHealth();

                // adding/subtracing health
                //debt and healing
                if (!debt && takeDamage) {
                    this.health -= Math.round(damageValue);
                }

                attacker.addDamage(damageValue, gameState.flags().hasFlag(this));
                playHurtAnimation(this.entity, attacker);
                recordDamage.add(damageValue);

                // The player died.
                if (this.health <= 0 && !cooldownManager.checkUndyingArmy(false)) {
                    if (attacker.entity instanceof Player) {
                        ((Player) attacker.entity).playSound(attacker.getLocation(), Sound.ORB_PICKUP, 500f, 1);
                        ((Player) attacker.entity).playSound(attacker.getLocation(), Sound.ORB_PICKUP, 500f, 0.5f);
                    }

                    die(attacker);

                    attacker.addKill();

                    sendMessage(ChatColor.GRAY + "You were killed by " + attacker.getColoredName());
                    attacker.sendMessage(ChatColor.GRAY + "You killed " + getColoredName());

                    // Assassin Mark
                    gameState.getGame().forEachOfflineWarlordsPlayer(p -> {
                        if (p.getMarkedTarget() == uuid) {
                          p.setMarkedTarget(null);
                          if (attacker.getUuid() == p.getUuid()) {
                              p.sendMessage("");
                              p.sendMessage(ChatColor.GRAY + "You have killed your mark," + ChatColor.YELLOW + " your cooldowns have been reset" + ChatColor.GRAY + "!");
                          } else {
                              p.sendMessage(ChatColor.RED + "Your marked target has died!");
                          }
                          if (p.getEntity() instanceof Player) {
                              ((Player) p.getEntity()).playSound(p.getLocation(), Sound.AMBIENCE_THUNDER, 1, 2);
                          }
                          p.getSpec().getPurple().setCurrentCooldown(0);
                          p.getSpec().getOrange().setCurrentCooldown(0);
                          p.subtractEnergy(-100);
                          p.updatePurpleItem();
                          p.updateOrangeItem();
                        }
                    });

                    // Regular Kill Feed
                    gameState.getGame().forEachOnlinePlayer((p, t) -> {
                        if (p != this.entity && p != attacker.entity) {
                            p.sendMessage(getColoredName() + ChatColor.GRAY + " was killed by " + attacker.getColoredName());
                        }
                    });

                    // Spectator Kill Feed
                    gameState.getGame().getSpectators().forEach(uuid -> {
                        if (Bukkit.getPlayer(uuid) != null) {
                            Bukkit.getPlayer(uuid).sendMessage(getColoredName() + ChatColor.GRAY + " was killed by " + attacker.getColoredName());
                        }
                    });
                    gameState.addKill(team, false);

                    // Title card "YOU DIED!"
                    if (this.entity instanceof Player) {
                        PacketUtils.sendTitle((Player) entity, ChatColor.RED + "YOU DIED!", ChatColor.GRAY + attacker.getName() + " killed you.", 0, 40, 0);
                    }
                } else {
                    if (!isMeleeHit && this != attacker && damageValue != 0) {
                        playHitSound(attacker);
                    }
                }
            }
        }

        // Windfury Weapon
        if (isMeleeHit) {
            if (!attacker.getCooldownManager().getCooldown(Windfury.class).isEmpty()) {
                int windfuryActivate = (int) (Math.random() * 100);
                if (((Windfury) attacker.getSpec().getPurple()).isFirstProc()) {
                    ((Windfury) attacker.getSpec().getPurple()).setFirstProc(false);
                    windfuryActivate = 0;
                }
                if (windfuryActivate < ((Windfury) attacker.getSpec().getPurple()).getProcChance()) {
                    new BukkitRunnable() {
                        int counter = 0;

                        @Override
                        public void run() {
                            gameState.getGame().forEachOnlinePlayer((player1, t) -> {
                                player1.playSound(getLocation(), "shaman.windfuryweapon.impact", 2, 1);
                            });

                            if (Warlords.getPlayerSettings(attacker.uuid).getSkillBoostForClass() == ClassesSkillBoosts.WINDFURY_WEAPON) {
                                addDamageInstance(attacker, "Windfury Weapon", min * 1.35f * 1.2f, max * 1.35f * 1.2f, 25, 200, false);
                            } else {
                                addDamageInstance(attacker, "Windfury Weapon", min * 1.35f, max * 1.35f, 25, 200, false);
                            }

                            counter++;

                            if (counter == 2) {
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(Warlords.getInstance(), 3, 3);
                }

            // Earthliving Weapon.
            } else if (!attacker.getCooldownManager().getCooldown(Earthliving.class).isEmpty()) {
                int earthlivingActivate = (int) (Math.random() * 100);
                Earthliving earthliving = (Earthliving) attacker.getSpec().getPurple();
                if (earthliving.isFirstProc()) {
                    earthliving.setFirstProc(false);
                    earthlivingActivate = 0;
                }
                if (earthlivingActivate < earthliving.getProcChance()) {
                    boolean earthlivingBoost = Warlords.getPlayerSettings(attacker.uuid).getSkillBoostForClass() == ClassesSkillBoosts.EARTHLIVING_WEAPON;
                    float multiplyBy = earthlivingBoost ? 2.5f : 2.4f;

                    attacker.addHealingInstance(attacker, "Earthliving Weapon", 132 * multiplyBy, 179 * multiplyBy, 25, 200, false);

                    gameState.getGame().forEachOnlinePlayer((p, t) -> {
                        p.playSound(getLocation(), "shaman.earthlivingweapon.impact", 2, 1);
                    });

                    for (WarlordsPlayer nearPlayer : PlayerFilter
                            .entitiesAround(attacker, 6, 6, 6)
                            .aliveTeammatesOfExcludingSelf(attacker)
                            .limit(2)
                    ) {
                        nearPlayer.addHealingInstance(attacker, "Earthliving Weapon", 132 * multiplyBy, 179 * multiplyBy, 25, 200, false);
                    }
                }
            }
        }
    }

    /**
     * Adds a healing instance to an ability or a player.
     *
     * @param attacker Assigns the damage value to the original caster.
     * @param ability Name of the ability.
     * @param min The minimum healing amount.
     * @param max The maximum healing amount.
     * @param critChance The critical chance of the damage instance.
     * @param critMultiplier The critical multiplier of the damage instance.
     * @param ignoreReduction Whether the instance has to ignore damage reductions.
     */
    public void addHealingInstance(
            WarlordsPlayer attacker,
            String ability,
            float min,
            float max,
            int critChance,
            int critMultiplier,
            boolean ignoreReduction
    ) {
        boolean isMeleeHit = ability.isEmpty();

        // Spawn Protection / Undying Army / Game State
        if (spawnProtection != 0 || (dead && !cooldownManager.checkUndyingArmy(false)) || getGameState() != getGame().getState()) {
            if (spawnProtection != 0) {
                removeHorse();
            }
            return;
        }

        // Critical Hits
        float healValue = (int) ((Math.random() * (max - min)) + min);
        int crit = (int) ((Math.random() * (100)));
        boolean isCrit = false;

        if (crit <= critChance && attacker.canCrit) {
            isCrit = true;
            healValue *= critMultiplier / 100f;
        }

        // Checks whether the player has been wounded.
        if (!cooldownManager.getCooldown(WoundingStrikeBerserker.class).isEmpty()) {
            healValue *= .6;
        } else if (!cooldownManager.getCooldown(WoundingStrikeDefender.class).isEmpty()) {
            healValue *= .75;
        }

        // Self Healing
        if (this == attacker) {

            if (this.health + healValue > this.maxHealth) {
                healValue = this.maxHealth - this.health;
            }

            if (healValue < 0) return;

            if (healValue != 0) {
                if (isCrit) {
                    sendMessage(RECEIVE_ARROW + ChatColor.GRAY + " Your " + ability + " critically healed you for " + ChatColor.GREEN + "§l" + Math.round(healValue) + "! " + ChatColor.GRAY + "health.");
                } else {
                    sendMessage(RECEIVE_ARROW + ChatColor.GRAY + " Your " + ability + " healed you for " + ChatColor.GREEN + "" + Math.round(healValue) + " " + ChatColor.GRAY + "health.");
                }
                health += healValue;
                addHealing(healValue, gameState.flags().hasFlag(this));

                if (!isMeleeHit && !ability.equals("Healing Rain") && !ability.equals("Blood Lust")) {
                    playHitSound(attacker);
                }
            }

        } else {

            // Teammate Healing
            if (isTeammate(attacker)) {

                int maxHealth = this.maxHealth;
                if (ability.equals("Water Bolt") || ability.equals("Water Breath") || ability.equals("Healing Rain")) {
                    maxHealth *= 1.1;
                }

                if (this.health + healValue > maxHealth) {
                    healValue = maxHealth - this.health;
                }

                if (healValue < 0) return;

                if (healValue != 0) {
                    if (isCrit) {
                        sendMessage(ChatColor.GREEN + "\u00AB" + ChatColor.GRAY + " " + attacker.getName() + "'s " + ability + " critically healed you for " + ChatColor.GREEN + "§l" + Math.round(healValue) + "! " + ChatColor.GRAY + "health.");
                        attacker.sendMessage(RECEIVE_ARROW + ChatColor.GRAY + " " + "Your " + ability + " critically healed " + name + " for " + ChatColor.GREEN + "§l" + Math.round(healValue) + "! " + ChatColor.GRAY + "health.");
                    } else {
                        sendMessage(ChatColor.GREEN + "\u00AB" + ChatColor.GRAY + " " + attacker.getName() + "'s " + ability + " healed for " + ChatColor.GREEN + "" + Math.round(healValue) + " " + ChatColor.GRAY + "health.");
                        attacker.sendMessage(RECEIVE_ARROW + ChatColor.GRAY + " " + "Your " + ability + " healed " + name + " for " + ChatColor.GREEN + "" + Math.round(healValue) + " " + ChatColor.GRAY + "health.");
                    }
                }

                health += healValue;
                attacker.addHealing(healValue, gameState.flags().hasFlag(this));

                if (!isMeleeHit && !ability.equals("Healing Rain")) {
                    playHitSound(attacker);
                }
            }
        }
    }

    /**
     * @param attacker which player should hear the hitsound?
     */
    private void playHitSound(WarlordsPlayer attacker) {
        if (attacker.entity instanceof Player) {
            ((Player) attacker.entity).playSound(attacker.getLocation(), Sound.ORB_PICKUP, 1, 1);
        }
    }

    /**
     * @param entity which entity is assigned to the hurt animation?
     * @param hurtPlayer what warlords player should play the hurt animation?
     */
    private void playHurtAnimation(LivingEntity entity, WarlordsPlayer hurtPlayer) {
        entity.playEffect(EntityEffect.HURT);
        for (Player player1 : hurtPlayer.getWorld().getPlayers()) {
            player1.playSound(entity.getLocation(), Sound.HURT_FLESH, 2, 1);
        }
    }

    /**
     * @param ability which ability should drop Orbs of Life.
     * @param attacker is the caster of the ability.
     */
    public void spawnOrbs(String ability, WarlordsPlayer attacker) {
        if (!attacker.getCooldownManager().getCooldown(OrbsOfLife.class).isEmpty() && !ability.isEmpty() && !ability.equals("Intervene")) {
            attacker.getCooldownManager().getCooldown(OrbsOfLife.class).stream()
                    .filter(cooldown -> !cooldown.isHidden())
                    .forEach(cooldown -> {
                        OrbsOfLife orbsOfLife = (OrbsOfLife) cooldown.getCooldownObject();
                        Location location = getLocation();
                        Location spawnLocation = orbsOfLife.generateSpawnLocation(location);

                        OrbsOfLife.Orb orb = new OrbsOfLife.Orb(((CraftWorld) location.getWorld()).getHandle(), spawnLocation, attacker);
                        orbsOfLife.getSpawnedOrbs().add(orb);
                    });
        }
    }

    public void cancelHealingPowerUp() {
        if (powerUpHeal) {
            powerUpHeal = false;
            sendMessage(ChatColor.GOLD + "Your §a§lHealing Powerup §6has worn off.");
            setHealPowerupDuration(4);
        }
    }

    public void removeHorse() {
        if (entity.getVehicle() != null) {
            entity.getVehicle().remove();
        }
    }

    public void die(WarlordsPlayer attacker) {
        dead = true;

        removeHorse();

        addGrave();

        showDeathAnimation();

        if (attacker != this) {
            hitBy.putAll(attacker.getHealedBy());
        }

        hitBy.remove(attacker);
        hitBy.put(attacker, 10);

        this.addDeath();
        gameState.flags().dropFlag(this);
        Bukkit.getPluginManager().callEvent(new WarlordsDeathEvent(this));

        if (entity instanceof Player) {
            //removing yellow hearts
            ((EntityLiving) ((CraftPlayer) entity).getHandle()).setAbsorptionHearts(0);
            //removing sg shiny weapon
            ((CraftPlayer) entity).getInventory().getItem(0).removeEnchantment(Enchantment.OXYGEN);
        }
    }

    public void addGrave() {
        LivingEntity player = this.entity;

        Location deathLocation = player.getLocation();
        Block bestGraveCandidate = null;
        boolean isFlagCarrier = this.getFlagDamageMultiplier() > 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (isFlagCarrier && x == 0 && z == 0) {
                    // This player is a flag carrier, prevent placing the grave at the direct location of the player
                    continue;
                }

                Location toTest = deathLocation.clone().add(x, 2, z);
                Block lastBlock = toTest.getBlock();

                if (lastBlock.getType() == Material.AIR) {
                    toTest.subtract(0, 1, 0);
                    for (; toTest.getY() > 0; toTest.subtract(0, 1, 0)) {
                        Block underTest = toTest.getBlock();
                        if (underTest.getType() != Material.AIR) {
                            if (underTest.getType().isTransparent()) {
                                // We have hit a sappling, fence, torch or other non-solid
                                break;
                            }
                            // We have hit a solid block. Go back 1 tile
                            toTest.add(0, 1, 0);
                            // Check if we found a better tile for the grave
                            if (bestGraveCandidate != null) {
                                double newDistance = toTest.distanceSquared(deathLocation);
                                double existingDistance = bestGraveCandidate.getLocation(toTest).distanceSquared(deathLocation);
                                if (newDistance >= existingDistance) {
                                    // Our new candidate is not closer, skip
                                    break;
                                }
                            }
                            bestGraveCandidate = lastBlock;
                            //
                            break;
                        }
                        lastBlock = underTest;
                    }
                }
            }
        }

        if (bestGraveCandidate != null) {
            //spawn grave
            bestGraveCandidate.setType(Material.SAPLING);
            bestGraveCandidate.setData((byte) 5);

            this.deathLocation = bestGraveCandidate.getLocation();

            this.deathStand = (ArmorStand) player.getWorld().spawnEntity(bestGraveCandidate.getLocation().add(.5, -1.5, .5), EntityType.ARMOR_STAND);
            this.deathStand.setCustomName(team.teamColor() + name + ChatColor.GRAY + " - " + ChatColor.YELLOW + "DEAD");
            this.deathStand.setCustomNameVisible(true);
            this.deathStand.setGravity(false);
            this.deathStand.setVisible(false);
        }
    }

    public Zombie spawnJimmy(@Nonnull Location loc, @Nullable PlayerInventory inv) {
        Zombie jimmy = loc.getWorld().spawn(loc, Zombie.class);
        jimmy.setBaby(false);
        jimmy.setCustomNameVisible(true);
        jimmy.setCustomName(this.getSpec().getClassNameShortWithBrackets() + " " + this.getColoredName() + " " + ChatColor.RED + this.health + "❤"); // TODO add level and class into the name of this jimmy
        jimmy.setMetadata("WARLORDS_PLAYER", new FixedMetadataValue(Warlords.getInstance(), this));
        ((EntityLiving) ((CraftEntity) jimmy).getHandle()).getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(0);
        ((EntityLiving) ((CraftEntity) jimmy).getHandle()).getAttributeInstance(GenericAttributes.FOLLOW_RANGE).setValue(0);
        //prevents jimmy from moving
        net.minecraft.server.v1_8_R3.Entity nmsEn = ((CraftEntity) jimmy).getHandle();
        NBTTagCompound compound = new NBTTagCompound();
        nmsEn.c(compound);
        compound.setByte("NoAI", (byte) 1);
        nmsEn.f(compound);

        if (inv != null) {
            jimmy.getEquipment().setBoots(inv.getBoots());
            jimmy.getEquipment().setLeggings(inv.getLeggings());
            jimmy.getEquipment().setChestplate(inv.getChestplate());
            jimmy.getEquipment().setHelmet(inv.getHelmet());
            jimmy.getEquipment().setItemInHand(inv.getItemInHand());
        } else {
            jimmy.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        }
        return jimmy;
    }

    public void updateJimmyHealth() {
        if (getEntity() instanceof Zombie) {
            if (isDeath()) {
                getEntity().setCustomName("");
            } else {
                String oldName = getEntity().getCustomName();
                String newName = oldName.substring(0, oldName.lastIndexOf(" ") + 1) + ChatColor.RED + getHealth() + "❤";
                getEntity().setCustomName(newName);
            }
        }
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    private void setWalkSpeed(float walkspeed) {
        this.walkspeed = walkspeed;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) player.setWalkSpeed(this.walkspeed);
    }

    public void displayActionBar() {
        StringBuilder actionBarMessage = new StringBuilder(ChatColor.GOLD + "§lHP: ");
        float healthRatio = (float) health / maxHealth;
        if (healthRatio >= .75) {
            actionBarMessage.append(ChatColor.DARK_GREEN);

        } else if (healthRatio >= .25) {
            actionBarMessage.append(ChatColor.YELLOW);

        } else {
            actionBarMessage.append(ChatColor.RED);

        }
        actionBarMessage.append("§l").append(health).append(ChatColor.GOLD).append("§l/§l").append(maxHealth).append("    ");
        actionBarMessage.append(team.boldColoredPrefix()).append(" TEAM  ");
        for (Cooldown cooldown : cooldownManager.getCooldowns()) {
            if (!cooldown.isHidden()) {
                if (cooldown.getActionBarName().equals("WND") || cooldown.getActionBarName().equals("CRIP")) {
                    actionBarMessage.append(ChatColor.RED);
                } else {
                    actionBarMessage.append(ChatColor.GREEN);
                }
                actionBarMessage.append(cooldown.getActionBarName()).append(ChatColor.GRAY).append(":").append(ChatColor.GOLD).append((int) cooldown.getTimeLeft() + 1).append(" ");
            }
        }
        if (entity instanceof Player) {
            PacketUtils.sendActionBar((Player) entity, actionBarMessage.toString());
        }
    }


    public void displayFlagActionBar(@Nonnull Player player) {
        FlagManager flags = this.gameState.flags();

        if (teamFlagCompass) {
            FlagLocation flag = flags.get(team).getFlag();
            double flagDistance = Math.round(flag.getLocation().distance(player.getLocation()) * 10) / 10.0;
            String start = team.teamColor().toString() + ChatColor.BOLD + "YOUR ";
            if (flag instanceof PlayerFlagLocation) {
                PacketUtils.sendActionBar(player, start + "Flag " + ChatColor.WHITE + "is stolen " + ChatColor.RED + flagDistance + "m " + ChatColor.WHITE + "away!");
            } else if (flag instanceof GroundFlagLocation) {
                PacketUtils.sendActionBar(player, start + "Flag " + ChatColor.GOLD + "is dropped " + ChatColor.RED + flagDistance + "m " + ChatColor.WHITE + "away!");
            } else {
                PacketUtils.sendActionBar(player, start + ChatColor.GREEN + "Flag is safe");
            }
        } else {
            FlagLocation flag = flags.get(team.enemy()).getFlag();
            double flagDistance = Math.round(flag.getLocation().distance(player.getLocation()) * 10) / 10.0;
            String start = team.enemy().teamColor().toString() + ChatColor.BOLD + "ENEMY ";
            if (flag instanceof PlayerFlagLocation) {
                PacketUtils.sendActionBar(player, start + "Flag " + ChatColor.WHITE + "is stolen " + ChatColor.RED + flagDistance + "m " + ChatColor.WHITE + "away!");
            } else if (flag instanceof GroundFlagLocation) {
                PacketUtils.sendActionBar(player, start + "ENEMY Flag " + ChatColor.GOLD + "is dropped " + ChatColor.RED + flagDistance + "m " + ChatColor.WHITE + "away!");
            } else {
                PacketUtils.sendActionBar(player, start + ChatColor.GREEN + "Flag is safe");
            }
        }
    }

    public void applySkillBoost(Player player) {
        ClassesSkillBoosts selectedBoost = Classes.getSelectedBoost(Bukkit.getOfflinePlayer(uuid));
        if (spec.getWeapon().getClass() == selectedBoost.ability) {
            spec.getWeapon().boostSkill(selectedBoost, spec);
            spec.getWeapon().updateDescription(player);
        } else if (spec.getRed().getClass() == selectedBoost.ability) {
            spec.getRed().boostSkill(selectedBoost, spec);
            spec.getRed().updateDescription(player);
        } else if (spec.getPurple().getClass() == selectedBoost.ability) {
            spec.getPurple().boostSkill(selectedBoost, spec);
            spec.getPurple().updateDescription(player);
        } else if (spec.getBlue().getClass() == selectedBoost.ability) {
            spec.getBlue().boostSkill(selectedBoost, spec);
            spec.getBlue().updateDescription(player);
        } else if (spec.getOrange().getClass() == selectedBoost.ability) {
            spec.getOrange().boostSkill(selectedBoost, spec);
            spec.getOrange().updateDescription(player);
        }
    }

    public void updateArmor() {
        if (!(this.entity instanceof Player)) {
            return;
        }

        Player player = (Player) this.entity;

        ArmorManager.resetArmor(player, getSpecClass(), getTeam());

        if (cooldownManager.hasCooldownFromName("Cloaked")) {
            player.getInventory().setArmorContents(new ItemStack[]{player.getInventory().getBoots(), null, null, null});
        }

        if (this.flagDamageMultiplier > 0) {
            ItemStack item = new ItemStack(Material.BANNER);
            BannerMeta banner = (BannerMeta) item.getItemMeta();
            banner.setBaseColor(getTeam() == Team.RED ? DyeColor.BLUE : DyeColor.RED);
            banner.addPattern(new Pattern(DyeColor.BLACK, PatternType.SKULL));
            banner.addPattern(new Pattern(DyeColor.BLACK, PatternType.TRIANGLES_TOP));
            item.setItemMeta(banner);
            player.getInventory().setHelmet(item);
        }
    }

    public void assignItemLore(Player player) {
        //§
        ItemStack weapon = new ItemStack(this.weapon.item);
        ItemMeta weaponMeta = weapon.getItemMeta();
        weaponMeta.setDisplayName("§cWarlord's Felflame of the " + spec.getWeapon().getName());
        ArrayList<String> weaponLore = new ArrayList<>();
        weaponLore.add("§7Damage: §c132 §7- §c179");
        weaponLore.add("§7Crit Chance: §c25%");
        weaponLore.add("§7Crit Multiplier: §c200%");
        weaponLore.add("");
        String classNamePath = spec.getClass().getGenericSuperclass().getTypeName();
        weaponLore.add("§a" + classNamePath.substring(classNamePath.indexOf("Abstract") + 8) + " (" + spec.getClass().getSimpleName() + "):");
        weaponLore.add("§aIncreases the damage you");
        weaponLore.add("§adeal with " + spec.getWeapon().getName() + " by §c20%");
        weaponLore.add("");
        weaponLore.add("§7Health: §a+800");
        weaponLore.add("§7Max Energy: §a+35");
        weaponLore.add("§7Cooldown Reduction: §a+13%");
        weaponLore.add("§7Speed: §a+13%");
        weaponLore.add("");
        weaponLore.add("§6Skill Boost Unlocked");
        weaponLore.add("§3Crafted");
        weaponLore.add("§dVoid Forged [4/4]");
        weaponLore.add("§aEQUIPPED");
        weaponLore.add("§bBOUND");
        weaponMeta.setLore(weaponLore);
        weapon.setItemMeta(weaponMeta);
        weaponMeta.spigot().setUnbreakable(true);
        player.getInventory().setItem(0, weapon);
        weaponLeftClick(player);

        updateRedItem(player);
        updatePurpleItem(player);
        updateBlueItem(player);
        updateOrangeItem(player);
        updateHorseItem(player);

        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        compassMeta.setDisplayName(ChatColor.GREEN + "Flag Finder");
        compass.setItemMeta(compassMeta);
        compassMeta.spigot().setUnbreakable(true);
        player.getInventory().setItem(8, compass);
    }

    public void weaponLeftClick(Player player) {
        player.getInventory().setItem(
                0,
                new ItemBuilder(weapon.item)
                        .name(ChatColor.GOLD + "Warlord's " + weapon.name + " of the " + spec.getClass().getSimpleName())
                        .lore(
                            ChatColor.GRAY + "Damage: " + ChatColor.RED + "132 " + ChatColor.GRAY + "- " + ChatColor.RED + "179",
                            ChatColor.GRAY + "Crit Chance: " + ChatColor.RED + "25%",
                            ChatColor.GRAY + "Crit Multiplier: " + ChatColor.RED + "200%",
                            "",
                            ChatColor.GREEN + spec.getClassName() + " (" + spec.getClass().getSimpleName() + "):",
                            Classes.getSelectedBoost(player).selectedDescription,
                            "",
                            ChatColor.GRAY + "Health: " + ChatColor.GREEN + "+800",
                            ChatColor.GRAY + "Max Energy: " + ChatColor.GREEN + "+35",
                            ChatColor.GRAY + "Cooldown Reduction: " + ChatColor.GREEN + "+13%",
                            ChatColor.GRAY + "Speed: " + ChatColor.GREEN + "+13%",
                            "",
                            ChatColor.GOLD + "Skill Boost Unlocked",
                            ChatColor.DARK_AQUA + "Crafted",
                            ChatColor.LIGHT_PURPLE + "Void Forged [4/4]",
                            ChatColor.GREEN + "EQUIPPED",
                            ChatColor.AQUA + "BOUND",
                            "",
                            ChatColor.YELLOW + ChatColor.BOLD.toString() + "RIGHT-CLICK " + ChatColor.GREEN + "to view " + ChatColor.YELLOW + spec.getWeapon().getName(),
                            ChatColor.GREEN + "stats!")
                        .unbreakable()
                        .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
                        .get());
    }

    public void weaponRightClick(Player player) {
        player.getInventory().setItem(
                0,
                new ItemBuilder(weapon.item)
                        .name(ChatColor.GREEN + spec.getWeapon().getName() + ChatColor.GRAY + " - " + ChatColor.YELLOW + "Right-Click!")
                        .lore(ChatColor.GRAY + "Energy Cost: " + ChatColor.YELLOW + spec.getWeapon().getEnergyCost(),
                                ChatColor.GRAY + "Crit Chance: " + ChatColor.RED + spec.getWeapon().getCritChance() + "%",
                                ChatColor.GRAY + "Crit Multiplier: " + ChatColor.RED + spec.getWeapon().getCritMultiplier() + "%",
                                "",
                                spec.getWeapon().getDescription(),
                                "",
                                ChatColor.YELLOW + ChatColor.BOLD.toString() + "LEFT-CLICK " + ChatColor.GREEN + "to view weapon stats!")
                        .unbreakable()
                        .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
                        .get());
    }

    public void updateItem(Player player, int slot, AbstractAbility ability, ItemStack item) {
        if (ability.getCurrentCooldown() > 0) {
            ItemStack cooldown = new ItemStack(Material.INK_SACK, ability.getCurrentCooldownItem(), (byte) 8);
            player.getInventory().setItem(slot, cooldown);
        } else {
            player.getInventory().setItem(
                    slot,
                    ability.getItem(item)
            );
        }
    }

    public void updateRedItem() {
        if (entity instanceof Player) {
            updateRedItem((Player) entity);
        }
    }

    public void updateRedItem(Player player) {
        updateItem(player, 1, spec.getRed(), new ItemStack(Material.INK_SACK, 1, (byte) 1));
    }

    public void updatePurpleItem() {
        if (entity instanceof Player) {
            updatePurpleItem((Player) entity);
        }
    }

    public void updatePurpleItem(Player player) {
        updateItem(player, 2, spec.getPurple(), new ItemStack(Material.GLOWSTONE_DUST));
    }

    public void updateBlueItem() {
        if (entity instanceof Player) {
            updateBlueItem((Player) entity);
        }
    }

    public void updateBlueItem(Player player) {
        updateItem(player, 3, spec.getBlue(), new ItemStack(Material.INK_SACK, 1, (byte) 10));
    }

    public void updateOrangeItem() {
        if (entity instanceof Player) {
            updateOrangeItem((Player) entity);
        }
    }

    public void updateOrangeItem(Player player) {
        updateItem(player, 4, spec.getOrange(), new ItemStack(Material.INK_SACK, 1, (byte) 14));
    }

    public void updateHorseItem() {
        if (entity instanceof Player) {
            updateHorseItem((Player) entity);
        }
    }

    public void updateHorseItem(Player player) {
        if (horseCooldown > 0) {
            ItemStack cooldown = new ItemStack(Material.IRON_BARDING, (int) horseCooldown + 1);
            player.getInventory().setItem(7, cooldown);
        } else {
            ItemStack horse = new ItemStack(Material.GOLD_BARDING);
            ItemMeta horseMeta = horse.getItemMeta();
            horseMeta.setDisplayName(ChatColor.GREEN + "Mount " + ChatColor.GRAY + "- §eRight-Click!");
            ArrayList<String> horseLore = new ArrayList<>();
            horseLore.add(ChatColor.GRAY + "Cooldown: §b15 seconds");
            horseLore.add("");
            horseLore.add(ChatColor.GRAY + "Call your steed to assists you in battle");
            horseMeta.setLore(horseLore);
            horse.setItemMeta(horseMeta);
            horseMeta.spigot().setUnbreakable(true);
            player.getInventory().setItem(7, horse);
        }
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public AbstractPlayerClass getSpec() {
        return spec;
    }

    public void setSpec(AbstractPlayerClass spec, ClassesSkillBoosts skillBoost) {
        Warlords.getPlayerSettings(uuid).setSelectedClass(Classes.getClass(spec.getName()));
        Warlords.getPlayerSettings(uuid).setSkillBoostForSelectedClass(skillBoost);
        Player player = Bukkit.getPlayer(uuid);
        this.spec = spec;
        this.specClass = Warlords.getPlayerSettings(uuid).getSelectedClass();
        this.weapon = Weapons.getSelected(player, this.specClass);
        this.maxHealth = (int) (this.spec.getMaxHealth() * (gameState.getGame().getCooldownMode() ? 1.5 : 1));
        this.health = this.maxHealth;
        this.maxEnergy = this.spec.getMaxEnergy();
        this.energy = this.maxEnergy;
        ArmorManager.resetArmor(player, specClass, team);
        applySkillBoost(player);
        this.spec.getWeapon().updateDescription(player);
        this.spec.getRed().updateDescription(player);
        this.spec.getPurple().updateDescription(player);
        this.spec.getBlue().updateDescription(player);
        this.spec.getOrange().updateDescription(player);
        assignItemLore(Bukkit.getPlayer(uuid));
        //sync bc player should be cached
        DatabasePlayer databasePlayer = DatabaseManager.playerService.findByUUID(player.getUniqueId());
        databasePlayer.getSpec(specClass).setSkillBoost(skillBoost);
        DatabaseManager.updatePlayerAsync(databasePlayer);
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public void showDeathAnimation() {
        if (this.entity instanceof Zombie) {
            this.entity.damage(200);
        } else {
            Player player = (Player) this.entity;
            Zombie zombie = player.getWorld().spawn(player.getLocation(), Zombie.class);
            zombie.getEquipment().setBoots(player.getInventory().getBoots());
            zombie.getEquipment().setLeggings(player.getInventory().getLeggings());
            zombie.getEquipment().setChestplate(player.getInventory().getChestplate());
            zombie.getEquipment().setHelmet(player.getInventory().getHelmet());
            zombie.getEquipment().setItemInHand(player.getInventory().getItemInHand());
            zombie.damage(2000);
        }
    }

    public void heal() {
        this.health = this.maxHealth;
    }

    public void respawn() {
        if (entity instanceof Player && ((Player) entity).isOnline()) {
            PacketUtils.sendTitle((Player) entity, "", "", 0, 0, 0);
            setRespawnTimer(BigDecimal.valueOf(-1));
            setSpawnProtection(3);
            setEnergy(getMaxEnergy() / 2);
            setDead(false);
            Location respawnPoint = getGame().getMap().getRespawn(getTeam());
            teleport(respawnPoint);
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location location = getLocation();
                    Location respawn = getGame().getMap().getRespawn(getTeam());
                    if (
                            location.getWorld() != respawn.getWorld() ||
                                    location.distanceSquared(respawn) > Warlords.SPAWN_PROTECTION_RADIUS * Warlords.SPAWN_PROTECTION_RADIUS
                    ) {
                        setSpawnProtection(0);
                    }
                    if (getSpawnProtection() == 0) {
                        this.cancel();
                    }
                }
            }.runTaskTimer(Warlords.getInstance(), 0, 5);

            this.health = this.maxHealth;
            if (deathStand != null) {
                deathStand.remove();
                deathStand = null;
            }
            removeGrave();
            if (entity instanceof Player) {
                ((Player) entity).setGameMode(GameMode.ADVENTURE);
            }
        } else {
            giveRespawnTimer();
        }
    }

    public void removeGrave() {
        if (deathLocation != null) {
            Block deathBlock = deathLocation.getBlock();
            if (deathBlock.getType() == Material.SAPLING) {
                deathBlock.setType(Material.AIR);
            }
            deathLocation = null;
        }
    }

    public int getRegenTimer() {
        return regenTimer;
    }

    public void setRegenTimer(int regenTimer) {
        this.regenTimer = regenTimer;
    }

    public BigDecimal getRespawnTimer() {
        return respawnTimer;
    }

    public void setRespawnTimer(BigDecimal respawnTimer) {
        this.respawnTimer = respawnTimer;
    }

    public void giveRespawnTimer() {
        BigDecimal respawn = BigDecimal.valueOf(gameState.getTimer() / 20.0).remainder(BigDecimal.valueOf(12));
        if (respawn.doubleValue() <= 4) {
            respawn = respawn.add(BigDecimal.valueOf(12));
        }
        setRespawnTimer(respawn);
    }

    public float getEnergy() {
        return energy;
    }

    public void setEnergy(float energy) {
        this.energy = energy;
    }

    public void addEnergy(WarlordsPlayer giver, String ability, float amount) {
        if (energy + amount > maxEnergy) {
            this.energy = maxEnergy;
        } else if (energy + amount > 0) {
            this.energy += amount;
        } else {
            this.energy = 1;
        }
        if ((int) amount != 0) {
            if (this == giver) {
                sendMessage(ChatColor.GREEN + "\u00AB" + ChatColor.GRAY + " Your " + ability + " gave you " + ChatColor.YELLOW + (int) amount + " " + ChatColor.GRAY + "energy.");
            } else {
                sendMessage(ChatColor.GREEN + "\u00AB" + ChatColor.GRAY + " " + giver.getName() + "'s " + ability + " gave you " + ChatColor.YELLOW + (int) amount + " " + ChatColor.GRAY + "energy.");
                giver.sendMessage(RECEIVE_ARROW + ChatColor.GRAY + " " + "Your " + ability + " gave " + name + " " + ChatColor.YELLOW + (int) amount + " " + ChatColor.GRAY + "energy.");
            }
        }
    }

    public void subtractEnergy(int amount) {
        if (!infiniteEnergy) {
            amount *= energyModifier;
            if (energy - amount > maxEnergy) {
                energy = maxEnergy;
            } else {
                this.energy -= amount;
            }
        }
    }

    public void sendMessage(String message) {
        if (this.entity instanceof Player) { // TODO check if this if is really needed, we can send a message to any entity??
            this.entity.sendMessage(message);
        }
    }

    public float getMaxEnergy() {
        return maxEnergy;
    }

    public void setMaxEnergy(int maxEnergy) {
        this.maxEnergy = maxEnergy;
    }

    public float getHorseCooldown() {
        return horseCooldown;
    }

    public void setHorseCooldown(float horseCooldown) {
        this.horseCooldown = horseCooldown;
    }

    public int getFlagCooldown() {
        return flagCooldown;
    }

    public void setFlagCooldown(int flagCooldown) {
        this.flagCooldown = flagCooldown;
    }

    public int getHitCooldown() {
        return hitCooldown;
    }

    public void setHitCooldown(int hitCooldown) {
        this.hitCooldown = hitCooldown;
    }

    public boolean isPowerUpHeal() {
        return powerUpHeal;
    }

    public void setPowerUpHeal(boolean powerUpHeal) {
        this.powerUpHeal = powerUpHeal;
    }

    public int[] getKills() {
        return kills;
    }

    public void addKill() {
        this.kills[this.gameState.getTimer() / (20 * 60)]++;
    }

    public int getTotalKills() {
        return IntStream.of(kills).sum();
    }

    public int[] getAssists() {
        return assists;
    }

    public void addAssist() {
        this.assists[this.gameState.getTimer() / (20 * 60)]++;
    }

    public int getTotalAssists() {
        return IntStream.of(assists).sum();
    }

    public LinkedHashMap<WarlordsPlayer, Integer> getHitBy() {
        return hitBy;
    }

    public LinkedHashMap<WarlordsPlayer, Integer> getHealedBy() {
        return healedBy;
    }

    public int[] getDeaths() {
        return deaths;
    }

    public int getTotalDeaths() {
        return IntStream.of(deaths).sum();
    }

    public void addDeath() {
        this.deaths[this.gameState.getTimer() / (20 * 60)]++;
    }

    public long[] getDamage() {
        return damage;
    }

    public void addDamage(float amount, boolean onCarrier) {
        this.damage[this.gameState.getTimer() / (20 * 60)] += amount;
        if (onCarrier) {
            this.damageOnCarrier[this.gameState.getTimer() / (20 * 60)] += amount;
        }
    }

    public long getTotalDamage() {
        return Arrays.stream(damage).sum();
    }

    public long[] getHealing() {
        return healing;
    }

    public void addHealing(float amount, boolean onCarrier) {
        this.healing[this.gameState.getTimer() / (20 * 60)] += amount;
        if (onCarrier) {
            this.healingOnCarrier[this.gameState.getTimer() / (20 * 60)] += amount;
        }
    }

    public long getTotalHealing() {
        return Arrays.stream(healing).sum();
    }

    public long[] getAbsorbed() {
        return absorbed;
    }

    public void addAbsorbed(float amount) {
        this.absorbed[this.gameState.getTimer() / (20 * 60)] += amount;
    }

    public long getTotalAbsorbed() {
        return Arrays.stream(absorbed).sum();
    }

    public long[] getDamageOnCarrier() {
        return damageOnCarrier;
    }

    public long getTotalDamageOnCarrier() {
        return Arrays.stream(damageOnCarrier).sum();
    }

    public long[] getHealingOnCarrier() {
        return healingOnCarrier;
    }

    public long getTotalHealingOnCarrier() {
        return Arrays.stream(healingOnCarrier).sum();
    }

    public ItemStack getStatItemStack(String name) {
        ItemStack itemStack = new ItemStack(Material.STONE);
        ItemMeta meta = itemStack.getItemMeta();
        List<String> lore = new ArrayList<>();
        meta.setDisplayName(ChatColor.AQUA + "Stat Breakdown (" + name + "):");
        int minute = (this.gameState.getGame().getMap().getGameTimerInTicks() - this.gameState.getTimer()) / (20 * 60);
        int totalMinutes = (gameState.getGame().getMap().getGameTimerInTicks() / 20 / 60) - 1;
        for (int i = 0; i < damage.length - 1 && i < minute + 1; i++) {
            if (name.equals("Kills")) {
                lore.add(ChatColor.WHITE + "Minute " + (i + 1) + ": " + ChatColor.GOLD + NumberFormat.addCommaAndRound(kills[totalMinutes - i]));
            } else if (name.equals("Assists")) {
                lore.add(ChatColor.WHITE + "Minute " + (i + 1) + ": " + ChatColor.GOLD + NumberFormat.addCommaAndRound(assists[totalMinutes - i]));
            } else if (name.equals("Deaths")) {
                lore.add(ChatColor.WHITE + "Minute " + (i + 1) + ": " + ChatColor.GOLD + NumberFormat.addCommaAndRound(deaths[totalMinutes - i]));
            } else if (name.equals("Damage")) {
                lore.add(ChatColor.WHITE + "Minute " + (i + 1) + ": " + ChatColor.GOLD + NumberFormat.addCommaAndRound(damage[totalMinutes - i]));
            } else if (name.equals("Healing")) {
                lore.add(ChatColor.WHITE + "Minute " + (i + 1) + ": " + ChatColor.GOLD + NumberFormat.addCommaAndRound(healing[totalMinutes - i]));
            } else if (name.equals("Absorbed")) {
                lore.add(ChatColor.WHITE + "Minute " + (i + 1) + ": " + ChatColor.GOLD + NumberFormat.addCommaAndRound(absorbed[totalMinutes - i]));
            }
        }
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public boolean isTeamFlagCompass() {
        return teamFlagCompass;
    }

    public void toggleTeamFlagCompass() {
        teamFlagCompass = !teamFlagCompass;
    }

    public CalculateSpeed getSpeed() {
        return speed;
    }

    public Location getDeathLocation() {
        return deathLocation;
    }

    public void setDeathLocation(Location deathLocation) {
        this.deathLocation = deathLocation;
    }

    public ArmorStand getDeathStand() {
        return deathStand;
    }

    public void setDeathStand(ArmorStand deathStand) {
        this.deathStand = deathStand;
    }

    public int getFlagsCaptured() {
        return flagsCaptured;
    }

    public void addFlagCap() {
        this.flagsCaptured++;
    }

    public int getFlagsReturned() {
        return flagsReturned;
    }

    public void addFlagReturn() {
        this.flagsReturned++;
    }

    public int getTotalCapsAndReturnsWeighted() {
        return (this.flagsCaptured * 5) + this.flagsReturned;
    }

    public int getSpawnProtection() {
        return spawnProtection;
    }

    public void setSpawnProtection(int spawnProtection) {
        this.spawnProtection = spawnProtection;
    }

    public void setSpawnDamage(int spawnDamage) {
        this.spawnDamage = spawnDamage;
    }

    public int getSpawnDamage() {
        return spawnDamage;
    }

    public boolean isDead() {
        return dead;
    }

    public void updatePlayerReference(@Nullable Player player) {
        if (player == this.entity) {
            return;
        }
        Location loc = this.getLocation();

        if (player == null) {
            if (this.entity instanceof Player) {
                this.entity = spawnJimmy(loc, ((Player) this.entity).getInventory());
            }
        } else {
            if (this.entity instanceof Zombie) { // This could happen if there was a problem during the quit event
                this.entity.remove();
            }
            player.teleport(loc);
            this.entity = player;
            player.removeMetadata("WARLORDS_PLAYER", Warlords.getInstance());
            player.setMetadata("WARLORDS_PLAYER", new FixedMetadataValue(Warlords.getInstance(), this));
            player.setWalkSpeed(walkspeed);
            player.setMaxHealth(40);
            player.setLevel((int) this.getMaxEnergy());
            player.getInventory().clear();
            this.spec.getWeapon().updateDescription(player);
            this.spec.getRed().updateDescription(player);
            this.spec.getPurple().updateDescription(player);
            this.spec.getBlue().updateDescription(player);
            this.spec.getOrange().updateDescription(player);
            applySkillBoost(player);
            player.closeInventory();
            ((EntityLiving) ((CraftPlayer) player).getHandle()).setAbsorptionHearts(0);
            this.assignItemLore(player);
            ArmorManager.resetArmor(player, getSpecClass(), getTeam());

            if (isDeath()) {
                player.setGameMode(GameMode.SPECTATOR);
                giveRespawnTimer();
            }
            // TODO Update the inventory based on the status of isUndyingArmyDead here
        }
    }

    public Classes getSpecClass() {
        return specClass;
    }

    public Team getTeam() {
        return team;
    }

    public Game getGame() {
        return this.gameState.getGame();
    }

    public boolean isDeath() {
        return this.health <= 0 || this.dead || (entity instanceof Player && ((Player) entity).getGameMode() == GameMode.SPECTATOR);
    }

    public boolean isAlive() {
        return !isDeath();
    }

    @Nonnull
    public LivingEntity getEntity() {
        return this.entity;
    }

    @Nonnull
    public Location getLocation() {
        return this.entity.getLocation();
    }

    @Nonnull
    public Location getLocation(@Nonnull Location copyInto) {
        return this.entity.getLocation(copyInto);
    }

    public boolean isEnemyAlive(Entity other) {
        return isEnemyAlive(Warlords.getPlayer(other));
    }

    public boolean isEnemyAlive(@Nullable WarlordsPlayer p) {
        return p != null &&
                p.getGame() == getGame() &&
                !p.isDeath() &&
                p.getTeam() != getTeam();
    }

    public boolean isEnemy(@Nullable WarlordsPlayer p) {
        return p != null &&
                p.getGame() == getGame() &&
                p.getTeam() != getTeam();
    }

    public boolean isTeammateAlive(Entity other) {
        return isEnemyAlive(Warlords.getPlayer(other));
    }

    public boolean isTeammateAlive(@Nullable WarlordsPlayer p) {
        return p != null &&
                p.getGame() == getGame() &&
                !p.isDeath() &&
                p.getTeam() == getTeam();
    }

    public boolean isTeammate(@Nullable WarlordsPlayer p) {
        return p != null &&
                p.getGame() == getGame() &&
                p.getTeam() == getTeam();
    }

    public void teleport(Location location) {
        this.entity.teleport(location);
    }

    public PlayingState getGameState() {
        return this.gameState;
    }

    public double getFlagDamageMultiplier() {
        return flagDamageMultiplier;
    }

    public void setFlagDamageMultiplier(double flagDamageMultiplier) {
        this.flagDamageMultiplier = flagDamageMultiplier;
    }

    public String getColoredName() {
        return getTeam().teamColor() + getName();
    }

    public String getColoredNameBold() {
        return getTeam().teamColor().toString() + ChatColor.BOLD + getName();
    }

    public void setVelocity(org.bukkit.util.Vector v) {
        if (cooldownManager.hasCooldownFromName("KB Resistance")) {
            setVelocity(v.multiply(1), true);
        } else {
            setVelocity(v, true);
        }
    }

    public void setVelocity(org.bukkit.util.Vector v, boolean kbAfterHorse) {
        if ((kbAfterHorse || this.entity.getVehicle() == null)) {
            if (cooldownManager.hasCooldownFromName("KB Resistance")) {
                this.entity.setVelocity(v.multiply(.75));
            } else {
                this.entity.setVelocity(v);
            }
        }
    }

    public void setVelocity(Location from, double multipliedBy, double y, boolean kbAfterHorse) {
        this.setVelocity(from, getLocation(), multipliedBy, y, kbAfterHorse);
    }

    public void setVelocity(Location from, Location to, double multipliedBy, double y, boolean kbAfterHorse) {
        if (((kbAfterHorse && this.entity.getVehicle() != null) || (!kbAfterHorse && this.entity.getVehicle() == null))) {
            if (cooldownManager.hasCooldownFromName("KB Resistance")) {
                this.entity.setVelocity((to.toVector().subtract(from.toVector()).normalize().multiply(multipliedBy).setY(y)).multiply(.75));
            } else {
                this.entity.setVelocity(to.toVector().subtract(from.toVector()).normalize().multiply(multipliedBy).setY(y));
            }
        }
    }

    public World getWorld() {
        return this.entity.getWorld();
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public void addTimeInCombat() {
        timeInCombat++;
    }

    public int getTimeInCombat() {
        return timeInCombat;
    }

    public void addTotalRespawnTime() {
        respawnTimeSpent += respawnTimer.floatValue();
    }

    public float getRespawnTimeSpent() {
        return respawnTimeSpent;
    }

    public boolean isInfiniteEnergy() {
        return infiniteEnergy;
    }

    public void setInfiniteEnergy(boolean infiniteEnergy) {
        this.infiniteEnergy = infiniteEnergy;
    }

    public boolean isDisableCooldowns() {
        return disableCooldowns;
    }

    public void setDisableCooldowns(boolean disableCooldowns) {
        this.disableCooldowns = disableCooldowns;
    }

    public double getEnergyModifier() {
        return energyModifier;
    }

    public void setEnergyModifier(double energyModifier) {
        this.energyModifier = energyModifier;
    }

    public double getCooldownModifier() {
        return cooldownModifier;
    }

    public void setCooldownModifier(double cooldownModifier) {
        this.cooldownModifier = cooldownModifier;
    }

    public boolean isTakeDamage() {
        return takeDamage;
    }

    public void setTakeDamage(boolean takeDamage) {
        this.takeDamage = takeDamage;
    }

    public boolean isCanCrit() {
        return canCrit;
    }

    public void setCanCrit(boolean canCrit) {
        this.canCrit = canCrit;
    }

    public int getBlocksTravelledCM() {
        return blocksTravelledCM;
    }

    public void setBlocksTravelledCM(int blocksTravelledCM) {
        this.blocksTravelledCM = blocksTravelledCM;
    }

    public float getWalkspeed() {
        return walkspeed;
    }

    public List<Float> getRecordDamage() {
        return recordDamage;
    }

    public UUID getMarkedTarget() {
        return markedTarget;
    }

    public void setMarkedTarget(UUID markedTarget) {
        this.markedTarget = markedTarget;
    }

    public float getCurrentHealthModifier() {
        return currentHealthModifier;
    }

    public void setCurrentHealthModifier(float currentHealthModifier) {
        this.currentHealthModifier = currentHealthModifier;
    }

    public int getHealPowerupDuration() {
        return healPowerupDuration;
    }

    public void setHealPowerupDuration(int healPowerupDuration) {
        this.healPowerupDuration = healPowerupDuration;
    }
}
