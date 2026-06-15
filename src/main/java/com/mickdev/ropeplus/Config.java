package com.mickdev.ropeplus;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue MAX_HOOKSHOT_ROPE_LENGTH = BUILDER
            .comment("Maximum length of the Hookshot rope, in blocks")
            .defineInRange("maxHookShotRopeLength", 50.0D, 10.0D, 256.0D);

    public static final ModConfigSpec.IntValue ARROW_ROPE_LENGTH = BUILDER
            .comment("How many rope blocks a Rope Arrow / Grappling Hook deploys downwards")
            .defineInRange("arrowRopeLength", 32, 4, 128);

    static final ModConfigSpec SPEC = BUILDER.build();
}
