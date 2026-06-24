package com.mickdev.ropeplus.network;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.netty.buffer.ByteBuf;

/**
 * Bidirectional. Server to client: start reeling the player in.
 * Client to server: the pull finished, kill the rope entity with the given id.
 */
public record HookshotPullPayload(int ropeId) implements CustomPacketPayload {

    public static final Type<HookshotPullPayload> TYPE = new Type<>(RopePlus.rl("hookshot_pull"));

    public static final StreamCodec<ByteBuf, HookshotPullPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, HookshotPullPayload::ropeId,
            HookshotPullPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
