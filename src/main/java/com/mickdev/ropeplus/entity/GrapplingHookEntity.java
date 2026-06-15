package com.mickdev.ropeplus.entity;

import com.mickdev.ropeplus.Config;
import com.mickdev.ropeplus.RopePlus;
import com.mickdev.ropeplus.common.RopeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Thrown grappling hook. Lands on top of a ledge, plants a hook block there and
 * unrolls a wall rope down the side. Can also harpoon entities and reel them in.
 */
public class GrapplingHookEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID = SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.INT);

    @Nullable
    private Player owner;
    @Nullable
    private Entity plantedHook;
    private boolean inGround;
    private BlockPos tilePos = BlockPos.ZERO;
    private BlockState inState;
    private int ticksInGround;
    private int ticksInAir;
    private double startPosX;
    private double startPosZ;

    public GrapplingHookEntity(EntityType<? extends GrapplingHookEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public GrapplingHookEntity(Level level, Player player) {
        this(RopePlus.GRAPPLING_HOOK_ENTITY.get(), level);
        owner = player;
        entityData.set(DATA_OWNER_ID, player.getId());
        moveTo(player.getX(), player.getEyeY() - 0.1D, player.getZ(), player.getYRot(), player.getXRot());
        float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = player.getXRot() * Mth.DEG_TO_RAD;
        setPos(getX() - Mth.cos(yawRad) * 0.16F, getY(), getZ() - Mth.sin(yawRad) * 0.16F);
        Vec3 motion = new Vec3(
                -Mth.sin(yawRad) * Mth.cos(pitchRad),
                -Mth.sin(pitchRad),
                Mth.cos(yawRad) * Mth.cos(pitchRad)).normalize().scale(1.5D);
        setDeltaMovement(motion.add(
                random.nextGaussian() * 0.0075D,
                random.nextGaussian() * 0.0075D,
                random.nextGaussian() * 0.0075D));
        startPosX = player.getX();
        startPosZ = player.getZ();
        updateRotationFromMotion();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER_ID, -1);
    }

    @Override
    public void recreateFromPacket(net.minecraft.network.protocol.game.ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        // base Entity ignores the packet velocity; without this the client-side
        // hook stays frozen at the spawn point instead of flying
        lerpMotion(packet.getXa(), packet.getYa(), packet.getZa());
    }

    @Nullable
    public Player getOwnerPlayer() {
        if (owner != null) {
            return owner;
        }
        int id = entityData.get(DATA_OWNER_ID);
        if (id >= 0 && level().getEntity(id) instanceof Player player) {
            return player;
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            if (owner == null || !owner.isAlive()
                    || !owner.getMainHandItem().is(RopePlus.GRAPPLING_HOOK.get())
                    || distanceToSqr(owner) > 1024D) {
                discard();
                return;
            }
            if (plantedHook != null) {
                if (!plantedHook.isAlive()) {
                    plantedHook = null;
                } else {
                    setPos(plantedHook.getX(),
                            plantedHook.getBoundingBox().minY + plantedHook.getBbHeight() * 0.8D,
                            plantedHook.getZ());
                    return;
                }
            }
        }

        if (inGround) {
            if (!level().getBlockState(tilePos).equals(inState)) {
                inGround = false;
                setDeltaMovement(getDeltaMovement().multiply(
                        random.nextFloat() * 0.2F, random.nextFloat() * 0.2F, random.nextFloat() * 0.2F));
                ticksInGround = 0;
                ticksInAir = 0;
            } else {
                ticksInGround++;
                if (ticksInGround == 1200) {
                    discard();
                }
                return;
            }
        } else {
            ticksInAir++;
        }

        Vec3 pos = position();
        Vec3 next = pos.add(getDeltaMovement());
        HitResult hitResult = level().clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hitResult.getType() != HitResult.Type.MISS) {
            next = hitResult.getLocation();
        }

        // entity collision: only takes priority over the block hit if it is closer
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, pos, next,
                getBoundingBox().expandTowards(getDeltaMovement()).inflate(1.0D),
                e -> e.isPickable() && (e != owner || ticksInAir >= 5));
        if (entityHit != null
                && (hitResult.getType() == HitResult.Type.MISS
                        || pos.distanceToSqr(entityHit.getLocation()) < pos.distanceToSqr(hitResult.getLocation()))) {
            hitResult = entityHit;
        }

        if (!level().isClientSide && hitResult.getType() != HitResult.Type.MISS) {
            if (hitResult instanceof EntityHitResult ehr) {
                if (owner != null && ehr.getEntity().hurt(damageSources().playerAttack(owner), 0)) {
                    plantedHook = ehr.getEntity();
                }
            } else if (hitResult instanceof BlockHitResult bhr) {
                onHitBlock(bhr);
                if (isRemoved() || inGround) {
                    return;
                }
            }
        }

        if (inGround) {
            return;
        }

        // move to the clipped position so the hook never flies through walls (notably client-side)
        setPos(next.x, next.y, next.z);
        updateRotationFromMotion();

        float friction = (onGround() || horizontalCollision) ? 0.5F : 0.92F;
        setDeltaMovement(getDeltaMovement().scale(friction).add(0, -0.04D, 0));
    }

    private void onHitBlock(BlockHitResult bhr) {
        BlockPos hitPos = bhr.getBlockPos();
        BlockState hitState = level().getBlockState(hitPos);
        double hookLyingHeight = bhr.getLocation().y - hitPos.getY();
        boolean snowSituation = hitState.is(net.minecraft.world.level.block.Blocks.SNOW);

        // direct hit on top of a ledge
        boolean canPlant = (hookLyingHeight >= 1.0D && level().getBlockState(hitPos.above()).isAir()) || snowSituation;

        // hit against a wall: climb the column to the lip of the cliff, however tall it is
        if (!canPlant && bhr.getDirection().getAxis().isHorizontal()) {
            BlockPos lip = hitPos;
            while (lip.getY() + 1 < level().getMaxBuildHeight() && !level().getBlockState(lip.above()).isAir()) {
                lip = lip.above();
            }
            if (level().getBlockState(lip.above()).isAir()) {
                hitPos = lip;
                canPlant = true;
            }
        }

        if (canPlant && hitPos.getY() + 1 < level().getMaxBuildHeight()) {

            double orientationX = getDeltaMovement().x;
            double orientationZ = getDeltaMovement().z;
            if (orientationX == 0.0D || orientationZ == 0.0D) {
                orientationX = getX() - startPosX;
                orientationZ = getZ() - startPosZ;
            }
            int xOffset = orientationX <= 0.0D ? -1 : 1;
            int zOffset = orientationZ <= 0.0D ? -1 : 1;

            if (snowSituation) {
                // lower hook and rope by one, into the snow layer
                hitPos = hitPos.below();
            }

            boolean canPlaceAtXOffset = isFreeForRope(hitPos.offset(-xOffset, 0, 0));
            boolean canPlaceAtZOffset = isFreeForRope(hitPos.offset(0, 0, -zOffset));

            BlockPos ropePos = hitPos;
            Direction ropeFacing = null;
            if (canPlaceAtXOffset && (!canPlaceAtZOffset || Math.abs(orientationX) > Math.abs(orientationZ))) {
                ropePos = hitPos.offset(-xOffset, 0, 0);
                ropeFacing = xOffset > 0 ? Direction.EAST : Direction.WEST;
            } else if (canPlaceAtZOffset) {
                ropePos = hitPos.offset(0, 0, -zOffset);
                ropeFacing = zOffset > 0 ? Direction.SOUTH : Direction.NORTH;
            }

            if (ropeFacing != null) {
                level().setBlock(hitPos.above(), RopePlus.GRAPPLING_HOOK_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
                BlockState ropeState = RopePlus.WALL_ROPE.get().defaultBlockState()
                        .setValue(com.mickdev.ropeplus.block.WallRopeBlock.FACING, ropeFacing);
                level().setBlock(ropePos, ropeState, Block.UPDATE_ALL);
                if (level() instanceof ServerLevel serverLevel) {
                    RopeManager.queueGrowth(serverLevel, ropePos, ropeState, Config.ARROW_ROPE_LENGTH.get());
                }

                if (owner != null) {
                    owner.getMainHandItem().shrink(1);
                    if (!level().isClientSide) {
                        RopeManager.setPlayerHook(owner, null);
                    }
                }
                discard();
                return;
            }
        }

        // stick in the block, right at the impact point (slightly backed off)
        tilePos = bhr.getBlockPos();
        inState = level().getBlockState(tilePos);
        Vec3 hitLocation = bhr.getLocation();
        Vec3 toHit = hitLocation.subtract(position());
        if (toHit.length() > 0) {
            Vec3 backOff = toHit.normalize().scale(0.05D);
            setPos(hitLocation.x - backOff.x, hitLocation.y - backOff.y, hitLocation.z - backOff.z);
        } else {
            setPos(hitLocation.x, hitLocation.y, hitLocation.z);
        }
        setDeltaMovement(Vec3.ZERO);
        inGround = true;
    }

    private boolean isFreeForRope(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        BlockState above = level().getBlockState(pos.above());
        return (state.isAir() || state.is(net.minecraft.world.level.block.Blocks.SNOW)) && above.isAir();
    }

    private void updateRotationFromMotion() {
        Vec3 motion = getDeltaMovement();
        double flat = motion.horizontalDistance();
        setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(motion.y, flat) * Mth.RAD_TO_DEG));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    /**
     * Recalls the hook; if an entity is harpooned, yanks it towards the player.
     */
    public void recallHook(Player player) {
        if (owner == null) {
            owner = player;
        }
        if (plantedHook != null) {
            double dx = owner.getX() - getX();
            double dy = owner.getY() - getY();
            double dz = owner.getZ() - getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double pull = 0.1D;
            plantedHook.setDeltaMovement(plantedHook.getDeltaMovement().add(
                    dx * pull,
                    dy * pull + Math.sqrt(dist) * 0.08D,
                    dz * pull));
        }
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        // whatever the cause of death, untrack the hook and update the owner's client state
        if (!isRemoved() && !level().isClientSide && owner != null) {
            RopeManager.setPlayerHook(owner, null);
            if (owner instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                        new com.mickdev.ropeplus.network.GrapplingHookPayload(false));
            }
        }
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("inGround", inGround);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        inGround = tag.getBoolean("inGround");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
