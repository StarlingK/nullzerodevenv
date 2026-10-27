package nullmod.core;

import net.minecraft.world.entity.Entity;

public final class TimeScale {
    private TimeScale() {}

    public static int factor(Entity entity) {
        return entity.level().isClientSide ? 1 : AbilityManager.factorFor(entity);
    }

    public static boolean sitOut(Entity entity, int factor) {
        return factor > 1 && (entity.level().getGameTime() + Math.abs(entity.getId())) % factor != 0;
    }
}
