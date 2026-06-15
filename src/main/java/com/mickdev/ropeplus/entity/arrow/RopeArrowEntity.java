package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.Config;
import com.mickdev.ropeplus.RopePlus;
import com.mickdev.ropeplus.block.WallRopeBlock;
import com.mickdev.ropeplus.common.RopeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Deploys a long climbable rope where it lands, either hanging from the
 * block it hits or against a wall.
 */
public class RopeArrowEntity extends Arrow303Entity {

    public RopeArrowEntity(EntityType<? extends RopeArrowEntity> type, Level level) {
        super(type, level);
    }

    public RopeArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.ROPE_ARROW_ENTITY.get(), owner, level, ammo, weapon);
    }

    public RopeArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.ROPE_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.ROPE_ARROW.get());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide) {
            return;
        }

        BlockState rope = RopePlus.ROPE.get().defaultBlockState();
        if (tryToPlaceBlock(rope)) {
            if (placedPos != null && level() instanceof ServerLevel serverLevel) {
                RopeManager.queueGrowth(serverLevel, placedPos, rope, Config.ARROW_ROPE_LENGTH.get() - 1);
            }
            discard();
        } else if (tryToPlaceWallRope()) {
            discard();
        }
    }

    private boolean tryToPlaceWallRope() {
        // figure out which wall we are flying against
        Direction facing;
        var motion = getDeltaMovement();
        if (Math.abs(motion.x) > Math.abs(motion.z)) {
            facing = motion.x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            facing = motion.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }

        BlockState ropeState = RopePlus.WALL_ROPE.get().defaultBlockState().setValue(WallRopeBlock.FACING, facing);
        BlockPos base = blockPosition();
        for (int[] off : CANDIDATES) {
            BlockPos pos = base.offset(off[0], off[1], off[2]);
            BlockState existing = level().getBlockState(pos);
            if ((existing.isAir() || existing.canBeReplaced()) && ropeState.canSurvive(level(), pos)) {
                level().setBlock(pos, ropeState, Block.UPDATE_ALL);
                if (level() instanceof ServerLevel serverLevel) {
                    RopeManager.queueGrowth(serverLevel, pos, ropeState, Config.ARROW_ROPE_LENGTH.get());
                }
                placedPos = pos;
                return true;
            }
        }
        return false;
    }
}
