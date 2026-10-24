package dev.nullmod.client;

import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.nullmod.entity.ChronosEchoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

public class EchoRenderer extends EntityRenderer<ChronosEchoEntity> {
    private final PlayerModel<AbstractClientPlayer> wide;
    private final PlayerModel<AbstractClientPlayer> slim;

 // this entire section was made at like 4 am off no sleep so please don't be too harsh ty

    public EchoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.wide = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.wide.young = false;
        this.slim.young = false;
    }

    private static PlayerSkin skinOf(ChronosEchoEntity entity) {
        UUID id = entity.getOwnerId().orElse(null);
        if (id == null) {
            return DefaultPlayerSkin.get(UUID.randomUUID());
        }
        
        PlayerInfo info = Minecraft.getInstance().getConnection() != null
                ? Minecraft.getInstance().getConnection().getPlayerInfo(id) : null;
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(id);
    }

    @Override
    public ResourceLocation getTextureLocation(ChronosEchoEntity entity) {
        return skinOf(entity).texture();
    }

    @Override
    public void render(ChronosEchoEntity entity, float yaw, float partialTick, PoseStack poseStack,
                         MultiBufferSource buffer, int light) {
        PlayerSkin skin = skinOf(entity);
        PlayerModel<AbstractClientPlayer> model = skin.model() == PlayerSkin.Model.SLIM ? slim : wide;
        boolean trail = entity.getVariant() == ChronosEchoEntity.TRAIL;

        int color;
        RenderType type;
        int packedLight = light;
        if (trail) {
            float life = 1f - (entity.tickCount + partialTick) / ChronosEchoEntity.TRAIL_LIFETIME;
            int alpha = Math.max(0, Math.min(255, (int) (170 * life)));
            boolean cyan = (entity.getId() & 1) == 0;
            color = cyan ? FastColor.ARGB32.color(alpha, 0, 229, 255) : FastColor.ARGB32.color(alpha, 255, 43, 214);
            type = RenderType.entityTranslucentEmissive(skin.texture());
            packedLight = 15728880;
        } else {
            color = FastColor.ARGB32.color(210, 235, 235, 255);
            type = RenderType.entityTranslucent(skin.texture());
        }

        model.head.xRot = entity.getXRot() * ((float) Math.PI / 180f);
        model.head.yRot = 0f;
        model.hat.copyFrom(model.head);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entity.getYRot()));
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.scale(0.9375f, 0.9375f, 0.9375f);
        poseStack.translate(0.0f, -1.501f, 0.0f);
        model.renderToBuffer(poseStack, buffer.getBuffer(type), packedLight, OverlayTexture.NO_OVERLAY, color);
        poseStack.popPose();
    }
}