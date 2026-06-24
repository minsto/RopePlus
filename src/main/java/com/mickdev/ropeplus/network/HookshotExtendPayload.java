package com.mickdev.ropeplus.network;

import com.mickdev.ropeplus.RopePlus;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: while sneaking on an active hookshot rope, request +10 max range
 * in exchange for two Hookshot Cartridges (one from each hand).
 */
public record HookshotExtendPayload() implements CustomPacketPayload {

    public static final Type<HookshotExtendPayload> TYPE = new Type<>(RopePlus.rl("hookshot_extend"));

    public static final StreamCodec<ByteBuf, HookshotExtendPayload> STREAM_CODEC = StreamCodec.unit(new HookshotExtendPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
