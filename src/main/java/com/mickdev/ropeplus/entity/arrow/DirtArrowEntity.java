package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class DirtArrowEntity extends Arrow303Entity {

    public DirtArrowEntity(EntityType<? extends DirtArrowEntity> type, Level level) {
        super(type, level);
    }

    public DirtArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.DIRT_ARROW_ENTITY.get(), owner, level, ammo, weapon);
    }

    public DirtArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.DIRT_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.DIRT_ARROW.get());
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide && tryToPlaceBlock(Blocks.DIRT.defaultBlockState())) {
            discard();
        }
    }
}
