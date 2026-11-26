package xubache.com.strawgolem.ai;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
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
    private static final Map<ServerPlayer, UUID> SELECTED_GOLEMS = new WeakHashMap<>();

    private GolemOrders() {
    }

    public static void register() {
        GolemDepositHelper.registerChestEffects();

        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || !(entity instanceof StrawGolemEntity golem)
                    || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()
                    || !player.getMainHandItem().isEmpty() || player.isSpectator()) {
                return InteractionResult.PASS;
            }

            SELECTED_GOLEMS.put(serverPlayer, golem.getUUID());
            player.sendOverlayMessage(Component.literal("Đã chọn Straw Golem. Shift + chuột phải vào kho."));
            return InteractionResult.SUCCESS;
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)
                    || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()
                    || !player.getMainHandItem().isEmpty() || player.isSpectator()) {
                return InteractionResult.PASS;
            }

            UUID selected = SELECTED_GOLEMS.get(serverPlayer);
            if (selected == null || !(serverLevel.getEntity(selected) instanceof StrawGolemEntity golem)
                    || GolemDepositHelper.getContainer(serverLevel, hit.getBlockPos()) == null) {
                return InteractionResult.PASS;
            }

            golem.setDepositStorage(new GlobalPos(serverLevel.dimension(), hit.getBlockPos()));
            SELECTED_GOLEMS.remove(serverPlayer);
            player.sendOverlayMessage(Component.literal("Đã chỉ định kho cho Straw Golem."));
            return InteractionResult.SUCCESS;
        });
    }
}