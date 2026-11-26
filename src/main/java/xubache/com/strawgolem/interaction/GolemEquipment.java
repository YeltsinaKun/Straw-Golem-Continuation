package xubache.com.strawgolem.interaction;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xubache.com.strawgolem.Entity.StrawGolemEntity;
import xubache.com.strawgolem.registry.ModItems;

public final class GolemEquipment {
    private GolemEquipment() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(level instanceof ServerLevel)
                    || hand != InteractionHand.MAIN_HAND
                    || player.isSpectator()
                    || !(entity instanceof StrawGolemEntity golem)) {
                return InteractionResult.PASS;
            }

            ItemStack held = player.getItemInHand(hand);
            if (held.is(Items.WHEAT)) {
                if (!golem.repairWithWheat()) {
                    return InteractionResult.PASS;
                }
            } else if (!held.is(ModItems.STRAW_HAT) || !golem.tryEquipStrawHat()) {
                return InteractionResult.PASS;
            }

            held.consume(1, player);
            return InteractionResult.CONSUME;
        });
    }
}