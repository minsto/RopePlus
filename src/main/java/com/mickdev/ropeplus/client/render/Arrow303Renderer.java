package com.mickdev.ropeplus.client.render;

import com.mickdev.ropeplus.entity.arrow.Arrow303Entity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class Arrow303Renderer extends ArrowRenderer<Arrow303Entity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

    public Arrow303Renderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Arrow303Entity entity) {
        return TEXTURE;
    }
}
