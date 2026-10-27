package nullmod.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class RiftShards {
    private RiftShards() {}

    public static Vec3[] shard(Vec3 center, Vec3 normal, float rot, float size, float skew) {
        Vec3 helper = Math.abs(normal.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u0 = normal.cross(helper).normalize();
        Vec3 v0 = normal.cross(u0);
        double cs = Math.cos(rot), sn = Math.sin(rot);
        Vec3 u = u0.scale(cs).add(v0.scale(sn));
        Vec3 v = u0.scale(-sn).add(v0.scale(cs)).scale(skew);
        return new Vec3[] {
                center.add(u.scale(size)),
                center.add(u.scale(-0.5 * size)).add(v.scale(0.866 * size)),
                center.add(u.scale(-0.5 * size)).add(v.scale(-0.866 * size))
        };
    }

    public static void portal(VertexConsumer vc, Matrix4f m, Vec3[] t) {
        vertex(vc, m, t[0]);
        vertex(vc, m, t[1]);
        vertex(vc, m, t[2]);
        vertex(vc, m, t[2]);
        vertex(vc, m, t[2]);
        vertex(vc, m, t[2]);
        vertex(vc, m, t[1]);
        vertex(vc, m, t[0]);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z);
    }

    public static void fill(VertexConsumer vc, Matrix4f m, Vec3[] t, float r, float g, float b, float a) {
        colored(vc, m, t[0], r, g, b, a);
        colored(vc, m, t[1], r, g, b, a);
        colored(vc, m, t[2], r, g, b, a);
        colored(vc, m, t[2], r, g, b, a);
    }

    public static void edges(VertexConsumer vc, Matrix4f m, Vec3[] t, Vec3 normal, float width,
                             float r, float g, float b, float a) {
        for (int i = 0; i < 3; i++) {
            Vec3 p = t[i];
            Vec3 q = t[(i + 1) % 3];
            Vec3 side = normal.cross(q.subtract(p));
            if (side.lengthSqr() < 1.0e-8) continue;
            side = side.normalize().scale(width);
            colored(vc, m, p.subtract(side), r, g, b, a);
            colored(vc, m, p.add(side), r, g, b, a);
            colored(vc, m, q.add(side), r, g, b, a);
            colored(vc, m, q.subtract(side), r, g, b, a);
        }
    }

    public static void colored(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, a);
    }
}
