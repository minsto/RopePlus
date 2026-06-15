package com.mickdev.ropeplus.entity.arrow;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all Ropes+ special arrows ("Arrow303" in the original mod).
 */
public abstract class Arrow303Entity extends AbstractArrow {

    /** placement candidate offsets around the impact point, ordered by proximity */
    protected static final int[][] CANDIDATES = {
            {0, 0, 0}, {0, -1, 0}, {0, 1, 0}, {-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1},
            {-1, -1, 0}, {-1, 0, -1}, {-1, 0, 1}, {-1, 1, 0}, {0, -1, -1}, {0, -1, 1},
            {0, 1, -1}, {0, 1, 1}, {1, -1, 0}, {1, 0, -1}, {1, 0, 1}, {1, 1, 0},
            {-1, -1, -1}, {-1, -1, 1}, {-1, 1, -1}, {-1, 1, 1}, {1, -1, -1}, {1, -1, 1},
            {1, 1, -1}, {1, 1, 1}};

    @Nullable
    protected BlockPos placedPos;

    protected Arrow303Entity(EntityType<? extends Arrow303Entity> type, Level level) {
        super(type, level);
    }

    protected Arrow303Entity(EntityType<? extends Arrow303Entity> type, LivingEntity owner, Level level,
                             ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon) {
        super(type, owner, level, pickupItemStack, firedFromWeapon);
    }

    protected Arrow303Entity(EntityType<? extends Arrow303Entity> type, double x, double y, double z, Level level,
                             ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon) {
        super(type, x, y, z, level, pickupItemStack, firedFromWeapon);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && !this.inGround) {
            spawnTrailParticles();
        }
    }

    protected void spawnTrailParticles() {
        ParticleOptions particle = getTrailParticle();
        if (particle == null) {
            return;
        }
        Vec3 motion = getDeltaMovement();
        for (int i = 0; i < 4; i++) {
            level().addParticle(particle,
                    getX() + motion.x * i / 4.0D,
                    getY() + motion.y * i / 4.0D,
                    getZ() + motion.z * i / 4.0D,
                    -motion.x, -motion.y + 0.2D, -motion.z);
        }
    }

    @Nullable
    protected ParticleOptions getTrailParticle() {
        return null;
    }

    /**
     * Tries to place the given block state at or around the arrow's position.
     * Sets {@link #placedPos} on success.
     */
    protected boolean tryToPlaceBlock(BlockState placeState) {
        BlockPos base = blockPosition();
        for (int[] off : CANDIDATES) {
            BlockPos pos = base.offset(off[0], off[1], off[2]);
            BlockState existing = level().getBlockState(pos);
            if ((existing.isAir() || existing.canBeReplaced()) && placeState.canSurvive(level(), pos)) {
                if (!level().isClientSide) {
                    level().setBlock(pos, placeState, Block.UPDATE_ALL);
                }
                placedPos = pos;
                return true;
            }
        }
        return false;
    }

    /**
     * Approximation of the original arrow damage formula, used by arrows that
     * bypass the vanilla hit handling.
     */
    protected float computeCustomDamage() {
        float dmg = (float) (getDeltaMovement().length() + getBaseDamage());
        if (isCritArrow()) {
            dmg *= 2;
        }
        return dmg;
    }
}
