package com.mickdev.ropeplus.network;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.netty.buffer.ByteBuf;

/**
 * Server to client: whether the player's grappling hook is currently out.
 */
public record GrapplingHookPayload(boolean hookOut) implements CustomPacketPayload {

    public static final Type<GrapplingHookPayload> TYPE = new Type<>(RopePlus.rl("grappling_hook"));

    public static final StreamCodec<ByteBuf, GrapplingHookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GrapplingHookPayload::hookOut,
            GrapplingHookPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
