package dev.nullmod.net;

import dev.nullmod.NullMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncPayload(float blood, int tier, int dependency, int affinity,
                          int cleanseLeft, int bulletCd, int holdCd, int dragonCd,
                          int stage, int flags, int dragonMax) implements CustomPacketPayload {

    public static final int FLAG_BULLET = 1;
    public static final int FLAG_GHOST = 2;
    public static final int FLAG_STOP = 4;
    public static final int FLAG_GLITCH = 8;
    public static final int FLAG_FORCED = 16;
    public static final int FLAG_REWIND = 32;
    public static final int FLAG_CLASH_GLITCH = 64;
    public static final int FLAG_IMPACT = 128;

    public static final CustomPacketPayload.Type<SyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "sync"));

    public static final StreamCodec<ByteBuf, SyncPayload> CODEC = StreamCodec.of(SyncPayload::write, SyncPayload::read);

    private static void write(ByteBuf buf, SyncPayload p) {
        buf.writeFloat(p.blood);
        buf.writeByte(p.tier);
        buf.writeByte(p.dependency);
        buf.writeShort(p.affinity);
        buf.writeInt(p.cleanseLeft);
        buf.writeInt(p.bulletCd);
        buf.writeInt(p.holdCd);
        buf.writeInt(p.dragonCd);
        buf.writeByte(p.stage);
        buf.writeShort(p.flags);
        buf.writeByte(p.dragonMax);
    }

    private static SyncPayload read(ByteBuf buf) {
        return new SyncPayload(buf.readFloat(), buf.readByte(), buf.readByte(), buf.readShort(),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readByte(), buf.readShort(), buf.readByte());
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
