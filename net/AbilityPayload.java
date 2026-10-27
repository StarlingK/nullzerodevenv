package nullmod.net;

import nullmod.NullMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AbilityPayload(int action, float range) implements CustomPacketPayload {

    public static final int TAP = 0;
    public static final int GHOST_START = 1;
    public static final int GHOST_END = 2;
    public static final int DRAGON_START = 3;

    public static final int DRAGON_FIRE = 4;
    public static final int DRAGON_CANCEL = 5;

    public static final CustomPacketPayload.Type<AbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "ability"));

    public static final StreamCodec<ByteBuf, AbilityPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.action);
                buf.writeFloat(p.range);
            },
            buf -> new AbilityPayload(buf.readByte(), buf.readFloat()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
