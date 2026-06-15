package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes all mobs around the impact point turn on each other.
 */
public class ConfusionArrowEntity extends Arrow303Entity {

    private static final double CONFUSION_EFFECT_SIZE = 6D;

    public ConfusionArrowEntity(EntityType<? extends ConfusionArrowEntity> type, Level level) {
        super(type, level);
    }

    public ConfusionArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.CONFUSING_ARROW_ENTITY.get(), owner, level, ammo, weapon);
    }

    public ConfusionArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.CONFUSING_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.CONFUSING_ARROW.get());
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        confuse(this);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        confuse(result.getEntity());
        super.onHitEntity(result);
    }

    private void confuse(Entity center) {
        if (level().isClientSide) {
            return;
        }
        List<PathfinderMob> hitList = new ArrayList<>();
        for (Entity entity : level().getEntities(this, center.getBoundingBox().inflate(CONFUSION_EFFECT_SIZE))) {
            if (entity instanceof PathfinderMob mob && entity != getOwner()) {
                hitList.add(mob);
            }
        }
        if (hitList.size() < 2) {
            return;
        }
        for (int i = 0; i < hitList.size(); i++) {
            PathfinderMob creatureA = hitList.get(i);
            PathfinderMob creatureB = hitList.get(i != 0 ? i - 1 : hitList.size() - 1);
            creatureA.hurt(damageSources().mobAttack(creatureB), 0f);
            creatureB.hurt(damageSources().mobAttack(creatureA), 0f);
            creatureA.setTarget(creatureB);
            creatureB.setTarget(creatureA);
        }
        discard();
    }
}
