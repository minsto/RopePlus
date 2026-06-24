package com.mickdev.ropeplus.network;

import com.mickdev.ropeplus.RopePlus;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.netty.buffer.ByteBuf;

/**
 * Client to server: play a rope sound at the player so everyone hears it.
 */
public record SoundPayload(int soundId) implements CustomPacketPayload {

    public static final int ROPE_TENSION = 0;
    public static final int JUNGLE_KING = 1;

    public static final Type<SoundPayload> TYPE = new Type<>(RopePlus.rl("sound"));

    public static final StreamCodec<ByteBuf, SoundPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SoundPayload::soundId,
            SoundPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
