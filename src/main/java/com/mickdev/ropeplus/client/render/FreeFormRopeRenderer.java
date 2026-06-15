package com.mickdev.ropeplus.client.render;

import com.mickdev.ropeplus.RopePlus;
import com.mickdev.ropeplus.entity.FreeFormRopeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a {@link FreeFormRopeEntity} as a chain of camera-facing crossed quads,
 * following the sag curve of the rope. Port of the 1.12 renderer.
 */
public class FreeFormRopeRenderer extends EntityRenderer<FreeFormRopeEntity> {

    private static final ResourceLocation TEXTURE = RopePlus.rl("textures/entity/rope_segment.png");
    private static final double THICKNESS = 0.05D;

    public FreeFormRopeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(FreeFormRopeEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(FreeFormRopeEntity rope, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(rope, entityYaw, partialTick, poseStack, buffer, packedLight);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Vec3 cam = entityRenderDispatcher.camera.getPosition();

        // the pose stack origin is the interpolated entity position
        double originX = Mth.lerp(partialTick, rope.xOld, rope.getX());
        double originY = Mth.lerp(partialTick, rope.yOld, rope.getY());
        double originZ = Mth.lerp(partialTick, rope.zOld, rope.getZ());

        PoseStack.Pose pose = poseStack.last();

        int segCount = rope.getSegmentCount();
        float jointInterval = 1F / segCount;
        double[] prev = rope.getCoordsAtRelativeLength(1F);

        for (int s = segCount - 1; s >= 0; s--) {
            double[] cur = rope.getCoordsAtRelativeLength(s * jointInterval);

            // segment vector, from current point to previous point
            double segX = prev[0] - cur[0];
            double segY = prev[1] - cur[1];
            double segZ = prev[2] - cur[2];

            // vector from camera to the current point
            double lookX = cur[0] - cam.x;
            double lookY = cur[1] - cam.y;
            double lookZ = cur[2] - cam.z;

            // width vector orthogonal to both: makes the quad face the camera
            Vec3 width = new Vec3(
                    lookY * segZ - lookZ * segY,
                    lookZ * segX - lookX * segZ,
                    lookX * segY - lookY * segX);
            if (width.lengthSqr() < 1.0E-7D) {
                prev = cur;
                continue;
            }
            width = width.normalize().scale(THICKNESS);

            // second width vector, perpendicular to the first (crossed quads)
            Vec3 seg = new Vec3(segX, segY, segZ);
            Vec3 width2 = width.cross(seg);
            if (width2.lengthSqr() > 1.0E-7D) {
                width2 = width2.normalize().scale(THICKNESS);
            }

            // sample world light along the rope: the entity itself sits inside
            // the target block, where the packed light is always 0 (pitch black)
            int segLight = LevelRenderer.getLightColor(rope.level(),
                    BlockPos.containing(cur[0], cur[1] + 0.1D, cur[2]));

            drawQuad(vc, pose, segLight, originX, originY, originZ, cur, prev, width);
            if (width2.lengthSqr() > 1.0E-7D) {
                drawQuad(vc, pose, segLight, originX, originY, originZ, cur, prev, width2);
            }

            prev = cur;
        }
    }

    private static void drawQuad(VertexConsumer vc, PoseStack.Pose pose, int light,
                                 double originX, double originY, double originZ,
                                 double[] cur, double[] prev, Vec3 width) {
        // like the 1.12 renderer: U runs along the segment, V only samples the
        // opaque 0..0.25 strip of rope_segment.png (the rest is transparent)
        vertex(vc, pose, light, cur[0] - width.x - originX, cur[1] - width.y - originY, cur[2] - width.z - originZ, 0f, 0.25f);
        vertex(vc, pose, light, prev[0] - width.x - originX, prev[1] - width.y - originY, prev[2] - width.z - originZ, 1f, 0.25f);
        vertex(vc, pose, light, prev[0] + width.x - originX, prev[1] + width.y - originY, prev[2] + width.z - originZ, 1f, 0f);
        vertex(vc, pose, light, cur[0] + width.x - originX, cur[1] + width.y - originY, cur[2] + width.z - originZ, 0f, 0f);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, int light, double x, double y, double z, float u, float v) {
        vc.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
    }

    @Override
    public ResourceLocation getTextureLocation(FreeFormRopeEntity entity) {
        return TEXTURE;
    }
}
