package xubache.com.strawgolem.ai;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xubache.com.strawgolem.Entity.StrawGolemEntity;
import xubache.com.strawgolem.config.StrawGolemConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

final class GolemDepositHelper {
    private static final Map<ServerLevel, Map<BlockPos, Long>> CHEST_CLOSE_TIMES = new WeakHashMap<>();

    private GolemDepositHelper() {
    }

    static boolean isLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    static Container getContainer(ServerLevel level, BlockPos pos) {
        return isLoaded(level, pos) && level.getBlockEntity(pos) instanceof Container container
                ? container : null;
    }

    static List<BlockPos> findContainers(ServerLevel level, BlockPos origin) {
        StrawGolemConfig config = StrawGolemConfig.get();
        int horizontalRange = config.depositHorizontalRange;
        int verticalRange = config.depositVerticalRange;
        List<BlockPos> result = new ArrayList<>();

        for (int x = (origin.getX() - horizontalRange) >> 4;
             x <= (origin.getX() + horizontalRange) >> 4; x++) {
            for (int z = (origin.getZ() - horizontalRange) >> 4;
                 z <= (origin.getZ() + horizontalRange) >> 4; z++) {
                var chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    if (entry.getValue() instanceof Container
                            && Math.abs(pos.getX() - origin.getX()) <= horizontalRange
                            && Math.abs(pos.getY() - origin.getY()) <= verticalRange
                            && Math.abs(pos.getZ() - origin.getZ()) <= horizontalRange) {
                        result.add(pos);
                    }
                }
            }
        }
        result.sort(Comparator.comparingDouble(origin::distSqr));
        return result;
    }

    private static int room(Container container, int slot, ItemStack held) {
        if (!container.canPlaceItem(slot, held)) {
            return 0;
        }

        ItemStack stored = container.getItem(slot);
        if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, held)) {
            return 0;
        }

        int limit = Math.min(held.getMaxStackSize(), container.getMaxStackSize(held));
        return Math.max(0, limit - stored.getCount());
    }

    static boolean canAccept(Container container, ItemStack held) {
        if (container == null || held.isEmpty()) {
            return false;
        }

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (room(container, slot, held) > 0) {
                return true;
            }
        }
        return false;
    }

    static int deposit(Container container, StrawGolemEntity golem) {
        ItemStack remaining = golem.getCargo().copy();
        int before = remaining.getCount();

        for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
            for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                ItemStack stored = container.getItem(slot);
                if (stored.isEmpty() != (pass == 1)) {
                    continue;
                }

                int moved = Math.min(remaining.getCount(), room(container, slot, remaining));
                if (moved > 0) {
                    int oldCount = stored.getCount();
                    container.setItem(slot, remaining.copyWithCount(oldCount + moved));
                    ItemStack updated = container.getItem(slot);
                    if (ItemStack.isSameItemSameComponents(updated, remaining)) {
                        remaining.shrink(Math.clamp(updated.getCount() - oldCount, 0, moved));
                    }
                }
            }
        }

        int inserted = before - remaining.getCount();
        if (inserted > 0) {
            container.setChanged();
            golem.setCargo(remaining);
        }
        return inserted;
    }

    static BlockPos findApproach(ServerLevel level, StrawGolemEntity golem, BlockPos target) {
        List<BlockPos> candidates = new ArrayList<>();
        List<BlockPos> columns = List.of(target, target.north(), target.south(), target.east(), target.west());
        for (BlockPos column : columns) {
            for (int dy = -2; dy <= 2; dy++) {
                BlockPos stand = column.offset(0, dy, 0);
                if (!isLoaded(level, stand)
                        || hasCollision(level, stand)
                        || hasCollision(level, stand.above())
                        || !hasCollision(level, stand.below())) {
                    continue;
                }

                Vec3 eyes = Vec3.atBottomCenterOf(stand).add(0.0, golem.getEyeHeight(), 0.0);
                if (canInteract(level, golem, eyes, target)) {
                    candidates.add(stand);
                }
            }
        }

        candidates.sort(Comparator
                .comparingDouble((BlockPos stand) -> stand.distSqr(target) + (stand.getY() > target.getY() ? 1 : 0))
                .thenComparingDouble(stand -> golem.distanceToSqr(Vec3.atBottomCenterOf(stand))));
        for (BlockPos stand : candidates) {
            var path = golem.getNavigation().createPath(stand, 0);
            if (path != null && path.canReach()) {
                return stand;
            }
        }
        return null;
    }

    private static boolean hasCollision(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    static boolean canInteract(ServerLevel level, StrawGolemEntity golem, Vec3 eyes, BlockPos target) {
        if (new AABB(target).distanceToSqr(eyes) > 4.0) {
            return false;
        }

        BlockPos eyeBlock = BlockPos.containing(eyes);
        for (int x = Math.min(eyeBlock.getX(), target.getX()) >> 4;
             x <= Math.max(eyeBlock.getX(), target.getX()) >> 4; x++) {
            for (int z = Math.min(eyeBlock.getZ(), target.getZ()) >> 4;
                 z <= Math.max(eyeBlock.getZ(), target.getZ()) >> 4; z++) {
                if (!level.getChunkSource().hasChunk(x, z)) {
                    return false;
                }
            }
        }

        Vec3 center = Vec3.atCenterOf(target);
        var hit = level.clip(new ClipContext(
                eyes, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, golem
        ));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(target);
    }

    static void openChest(ServerLevel level, BlockPos pos) {
        if (isLoaded(level, pos) && level.getBlockState(pos).getBlock() instanceof ChestBlock chest) {
            level.blockEvent(pos, chest, 1, 1);
            CHEST_CLOSE_TIMES.computeIfAbsent(level, ignored -> new HashMap<>())
                    .put(pos, level.getGameTime() + 16);
        }
    }

    static void registerChestEffects() {
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            Map<BlockPos, Long> pending = CHEST_CLOSE_TIMES.get(level);
            if (pending == null) {
                return;
            }

            pending.entrySet().removeIf(entry -> {
                if (entry.getValue() > level.getGameTime()) {
                    return false;
                }

                BlockPos pos = entry.getKey();
                if (isLoaded(level, pos) && level.getBlockState(pos).getBlock() instanceof ChestBlock chest) {
                    level.blockEvent(pos, chest, 1, 0);
                }
                return true;
            });

            if (pending.isEmpty()) {
                CHEST_CLOSE_TIMES.remove(level);
            }
        });
    }
}