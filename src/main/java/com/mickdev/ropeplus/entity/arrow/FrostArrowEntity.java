package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Freezes water, hardens lava, extinguishes fire and slows hit entities in place.
 */
public class FrostArrowEntity extends Arrow303Entity {

    @Nullable
    private LivingEntity victim;
    private float freezeFactor;
    private int freezeTimer;

    public FrostArrowEntity(EntityType<? extends FrostArrowEntity> type, Level level) {
        super(type, level);
    }

    public FrostArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.FROST_ARROW_ENTITY.get(), owner, level, ammo, weapon);
    }

    public FrostArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.FROST_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.FROST_ARROW.get());
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.ITEM_SNOWBALL;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (victim != null) {
            return;
        }
        if (result.getEntity() instanceof LivingEntity living) {
            living.hurt(damageSources().arrow(this, getOwner()), computeCustomDamage());
            victim = living;
            freezeFactor = living instanceof Player ? 0.5F : 0.1F;
            freezeTimer = living instanceof Player ? 5 : 10 * 20;
            setDeltaMovement(Vec3.ZERO);
        }
        // no super call: the arrow stays on the victim while it is frozen
    }

    @Override
    public void tick() {
        super.tick();
        if (victim != null) {
            if (!victim.isAlive()) {
                discard();
                return;
            }
            setPos(victim.getX(), victim.getBoundingBox().minY + victim.getBbHeight() * 0.5D, victim.getZ());
            victim.setDeltaMovement(victim.getDeltaMovement().scale(freezeFactor));
            if (--freezeTimer <= 0) {
                discard();
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide) {
            return;
        }
        BlockPos center = result.getBlockPos();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            BlockState state = level().getBlockState(pos);
            if (state.is(Blocks.WATER) && state.getValue(LiquidBlock.LEVEL) == 0) {
                level().setBlock(pos, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
            } else if (state.is(Blocks.LAVA) && state.getValue(LiquidBlock.LEVEL) == 0) {
                level().setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
            } else if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                level().removeBlock(pos, false);
            } else if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
                level().destroyBlock(pos, true);
            }
        }
    }
}
