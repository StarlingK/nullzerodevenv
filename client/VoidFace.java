package nullmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class VoidFace {
    private static final int SHARDS = 9;

    private VoidFace() {}

    public static void draw(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        float age = entity.tickCount + partialTick;
        float scale = Mth.clamp(entity.getBbWidth() / 0.6f, 0.5f, 1.6f);
        float height = entity.getBbHeight();
        long step = (long) (age * 0.5f);

        poseStack.pushPose();
        Matrix4f m = poseStack.last().pose();
        Vec3[][] shards = new Vec3[SHARDS][];
        Vec3[] normals = new Vec3[SHARDS];
        for (int i = 0; i < SHARDS; i++) {
            long seed = i * 7919L + entity.getId() * 31L;
            float a = hash(seed) * Mth.TWO_PI + age * (0.08f + 0.05f * hash(seed + 1));
            float radius = (0.45f + 0.5f * hash(seed + 2)) * scale;
            float y = height * (0.15f + 0.95f * hash(seed + 3)) + Mth.sin(age * 0.2f + i) * 0.06f;
            float flicker = hash(seed + step) > 0.18f ? 1f : 0.35f;
            float size = (0.14f + 0.22f * hash(seed + 4)) * scale * flicker;
            Vec3 center = new Vec3(Mth.cos(a) * radius, y, Mth.sin(a) * radius);
            Vec3 normal = new Vec3(Mth.cos(a), 0.25 * (hash(seed + 5) - 0.5), Mth.sin(a)).normalize();
            normals[i] = normal;
            shards[i] = RiftShards.shard(center, normal, age * 0.15f + i, size, 0.8f + 0.5f * hash(seed + 6));
        }

        VertexConsumer portal = buffer.getBuffer(RenderType.endPortal());
        for (Vec3[] shard : shards) {
            RiftShards.portal(portal, m, shard);
        }
        VertexConsumer glow = buffer.getBuffer(GhostTypes.GLOW);
        for (int i = 0; i < SHARDS; i++) {
            RiftShards.edges(glow, m, shards[i], normals[i], 0.012f * scale, 0.7f, 0.55f, 1.0f, 0.55f);
        }
        poseStack.popPose();
    }

    private static float hash(long seed) {
        long h = seed * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
