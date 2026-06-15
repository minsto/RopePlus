package com.mickdev.ropeplus;

import com.mickdev.ropeplus.client.render.Arrow303Renderer;
import com.mickdev.ropeplus.client.render.FreeFormRopeRenderer;
import com.mickdev.ropeplus.client.render.GrapplingHookRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = RopePlus.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = RopePlus.MODID, value = Dist.CLIENT)
public class RopePlusClient {

    public RopePlusClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RopePlus.FREEFORM_ROPE.get(), FreeFormRopeRenderer::new);
        event.registerEntityRenderer(RopePlus.GRAPPLING_HOOK_ENTITY.get(), GrapplingHookRenderer::new);

        event.registerEntityRenderer(RopePlus.DIRT_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.EXPLODING_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.FIRE_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.SEED_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.FROST_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.PENETRATING_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.SLIME_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.TORCH_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.WARP_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.CONFUSING_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.ROPE_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.REDSTONE_TORCH_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.EGG_ARROW_ENTITY.get(), Arrow303Renderer::new);
        event.registerEntityRenderer(RopePlus.MOB_ARROW_ENTITY.get(), Arrow303Renderer::new);
    }
}
