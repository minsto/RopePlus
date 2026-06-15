package com.mickdev.ropeplus.network;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.netty.buffer.ByteBuf;

/**
 * Bidirectional. Server to client: start riding the zipline with the given rope entity.
 * Client to server: progress update along the rope (teleports the server-side player).
 */
public record ZiplinePayload(int ropeId, float relativeDistance) implements CustomPacketPayload {

    public static final Type<ZiplinePayload> TYPE = new Type<>(RopePlus.rl("zipline"));

    public static final StreamCodec<ByteBuf, ZiplinePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ZiplinePayload::ropeId,
            ByteBufCodecs.FLOAT, ZiplinePayload::relativeDistance,
            ZiplinePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
