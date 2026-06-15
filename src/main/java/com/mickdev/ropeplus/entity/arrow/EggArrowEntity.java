package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Breaks like a thrown egg on impact: 1/8 chance to hatch a chick, 1/32 to hatch four.
 * Recreation of the Egg Arrow from the original Elemental Arrows mod.
 */
public class EggArrowEntity extends Arrow303Entity {

    public EggArrowEntity(EntityType<? extends EggArrowEntity> type, Level level) {
        super(type, level);
    }

    public EggArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.EGG_ARROW_ENTITY.get(), owner, level, ammo, weapon);
    }

    public EggArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.EGG_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.EGG_ARROW.get());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) {
            crack(position());
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            crack(result.getEntity().position());
            discard();
        }
    }

    private void crack(Vec3 pos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.EGG)),
                pos.x, pos.y + 0.2D, pos.z, 8, 0.1D, 0.1D, 0.1D, 0.05D);

        // same odds as a thrown egg
        if (random.nextInt(8) == 0) {
            int count = random.nextInt(32) == 0 ? 4 : 1;
            for (int i = 0; i < count; i++) {
                Chicken chicken = EntityType.CHICKEN.create(serverLevel);
                if (chicken != null) {
                    chicken.setAge(-24000);
                    chicken.moveTo(pos.x, pos.y, pos.z, getYRot(), 0.0F);
                    chicken.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                            MobSpawnType.MOB_SUMMONED, null);
                    serverLevel.addFreshEntity(chicken);
                }
            }
        }
    }
}
