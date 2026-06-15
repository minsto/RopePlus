package com.mickdev.ropeplus.item;

import com.mickdev.ropeplus.entity.arrow.Arrow303Entity;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Item for the Ropes+ special arrows. Extends {@link ArrowItem} so that
 * vanilla bows, crossbows and dispensers fire them natively.
 */
public class Arrow303Item extends ArrowItem {

    @FunctionalInterface
    public interface ShotFactory {
        Arrow303Entity create(Level level, LivingEntity shooter, ItemStack ammo, @Nullable ItemStack weapon);
    }

    @FunctionalInterface
    public interface PositionFactory {
        Arrow303Entity create(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon);
    }

    private final ShotFactory shotFactory;
    private final PositionFactory positionFactory;

    public Arrow303Item(Properties properties, ShotFactory shotFactory, PositionFactory positionFactory) {
        super(properties);
        this.shotFactory = shotFactory;
        this.positionFactory = positionFactory;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        return shotFactory.create(level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        Arrow303Entity arrow = positionFactory.create(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }
}
