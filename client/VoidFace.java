package dev.nullmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
// this way of doing it sucks, remove?
public final class VoidFace {
    private VoidFace() {}

    public static void draw(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        float age = entity.tickCount + partialTick;
        float scale = Mth.clamp(entity.getBbWidth() / 0.6f, 0.4f, 1.6f);
        float pulse = 1.0f + 0.06f * Mth.sin(age * 0.7f);
        float half = 0.26f * scale * pulse;
        float depth = 0.265f * scale;
        float jx = Mth.sin(age * 5.1f) * 0.012f * scale;
        float jy = Mth.cos(age * 4.3f) * 0.012f * scale;

        poseStack.pushPose();
        poseStack.translate(0.0, entity.getEyeHeight(), 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-headYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        VertexConsumer vc = buffer.getBuffer(RenderType.endPortal());
        Matrix4f m = poseStack.last().pose();
        float x0 = -half + jx, x1 = half + jx, y0 = -half + jy, y1 = half + jy;
        vc.addVertex(m, x0, y0, depth);
        vc.addVertex(m, x1, y0, depth);
        vc.addVertex(m, x1, y1, depth);
        vc.addVertex(m, x0, y1, depth);
        vc.addVertex(m, x0, y1, depth);
        vc.addVertex(m, x1, y1, depth);
        vc.addVertex(m, x1, y0, depth);
        vc.addVertex(m, x0, y0, depth);
        poseStack.popPose();
    }
}
