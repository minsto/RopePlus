package com.mickdev.ropeplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;

/**
 * Hanging climbable rope. Survives below a solid block or another rope.
 * Placing a rope next to an existing rope column extends that column downwards
 * (slide-down placement from the original 1.12 mod).
 */
public class RopeBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(6.4D, 0.0D, 6.4D, 9.6D, 16.0D, 9.6D);

    public RopeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.is(this) || aboveState.isFaceSturdy(level, above, Direction.DOWN)) {
            return true;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(dir)).is(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || oldState.is(this)) {
            return;
        }

        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        boolean supportedFromAbove = aboveState.is(this) || aboveState.isFaceSturdy(level, above, Direction.DOWN);
        if (supportedFromAbove) {
            return;
        }

        // Placed against an adjacent rope column: slide down to extend that column
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.getBlockState(neighbor).is(this)) {
                continue;
            }
            BlockPos cursor = neighbor.below();
            for (int i = 0; i < 32; i++) {
                BlockState s = level.getBlockState(cursor);
                if (s.is(this)) {
                    cursor = cursor.below();
                    continue;
                }
                if (s.isAir()) {
                    level.removeBlock(pos, false);
                    level.setBlock(cursor, defaultBlockState(), Block.UPDATE_ALL);
                }
                return;
            }
            return;
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }
}
