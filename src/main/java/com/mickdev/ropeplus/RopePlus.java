package com.mickdev.ropeplus;

import com.mickdev.ropeplus.block.GrapplingHookBlock;
import com.mickdev.ropeplus.block.RopeBlock;
import com.mickdev.ropeplus.block.WallRopeBlock;
import com.mickdev.ropeplus.block.ZiplineAnchorBlock;
import com.mickdev.ropeplus.block.ZiplineAnchorBlockEntity;
import com.mickdev.ropeplus.entity.FreeFormRopeEntity;
import com.mickdev.ropeplus.entity.GrapplingHookEntity;
import com.mickdev.ropeplus.entity.arrow.Arrow303Entity;
import com.mickdev.ropeplus.entity.arrow.ConfusionArrowEntity;
import com.mickdev.ropeplus.entity.arrow.DirtArrowEntity;
import com.mickdev.ropeplus.entity.arrow.EggArrowEntity;
import com.mickdev.ropeplus.entity.arrow.ExplodingArrowEntity;
import com.mickdev.ropeplus.entity.arrow.MobArrowEntity;
import com.mickdev.ropeplus.entity.arrow.FireArrowEntity;
import com.mickdev.ropeplus.entity.arrow.FrostArrowEntity;
import com.mickdev.ropeplus.entity.arrow.PenetratingArrowEntity;
import com.mickdev.ropeplus.entity.arrow.RedstoneTorchArrowEntity;
import com.mickdev.ropeplus.entity.arrow.RopeArrowEntity;
import com.mickdev.ropeplus.entity.arrow.SeedArrowEntity;
import com.mickdev.ropeplus.entity.arrow.SlimeArrowEntity;
import com.mickdev.ropeplus.entity.arrow.TorchArrowEntity;
import com.mickdev.ropeplus.entity.arrow.WarpArrowEntity;
import com.mickdev.ropeplus.item.Arrow303Item;
import com.mickdev.ropeplus.item.GrapplingHookItem;
import com.mickdev.ropeplus.item.HookshotCartridgeItem;
import com.mickdev.ropeplus.item.HookshotItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Ropes+ port to NeoForge 1.21.1.
 * Original 1.12.2 mod by AtomicStryker, porte by mickdev.
 */
@Mod(RopePlus.MODID)
public class RopePlus {
    public static final String MODID = "ropeplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    // ================= Blocks =================

    public static final DeferredBlock<RopeBlock> ROPE = BLOCKS.register("rope",
            () -> new RopeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.3F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<WallRopeBlock> WALL_ROPE = BLOCKS.register("wall_rope",
            () -> new WallRopeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.5F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .noLootTable()
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<GrapplingHookBlock> GRAPPLING_HOOK_BLOCK = BLOCKS.register("grappling_hook_block",
            () -> new GrapplingHookBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<ZiplineAnchorBlock> ZIPLINE_ANCHOR = BLOCKS.register("zipline_anchor",
            () -> new ZiplineAnchorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.3F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    // ================= Items =================

    public static final DeferredItem<BlockItem> ROPE_ITEM = ITEMS.registerSimpleBlockItem("rope", ROPE);
    public static final DeferredItem<BlockItem> ZIPLINE_ANCHOR_ITEM = ITEMS.registerSimpleBlockItem("zipline_anchor", ZIPLINE_ANCHOR);

    public static final DeferredItem<GrapplingHookItem> GRAPPLING_HOOK = ITEMS.register("grappling_hook",
            () -> new GrapplingHookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<HookshotItem> HOOKSHOT = ITEMS.register("hookshot",
            () -> new HookshotItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<HookshotCartridgeItem> HOOKSHOT_CARTRIDGE = ITEMS.register("hookshot_cartridge",
            () -> new HookshotCartridgeItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Arrow303Item> DIRT_ARROW = ITEMS.register("dirt_arrow",
            () -> new Arrow303Item(arrowProps(), DirtArrowEntity::new, DirtArrowEntity::new));
    public static final DeferredItem<Arrow303Item> EXPLODING_ARROW = ITEMS.register("exploding_arrow",
            () -> new Arrow303Item(arrowProps(), ExplodingArrowEntity::new, ExplodingArrowEntity::new));
    public static final DeferredItem<Arrow303Item> FIRE_ARROW = ITEMS.register("fire_arrow",
            () -> new Arrow303Item(arrowProps(), FireArrowEntity::new, FireArrowEntity::new));
    public static final DeferredItem<Arrow303Item> SEED_ARROW = ITEMS.register("seed_arrow",
            () -> new Arrow303Item(arrowProps(), SeedArrowEntity::new, SeedArrowEntity::new));
    public static final DeferredItem<Arrow303Item> FROST_ARROW = ITEMS.register("frost_arrow",
            () -> new Arrow303Item(arrowProps(), FrostArrowEntity::new, FrostArrowEntity::new));
    public static final DeferredItem<Arrow303Item> PENETRATING_ARROW = ITEMS.register("penetrating_arrow",
            () -> new Arrow303Item(arrowProps(), PenetratingArrowEntity::new, PenetratingArrowEntity::new));
    public static final DeferredItem<Arrow303Item> SLIME_ARROW = ITEMS.register("slime_arrow",
            () -> new Arrow303Item(arrowProps(), SlimeArrowEntity::new, SlimeArrowEntity::new));
    public static final DeferredItem<Arrow303Item> TORCH_ARROW = ITEMS.register("torch_arrow",
            () -> new Arrow303Item(arrowProps(), TorchArrowEntity::new, TorchArrowEntity::new));
    public static final DeferredItem<Arrow303Item> WARP_ARROW = ITEMS.register("warp_arrow",
            () -> new Arrow303Item(arrowProps(), WarpArrowEntity::new, WarpArrowEntity::new));
    public static final DeferredItem<Arrow303Item> CONFUSING_ARROW = ITEMS.register("confusing_arrow",
            () -> new Arrow303Item(arrowProps(), ConfusionArrowEntity::new, ConfusionArrowEntity::new));
    public static final DeferredItem<Arrow303Item> ROPE_ARROW = ITEMS.register("rope_arrow",
            () -> new Arrow303Item(arrowProps(), RopeArrowEntity::new, RopeArrowEntity::new));
    public static final DeferredItem<Arrow303Item> REDSTONE_TORCH_ARROW = ITEMS.register("redstone_torch_arrow",
            () -> new Arrow303Item(arrowProps(), RedstoneTorchArrowEntity::new, RedstoneTorchArrowEntity::new));
    public static final DeferredItem<Arrow303Item> EGG_ARROW = ITEMS.register("egg_arrow",
            () -> new Arrow303Item(arrowProps(), EggArrowEntity::new, EggArrowEntity::new));
    public static final DeferredItem<Arrow303Item> MOB_ARROW = ITEMS.register("mob_arrow",
            () -> new Arrow303Item(arrowProps(), MobArrowEntity::new, MobArrowEntity::new));

    private static Item.Properties arrowProps() {
        return new Item.Properties().rarity(Rarity.RARE);
    }

    // ================= Entities =================

    public static final DeferredHolder<EntityType<?>, EntityType<FreeFormRopeEntity>> FREEFORM_ROPE = ENTITY_TYPES.register("freeform_rope",
            () -> EntityType.Builder.<FreeFormRopeEntity>of(FreeFormRopeEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(10)
                    .updateInterval(5)
                    .build("freeform_rope"));

    public static final DeferredHolder<EntityType<?>, EntityType<GrapplingHookEntity>> GRAPPLING_HOOK_ENTITY = ENTITY_TYPES.register("grappling_hook",
            () -> EntityType.Builder.<GrapplingHookEntity>of(GrapplingHookEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(5)
                    .build("grappling_hook"));

    public static final DeferredHolder<EntityType<?>, EntityType<DirtArrowEntity>> DIRT_ARROW_ENTITY = arrowType("dirt_arrow", DirtArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<ExplodingArrowEntity>> EXPLODING_ARROW_ENTITY = arrowType("exploding_arrow", ExplodingArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<FireArrowEntity>> FIRE_ARROW_ENTITY = arrowType("fire_arrow", FireArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SeedArrowEntity>> SEED_ARROW_ENTITY = arrowType("seed_arrow", SeedArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<FrostArrowEntity>> FROST_ARROW_ENTITY = arrowType("frost_arrow", FrostArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<PenetratingArrowEntity>> PENETRATING_ARROW_ENTITY = arrowType("penetrating_arrow", PenetratingArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SlimeArrowEntity>> SLIME_ARROW_ENTITY = arrowType("slime_arrow", SlimeArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<TorchArrowEntity>> TORCH_ARROW_ENTITY = arrowType("torch_arrow", TorchArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<WarpArrowEntity>> WARP_ARROW_ENTITY = arrowType("warp_arrow", WarpArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<ConfusionArrowEntity>> CONFUSING_ARROW_ENTITY = arrowType("confusing_arrow", ConfusionArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<RopeArrowEntity>> ROPE_ARROW_ENTITY = arrowType("rope_arrow", RopeArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<RedstoneTorchArrowEntity>> REDSTONE_TORCH_ARROW_ENTITY = arrowType("redstone_torch_arrow", RedstoneTorchArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<EggArrowEntity>> EGG_ARROW_ENTITY = arrowType("egg_arrow", EggArrowEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<MobArrowEntity>> MOB_ARROW_ENTITY = arrowType("mob_arrow", MobArrowEntity::new);

    private static <T extends Arrow303Entity> DeferredHolder<EntityType<?>, EntityType<T>> arrowType(String name, EntityType.EntityFactory<T> factory) {
        return ENTITY_TYPES.register(name,
                () -> EntityType.Builder.of(factory, MobCategory.MISC)
                        .sized(0.5F, 0.5F)
                        .clientTrackingRange(4)
                        .updateInterval(20)
                        .build(name));
    }

    // ================= Block entities =================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZiplineAnchorBlockEntity>> ZIPLINE_ANCHOR_BE = BLOCK_ENTITY_TYPES.register("zipline_anchor",
            () -> BlockEntityType.Builder.of(ZiplineAnchorBlockEntity::new, ZIPLINE_ANCHOR.get()).build(null));

    // ================= Sounds =================

    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_HOOKSHOT_FIRE = sound("hookshot_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_HOOKSHOT_PULL = sound("hookshot_pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_JUNGLE_KING = sound("jungle_king");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ROPE_TENSION = sound("rope_tension");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ZIPLINE = sound("zipline");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(rl(name)));
    }

    // ================= Creative tab =================

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ROPEPLUS_TAB = CREATIVE_MODE_TABS.register("ropeplus_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ropeplus"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> HOOKSHOT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ROPE_ITEM.get());
                        output.accept(ZIPLINE_ANCHOR_ITEM.get());
                        output.accept(GRAPPLING_HOOK.get());
                        output.accept(HOOKSHOT.get());
                        output.accept(HOOKSHOT_CARTRIDGE.get());
                        output.accept(DIRT_ARROW.get());
                        output.accept(EXPLODING_ARROW.get());
                        output.accept(FIRE_ARROW.get());
                        output.accept(SEED_ARROW.get());
                        output.accept(FROST_ARROW.get());
                        output.accept(PENETRATING_ARROW.get());
                        output.accept(SLIME_ARROW.get());
                        output.accept(TORCH_ARROW.get());
                        output.accept(WARP_ARROW.get());
                        output.accept(CONFUSING_ARROW.get());
                        output.accept(ROPE_ARROW.get());
                        output.accept(REDSTONE_TORCH_ARROW.get());
                        output.accept(EGG_ARROW.get());
                        output.accept(MOB_ARROW.get());
                    }).build());

    public RopePlus(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DispenserBlock.registerProjectileBehavior(DIRT_ARROW.get());
            DispenserBlock.registerProjectileBehavior(EXPLODING_ARROW.get());
            DispenserBlock.registerProjectileBehavior(FIRE_ARROW.get());
            DispenserBlock.registerProjectileBehavior(SEED_ARROW.get());
            DispenserBlock.registerProjectileBehavior(FROST_ARROW.get());
            DispenserBlock.registerProjectileBehavior(PENETRATING_ARROW.get());
            DispenserBlock.registerProjectileBehavior(SLIME_ARROW.get());
            DispenserBlock.registerProjectileBehavior(TORCH_ARROW.get());
            DispenserBlock.registerProjectileBehavior(WARP_ARROW.get());
            DispenserBlock.registerProjectileBehavior(CONFUSING_ARROW.get());
            DispenserBlock.registerProjectileBehavior(ROPE_ARROW.get());
            DispenserBlock.registerProjectileBehavior(REDSTONE_TORCH_ARROW.get());
            DispenserBlock.registerProjectileBehavior(EGG_ARROW.get());
            DispenserBlock.registerProjectileBehavior(MOB_ARROW.get());
        });
    }
}
