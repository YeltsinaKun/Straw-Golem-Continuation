package xubache.com.strawgolem.ai;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import xubache.com.strawgolem.Entity.StrawGolemEntity;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class GolemOrders {
    private static final int SELECTION_DURATION_TICKS = 30 * 20;
    private static final Map<ServerPlayer, GolemSelection> SELECTED_GOLEMS = new WeakHashMap<>();

    private record GolemSelection(UUID golemId, long expiresAt) {
    }

    private GolemOrders() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || !(entity instanceof StrawGolemEntity golem)
                    || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()
                    || !player.getMainHandItem().isEmpty() || player.isSpectator()) {
                return InteractionResult.PASS;
            }

            SELECTED_GOLEMS.put(serverPlayer, new GolemSelection(
                    golem.getUUID(), level.getGameTime() + SELECTION_DURATION_TICKS
            ));
            player.sendOverlayMessage(Component.translatable("message.straw-golem-continuation.golem_selected"));
            return InteractionResult.SUCCESS;
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)
                    || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()
                    || !player.getMainHandItem().isEmpty() || player.isSpectator()) {
                return InteractionResult.PASS;
            }

            GolemSelection selected = SELECTED_GOLEMS.get(serverPlayer);
            if (selected == null) {
                return InteractionResult.PASS;
            }
            if (serverLevel.getGameTime() >= selected.expiresAt()) {
                SELECTED_GOLEMS.remove(serverPlayer);
                return InteractionResult.PASS;
            }

            BlockPos storagePos = hit.getBlockPos();
            if (!serverPlayer.mayInteract(serverLevel, storagePos)
                    || !(serverLevel.getEntity(selected.golemId()) instanceof StrawGolemEntity golem)
                    || GolemDepositHelper.getContainer(serverLevel, storagePos) == null) {
                return InteractionResult.PASS;
            }

            golem.setDepositStorage(new GlobalPos(serverLevel.dimension(), storagePos));
            SELECTED_GOLEMS.remove(serverPlayer);
            player.sendOverlayMessage(Component.translatable("message.straw-golem-continuation.storage_assigned"));
            return InteractionResult.SUCCESS;
        });
    }
}
