package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Teleports its shooter to the impact point.
 */
public class WarpArrowEntity extends Arrow303Entity {

    public WarpArrowEntity(EntityType<? extends WarpArrowEntity> type, Level level) {
        super(type, level);
    }

    public WarpArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.WARP_ARROW_ENTITY.get(), owner, level, ammo, weapon);
    }

    public WarpArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.WARP_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.WARP_ARROW.get());
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.PORTAL;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        Entity owner = getOwner();
        BlockPos pos = result.getBlockPos();
        if (!level().isClientSide && owner != null && pos.getY() > level().getMinBuildHeight() + 8) {
            level().playSound(null, owner.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.0F, 1.0F);
            owner.teleportTo(pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D);
            owner.resetFallDistance();
            discard();
            return;
        }
        super.onHitBlock(result);
    }
}
