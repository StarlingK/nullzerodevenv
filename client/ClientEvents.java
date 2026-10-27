package nullmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import nullmod.NullMod;
import nullmod.core.AimMath;
import nullmod.net.AbilityPayload;
import nullmod.net.SyncPayload;
import nullmod.registry.ModPotions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = NullMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static boolean wasDown;
    private static boolean ghostSent;
    private static boolean firedOnPress;
    private static int pressTick;
    private static int tickCounter;

    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            ClientState.reset();
            WithdrawalAudio.stop();
            ThemeAudio.stopAll();
            ZoneState.clear();
            WithdrawalHallucination.clear();
            ClashRift.clear();
            wasDown = false;
            return;
        }
        tickCounter++;
        ClientState.tick();
        ZoneState.tick(mc);
        ThemeAudio.tick();
        ShaderController.tick();
        WithdrawalAudio.tick();
        WithdrawalHallucination.tick();
        ClashRift.tick();
        handleKey(mc);
    }

    private static boolean hasPrev;
    private static double prevX, prevY, prevZ;
    private static float prevYaw, prevPitch;

    @SubscribeEvent
    public static void onPlayerTickPost(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide || p != Minecraft.getInstance().player) return;
        if (hasPrev && ClientState.has(SyncPayload.FLAG_REWIND)) {
            p.xo = prevX;
            p.yo = prevY;
            p.zo = prevZ;
            p.xOld = prevX;
            p.yOld = prevY;
            p.zOld = prevZ;
            p.yRotO = p.getYRot() + Mth.wrapDegrees(prevYaw - p.getYRot());
            p.xRotO = prevPitch;
        }
        hasPrev = true;
        prevX = p.getX();
        prevY = p.getY();
        prevZ = p.getZ();
        prevYaw = p.getYRot();
        prevPitch = p.getXRot();
    }

    private static void send(int action, float range) {
        PacketDistributor.sendToServer(new AbilityPayload(action, range));
    }

    private static void handleKey(Minecraft mc) {
        boolean down = mc.screen == null && ModKeys.CHRONOS.isDown();
        int tier = ClientState.tier;

        if (down && !wasDown) {
            pressTick = tickCounter;
            ghostSent = false;
            firedOnPress = false;
            if (tier == 1 && (ClientState.has(SyncPayload.FLAG_BULLET) || (ClientState.canAct() && ClientState.bulletCd == 0))) {
                send(AbilityPayload.TAP, 0f);
            } else if (tier == 2 && (ClientState.has(SyncPayload.FLAG_GHOST)
                    || (ClientState.canAct() && ClientState.holdCd == 0))) {
                send(AbilityPayload.GHOST_START, 0f);
            } else if (tier == 4 && (ClientState.has(SyncPayload.FLAG_BULLET)
                    || (ClientState.canAct() && ClientState.bulletCd == 0))) {
                send(AbilityPayload.TAP, 0f);
            } else if (tier == 3) {
                if (ClientState.aiming) {
                    fire();
                    firedOnPress = true;
                } else if (ClientState.canAct() && ClientState.dragonCd == 0) {
                    send(AbilityPayload.DRAGON_START, 0f);
                    ClientState.aiming = true;
                    ClientState.aimStartedTick = tickCounter;
                    ClientState.aimRange = Math.min(15f, ClientState.maxRange());
                }
            }
        } else if (!down && wasDown) {
            if (tier == 3 && ClientState.aiming && !firedOnPress && tickCounter - pressTick >= 6) {
                fire();
            }
        }
        wasDown = down;

        if (ClientState.aiming) {
            if (!ClientState.has(SyncPayload.FLAG_STOP) && tickCounter - ClientState.aimStartedTick > 10) {
                ClientState.aiming = false;
            } else if (mc.options.keyUse.isDown()) {
                send(AbilityPayload.DRAGON_CANCEL, 0f);
                ClientState.aiming = false;
            }
        }
    }

    private static void fire() {
        send(AbilityPayload.DRAGON_FIRE, ClientState.aimRange);
        ClientState.aiming = false;
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!ClientState.aiming) return;
        double delta = event.getScrollDeltaY();
        if (delta != 0) {
            ClientState.aimRange = Mth.clamp(ClientState.aimRange + (float) Math.signum(delta), 1f, ClientState.maxRange());
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.reset();
        WithdrawalAudio.stop();
        ThemeAudio.stopAll();
        ZoneState.clear();
        WithdrawalHallucination.clear();
        ClashRift.clear();
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        ShaderController.frame();
    }

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inv) {
            InventoryBar.render(event.getGuiGraphics(), inv, event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onScreenClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen
                && event.getButton() == 0 && ClientState.hasChronos()
                && InventoryBar.hit(event.getMouseX(), event.getMouseY())) {
            ClientState.showInfo = !ClientState.showInfo;
            Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide && ZoneState.shouldSkip(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        PoseStack ps = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        ClashRift.render(mc, ps, cam, pt);
        if (!ClientState.aiming) return;

        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition(pt).add(0, -0.25, 0).add(look.scale(0.3));
        Vec3 travelled = AimMath.travel(player, ClientState.aimRange);
        Vec3 feetNow = new Vec3(Mth.lerp(pt, player.xo, player.getX()), Mth.lerp(pt, player.yo, player.getY()),
                Mth.lerp(pt, player.zo, player.getZ()));
        Vec3 destination = feetNow.add(travelled);
        Vec3 end = destination.add(0, player.getBbHeight() * 0.5, 0);
        float time = (mc.level.getGameTime() + pt) / 20f;

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        AimLine.render(mc, ps, cam, start, end, destination, time);
        if (travelled.lengthSqr() > 0.04) {
            DestinationGhost.render(mc, player, destination, ps, buffers, pt);
        }
        ps.popPose();
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity proxy = WithdrawalHallucination.proxyFor(event.getEntity());
        if (proxy == null) return;
        net.minecraft.client.renderer.entity.EntityRenderer<LivingEntity> renderer =
                (net.minecraft.client.renderer.entity.EntityRenderer<LivingEntity>)
                        (net.minecraft.client.renderer.entity.EntityRenderer<?>)
                        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(proxy);
        float pt = event.getPartialTick();
        LivingEntity real = event.getEntity();
        renderer.render(proxy, Mth.rotLerp(pt, real.yRotO, real.getYRot()), pt,
                event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide && event.getEntity() == Minecraft.getInstance().player) {
            WithdrawalHallucination.shatter(event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        ClashRift.captainText(event.getEntity(), event.getPoseStack(), event.getMultiBufferSource(),
                event.getPartialTick(), event.getPackedLight());
        if (ClientState.stage < 3 || !ClientState.glitching()) return;
        LivingEntity entity = event.getEntity();
        if (entity == Minecraft.getInstance().player || entity.isInvisible()) return;
        VoidFace.draw(entity, event.getPoseStack(), event.getMultiBufferSource(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null || sound instanceof TickableSoundInstance
                || sound.getLocation().getNamespace().equals(NullMod.MODID)
                || sound.getSource() == SoundSource.MUSIC || sound.getSource() == SoundSource.RECORDS) {
            return;
        }
        boolean stopped = ClientState.has(SyncPayload.FLAG_STOP);
        boolean slowed = stopped || ClientState.has(SyncPayload.FLAG_BULLET) || ClientState.has(SyncPayload.FLAG_GHOST);
        boolean glitch = ClientState.glitching();
        if (!slowed && !glitch) return;

        float pitch = 1f, volume = 1f;
        if (slowed) {
            pitch *= stopped ? 0.5f : 0.7f;
            volume *= 0.85f;
        }
        if (glitch) {
            float r = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.random.nextFloat() : 0.5f;
            pitch *= 0.5f + r * 1.3f;
            volume *= 0.6f + r * 0.7f;
        }
        try {
            event.setSound(new DistortedSound(sound, pitch, volume));
        } catch (RuntimeException e) {
            event.setSound(sound);
        }
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() == Minecraft.getInstance().player && event.getEntity().level().isClientSide
                && ClientState.cleanseLeft > 0 && "chronos".equals(ModPotions.ourPotionPath(event.getItem()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(Items.POTION)) return;
        String path = ModPotions.ourPotionPath(event.getItemStack());
        if (path == null) return;
        event.getToolTip().removeIf(c -> c.getContents() instanceof TranslatableContents t && t.getKey().equals("effect.none"));
        event.getToolTip().add(Component.translatable("item.nullmod.potion." + path + ".lore1")
                .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
        event.getToolTip().add(Component.translatable("item.nullmod.potion." + path + ".lore2")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
    }
}
