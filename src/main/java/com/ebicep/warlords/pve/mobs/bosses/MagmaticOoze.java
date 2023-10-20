package com.ebicep.warlords.pve.mobs.bosses;

import com.ebicep.warlords.abilities.internal.ProjectileAbility;
import com.ebicep.warlords.abilities.internal.icon.RedAbilityIcon;
import com.ebicep.warlords.effects.EffectUtils;
import com.ebicep.warlords.events.player.ingame.WarlordsDamageHealingEvent;
import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.game.option.pve.PveOption;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsNPC;
import com.ebicep.warlords.player.ingame.cooldowns.instances.InstanceFlags;
import com.ebicep.warlords.pve.mobs.AbstractMob;
import com.ebicep.warlords.pve.mobs.Mob;
import com.ebicep.warlords.pve.mobs.abilities.AbstractPveAbility;
import com.ebicep.warlords.pve.mobs.tiers.BossMob;
import com.ebicep.warlords.util.bukkit.LocationBuilder;
import com.ebicep.warlords.util.bukkit.LocationUtils;
import com.ebicep.warlords.util.java.MathUtils;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.PlayerFilter;
import com.ebicep.warlords.util.warlords.Utils;
import net.citizensnpcs.trait.SlimeSize;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;

public class MagmaticOoze extends AbstractMob implements BossMob {

    private static final Material DAMAGE_BLOCK = Material.MAGMA_BLOCK;
    private final Map<LocationUtils.TimedLocationBlockHolder, Material> previousBlocks;
    private int splitNumber;

    public MagmaticOoze(Location spawnLocation) {
        this(spawnLocation, 75_000f, 0, new HashMap<>());
    }

    public MagmaticOoze(Location spawnLocation, float health, int splitNumber, Map<LocationUtils.TimedLocationBlockHolder, Material> previousBlocks) {
        this(spawnLocation, "Magmatic Ooze", (int) (health / (splitNumber + 1)), 1f, 40, 100, 200, splitNumber, previousBlocks);
    }

    public MagmaticOoze(
            Location spawnLocation,
            String name,
            int maxHealth,
            float walkSpeed,
            int damageResistance,
            float minMeleeDamage,
            float maxMeleeDamage
    ) {
        this(spawnLocation, name, maxHealth, walkSpeed, damageResistance, minMeleeDamage, maxMeleeDamage, 0, new HashMap<>());
    }

    public MagmaticOoze(
            Location spawnLocation,
            String name,
            int maxHealth,
            float walkSpeed,
            int damageResistance,
            float minMeleeDamage,
            float maxMeleeDamage,
            int splitNumber,
            Map<LocationUtils.TimedLocationBlockHolder, Material> previousBlocks
    ) {
        super(spawnLocation,
                name,
                maxHealth,
                walkSpeed,
                damageResistance,
                minMeleeDamage,
                maxMeleeDamage,
                new FieryProjectile(600 - (splitNumber * 10), 700 - (splitNumber * 10)),
                new FlamingSlam(900 - (splitNumber * 100), 1400 - (splitNumber * 100)),
                new HeatAura(100 - (splitNumber * 10), 10 - splitNumber),
                new MoltenFissure(previousBlocks),
                new Split(splitNumber, (loc, we) -> new MagmaticOoze(loc, we.getHealth(), splitNumber + 1, previousBlocks))
        );
        this.splitNumber = splitNumber;
        this.previousBlocks = previousBlocks;
    }

    @Override
    public Mob getMobRegistry() {
        return Mob.MAGMATIC_OOZE;
    }

    @Override
    public void onNPCCreate() {
        super.onNPCCreate();
        npc.getOrAddTrait(SlimeSize.class).setSize(10 - (splitNumber * 2));
    }

    @Override
    public void onSpawn(PveOption option) {
        super.onSpawn(option);
        if (splitNumber == 0 && option.getMobs().stream().noneMatch(MagmaticOoze.class::isInstance)) {
            Game game = option.getGame();
            new GameRunnable(game) {

                final Map<WarlordsEntity, Instant> damageCooldown = new HashMap<>();

                @Override
                public void run() {
                    previousBlocks.entrySet().removeIf(timedLocationBlockHolderMaterialEntry -> {
                        long time = timedLocationBlockHolderMaterialEntry.getKey().time();
                        // remove if 30*5 seconds have passed
                        if (time < System.currentTimeMillis() - 150_000) {
                            Block block = timedLocationBlockHolderMaterialEntry.getKey().locationBlockHolder().getBlock();
                            block.setType(timedLocationBlockHolderMaterialEntry.getValue());
                            return true;
                        }
                        return false;
                    });
                    PlayerFilter.playingGame(getGame())
                                .aliveEnemiesOf(warlordsNPC)
                                .forEach(warlordsEntity -> {
                                    Block block = warlordsEntity.getLocation().add(0, -1, 0).getBlock();
                                    if (block.getType() == DAMAGE_BLOCK) {
                                        if (damageCooldown.containsKey(warlordsEntity)) {
                                            Instant lastDamage = damageCooldown.get(warlordsEntity);
                                            if (lastDamage.isAfter(Instant.now().minusMillis(500))) {
                                                return;
                                            }
                                        }
                                        damageCooldown.put(warlordsEntity, Instant.now());
                                        warlordsEntity.addDamageInstance(
                                                warlordsNPC,
                                                "Magma",
                                                100,
                                                150,
                                                0,
                                                100,
                                                EnumSet.of(InstanceFlags.TRUE_DAMAGE)
                                        );
                                    } else {
                                        damageCooldown.remove(warlordsEntity); // remove if not on magma
                                    }
                                });
                }
            }.runTaskTimer(20, 3);
        }
    }

    @Override
    public void whileAlive(int ticksElapsed, PveOption option) {

    }

    @Override
    public void onAttack(WarlordsEntity attacker, WarlordsEntity receiver, WarlordsDamageHealingEvent event) {

    }

    @Override
    public void onDeath(WarlordsEntity killer, Location deathLocation, PveOption option) {
        super.onDeath(killer, deathLocation, option);
        new GameRunnable(option.getGame()) {

            @Override
            public void run() {
                if (option.getMobs().stream().noneMatch(abstractMob -> abstractMob instanceof MagmaticOoze)) {
                    // all dead
                    // restore blocks
                    previousBlocks.forEach((location, material) -> {
                        Block block = location.locationBlockHolder().getBlock();
                        block.setType(material);
                    });
                }
            }
        }.runTaskLater(3);
    }

    public static class FieryProjectile extends AbstractPveAbility implements ProjectileAbility {

        private final double speed = 0.160;
        private final double gravity = -0.005;
        private final double hitbox = 7;
        private final double kbVelocity = 1.2;

        public FieryProjectile(float minDamageHeal, float maxDamageHeal) {
            super("Fiery Projectile", minDamageHeal, maxDamageHeal, 5, 50, 10, 200);
        }

        @Override
        public boolean onPveActivate(@Nonnull WarlordsEntity wp, PveOption pveOption) {
            if (!wp.getEntity().isOnGround()) {
                return false;
            }
            wp.subtractEnergy(name, energyCost, false);

            Location location = wp.getLocation();
            Vector speed = wp.getLocation().getDirection().normalize().multiply(this.speed).setY(.01);

            if (wp instanceof WarlordsNPC warlordsNPC && warlordsNPC.getMob() != null) {
                AbstractMob npcMob = warlordsNPC.getMob();
                Entity target = npcMob.getTarget();
                if (target != null) {
                    double distance = location.distance(target.getLocation());
                    speed.setY(distance * .002);
                }
            }

            Utils.spawnThrowableProjectile(
                    wp.getGame(),
                    Utils.spawnArmorStand(location, armorStand -> {
                        armorStand.getEquipment().setHelmet(new ItemStack(Material.FIRE_CHARGE));
                    }),
                    speed,
                    gravity,
                    this.speed,
                    (newLoc, integer) -> wp.getLocation().getWorld().spawnParticle(
                            Particle.FLAME,
                            newLoc.clone().add(0, -1, 0),
                            6,
                            0.3F,
                            0.3F,
                            0.3F,
                            0.1F,
                            null,
                            true
                    ),
                    newLoc -> PlayerFilter
                            .entitiesAroundRectangle(newLoc, 1, 2, 1)
                            .aliveTeammatesOfExcludingSelf(wp)
                            .findFirstOrNull(),
                    (newLoc, directHit) -> {
                        new GameRunnable(wp.getGame()) {
                            @Override
                            public void run() {
                                for (WarlordsEntity p : PlayerFilter
                                        .entitiesAround(newLoc, hitbox, hitbox, hitbox)
                                        .aliveEnemiesOf(wp)
                                ) {
                                    Vector v;
                                    if (p == directHit) {
                                        v = new LocationBuilder(location).getVectorTowards(p.getLocation()).multiply(kbVelocity).setY(1);
                                    } else {
                                        v = new LocationBuilder(p.getLocation()).getVectorTowards(newLoc).multiply(-kbVelocity).setY(1);
                                    }
                                    p.setVelocity(name, v, false, false);
                                    p.addDamageInstance(wp, name, minDamageHeal, maxDamageHeal, critChance, critMultiplier);
                                }

                                newLoc.setPitch(-12);
                                Location impactLocation = newLoc.clone().subtract(speed);
                                Utils.spawnFallingBlocks(impactLocation, 1.4, 20, -.5, ThreadLocalRandom.current().nextDouble(1, 1.2));
                                Utils.spawnFallingBlocks(impactLocation, 1.1, 9, -.4, ThreadLocalRandom.current().nextDouble(.8, 1));
                                Utils.spawnFallingBlocks(impactLocation, .7, 6, -.3, ThreadLocalRandom.current().nextDouble(.6, .8));
                                Utils.spawnFallingBlocks(impactLocation, .4, 3, -.2, ThreadLocalRandom.current().nextDouble(.4, .6));
                            }
                        }.runTaskLater(1);
                    }
            );
            return true;
        }
    }

    public static class FlamingSlam extends AbstractPveAbility {

        private final int hitbox = 10;

        public FlamingSlam(float minDamageHeal, float maxDamageHeal) {
            super("Flaming Slam", minDamageHeal, maxDamageHeal, 12, 50, 15, 200);
        }

        @Override
        public boolean onPveActivate(@Nonnull WarlordsEntity wp, PveOption pveOption) {
            if (!wp.getEntity().isOnGround()) {
                return false;
            }
            wp.subtractEnergy(name, energyCost, false);

            // launch entity in air towards enemy player
            // on impact, fiery shockwave + lava?

            Game game = wp.getGame();

            //launch straight into air then down diagonally towards enemy player
            wp.setVelocity(name, new Vector(0, 1.75, 0), true);
            new GameRunnable(game) {

                boolean launchedTowardsPlayer = false;
                WarlordsEntity target = null;

                @Override
                public void run() {
                    // check if y velocity starts going down
                    Vector currentVector = wp.getEntity().getVelocity();
                    // TODO maybe tp look towards random ppl then launch towards them
                    if (currentVector.getY() <= 0 && !launchedTowardsPlayer) {
                        // diagonally towards enemy player
                        Vector vectorTowardsEnemy = new LocationBuilder(wp.getLocation()).getVectorTowards(target.getLocation());
                        wp.setVelocity(name, vectorTowardsEnemy.multiply(2.25 + (wp.getLocation().distance(target.getLocation()) * .025)), true);
                        launchedTowardsPlayer = true;
                    } else {
                        if (target == null || target.isDead()) {
                            PlayerFilter.playingGame(game)
                                        .aliveEnemiesOf(wp)
                                        .findAny()
                                        .ifPresent(enemy -> target = enemy);
                        }
                        // fire line particle to target
                        EffectUtils.playParticleLinkAnimation(wp.getLocation(), target.getLocation(), Particle.LANDING_LAVA); //TODO particle
                    }
                    if (launchedTowardsPlayer) {
                        // check if hit ground
                        boolean onGround = wp.getEntity().isOnGround();
                        if (onGround) {
                            // shockwave
                            shockwave(wp);
                            cancel();
                        }
                    }
                }

            }.runTaskTimer(20, 2);
            return true;
        }

        private void shockwave(WarlordsEntity wp) {
            // flying blocks
            Location impactLocation = wp.getLocation();
            Utils.spawnFallingBlocks(impactLocation, 2, 20, -.4, ThreadLocalRandom.current().nextDouble(1, 1.2));
            Utils.spawnFallingBlocks(impactLocation, 1.5, 9, -.3, ThreadLocalRandom.current().nextDouble(.8, 1));
            Utils.spawnFallingBlocks(impactLocation, 1, 6, -.2, ThreadLocalRandom.current().nextDouble(.6, .8));
            Utils.spawnFallingBlocks(impactLocation, .5, 3, -.1, ThreadLocalRandom.current().nextDouble(.4, .6));
            // damage
            PlayerFilter.entitiesAround(wp, hitbox, hitbox, hitbox)
                        .aliveEnemiesOf(wp)
                        .forEach(enemy -> {
                            enemy.addDamageInstance(
                                    wp,
                                    name,
                                    minDamageHeal,
                                    maxDamageHeal,
                                    critChance,
                                    critMultiplier
                            );
                        });
            // lava?
        }

    }

    public static class HeatAura extends AbstractPveAbility {

        private final int hitbox;

        public HeatAura(float startDamage, int hitbox) {
            super("Heat Aura", startDamage, startDamage, 2, 50, 25, 200);
            this.hitbox = hitbox;
        }

        @Override
        public boolean onPveActivate(@Nonnull WarlordsEntity wp, PveOption pveOption) {
            wp.subtractEnergy(name, energyCost, false);

            // increase heat / damage on every use
            if (this.timesUsed <= 40) { // ~700 max at split 0
                this.multiplyMinMax(1.05f);
            }
            PlayerFilter.entitiesAround(wp, hitbox, hitbox, hitbox)
                        .aliveEnemiesOf(wp)
                        .forEach(enemy -> {
                            enemy.addDamageInstance(
                                    wp,
                                    name,
                                    minDamageHeal,
                                    maxDamageHeal,
                                    critChance,
                                    critMultiplier
                            );
                        });
            return true;
        }

    }

    public static class MoltenFissure extends AbstractPveAbility implements RedAbilityIcon {

        private static final int MAX_FISSURE_LENGTH = 15;
        private static final int MIN_BREAK_SIZE = 3;
        private static final int MAX_BREAK_SIZE = 5;
        private static final int VALID_CHECK = 1;
        private final Map<LocationUtils.TimedLocationBlockHolder, Material> previousBlocks;

        public MoltenFissure(Map<LocationUtils.TimedLocationBlockHolder, Material> previousBlocks) {
            super("Molten Fissure", 30, 50);
            this.previousBlocks = previousBlocks;
        }

        @Override
        public boolean onPveActivate(@Nonnull WarlordsEntity wp, PveOption pveOption) {
            wp.subtractEnergy(name, energyCost, false);

            Location groundLocation = LocationUtils.getGroundLocation(wp.getLocation());
            double yDiff = wp.getLocation().getY() - groundLocation.getY();
            Game game = wp.getGame();
            new GameRunnable(game) {
                final Location flameParticleStart = wp.getLocation();
                int timer = 0;

                @Override
                public void run() {
                    timer++;
                    if (timer < yDiff) {
                        // flame particles going towards ground if ability casted while in air
                        EffectUtils.displayParticle(
                                Particle.FLAME,
                                flameParticleStart.add(0, -1, 0),
                                10,
                                .75,
                                .75,
                                .75,
                                0
                        );
                        return;
                    } else {
                        cancel();
                    }
                    EffectUtils.displayParticle(
                            Particle.BLOCK_CRACK,
                            groundLocation,
                            300,
                            5,
                            0,
                            5,
                            0,
                            Material.DIRT.createBlockData()
                    );
                    // --- initial ground break
                    int randomYawMultiplier = MathUtils.generateRandomValueBetweenInclusive(0, 8); // 360/45
                    LocationBuilder randomFacingStartLocation = new LocationBuilder(groundLocation.add(0, -.25, 0).toCenterLocation())
                            .pitch(0)
                            .yaw(randomYawMultiplier * 45);
                    boolean is45Degrees = randomYawMultiplier % 2 == 1;
                    int breakSize = MathUtils.generateRandomValueBetweenInclusive(MIN_BREAK_SIZE, MAX_BREAK_SIZE);
                    float shift = 1;
                    // iterate in square starting at bottom left
                    List<LocationBuilder> initialBreak = new ArrayList<>();
                    if (is45Degrees) {
                        if (breakSize != MIN_BREAK_SIZE) {
                            breakSize--;
                        }
                        shift = 1.414f; // accounts for diagonal
                        // add blocks to account for gaps in 45 degree angle
                        // second small break, breakSize - 1
                        // starts at bottomLeftCornerStart but forward and right shift
                        int secondBreakSize = breakSize - 1;
                        addSquareBlocks(
                                secondBreakSize,
                                shift,
                                initialBreak,
                                randomFacingStartLocation.clone()
                                                         .backward(breakSize / 2f).left(breakSize / 2f)
                                                         .forward(shift / 2).right(shift / 2)
                        );
                    }
                    addSquareBlocks(
                            breakSize,
                            shift,
                            initialBreak,
                            randomFacingStartLocation.clone()
                                                     .backward(breakSize / 2f).left(breakSize / 2f)
                    );

                    for (LocationBuilder location : initialBreak) {
                        if (cannotValidateLocation(location)) {
                            break;
                        }
                        spawnDamageBlock(location);
                    }

                    // --- fissures
                    new GameRunnable(game) {
                        final List<List<LocationBuilder>> fissures = getAllFissureLocations(randomFacingStartLocation);
                        final boolean[] discontinueIndexes = new boolean[4];
                        int spread = 0;

                        @Override
                        public void run() {
                            for (int i = 0; i < fissures.size(); i++) {
                                if (discontinueIndexes[i]) {
                                    continue;
                                }
                                List<LocationBuilder> fissure = fissures.get(i);
                                LocationBuilder location = fissure.get(spread);
                                if (cannotValidateLocation(location)) {
                                    discontinueIndexes[i] = true;
                                    break;
                                }
                                EffectUtils.displayParticle(
                                        Particle.LAVA,
                                        location.clone().add(0, 1, 0),
                                        3,
                                        .25,
                                        .25,
                                        .25,
                                        0
                                );
                                spawnDamageBlock(location);
                            }
                            spread++;
                            if (spread >= MAX_FISSURE_LENGTH) {
                                cancel();
                            }
                        }
                    }.runTaskTimer(20, 3);
                }

                private void addSquareBlocks(int breakSize, float shift, List<LocationBuilder> initialBreak, LocationBuilder bottomLeftCornerStart) {
                    boolean previouslySkipped = false;
                    for (int i = 0; i < breakSize * breakSize; i++) {
                        // random chance to skip, cant be in a row
                        if (MathUtils.generateRandomValueBetweenInclusive(0, MAX_BREAK_SIZE - breakSize + 2) != 0 || previouslySkipped) {
                            previouslySkipped = false;
                            for (int j = 1; j <= 3; j++) {
                                EffectUtils.displayParticle(
                                        Particle.LAVA,
                                        bottomLeftCornerStart.clone().add(0, j, 0),
                                        2,
                                        .25,
                                        .1,
                                        .25,
                                        0
                                );
                            }
                            initialBreak.add(bottomLeftCornerStart.clone());
                        } else {
                            previouslySkipped = true;
                        }
                        bottomLeftCornerStart.right(shift);
                        if (i % breakSize == breakSize - 1) {
                            bottomLeftCornerStart.left(breakSize * shift);
                            bottomLeftCornerStart.forward(shift);
                        }
                    }
                }

                private void spawnDamageBlock(LocationBuilder location) {
                    Block block = location.getBlock();
                    LocationUtils.LocationBlockHolder blockHolder = new LocationUtils.LocationBlockHolder(location);
                    previousBlocks.putIfAbsent(new LocationUtils.TimedLocationBlockHolder(blockHolder), block.getType());
                    game.getPreviousBlocks().putIfAbsent(blockHolder, block.getType());
                    block.setType(DAMAGE_BLOCK);
                }

                @Nonnull
                private List<List<LocationBuilder>> getAllFissureLocations(LocationBuilder randomFacingStartLocation) {
                    List<List<LocationBuilder>> fissures = new ArrayList<>();
                    fissures.add(getFissureLocations(randomFacingStartLocation.clone().forward(1)));
                    fissures.add(getFissureLocations(randomFacingStartLocation.clone().backward(1).lookBackwards()));
                    fissures.add(getFissureLocations(randomFacingStartLocation.clone().left(1).lookLeft()));
                    fissures.add(getFissureLocations(randomFacingStartLocation.clone().right(1).lookRight()));
                    return fissures;
                }

                public List<LocationBuilder> getFissureLocations(Location start) {
                    // begin at start location then go forward 2 times + random 0-2
                    // randomly go left or right 1 block
                    // go forward start 1 block forward
                    List<LocationBuilder> locations = new ArrayList<>();
                    //TODO normalize yaw, diagonal wont work?
                    LocationBuilder location = new LocationBuilder(start)
                            .pitch(0);
                    while (locations.size() < MAX_FISSURE_LENGTH) {
                        for (int i = 0; i < 2 + ThreadLocalRandom.current().nextInt(3); i++) {
                            location.forward(1);
                            locations.add(location.clone());
                        }
                        if (ThreadLocalRandom.current().nextBoolean()) {
                            location.left(1);
                        } else {
                            location.right(1);
                        }
                        locations.add(location.clone());
                    }
                    return locations;
                }
            }.runTaskTimer(0, 2);
            return true;
        }

        public static boolean cannotValidateLocation(LocationBuilder location) {
            // check if block is solid, if not then check up to VALID_CHECK blocks up/down depending if underground or above, if not then return
            boolean isSolid = !location.getBlock().getType().isSolid();
            if (isSolid) {
                if (location.clone().addY(-1).getBlock().getType().isSolid()) {
                    location.addY(-1);
                    return false;
                } else {
                    // check down
                    for (int i = 0; i < VALID_CHECK; i++) {
                        location.addY(-1);
                        if (location.getBlock().getType() != Material.AIR) {
                            return false;
                        }
                    }
                    return true;
                }
            } else if (location.clone().addY(1).getBlock().getType().isSolid()) {
                // check up
                for (int i = 0; i < VALID_CHECK; i++) {
                    location.addY(1);
                    if (!location.clone().addY(1).getBlock().getType().isSolid()) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

    }

    private static class Split extends AbstractPveAbility {
        private final int maxSplit = 4;
        private final int split;
        private final BiFunction<Location, WarlordsEntity, AbstractMob> splitSpawnFunction;
        private double splitChance = .2;
        private boolean init = false;

        public Split(int split, BiFunction<Location, WarlordsEntity, AbstractMob> splitSpawnFunction) {
            super("Split", 20, 50, true);
            this.split = split;
            this.splitSpawnFunction = splitSpawnFunction;
        }

        @Override
        public boolean onPveActivate(@Nonnull WarlordsEntity wp, PveOption pveOption) {
            wp.subtractEnergy(name, energyCost, false);
            if (!init) {
                init = true;
                resetSplitChance(pveOption);
            }
            if (split >= maxSplit) {
                return true;
            }
            // 25% chance to split
            if (ThreadLocalRandom.current().nextDouble() < splitChance) {
                pveOption.spawnNewMob(splitSpawnFunction.apply(wp.getLocation(), wp));
                resetSplitChance(pveOption);
            } else {
                splitChance += .05 * pveOption.playerCount();
            }
            return true;
        }

        public void resetSplitChance(PveOption pveOption) {
            splitChance = .05 * pveOption.playerCount() - .05;
        }


    }
}
