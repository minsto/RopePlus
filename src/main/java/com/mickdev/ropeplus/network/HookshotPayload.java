package com.mickdev.ropeplus.network;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.netty.buffer.ByteBuf;

/**
 * Server to client: a hookshot rope was attached (ropeId &gt;= 0, with the hit
 * block position) or released (ropeId &lt; 0).
 */
public record HookshotPayload(int ropeId, BlockPos pos) implements CustomPacketPayload {

    public static final Type<HookshotPayload> TYPE = new Type<>(RopePlus.rl("hookshot"));

    public static final StreamCodec<ByteBuf, HookshotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, HookshotPayload::ropeId,
            BlockPos.STREAM_CODEC, HookshotPayload::pos,
            HookshotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
