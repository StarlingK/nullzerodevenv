package nullmod.mixin;

import nullmod.core.TimeScale;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private boolean nullmod$scaling;

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nullmod$scaleMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (nullmod$scaling || type != MoverType.SELF || !(self instanceof LivingEntity) || self instanceof Player) {
            return;
        }
        int factor = TimeScale.factor(self);
        if (factor <= 1) {
            return;
        }
        nullmod$scaling = true;
        try {
            self.move(type, movement.scale(1.0 / factor));
        } finally {
            nullmod$scaling = false;
        }
        ci.cancel();
    }
}
