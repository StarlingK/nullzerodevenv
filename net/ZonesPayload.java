package nullmod.net;

import java.util.ArrayList;
import java.util.List;

import nullmod.NullMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ZonesPayload(float range, List<Entry> zones) implements CustomPacketPayload {

    public record Entry(int owner, float x, float y, float z, int kind, boolean clash) {}

    public static final CustomPacketPayload.Type<ZonesPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "zones"));

    public static final StreamCodec<ByteBuf, ZonesPayload> CODEC = StreamCodec.of(ZonesPayload::write, ZonesPayload::read);

    private static void write(ByteBuf buf, ZonesPayload p) {
        buf.writeFloat(p.range);
        buf.writeByte(p.zones.size());
        for (Entry e : p.zones) {
            buf.writeInt(e.owner);
            buf.writeFloat(e.x);
            buf.writeFloat(e.y);
            buf.writeFloat(e.z);
            buf.writeByte(e.kind);
            buf.writeBoolean(e.clash);
        }
    }

    private static ZonesPayload read(ByteBuf buf) {
        float range = buf.readFloat();
        int count = buf.readUnsignedByte();
        List<Entry> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new Entry(buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readByte(), buf.readBoolean()));
        }
        return new ZonesPayload(range, list);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
