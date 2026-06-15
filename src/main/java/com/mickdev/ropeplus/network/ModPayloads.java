package com.mickdev.ropeplus.network;

import com.mickdev.ropeplus.RopePlus;
import com.mickdev.ropeplus.client.ClientRopeState;
import com.mickdev.ropeplus.common.RopeManager;
import com.mickdev.ropeplus.entity.FreeFormRopeEntity;
import com.mickdev.ropeplus.item.HookshotItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = RopePlus.MODID)
public final class ModPayloads {

    private ModPayloads() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(HookshotPayload.TYPE, HookshotPayload.STREAM_CODEC, ModPayloads::handleHookshotOnClient);
        registrar.playToClient(GrapplingHookPayload.TYPE, GrapplingHookPayload.STREAM_CODEC,
                (payload, context) -> ClientRopeState.grapplingHookOut = payload.hookOut());
        registrar.playToServer(SoundPayload.TYPE, SoundPayload.STREAM_CODEC, ModPayloads::handleSoundOnServer);
        registrar.playToServer(HookshotExtendPayload.TYPE, HookshotExtendPayload.STREAM_CODEC, ModPayloads::handleHookshotExtend);
        registrar.playBidirectional(HookshotPullPayload.TYPE, HookshotPullPayload.STREAM_CODEC, ModPayloads::handleHookshotPull);
        registrar.playBidirectional(ZiplinePayload.TYPE, ZiplinePayload.STREAM_CODEC, ModPayloads::handleZipline);
    }

    private static void handleHookshotOnClient(HookshotPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (payload.ropeId() < 0) {
            ClientRopeState.hasRopeOut = false;
            ClientRopeState.shouldDisconnect = true;
            ClientRopeState.ropeChangeState = 0f;
            // detach any client rope attached to the local player
            for (FreeFormRopeEntity rope : player.level().getEntitiesOfClass(FreeFormRopeEntity.class,
                    player.getBoundingBox().inflate(256))) {
                if (rope.getShooter() == player) {
                    rope.discard();
                    break;
                }
            }
        } else {
            ClientRopeState.hasRopeOut = true;
            ClientRopeState.shouldDisconnect = false;
            ClientRopeState.ropeChangeState = 0f;
            if (player.level().getEntity(payload.ropeId()) instanceof FreeFormRopeEntity rope) {
                rope.setShooter(player);
            }
            BlockPos pos = payload.pos();
            player.level().addParticle(ParticleTypes.EXPLOSION,
                    pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 1.0D, 0.0D, 0.0D);
        }
    }

    private static void handleHookshotExtend(HookshotExtendPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        FreeFormRopeEntity rope = RopeManager.getPlayerRope(player);
        if (rope != null && rope.isAlive()) {
            if (rope.getMaxRangeCap() >= HookshotItem.BOOSTED_RANGE_CAP) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL,
                        SoundSource.PLAYERS, 0.5F, 1.0F);
                return;
            }
            if (!HookshotItem.consumeTwoCartridges(player)) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL,
                        SoundSource.PLAYERS, 0.5F, 1.0F);
                return;
            }
            rope.tryExtendMaxRange();
            player.level().playSound(null, player.blockPosition(), RopePlus.SOUND_HOOKSHOT_PULL.get(),
                    SoundSource.PLAYERS, 0.6F, 1.2F);
            return;
        }
        boolean holdingHookshot = player.getMainHandItem().is(RopePlus.HOOKSHOT.get())
                || player.getOffhandItem().is(RopePlus.HOOKSHOT.get());
        if (holdingHookshot && RopeManager.tryAddPendingRangeBonus(player)) {
            player.level().playSound(null, player.blockPosition(), RopePlus.SOUND_HOOKSHOT_PULL.get(),
                    SoundSource.PLAYERS, 0.6F, 1.2F);
        } else if (holdingHookshot) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL,
                    SoundSource.PLAYERS, 0.5F, 1.0F);
        }
    }

    private static void handleSoundOnServer(SoundPayload payload, IPayloadContext context) {
        Player player = context.player();
        var sound = payload.soundId() == SoundPayload.JUNGLE_KING
                ? RopePlus.SOUND_JUNGLE_KING.get()
                : RopePlus.SOUND_ROPE_TENSION.get();
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1f, 1f);
    }

    private static void handleHookshotPull(HookshotPullPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            // client -> server: kill the rope entity
            Entity target = serverPlayer.level().getEntity(payload.ropeId());
            if (target instanceof FreeFormRopeEntity) {
                target.discard();
            }
        } else {
            // server -> client: start zipping in
            ClientRopeState.ropeChangeState = -1f;
        }
    }

    private static void handleZipline(ZiplinePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player instanceof ServerPlayer serverPlayer) {
            // position update while riding
            Entity target = serverPlayer.level().getEntity(payload.ropeId());
            if (target instanceof FreeFormRopeEntity rope) {
                double[] coords = rope.getCoordsAtRelativeLength(payload.relativeDistance());
                // same offset as the client-side ride, otherwise the server pushes the player around
                serverPlayer.teleportTo(coords[0], coords[1] - 2.2D, coords[2]);
                serverPlayer.fallDistance = 0;
            }
        } else {
            // start riding the zipline on the client
            if (player.level().getEntity(payload.ropeId()) instanceof FreeFormRopeEntity rope) {
                ClientRopeState.zipline = rope;
                ClientRopeState.ziplineProgress = 0f;
                ClientRopeState.ziplineTicker = 0;
                ClientRopeState.wasZiplining = true;
            }
        }
    }
}
