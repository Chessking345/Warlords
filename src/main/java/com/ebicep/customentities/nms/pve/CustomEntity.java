package com.ebicep.customentities.nms.pve;

import com.ebicep.warlords.game.option.wavedefense.WaveDefenseOption;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;

public interface CustomEntity<T extends EntityInsentient> {

    default void resetAI(World world) {
        resetGoalAI(world);
        resetTargetAI(world);
    }

    default void resetGoalAI(World world) {
        get().goalSelector = new PathfinderGoalSelector(world != null && world.methodProfiler != null ? world.methodProfiler : null);
    }

    default void resetTargetAI(World world) {
        get().targetSelector = new PathfinderGoalSelector(world != null && world.methodProfiler != null ? world.methodProfiler : null);
    }

    default void giveBaseAI() {
        giveBaseAI(1.0, 1.0);
    }

    default void giveBaseAI(double speedTowardsTarget, double wanderSpeed) {
        T entity = get();
        //float in water
        aiFloat();
        if (entity instanceof EntityCreature) {
            //melee entity within range/onCollide?
            aiMeleeAttack(speedTowardsTarget);
            //wander around
            aiWander(wanderSpeed);

            //targets entity that hit it
            aiTargetHitBy();
            //targets closest entities
            aiTargetClosest();
        }
        //look at player
        aiLookAtPlayer();
        //look idle
        aiLookIdle();
    }

    //GOAL SELECTOR AI
    default void aiFloat() {
        get().goalSelector.a(0, new PathfinderGoalFloat(get()));
    }

    default void aiMeleeAttack(double speedTowardsTarget) {
        T entity = get();
        if (entity instanceof EntityCreature) {
            entity.goalSelector.a(1, new PathfinderGoalMeleeAttack((EntityCreature) entity, EntityHuman.class, speedTowardsTarget, true));
        }
    }

    default void aiWander(double wanderSpeed) {
        T entity = get();
        if (entity instanceof EntityCreature) {
            entity.goalSelector.a(7, new PathfinderGoalRandomStroll((EntityCreature) entity, wanderSpeed));
        }
    }

    default void aiLookAtPlayer() {
        get().goalSelector.a(8, new PathfinderGoalLookAtPlayer(get(), EntityHuman.class, 80.0F));
    }

    default void aiLookIdle() {
        get().goalSelector.a(8, new PathfinderGoalRandomLookaround(get()));
    }

    //TARGET SELECTOR AI
    default void aiTargetHitBy() {
        T entity = get();
        if (entity instanceof EntityCreature) {
            entity.targetSelector.a(1, new PathfinderGoalHurtByTarget((EntityCreature) entity, false));
        }
    }

    default void aiTargetClosest() {
        T entity = get();
        if (entity instanceof EntityCreature) {
            entity.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget<>((EntityCreature) entity, EntityHuman.class, true, false));
        }
    }


    default void spawn(Location location) {
        T customEntity = get();
        customEntity.setPosition(location.getX(), location.getY(), location.getZ());
        customEntity.setCustomNameVisible(true);

        ((CraftWorld) location.getWorld()).getHandle().addEntity(customEntity);
    }

    default void onDeath(T entity, Location deathLocation, WaveDefenseOption waveDefenseOption) {

    }

    T get();


}
