package nullmod.core;

import net.minecraft.network.chat.Component;

public enum ChronosTier {
    NONE(0, "none", 0, 0xFFFFFF),
    BASE(1, "base", 1, 0x808080),
    ZERO(2, "zero", 2, 0x000000),
    DRAGON(3, "dragon", 3, 0xFFD700),
    HEADHUNTER(4, "headhunter", 2, 0xAA0000);

    public final int id;
    public final String key;
    public final int rank;
    public final int color;

    ChronosTier(int id, String key, int rank, int color) {
        this.id = id;
        this.key = key;
        this.rank = rank;
        this.color = color;
    }

    public static ChronosTier byId(int id) {
        for (ChronosTier tier : values()) {
            if (tier.id == id) return tier;
        }
        return NONE;
    }

    public static int rankOf(int id) {
        return byId(id).rank;
    }

    public Component displayName() {
        return Component.translatable("tier.nullmod." + key);
    }

    public Component typeLabel() {
        return Component.translatable("gui.nullmod.type." + key);
    }
}
