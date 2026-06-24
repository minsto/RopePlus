package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Spawns a slime where it lands. Why? Because the original did.
 */
public class SlimeArrowEntity extends Arrow303Entity {

    public SlimeArrowEntity(EntityType<? extends SlimeArrowEntity> type, Level level) {
        super(type, level);
    }

    public SlimeArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.SLIME_ARROW_ENTITY.get(), owner, level, ammo, weapon);
    }

    public SlimeArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.SLIME_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.SLIME_ARROW.get());
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.ITEM_SLIME;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide && spawnSlime(position())) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            spawnSlime(result.getEntity().position());
        }
    }

    private boolean spawnSlime(Vec3 pos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Slime slime = EntityType.SLIME.create(serverLevel);
        if (slime == null) {
            return false;
        }
        slime.moveTo(pos.x, pos.y, pos.z, getYRot(), getXRot());
        slime.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        if (serverLevel.addFreshEntity(slime)) {
            serverLevel.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.5D, pos.z, 10, 0.3D, 0.3D, 0.3D, 0.02D);
            return true;
        }
        return false;
    }
}
