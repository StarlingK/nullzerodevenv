package dev.nullmod.core;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class AimMath {
    private AimMath() {}

    public static Vec3 travel(Player player, float range) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 far = eye.add(look.scale(range));
        BlockHitResult hit = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));

        double eyeHeight = player.getEyeHeight();
        Vec3 feet;
        if (hit.getType() == HitResult.Type.MISS) {
            feet = far.subtract(0, eyeHeight, 0);
        } else if (hit.getDirection() == Direction.UP) {
            feet = hit.getLocation();
        } else {
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            feet = hit.getLocation().add(normal.scale(0.35)).subtract(0, eyeHeight, 0);
        }

        Vec3 origin = player.position();
        AABB box = player.getBoundingBox();
        Vec3 delta = feet.subtract(origin);
        for (double lift = 0.0; lift <= 1.0 + 1.0e-4; lift += 0.25) {
            Vec3 candidate = delta.add(0, lift, 0);
            if (!level.getBlockCollisions(player, box.move(candidate)).iterator().hasNext()) {
                return candidate;
            }
        }
        double length = delta.length();
        if (length < 1.0e-3) {
            return Vec3.ZERO;
        }
        Vec3 dir = delta.scale(1.0 / length);
        for (double t = length - 0.25; t > 0.0; t -= 0.25) {
            Vec3 candidate = dir.scale(t);
            if (!level.getBlockCollisions(player, box.move(candidate)).iterator().hasNext()) {
                return candidate;
            }
        }
        return Vec3.ZERO;
    }
}
