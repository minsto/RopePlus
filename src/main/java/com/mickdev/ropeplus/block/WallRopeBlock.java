package com.mickdev.ropeplus.block;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Climbable rope hanging against a wall. FACING points towards the supporting wall.
 * Deployed by the grappling hook and rope arrows; hangs down in a column.
 */
public class WallRopeBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SOUTH_SHAPE = Block.box(0.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NORTH_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D);
    private static final VoxelShape EAST_SHAPE = Block.box(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 16.0D);

    public WallRopeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
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
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        if (clicked.getAxis().isHorizontal()) {
            BlockState state = defaultBlockState().setValue(FACING, clicked.getOpposite());
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState state = defaultBlockState().setValue(FACING, dir);
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        // solid wall behind
        BlockPos wallPos = pos.relative(facing);
        if (level.getBlockState(wallPos).isFaceSturdy(level, wallPos, facing.getOpposite())) {
            return true;
        }
        // wall rope above (hanging column)
        BlockState above = level.getBlockState(pos.above());
        if (above.is(this)) {
            return true;
        }
        // grappling hook block lying on the ledge above
        BlockPos hookPos = pos.above().relative(facing);
        return level.getBlockState(hookPos).is(RopePlus.GRAPPLING_HOOK_BLOCK.get());
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
            level.destroyBlock(pos, false);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }
}
