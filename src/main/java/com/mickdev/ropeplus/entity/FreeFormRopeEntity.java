package com.mickdev.ropeplus.entity;

import com.mickdev.ropeplus.RopePlus;
import com.mickdev.ropeplus.client.ClientRopeState;
import com.mickdev.ropeplus.common.RopeManager;
import com.mickdev.ropeplus.network.HookshotPullPayload;
import com.mickdev.ropeplus.network.SoundPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A free-form rope strung between two points. Used by the hookshot
 * (attached to the shooting player) and by ziplines (anchor to target block).
 */
public class FreeFormRopeEntity extends Entity {

    /** how long a piece of rope must be for a render segment */
    private static final double SEGMENT_LENGTH = 0.5D;

    private static final EntityDataAccessor<Vector3f> DATA_START = SynchedEntityData.defineId(FreeFormRopeEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> DATA_END = SynchedEntityData.defineId(FreeFormRopeEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> DATA_POW = SynchedEntityData.defineId(FreeFormRopeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MAX_RANGE_CAP = SynchedEntityData.defineId(FreeFormRopeEntity.class, EntityDataSerializers.FLOAT);

    private boolean hangsTaut = true;
    @Nullable
    private Player shooter;
    private double maxLength = 999D;
    private double inertiaSpeed = -1D;
    private long nextSoundTime;
    private boolean jungleCall;
    private Vec3 swingStartPoint = Vec3.ZERO;
    private Vec3 anchorLoc = Vec3.ZERO;

    public FreeFormRopeEntity(EntityType<? extends FreeFormRopeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_START, new Vector3f());
        builder.define(DATA_END, new Vector3f());
        builder.define(DATA_POW, 1.0F);
        builder.define(DATA_MAX_RANGE_CAP, com.mickdev.ropeplus.Config.MAX_HOOKSHOT_ROPE_LENGTH.get().floatValue());
    }

    public void setShooter(@Nullable Player player) {
        shooter = player;
        if (player != null) {
            maxLength = distanceTo(player);
            if (!level().isClientSide) {
                float cap = (float) com.mickdev.ropeplus.item.HookshotItem.baseMaxRange()
                        + RopeManager.takePendingRangeBonus(player);
                entityData.set(DATA_MAX_RANGE_CAP, Math.min(
                        (float) com.mickdev.ropeplus.item.HookshotItem.BOOSTED_RANGE_CAP, cap));
            }
        }
    }

    public float getMaxRangeCap() {
        return entityData.get(DATA_MAX_RANGE_CAP);
    }

    /** Sneak-extend: +10 blocks, capped at 200. Returns false if already at cap. */
    public boolean tryExtendMaxRange() {
        if (!level().isClientSide) {
            float cap = getMaxRangeCap();
            if (cap >= com.mickdev.ropeplus.item.HookshotItem.BOOSTED_RANGE_CAP) {
                return false;
            }
            float newCap = Math.min((float) com.mickdev.ropeplus.item.HookshotItem.BOOSTED_RANGE_CAP,
                    cap + (float) com.mickdev.ropeplus.item.HookshotItem.RANGE_PER_EXTEND);
            entityData.set(DATA_MAX_RANGE_CAP, newCap);
            maxLength = Math.max(maxLength, newCap);
            return true;
        }
        return false;
    }

    @Nullable
    public Player getShooter() {
        return shooter;
    }

    public void setLoosening() {
        hangsTaut = false;
    }

    public double getStartX() {
        return entityData.get(DATA_START).x();
    }

    public double getStartY() {
        return entityData.get(DATA_START).y();
    }

    public double getStartZ() {
        return entityData.get(DATA_START).z();
    }

    public double getEndX() {
        return entityData.get(DATA_END).x();
    }

    public double getEndY() {
        return entityData.get(DATA_END).y();
    }

    public double getEndZ() {
        return entityData.get(DATA_END).z();
    }

    public double getPowValue() {
        return entityData.get(DATA_POW);
    }

    private void setPowValue(double value) {
        if (!level().isClientSide) {
            entityData.set(DATA_POW, (float) value);
        }
    }

    public void setStartCoordinates(double x, double y, double z) {
        if (!level().isClientSide) {
            entityData.set(DATA_START, new Vector3f((float) x, (float) y, (float) z));
        }
        updateEntPos();
    }

    public void setEndCoordinates(double x, double y, double z) {
        if (!level().isClientSide) {
            entityData.set(DATA_END, new Vector3f((float) x, (float) y, (float) z));
        }
        updateEntPos();
    }

    /**
     * Attaches the rope end to the BOTTOM of the target block.
     */
    public void setEndBlock(BlockPos pos) {
        setEndCoordinates(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.getBoolean("attachedToEnt")) {
            discard();
        } else {
            entityData.set(DATA_START, new Vector3f(tag.getFloat("startX"), tag.getFloat("startY"), tag.getFloat("startZ")));
            entityData.set(DATA_END, new Vector3f(tag.getFloat("endX"), tag.getFloat("endY"), tag.getFloat("endZ")));
            entityData.set(DATA_POW, tag.getFloat("ropePOWvalue"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("attachedToEnt", shooter != null);
        tag.putFloat("startX", (float) getStartX());
        tag.putFloat("startY", (float) getStartY());
        tag.putFloat("startZ", (float) getStartZ());
        tag.putFloat("endX", (float) getEndX());
        tag.putFloat("endY", (float) getEndY());
        tag.putFloat("endZ", (float) getEndZ());
        tag.putFloat("ropePOWvalue", (float) getPowValue());
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!isRemoved() && shooter != null) {
            if (level().isClientSide) {
                ClientRopeState.hasRopeOut = false;
                ClientRopeState.shouldDisconnect = true;
                ClientRopeState.ropeChangeState = 0f;
            } else {
                RopeManager.setPlayerRope(shooter, null);
            }
        }
        super.remove(reason);
    }

    @Override
    public void tick() {
        super.tick();

        final BlockPos endPos = new BlockPos(
                Mth.floor(getEndX()),
                Mth.floor(getEndY()) + (shooter == null ? -1 : 0),
                Mth.floor(getEndZ()));
        boolean endValid = level().getBlockState(endPos).isCollisionShapeFullBlock(level(), endPos);
        if (!endValid && shooter == null) {
            // ziplines may end at another zipline anchor instead of a full block
            BlockPos endBlock = BlockPos.containing(getEndX(), getEndY(), getEndZ());
            endValid = level().getBlockState(endBlock).is(RopePlus.ZIPLINE_ANCHOR.get());
        }
        if (!endValid) {
            discard();
            return;
        }

        if (!hangsTaut && getPowValue() < 2D) {
            setPowValue(getPowValue() + 0.05D);
        }

        if (shooter != null) {
            if (!shooter.isAlive() || !shooter.getInventory().contains(RopePlus.HOOKSHOT.get().getDefaultInstance())) {
                discard();
                if (level().isClientSide) {
                    ClientRopeState.hasRopeOut = false;
                    ClientRopeState.shouldDisconnect = true;
                    ClientRopeState.ropeChangeState = 0f;
                }
                return;
            }

            setStartCoordinates(shooter.getX(), shooter.getY(), shooter.getZ());
            Vec3 playerToHookVec = new Vec3(getEndX() - shooter.getX(), getEndY() - shooter.getY(), getEndZ() - shooter.getZ());
            double dist = playerToHookVec.length();

            if (level().isClientSide) {
                tickClientSwing(playerToHookVec, dist);
            } else {
                shooter.fallDistance = 0;
            }
        } else {
            // zipline mode: start point must remain a zipline anchor
            BlockPos startPos = BlockPos.containing(getStartX(), getStartY(), getStartZ());
            if (!level().getBlockState(startPos).is(RopePlus.ZIPLINE_ANCHOR.get())) {
                discard();
                return;
            }
        }

        updateEntPos();
    }

    /**
     * Client-side physics for the local swinging player. Faithful port of the 1.12 logic.
     */
    private void tickClientSwing(Vec3 playerToHookVec, double dist) {
        if (shooter == null) {
            return;
        }
        if (ClientRopeState.shouldDisconnect) {
            // add a jump motion and let go
            shooter.playSound(SoundEvents.ITEM_PICKUP, 1f, 1f);
            shooter.setDeltaMovement(shooter.getDeltaMovement().add(0, 0.42D, 0));
            shooter = null;
            ClientRopeState.shouldDisconnect = false;
            return;
        }

        if (ClientRopeState.ropeChangeState < 0f) {
            // being pulled in
            if (dist < 3D) {
                ClientRopeState.hasRopeOut = false;
                ClientRopeState.shouldDisconnect = true;
                ClientRopeState.ropeChangeState = 0f;
                PacketDistributor.sendToServer(new HookshotPullPayload(getId()));
            } else {
                Vec3 pull = playerToHookVec.normalize();
                Vec3 motion = shooter.getDeltaMovement();
                shooter.setDeltaMovement(motion.scale(0.5D).add(pull.scale(0.5D)));
            }
            return;
        }

        maxLength = Math.min(getMaxRangeCap(), maxLength + ClientRopeState.ropeChangeState);
        ClientRopeState.ropeChangeState = 0f;
        if (inertiaSpeed < 0 && !shooter.verticalCollision && shooter.getDeltaMovement().y < -0.1D) {
            dist -= 1D;
            maxLength = dist;
        }

        if (dist < maxLength) {
            return;
        }

        if (shooter.verticalCollision) {
            // hit ground, reset
            inertiaSpeed = -1;
            return;
        }

        final double heightFromAnchor = getEndY() - shooter.getY();

        if (inertiaSpeed < 0) {
            inertiaSpeed = shooter.getDeltaMovement().length() + (maxLength - heightFromAnchor) / 10D;
            swingStartPoint = shooter.position();
            anchorLoc = new Vec3(getEndX(), getEndY(), getEndZ());
        }

        if (System.currentTimeMillis() > nextSoundTime) {
            nextSoundTime = System.currentTimeMillis() + 3000L;
            if (!jungleCall && maxLength > 25 && getEndY() - shooter.getY() < 5D) {
                jungleCall = true;
                PacketDistributor.sendToServer(new SoundPayload(SoundPayload.JUNGLE_KING));
            } else {
                PacketDistributor.sendToServer(new SoundPayload(SoundPayload.ROPE_TENSION));
            }
        }

        // shorten the rope back to max length, set swinger position accordingly
        Vec3 anchorToPlayer = new Vec3(shooter.getX() - getEndX(), shooter.getY() - getEndY(), shooter.getZ() - getEndZ())
                .normalize().scale(maxLength);
        shooter.setPos(anchorToPlayer.x + getEndX(), anchorToPlayer.y + getEndY(), anchorToPlayer.z + getEndZ());

        Vec3 playerToAnchorVec = new Vec3(getEndX() - shooter.getX(), heightFromAnchor, getEndZ() - shooter.getZ());
        Vec3 playerLoc = shooter.position();
        Vec3 rightVec = new Vec3(playerToAnchorVec.x, 0, playerToAnchorVec.z);

        double relativeEnergy = inertiaSpeed;

        boolean downSwing = distXZSq(swingStartPoint, playerLoc) < distXZSq(swingStartPoint, anchorLoc);
        rightVec = rightVec.yRot((float) Math.toRadians(downSwing ? -90 : 90));

        // below anchor, apply potential energy reduction
        if (heightFromAnchor > 0) {
            relativeEnergy *= heightFromAnchor / getRopeAbsLength();
        }

        Vec3 tangent = playerToAnchorVec.cross(rightVec).normalize();
        shooter.setDeltaMovement(tangent.scale(relativeEnergy));

        if (!downSwing && relativeEnergy < 0.15D) {
            // reset swing, start a new one
            inertiaSpeed = -1;
        }

        shooter.fallDistance = 0;
    }

    private static double distXZSq(Vec3 a, Vec3 b) {
        final double xd = a.x - b.x;
        final double zd = a.z - b.z;
        return xd * xd + zd * zd;
    }

    private void updateEntPos() {
        setPos(getEndX(), getEndY(), getEndZ());
    }

    public double getRopeAbsLength() {
        double dx = getEndX() - getStartX();
        double dy = getEndY() - getStartY();
        double dz = getEndZ() - getStartZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Determines how many segments the rope is broken into for rendering.
     */
    public int getSegmentCount() {
        return Math.max(1, (int) Math.rint(getRopeAbsLength() / SEGMENT_LENGTH));
    }

    /**
     * Computes the 3D coordinates of any point along the rope from the start and
     * end coordinates and the ease-in/out POW value (rope sag).
     *
     * @param relativeDistance position along the rope, 0 to 1
     * @return array of {x, y, z}
     */
    public double[] getCoordsAtRelativeLength(float relativeDistance) {
        double[] result = new double[3];
        result[0] = getStartX() + (getEndX() - getStartX()) * relativeDistance;
        result[2] = getStartZ() + (getEndZ() - getStartZ()) * relativeDistance;

        double pow = getPowValue();
        float d = relativeDistance * 2F;
        if (d < 1) {
            result[1] = getStartY() + (getEndY() - getStartY()) * (0.5D * Math.pow(d, pow));
        } else {
            result[1] = getStartY() + (getEndY() - getStartY()) * (1D - 0.5D * Math.abs(Math.pow(2D - d, pow)));
        }
        return result;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }
}
