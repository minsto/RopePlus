package com.mickdev.ropeplus.block;

import com.mickdev.ropeplus.RopePlus;
import com.mickdev.ropeplus.common.RopeManager;
import com.mickdev.ropeplus.entity.FreeFormRopeEntity;
import com.mickdev.ropeplus.network.HookshotPayload;
import com.mickdev.ropeplus.network.ZiplinePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Zipline anchor. Hangs from a ceiling block. Using it while having a hookshot
 * rope deployed converts that rope into a permanent zipline; using it again rides the line.
 */
public class ZiplineAnchorBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(6.4D, 0.0D, 6.4D, 9.6D, 16.0D, 9.6D);

    public ZiplineAnchorBlock(Properties properties) {
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
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        return level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN);
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ZiplineAnchorBlockEntity anchor) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (anchor.hasZipline()) {
            // ride the zipline
            PacketDistributor.sendToPlayer(serverPlayer, new ZiplinePayload(anchor.getZiplineEntity().getId(), 0f));
            level.playSound(null, player.blockPosition(), RopePlus.SOUND_ZIPLINE.get(), SoundSource.PLAYERS, 1.0F,
                    1.0F / (player.getRandom().nextFloat() * 0.1F + 0.95F));
            return InteractionResult.CONSUME;
        }

        // try converting the player's hookshot rope into a zipline
        FreeFormRopeEntity rope = RopeManager.getPlayerRope(player);
        if (rope != null && rope.isAlive()) {
            if (rope.getEndY() > pos.getY()) {
                player.displayClientMessage(Component.translatable("message.ropeplus.zipline_fail_up"), false);
                return InteractionResult.CONSUME;
            }
            BlockPos target = new BlockPos(Mth.floor(rope.getEndX()), Mth.floor(rope.getEndY() + 0.5D), Mth.floor(rope.getEndZ()));
            // if another anchor sits near the hook impact, snap the line onto it
            BlockPos anchorTarget = findNearbyAnchor(level, target, pos);
            if (anchorTarget != null) {
                target = anchorTarget;
            }
            if (anchorTarget != null || level.getBlockState(target).isCollisionShapeFullBlock(level, target)) {
                anchor.setTargetCoordinates(target);
                if (!player.getAbilities().instabuild) {
                    consumeItem(player, RopePlus.HOOKSHOT.get().getDefaultInstance());
                    // the range boost cartridges are spent on the permanent line:
                    // the hookshot goes back to its base 50-block range
                    ItemStack offhand = player.getOffhandItem();
                    if (offhand.is(RopePlus.HOOKSHOT_CARTRIDGE.get())) {
                        offhand.setCount(0);
                    }
                }
                PacketDistributor.sendToPlayer(serverPlayer, new HookshotPayload(-1, BlockPos.ZERO));
                rope.discard();
                level.playSound(null, player.blockPosition(), RopePlus.SOUND_ROPE_TENSION.get(), SoundSource.PLAYERS, 1.0F,
                        1.0F / (player.getRandom().nextFloat() * 0.1F + 0.95F));
                return InteractionResult.CONSUME;
            } else {
                player.displayClientMessage(Component.translatable("message.ropeplus.zipline_fail_target",
                        target.getX(), target.getY(), target.getZ()), false);
            }
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Scans a small cube around the hook impact point for another zipline anchor,
     * so lines snap anchor-to-anchor instead of ending inside the hit block.
     */
    @Nullable
    private static BlockPos findNearbyAnchor(Level level, BlockPos around, BlockPos self) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(around.offset(-2, -2, -2), around.offset(2, 2, 2))) {
            if (!p.equals(self) && level.getBlockState(p).is(RopePlus.ZIPLINE_ANCHOR.get())) {
                double dist = p.distSqr(around);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    private static void consumeItem(Player player, ItemStack match) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItem(stack, match)) {
                stack.shrink(1);
                return;
            }
        }
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZiplineAnchorBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != RopePlus.ZIPLINE_ANCHOR_BE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ZiplineAnchorBlockEntity.serverTick(lvl, pos, st, (ZiplineAnchorBlockEntity) be);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }
}
