package nullmod.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import nullmod.NullConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class WithdrawalHallucination {
    private static final Map<UUID, Integer> SHATTERED = new HashMap<>();
    private static Zombie zombie;
    private static Skeleton skeleton;
    private static Creeper creeper;
    private static Spider spider;

    private WithdrawalHallucination() {}

    public static boolean active() {
        return NullConfig.WITHDRAWAL_HALLUCINATIONS.get() && ClientState.stage >= 3;
    }

    private static boolean isProxy(Entity e) {
        return e == zombie || e == skeleton || e == creeper || e == spider;
    }

    public static LivingEntity proxyFor(LivingEntity real) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.level == null || real == mc.player || isProxy(real) || real instanceof ArmorStand
                || real.isInvisible() || SHATTERED.containsKey(real.getUUID())) {
            return null;
        }
        if (!(real instanceof Player || real instanceof AgeableMob || real instanceof IronGolem)) {
            return null;
        }
        if (zombie == null || zombie.level() != mc.level) {
            zombie = new Zombie(EntityType.ZOMBIE, mc.level);
            skeleton = new Skeleton(EntityType.SKELETON, mc.level);
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            creeper = new Creeper(EntityType.CREEPER, mc.level);
            spider = new Spider(EntityType.SPIDER, mc.level);
        }
        LivingEntity proxy = switch (Math.abs(real.getUUID().hashCode()) % 4) {
            case 1 -> skeleton;
            case 2 -> creeper;
            case 3 -> spider;
            default -> zombie;
        };
        copyState(proxy, real);
        return proxy;
    }

    private static void copyState(LivingEntity proxy, LivingEntity real) {
        proxy.setPosRaw(real.getX(), real.getY(), real.getZ());
        proxy.xo = real.xo;
        proxy.yo = real.yo;
        proxy.zo = real.zo;
        proxy.setYRot(real.getYRot());
        proxy.yRotO = real.yRotO;
        proxy.setXRot(real.getXRot());
        proxy.xRotO = real.xRotO;
        proxy.yBodyRot = real.yBodyRot;
        proxy.yBodyRotO = real.yBodyRotO;
        proxy.yHeadRot = real.yHeadRot;
        proxy.yHeadRotO = real.yHeadRotO;
        proxy.walkAnimation.setSpeed(real.walkAnimation.speed());
        proxy.attackAnim = real.attackAnim;
        proxy.oAttackAnim = real.oAttackAnim;
        proxy.swinging = real.swinging;
        proxy.tickCount = real.tickCount;
        proxy.hurtTime = real.hurtTime;
        proxy.hurtDuration = real.hurtDuration;
        proxy.setOnGround(real.onGround());
        proxy.setShiftKeyDown(real.isShiftKeyDown());
        proxy.setCustomName(null);
        proxy.setCustomNameVisible(false);
        if (proxy instanceof Zombie) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                proxy.setItemSlot(slot, real.getItemBySlot(slot));
            }
        }
    }

    public static void shatter(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (!(entity instanceof LivingEntity living) || mc.level == null || proxyFor(living) == null) return;
        SHATTERED.put(entity.getUUID(), 50);
        Vec3 pos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        mc.level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 1.3f, false);
        for (int i = 0; i < 20; i++) {
            double vx = (mc.level.random.nextDouble() - 0.5) * 0.4;
            double vy = mc.level.random.nextDouble() * 0.3;
            double vz = (mc.level.random.nextDouble() - 0.5) * 0.4;
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, vx, vy, vz);
        }
    }

    public static void tick() {
        Iterator<Map.Entry<UUID, Integer>> it = SHATTERED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            if (e.getValue() <= 1) it.remove();
            else e.setValue(e.getValue() - 1);
        }
    }

    public static void clear() {
        SHATTERED.clear();
        zombie = null;
        skeleton = null;
        creeper = null;
        spider = null;
    }
}
