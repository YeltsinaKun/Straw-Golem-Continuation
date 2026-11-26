package xubache.com.strawgolem.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xubache.com.strawgolem.Entity.StrawGolemEntity;
import xubache.com.strawgolem.registry.ModEntities;

@Mixin(Block.class)
public abstract class StrawGolemSummonMixin {

    @Inject(method = "setPlacedBy", at = @At("TAIL"))
    private void strawgolem$summon(Level level, BlockPos pos, BlockState state,
                                   LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        if (!state.is(Blocks.CARVED_PUMPKIN)
                || !(level instanceof ServerLevel serverLevel)
                || !(placer instanceof ServerPlayer)) {
            return;
        }

        if (!serverLevel.getBlockState(pos).is(Blocks.CARVED_PUMPKIN)) {
            return;
        }

        BlockPos hayPos = null;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (serverLevel.getChunkSource().hasChunk(neighbor.getX() >> 4, neighbor.getZ() >> 4)
                    && serverLevel.getBlockState(neighbor).is(Blocks.HAY_BLOCK)) {
                hayPos = neighbor;
                break;
            }
        }

        if (hayPos == null) {
            return;
        }

        StrawGolemEntity golem = ModEntities.STRAW_GOLEM.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (golem == null) {
            return;
        }

        golem.snapTo(hayPos.getX() + 0.5D, hayPos.getY(), hayPos.getZ() + 0.5D);
        if (serverLevel.addFreshEntity(golem)) {
            serverLevel.removeBlock(pos, false);
            serverLevel.removeBlock(hayPos, false);
        }
    }
}