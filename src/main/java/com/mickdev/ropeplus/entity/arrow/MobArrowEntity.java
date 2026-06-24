package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Spawns a random mob where it lands. Recreation of the Mob Arrow
 * from the original Elemental Arrows mod. Use at your own risk.
 */
public class MobArrowEntity extends Arrow303Entity {

    private static final List<EntityType<? extends Mob>> MOB_POOL = List.of(
            EntityType.PIG,
            EntityType.SHEEP,
            EntityType.COW,
            EntityType.CHICKEN,
            EntityType.WOLF,
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER,
            EntityType.SLIME);

    public MobArrowEntity(EntityType<? extends MobArrowEntity> type, Level level) {
        super(type, level);
    }

    public MobArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.MOB_ARROW_ENTITY.get(), owner, level, ammo, weapon);
    }

    public MobArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.MOB_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.MOB_ARROW.get());
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.WITCH;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide && spawnRandomMob(position())) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            spawnRandomMob(result.getEntity().position());
        }
    }

    private boolean spawnRandomMob(Vec3 pos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        RandomSource rand = serverLevel.getRandom();
        EntityType<? extends Mob> type = MOB_POOL.get(rand.nextInt(MOB_POOL.size()));
        Mob mob = type.create(serverLevel);
        if (mob == null) {
            return false;
        }
        mob.moveTo(pos.x, pos.y, pos.z, rand.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        if (serverLevel.addFreshEntity(mob)) {
            serverLevel.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 0.5D, pos.z, 15, 0.3D, 0.5D, 0.3D, 0.02D);
            return true;
        }
        return false;
    }
}
