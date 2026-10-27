package nullmod.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class ClashRift {
    private static final int SHARDS = 18;
    private static final int CAPTAIN_SHARDS = 5;
    private static final int TEXT_LIFE = 80;
    private static final Map<Integer, Integer> STARTS = new HashMap<>();
    private static final Map<Integer, Integer> TEXT_AGE = new HashMap<>();
    private static int ticks;

    private ClashRift() {}

    public static void tick() {
        ticks++;
        TEXT_AGE.replaceAll((id, age) -> age + 1);
        TEXT_AGE.values().removeIf(age -> age > TEXT_LIFE);
    }

    public static void say(int entityId) {
        TEXT_AGE.put(entityId, 0);
    }

    public static void clear() {
        STARTS.clear();
        TEXT_AGE.clear();
    }

    private static float beatAt(float pt) {
        float seconds = ThemeAudio.confessSeconds(pt);
        if (seconds >= 0f) return ClashBeat.beat(seconds);
        float phase = ((ticks + pt) / 20f * 93.75f / 60f) % 1f;
        return (float) Math.exp(-phase * 6.0);
    }

    private static boolean isCaptain(Minecraft mc, ZoneState.Zone z) {
        Entity e = mc.level == null ? null : mc.level.getEntity(z.owner());
        return e != null && !(e instanceof net.minecraft.world.entity.player.Player);
    }

    private static List<Object[]> centers() {
        Minecraft mc = Minecraft.getInstance();
        List<ZoneState.Zone> clashing = new ArrayList<>();
        for (ZoneState.Zone z : ZoneState.zones()) {
            if (z.clash()) clashing.add(z);
        }
        List<Object[]> out = new ArrayList<>();
        boolean[] used = new boolean[clashing.size()];
        double reach = ZoneState.range() * 2.0;
        for (int i = 0; i < clashing.size(); i++) {
            if (used[i]) continue;
            ZoneState.Zone a = clashing.get(i);
            int best = -1;
            double bestD = reach * reach;
            for (int j = i + 1; j < clashing.size(); j++) {
                if (used[j]) continue;
                ZoneState.Zone b = clashing.get(j);
                double d = (a.x() - b.x()) * (a.x() - b.x()) + (a.y() - b.y()) * (a.y() - b.y())
                        + (a.z() - b.z()) * (a.z() - b.z());
                if (d <= bestD) {
                    bestD = d;
                    best = j;
                }
            }
            used[i] = true;
            if (best >= 0) {
                used[best] = true;
                ZoneState.Zone b = clashing.get(best);
                out.add(new Object[] {Math.min(a.owner(), b.owner()),
                        new Vec3((a.x() + b.x()) / 2, (a.y() + b.y()) / 2, (a.z() + b.z()) / 2),
                        isCaptain(mc, a) || isCaptain(mc, b)});
            } else {
                out.add(new Object[] {a.owner(), new Vec3(a.x(), a.y(), a.z()), isCaptain(mc, a)});
            }
        }
        return out;
    }

    public static void render(Minecraft mc, PoseStack ps, Vec3 cam, float pt) {
        List<Object[]> centers = centers();
        if (centers.isEmpty() || mc.level == null) {
            STARTS.clear();
            return;
        }
        Set<Integer> keys = new HashSet<>();
        for (Object[] c : centers) keys.add((Integer) c[0]);
        STARTS.keySet().retainAll(keys);

        float beat = beatAt(pt);
        float time = (ticks + pt) / 20f;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();

        List<Vec3[]> shards = new ArrayList<>();
        List<Vec3[]> faint = new ArrayList<>();
        List<Vec3[]> all = new ArrayList<>();
        List<Boolean> faintFlags = new ArrayList<>();
        List<Vec3> normals = new ArrayList<>();
        for (Object[] c : centers) {
            int key = (Integer) c[0];
            Vec3 origin = (Vec3) c[1];
            boolean captain = (Boolean) c[2];
            float spread = captain ? 1.6f : 1.0f;
            float shrink = captain ? 0.5f : 1.0f;
            int start = STARTS.computeIfAbsent(key, k -> ticks);
            float ramp = Mth.clamp((ticks - start + pt) / 8f, 0f, 1f);
            Random rnd = new Random(key * 1327L);
            int count = captain ? CAPTAIN_SHARDS : SHARDS;
            for (int i = 0; i < count; i++) {
                float dist = (1.2f + rnd.nextFloat() * 3.8f) * spread;
                float ang = rnd.nextFloat() * Mth.TWO_PI + time * (0.1f + rnd.nextFloat() * 0.2f);
                float h = (captain ? 3.2f + rnd.nextFloat() * 2.0f : 0.1f + rnd.nextFloat() * 3.2f) + Mth.sin(time * 2f + i) * (captain ? 0.05f : 0.15f);
                float size = (0.5f + rnd.nextFloat() * 1.1f) * ramp * (captain ? 1f : 1f + 0.45f * beat) * shrink;
                float spin = time * (captain ? 0.15f + rnd.nextFloat() * 0.1f : 0.4f + rnd.nextFloat()) + i;
                float skew = 0.7f + rnd.nextFloat() * 0.6f;
                Vec3 normal = new Vec3(rnd.nextFloat() - 0.5, rnd.nextFloat() - 0.5, rnd.nextFloat() - 0.5);
                normal = normal.lengthSqr() < 1.0e-4 ? new Vec3(0, 1, 0) : normal.normalize();
                if (!captain && (i * 7 + ticks / 4) % 9 == 0 && beat < 0.3f) continue;
                Vec3 center = origin.add(Mth.cos(ang) * dist, h, Mth.sin(ang) * dist);
                Vec3[] shard = RiftShards.shard(center, normal, spin, size, skew);
                (captain ? faint : shards).add(shard);
                all.add(shard);
                faintFlags.add(captain);
                normals.add(normal);
            }
        }

        VertexConsumer portal = buffers.getBuffer(RenderType.endPortal());
        for (Vec3[] s : shards) RiftShards.portal(portal, m, s);
        buffers.endBatch(RenderType.endPortal());

        if (!faint.isEmpty()) {
            VertexConsumer fill = buffers.getBuffer(GhostTypes.BEAM);
            for (Vec3[] s : faint) RiftShards.fill(fill, m, s, 0.2f, 0.1f, 0.4f, 0.42f);
            buffers.endBatch(GhostTypes.BEAM);
        }

        VertexConsumer glow = buffers.getBuffer(GhostTypes.GLOW);
        float alpha = 0.35f + 0.65f * beat;
        for (int i = 0; i < all.size(); i++) {
            boolean f = faintFlags.get(i);
            RiftShards.edges(glow, m, all.get(i), normals.get(i), f ? 0.015f : 0.03f, 0.9f, 0.8f, 1.0f,
                    f ? 0.7f : alpha);
        }
        buffers.endBatch(GhostTypes.GLOW);
        ps.popPose();
    }

    public static void captainText(LivingEntity entity, PoseStack ps, MultiBufferSource buffer, float pt, int light) {
        Integer age = TEXT_AGE.get(entity.getId());
        if (age == null || age > TEXT_LIFE) return;
        Minecraft mc = Minecraft.getInstance();
        int left = TEXT_LIFE - age;
        int alpha = Mth.clamp((int) (255f * left / 15f), 0, 255);
        if (alpha < 6) return;
        Font font = mc.font;
        Component text = Component.translatable("speech.nullmod.captain")
                .withStyle(s -> s.withFont(SpeechOverlay.FONT));
        float w = font.width(text);
        ps.pushPose();
        ps.translate(0.0, entity.getBbHeight() + 0.8, 0.0);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        ps.scale(-0.025f, -0.025f, 0.025f);
        float shakeX = (mc.level.random.nextFloat() - 0.5f) * 1.5f;
        float shakeY = (mc.level.random.nextFloat() - 0.5f) * 1.5f;
        Matrix4f m = ps.last().pose();
        font.drawInBatch(text, -w / 2f + shakeX + 1, shakeY + 1, (alpha << 24) | 0x00E5FF, false, m, buffer,
                Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        ps.translate(0, 0, -0.03);
        m = ps.last().pose();
        font.drawInBatch(text, -w / 2f + shakeX, shakeY, (alpha << 24) | 0xFF2BD6, false, m, buffer,
                Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        ps.popPose();
    }
}
