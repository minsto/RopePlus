package com.mickdev.ropeplus.item;

import com.mickdev.ropeplus.Config;
import com.mickdev.ropeplus.RopePlus;
import com.mickdev.ropeplus.client.ClientRopeState;
import com.mickdev.ropeplus.common.RopeManager;
import com.mickdev.ropeplus.entity.FreeFormRopeEntity;
import com.mickdev.ropeplus.network.HookshotPayload;
import com.mickdev.ropeplus.network.HookshotPullPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Hookshot. Quick click fires a rope at the targeted block (or releases it);
 * holding it charged while attached reels the player in.
 */
public class HookshotItem extends Item {

    /** absolute range cap reachable via sneak-extend (+10 per 2 cartridges) */
    public static final double BOOSTED_RANGE_CAP = 200.0D;
    /** blocks added per successful sneak-extend */
    public static final double RANGE_PER_EXTEND = 10.0D;

    public HookshotItem(Properties properties) {
        super(properties);
    }

    /** Base hookshot range from config (50 by default). Only sneak-extend can raise it. */
    public static double baseMaxRange() {
        return Config.MAX_HOOKSHOT_ROPE_LENGTH.get();
    }

    /** Current max range including any sneak-charged bonus before firing. */
    public static double currentMaxRange(Player player) {
        return Math.min(BOOSTED_RANGE_CAP, baseMaxRange() + RopeManager.getPendingRangeBonus(player));
    }

    /** Consumes exactly two Hookshot Cartridges from anywhere in the inventory. */
    public static boolean consumeTwoCartridges(Player player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        int need = 2;
        for (int i = 0; i < player.getInventory().getContainerSize() && need > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(RopePlus.HOOKSHOT_CARTRIDGE.get())) {
                int take = Math.min(need, stack.getCount());
                stack.shrink(take);
                need -= take;
            }
        }
        return need == 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int heldTicks = getUseDuration(stack, entity) - timeLeft;
        float chargeRatio = heldTicks / 20.0F;

        if (chargeRatio < 0.5F) {
            if (level.isClientSide) {
                ClientRopeState.shouldDisconnect = ClientRopeState.hasRopeOut;
                ClientRopeState.ropeChangeState = 0f;
                player.swing(InteractionHand.MAIN_HAND);
            } else {
                quickFireOrRelease(level, player);
                player.swing(InteractionHand.MAIN_HAND);
            }
        } else if (!level.isClientSide) {
            FreeFormRopeEntity rope = RopeManager.getPlayerRope(player);
            if (rope != null && rope.isAlive() && player instanceof ServerPlayer serverPlayer) {
                // activate hook pull on the client side
                PacketDistributor.sendToPlayer(serverPlayer, new HookshotPullPayload(-1));
                level.playSound(null, player.blockPosition(), RopePlus.SOUND_HOOKSHOT_PULL.get(), SoundSource.PLAYERS,
                        1.0F, 1.0F / (level.random.nextFloat() * 0.1F + 0.95F));
            }
        }
    }

    private void quickFireOrRelease(Level level, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        FreeFormRopeEntity existingRope = RopeManager.getPlayerRope(player);
        if (existingRope == null) {
            double traceDistance = currentMaxRange(player);
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(player.getViewVector(1.0F).scale(traceDistance));
            BlockHitResult target = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

            if (target.getType() == HitResult.Type.BLOCK) {
                FreeFormRopeEntity ropeEnt = new FreeFormRopeEntity(RopePlus.FREEFORM_ROPE.get(), level);
                ropeEnt.setStartCoordinates(player.getX(), player.getY() + 0.5D, player.getZ());
                ropeEnt.setEndBlock(target.getBlockPos());
                ropeEnt.setShooter(player);
                level.addFreshEntity(ropeEnt);

                RopeManager.setPlayerRope(player, ropeEnt);
                PacketDistributor.sendToPlayer(serverPlayer, new HookshotPayload(ropeEnt.getId(), target.getBlockPos()));

                level.playSound(null, player.blockPosition(), RopePlus.SOUND_HOOKSHOT_FIRE.get(), SoundSource.PLAYERS,
                        1.0F, 1.0F / (level.random.nextFloat() * 0.1F + 0.95F));
            } else {
                RopeManager.setPlayerRope(player, null);
                player.displayClientMessage(Component.translatable("message.ropeplus.hookshot_no_target"), false);
            }
        } else {
            existingRope.discard();
            RopeManager.setPlayerRope(player, null);
            PacketDistributor.sendToPlayer(serverPlayer, new HookshotPayload(-1, net.minecraft.core.BlockPos.ZERO));
            level.playSound(null, player.blockPosition(), SoundEvents.ARROW_HIT, SoundSource.PLAYERS,
                    1.0F, 1.0F / (level.random.nextFloat() * 0.1F + 0.95F));
        }
    }
}
