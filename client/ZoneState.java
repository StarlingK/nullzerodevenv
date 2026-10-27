package nullmod.client;

import java.util.ArrayList;
import java.util.List;

import nullmod.core.AbilityManager;
import nullmod.entity.ChronosEchoEntity;
import nullmod.net.ZonesPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ZoneState {
    public record Zone(int owner, double x, double y, double z, int kind, boolean clash) {}

    private static List<Zone> zones = List.of();
    private static float range = 12f;
    private static Object lastLevel;

    private ZoneState() {}

    public static List<Zone> zones() {
        return zones;
    }

    public static float range() {
        return range;
    }

    public static void apply(ZonesPayload payload) {
        List<Zone> list = new ArrayList<>(payload.zones().size());
        for (ZonesPayload.Entry e : payload.zones()) {
            list.add(new Zone(e.owner(), e.x(), e.y(), e.z(), e.kind(), e.clash()));
        }
        zones = list;
        range = payload.range();
    }

    public static void clear() {
        zones = List.of();
        lastLevel = null;
    }

    public static void tick(Minecraft mc) {
        if (mc.level != lastLevel) {
            zones = List.of();
            lastLevel = mc.level;
        }
    }

    public static boolean shouldSkip(Entity entity) {
        if (zones.isEmpty() || entity instanceof Player || entity instanceof ChronosEchoEntity) {
            return false;
        }
        for (Zone z : zones) {
            if (z.owner() == entity.getId()) return false;
        }
        if (entity instanceof net.minecraft.world.entity.monster.Pillager) {
            double reach = (double) range * range;
            for (Zone z : zones) {
                if (z.kind() == AbilityManager.K_BASE
                        && entity.level().getEntity(z.owner()) instanceof net.minecraft.world.entity.monster.Pillager
                        && entity.distanceToSqr(z.x(), z.y(), z.z()) <= reach) {
                    return false;
                }
            }
        }
        long gameTime = entity.level().getGameTime();
        double r2 = (double) range * range;
        int factor = 1;
        for (Zone z : zones) {
            double dx = entity.getX() - z.x();
            double dy = entity.getY() - z.y();
            double dz = entity.getZ() - z.z();
            if (dx * dx + dy * dy + dz * dz > r2) continue;
            if (z.kind() == AbilityManager.K_STOP) {
                return true;
            }
            factor = Math.max(factor, AbilityManager.slowFactor(z.kind()));
        }
        if (entity instanceof net.minecraft.world.entity.LivingEntity) {
            return false;
        }
        return factor > 1 && (gameTime + entity.getId()) % factor != 0;
    }
}
