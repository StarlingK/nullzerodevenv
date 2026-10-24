package dev.nullmod.net;

import dev.nullmod.NullMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SpeechPayload(int kind, int arg) implements CustomPacketPayload {
    public static final int LOW_1 = 0;
    public static final int LOW_2 = 1;
    public static final int HIGH_1 = 2;
    public static final int HIGH_2 = 3;

    public static final int CLEANSE_BLOCKED = 4;
    public static final int NO_WORK = 5;
    public static final int CAPTAIN = 6;

    public static final CustomPacketPayload.Type<SpeechPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "speech"));

    public static final StreamCodec<ByteBuf, SpeechPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.kind);
                buf.writeInt(p.arg);
            },
            buf -> new SpeechPayload(buf.readByte(), buf.readInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
