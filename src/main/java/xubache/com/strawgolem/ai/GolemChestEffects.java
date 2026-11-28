package xubache.com.strawgolem.ai;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ChestBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class GolemChestEffects {
    private static final Map<ServerLevel, Map<BlockPos, Long>> CHEST_CLOSE_TIMES = new WeakHashMap<>();

    private GolemChestEffects() {
    }

    static void openChest(ServerLevel level, BlockPos pos) {
        if (GolemDepositHelper.isLoaded(level, pos)
                && level.getBlockState(pos).getBlock() instanceof ChestBlock chest) {
            level.blockEvent(pos, chest, 1, 1);
            CHEST_CLOSE_TIMES.computeIfAbsent(level, ignored -> new HashMap<>())
                    .put(pos, level.getGameTime() + 16);
        }
    }

    public static void register() {
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
                if (GolemDepositHelper.isLoaded(level, pos)
                        && level.getBlockState(pos).getBlock() instanceof ChestBlock chest) {
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
