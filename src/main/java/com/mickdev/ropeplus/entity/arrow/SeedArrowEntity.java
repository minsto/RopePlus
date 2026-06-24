package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Grows things: dirt to grass, grass to flowers, cobblestone to moss, farmland to wheat.
 */
public class SeedArrowEntity extends Arrow303Entity {

    public SeedArrowEntity(EntityType<? extends SeedArrowEntity> type, Level level) {
        super(type, level);
    }

    public SeedArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.SEED_ARROW_ENTITY.get(), owner, level, ammo, weapon);
    }

    public SeedArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.SEED_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.SEED_ARROW.get());
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide) {
            return;
        }
        BlockPos pos = result.getBlockPos();
        BlockState hitState = level().getBlockState(pos);
        BlockPos above = pos.above();
        boolean airAbove = level().getBlockState(above).isAir();

        if (hitState.is(Blocks.DIRT)) {
            level().setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            discard();
        } else if (hitState.is(Blocks.GRASS_BLOCK) && airAbove) {
            BlockState plant = switch (random.nextInt(3)) {
                case 0 -> Blocks.POPPY.defaultBlockState();
                case 1 -> Blocks.DANDELION.defaultBlockState();
                default -> Blocks.SHORT_GRASS.defaultBlockState();
            };
            level().setBlock(above, plant, Block.UPDATE_ALL);
            discard();
        } else if (hitState.is(Blocks.COBBLESTONE)) {
            level().setBlock(pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
            discard();
        } else if (hitState.is(Blocks.FARMLAND) && airAbove) {
            level().setBlock(above, Blocks.WHEAT.defaultBlockState(), Block.UPDATE_ALL);
            discard();
        }
    }
}
