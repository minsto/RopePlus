package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class FireArrowEntity extends Arrow303Entity {

    public FireArrowEntity(EntityType<? extends FireArrowEntity> type, Level level) {
        super(type, level);
    }

    public FireArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.FIRE_ARROW_ENTITY.get(), owner, level, ammo, weapon);
    }

    public FireArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.FIRE_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.FIRE_ARROW.get());
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        target.igniteForSeconds(15);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide && tryToPlaceBlock(BaseFireBlock.getState(level(), result.getBlockPos().relative(result.getDirection())))) {
            discard();
        }
    }
}
