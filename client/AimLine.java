package nullmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class AimLine {
    private static final double STEP = 0.25;

    private AimLine() {}

    private static float width(double f) {
        return (float) (0.25 + 0.75 * Math.pow(Math.sin(Math.PI * Mth.clamp(f, 0.0, 1.0)), 0.5));
    }

    public static void render(Minecraft mc, PoseStack ps, Vec3 cam, Vec3 start, Vec3 end, Vec3 feet, float time) {
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        if (length < 0.05) return;
        Vec3 n = dir.scale(1.0 / length);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Matrix4f m = ps.last().pose();

        VertexConsumer dark = buffers.getBuffer(GhostTypes.BEAM);
        strip(dark, m, cam, start, n, length, time, 0.15f, 0.02f, 0.0f, 0.03f, 0.85f, false);
        buffers.endBatch(GhostTypes.BEAM);

        VertexConsumer glow = buffers.getBuffer(GhostTypes.GLOW);
        strip(glow, m, cam, start, n, length, time, 0.22f, 0.85f, 0.03f, 0.08f, 0.35f, true);
        strip(glow, m, cam, start, n, length, time, 0.08f, 1.0f, 0.2f, 0.15f, 0.75f, true);
        strip(glow, m, cam, start, n, length, time, 0.022f, 1.0f, 0.95f, 0.9f, 1.0f, false);
        ring(glow, m, feet, time);
        buffers.endBatch(GhostTypes.GLOW);
    }

    private static void strip(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 start, Vec3 n, double length,
                              float time, float half, float r, float g, float b, float alpha, boolean pulse) {
        for (double t = 0; t < length; t += STEP) {
            double t2 = Math.min(length, t + STEP);
            Vec3 a = start.add(n.scale(t));
            Vec3 c = start.add(n.scale(t2));
            Vec3 mid = a.add(c).scale(0.5);
            Vec3 side = n.cross(cam.subtract(mid));
            side = side.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : side.normalize();
            float wa = half * width(t / length);
            float wc = half * width(t2 / length);
            float fa = alphaAt(t, time, alpha, pulse);
            float fc = alphaAt(t2, time, alpha, pulse);
            Vec3 sa = side.scale(wa);
            Vec3 sc = side.scale(wc);
            RiftShards.colored(vc, m, a.subtract(sa), r, g, b, fa);
            RiftShards.colored(vc, m, a.add(sa), r, g, b, fa);
            RiftShards.colored(vc, m, c.add(sc), r, g, b, fc);
            RiftShards.colored(vc, m, c.subtract(sc), r, g, b, fc);
        }
    }

    private static float alphaAt(double t, float time, float alpha, boolean pulse) {
        float fade = Mth.clamp((float) (t / 1.5), 0f, 1f);
        float p = pulse ? 0.65f + 0.35f * Mth.sin(time * 14f - (float) t * 2.5f) : 1f;
        return alpha * fade * p;
    }

    private static void ring(VertexConsumer vc, Matrix4f m, Vec3 feet, float time) {
        int dashes = 24;
        for (int i = 0; i < dashes; i++) {
            if ((i & 1) == 1) continue;
            float a0 = time * 1.6f + i * Mth.TWO_PI / dashes;
            float a1 = a0 + Mth.TWO_PI / dashes * 0.8f;
            double r0 = 0.5, r1 = 0.62;
            Vec3 p0 = feet.add(Mth.cos(a0) * r0, 0.06, Mth.sin(a0) * r0);
            Vec3 p1 = feet.add(Mth.cos(a0) * r1, 0.06, Mth.sin(a0) * r1);
            Vec3 p2 = feet.add(Mth.cos(a1) * r1, 0.06, Mth.sin(a1) * r1);
            Vec3 p3 = feet.add(Mth.cos(a1) * r0, 0.06, Mth.sin(a1) * r0);
            RiftShards.colored(vc, m, p0, 1.0f, 0.15f, 0.15f, 0.9f);
            RiftShards.colored(vc, m, p1, 1.0f, 0.15f, 0.15f, 0.9f);
            RiftShards.colored(vc, m, p2, 1.0f, 0.15f, 0.15f, 0.9f);
            RiftShards.colored(vc, m, p3, 1.0f, 0.15f, 0.15f, 0.9f);
        }
    }
}
