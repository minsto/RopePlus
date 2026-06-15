package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.RopePlus;
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
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Detonates roughly three seconds after being fired. Sticks in entities.
 */
public class ExplodingArrowEntity extends Arrow303Entity {

    private boolean charged;
    private int ticksCharged;
    @Nullable
    private Entity stuckInEntity;

    public ExplodingArrowEntity(EntityType<? extends ExplodingArrowEntity> type, Level level) {
        super(type, level);
    }

    public ExplodingArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.EXPLODING_ARROW_ENTITY.get(), owner, level, ammo, weapon);
    }

    public ExplodingArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.EXPLODING_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.EXPLODING_ARROW.get());
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SMOKE;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!charged) {
            charged = true;
            playFuseSound();
            stuckInEntity = result.getEntity();
            result.getEntity().hurt(damageSources().arrow(this, getOwner()), computeCustomDamage());
        }
        // intentionally no super call: the arrow stays alive until it detonates
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!charged) {
            charged = true;
            playFuseSound();
        }
    }

    private void playFuseSound() {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.TNT_PRIMED, SoundSource.NEUTRAL,
                1.0F, 1.2F / (random.nextFloat() * 0.2F + 0.9F));
    }

    @Override
    public void tick() {
        super.tick();

        if (stuckInEntity != null) {
            if (!stuckInEntity.isAlive()) {
                stuckInEntity = null;
            } else {
                setPos(stuckInEntity.getX(), stuckInEntity.getY(), stuckInEntity.getZ());
            }
        }

        if (ticksCharged++ > 60) {
            if (!level().isClientSide) {
                Entity source = getOwner() != null ? getOwner() : this;
                level().explode(source, getX(), getY(), getZ(), 2.0F, true, Level.ExplosionInteraction.TNT);
            }
            discard();
        }
    }
}
