package com.mickdev.ropeplus.client;

import com.mickdev.ropeplus.RopePlus;
import com.mickdev.ropeplus.entity.FreeFormRopeEntity;
import com.mickdev.ropeplus.network.HookshotExtendPayload;
import com.mickdev.ropeplus.network.ZiplinePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = RopePlus.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            if (ClientRopeState.zipline != null) {
                ClientRopeState.reset();
            }
            return;
        }

        // holding sneak with hookshot or active rope: +10 range, costs 2 cartridges
        boolean holdingHookshot = player.getMainHandItem().is(RopePlus.HOOKSHOT.get())
                || player.getOffhandItem().is(RopePlus.HOOKSHOT.get());
        if ((ClientRopeState.hasRopeOut || holdingHookshot) && mc.options.keyShift.isDown()) {
            if (ClientRopeState.extendCooldown > 0) {
                ClientRopeState.extendCooldown--;
            } else {
                ClientRopeState.extendCooldown = 10;
                PacketDistributor.sendToServer(new HookshotExtendPayload());
            }
        } else {
            ClientRopeState.extendCooldown = 0;
        }

        // zipline riding
        FreeFormRopeEntity zipline = ClientRopeState.zipline;
        if (zipline != null) {
            if (!zipline.isAlive()) {
                ClientRopeState.zipline = null;
                return;
            }
            if (mc.options.keyUse.isDown() && ClientRopeState.ziplineProgress > 0.2f) {
                PacketDistributor.sendToServer(new ZiplinePayload(zipline.getId(), ClientRopeState.ziplineProgress));
                ClientRopeState.zipline = null;
            } else {
                double[] coords = zipline.getCoordsAtRelativeLength(ClientRopeState.ziplineProgress);
                // hang below the line: feet ~2.2 under the rope puts the hands right on it
                player.setPos(coords[0], coords[1] - 2.2D, coords[2]);
                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 0f;
                ClientRopeState.ziplineProgress += 0.015f;

                if (++ClientRopeState.ziplineTicker == 10) {
                    ClientRopeState.ziplineTicker = 0;
                    PacketDistributor.sendToServer(new ZiplinePayload(zipline.getId(), ClientRopeState.ziplineProgress));
                }

                // stop roughly one block before the end of the line instead of 10% short
                float endThreshold = (float) Math.max(0.9D, 1.0D - 1.0D / Math.max(1.0D, zipline.getRopeAbsLength()));
                if (ClientRopeState.ziplineProgress > endThreshold) {
                    PacketDistributor.sendToServer(new ZiplinePayload(zipline.getId(), ClientRopeState.ziplineProgress));
                    ClientRopeState.zipline = null;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (ClientRopeState.wasZiplining && event.getEntity() == Minecraft.getInstance().player) {
            ClientRopeState.wasZiplining = false;
            event.setDistance(0f);
        }
    }
}
