package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class RedstoneTorchArrowEntity extends Arrow303Entity {

    public RedstoneTorchArrowEntity(EntityType<? extends RedstoneTorchArrowEntity> type, Level level) {
        super(type, level);
    }

    public RedstoneTorchArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.REDSTONE_TORCH_ARROW_ENTITY.get(), owner, level, ammo, weapon);
    }

    public RedstoneTorchArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.REDSTONE_TORCH_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.REDSTONE_TORCH_ARROW.get());
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return DustParticleOptions.REDSTONE;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide && tryToPlaceBlock(Blocks.REDSTONE_TORCH.defaultBlockState())) {
            discard();
        }
    }
}
