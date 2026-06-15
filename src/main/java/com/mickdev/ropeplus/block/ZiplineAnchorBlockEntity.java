package com.mickdev.ropeplus.block;

import com.mickdev.ropeplus.RopePlus;
import com.mickdev.ropeplus.entity.FreeFormRopeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ZiplineAnchorBlockEntity extends BlockEntity {

    @Nullable
    private BlockPos target;
    @Nullable
    private FreeFormRopeEntity ropeEntity;

    public ZiplineAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(RopePlus.ZIPLINE_ANCHOR_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ZiplineAnchorBlockEntity anchor) {
        anchor.checkRope();
    }

    public boolean hasZipline() {
        return ropeEntity != null && ropeEntity.isAlive();
    }

    @Nullable
    public FreeFormRopeEntity getZiplineEntity() {
        return ropeEntity;
    }

    public void setTargetCoordinates(BlockPos targetPos) {
        this.target = targetPos;
        setChanged();
        trySpawningRope();
    }

    private void checkRope() {
        if (!isRemoved()) {
            if (ropeEntity == null || !ropeEntity.isAlive()) {
                ropeEntity = null;
                trySpawningRope();
            }
        } else if (ropeEntity != null) {
            ropeEntity.discard();
            ropeEntity = null;
        }
    }

    @Override
    public void setRemoved() {
        if (ropeEntity != null) {
            ropeEntity.discard();
            ropeEntity = null;
        }
        super.setRemoved();
    }

    private void trySpawningRope() {
        if (level == null || level.isClientSide || target == null) {
            return;
        }
        boolean targetIsAnchor = level.getBlockState(target).is(RopePlus.ZIPLINE_ANCHOR.get());
        if (targetIsAnchor || level.getBlockState(target).isCollisionShapeFullBlock(level, target)) {
            FreeFormRopeEntity rope = new FreeFormRopeEntity(RopePlus.FREEFORM_ROPE.get(), level);
            rope.setStartCoordinates(worldPosition.getX() + 0.5D, worldPosition.getY(), worldPosition.getZ() + 0.5D);
            // anchor target: attach to the top of the anchor; block target: attach to the block top
            double endY = targetIsAnchor ? target.getY() + 0.9D : target.getY() + 1.0D;
            rope.setEndCoordinates(target.getX() + 0.5D, endY, target.getZ() + 0.5D);
            rope.setLoosening();
            level.addFreshEntity(rope);
            ropeEntity = rope;
        } else if (!isRemoved()) {
            target = null;
            setChanged();
            if (ropeEntity != null) {
                ropeEntity.discard();
                ropeEntity = null;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (target != null) {
            tag.putInt("targetX", target.getX());
            tag.putInt("targetY", target.getY());
            tag.putInt("targetZ", target.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("targetX")) {
            target = new BlockPos(tag.getInt("targetX"), tag.getInt("targetY"), tag.getInt("targetZ"));
        } else {
            target = null;
        }
    }
}
