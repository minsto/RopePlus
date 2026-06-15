package com.mickdev.ropeplus.client.render;

import com.mickdev.ropeplus.RopePlus;
import com.mickdev.ropeplus.entity.GrapplingHookEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the thrown grappling hook as a camera-facing sprite with a sagging
 * line back to its owner. Modeled on the vanilla FishingHookRenderer.
 */
public class GrapplingHookRenderer extends EntityRenderer<GrapplingHookEntity> {

    private static final ResourceLocation TEXTURE = RopePlus.rl("textures/entity/grappling_hook.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE);

    public GrapplingHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GrapplingHookEntity hook, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // billboard sprite (no extra Y flip: entityCutout culls back faces)
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vc = buffer.getBuffer(RENDER_TYPE);
        spriteVertex(vc, pose, packedLight, 0.0F, 0, 0, 1);
        spriteVertex(vc, pose, packedLight, 1.0F, 0, 1, 1);
        spriteVertex(vc, pose, packedLight, 1.0F, 1, 1, 0);
        spriteVertex(vc, pose, packedLight, 0.0F, 1, 0, 0);
        poseStack.popPose();

        // line back to the owner's hand
        Player owner = hook.getOwnerPlayer();
        if (owner != null) {
            Vec3 handPos = getPlayerHandPos(owner, partialTick);
            Vec3 hookPos = hook.getPosition(partialTick).add(0.0D, 0.25D, 0.0D);
            float dx = (float) (handPos.x - hookPos.x);
            float dy = (float) (handPos.y - hookPos.y);
            float dz = (float) (handPos.z - hookPos.z);

            VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lineStrip());
            PoseStack.Pose linePose = poseStack.last();
            for (int i = 0; i <= 16; i++) {
                stringVertex(dx, dy, dz, lineConsumer, linePose, i / 16.0F, (i + 1) / 16.0F);
            }
        }

        super.render(hook, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private Vec3 getPlayerHandPos(Player player, float partialTick) {
        int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;

        if (entityRenderDispatcher.options.getCameraType().isFirstPerson() && player == Minecraft.getInstance().player) {
            double fovScale = 960.0D / entityRenderDispatcher.options.fov().get().intValue();
            Vec3 offset = entityRenderDispatcher.camera.getNearPlane()
                    .getPointOnPlane(side * 0.525F, -0.1F)
                    .scale(fovScale);
            return player.getEyePosition(partialTick).add(offset);
        }

        float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(bodyYaw);
        double cos = Mth.cos(bodyYaw);
        float scale = player.getScale();
        double sideOffset = side * 0.35D * scale;
        double frontOffset = 0.8D * scale;
        float crouch = player.isCrouching() ? -0.1875F : 0.0F;
        return player.getEyePosition(partialTick)
                .add(-cos * sideOffset - sin * frontOffset, crouch - 0.45D * scale, -sin * sideOffset + cos * frontOffset);
    }

    private static void spriteVertex(VertexConsumer vc, PoseStack.Pose pose, int light, float x, int y, int u, int v) {
        vc.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void stringVertex(float x, float y, float z, VertexConsumer vc, PoseStack.Pose pose, float frac0, float frac1) {
        float px = x * frac0;
        float py = y * (frac0 * frac0 + frac0) * 0.5F + 0.25F;
        float pz = z * frac0;
        float dx = x * frac1 - px;
        float dy = y * (frac1 * frac1 + frac1) * 0.5F + 0.25F - py;
        float dz = z * frac1 - pz;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len <= 0) {
            return;
        }
        dx /= len;
        dy /= len;
        dz /= len;
        vc.addVertex(pose, px, py, pz)
                .setColor(0, 0, 0, 255)
                .setNormal(pose, dx, dy, dz);
    }

    @Override
    public ResourceLocation getTextureLocation(GrapplingHookEntity entity) {
        return TEXTURE;
    }
}
