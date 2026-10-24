package dev.nullmod.item;

import dev.nullmod.CaptainEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class CaptainEggItem extends Item {
    public CaptainEggItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawn = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() ? pos : pos.relative(face);
        Pillager pillager = EntityType.PILLAGER.create(level);
        if (pillager == null) {
            return InteractionResult.FAIL;
        }
        pillager.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                level.random.nextFloat() * 360.0f, 0.0f);
        pillager.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), MobSpawnType.SPAWN_EGG, null);
        CaptainEvents.makeChronos(pillager);
        level.addFreshEntityWithPassengers(pillager);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }
}
