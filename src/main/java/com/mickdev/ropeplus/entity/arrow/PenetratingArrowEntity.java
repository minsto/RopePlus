package com.mickdev.ropeplus.entity.arrow;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Flies flat and fast, piercing straight through any number of entities.
 */
public class PenetratingArrowEntity extends Arrow303Entity {

    public PenetratingArrowEntity(EntityType<? extends PenetratingArrowEntity> type, Level level) {
        super(type, level);
        configure();
    }

    public PenetratingArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.PENETRATING_ARROW_ENTITY.get(), owner, level, ammo, weapon);
        configure();
    }

    public PenetratingArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(RopePlus.PENETRATING_ARROW_ENTITY.get(), x, y, z, level, ammo, weapon);
        configure();
    }

    private void configure() {
        setNoGravity(true);
    }

    @Override
    public byte getPierceLevel() {
        // setPierceLevel is private in 1.21.1, overriding the getter has the same effect
        return 64;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(RopePlus.PENETRATING_ARROW.get());
    }
}
