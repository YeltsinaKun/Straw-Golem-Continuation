package xubache.com.strawgolem.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import xubache.com.strawgolem.StrawGolemContinuation;

public final class ModItems {
    private static final ResourceKey<Item> STRAW_HAT_KEY =
            ResourceKey.create(Registries.ITEM, StrawGolemContinuation.id("straw_hat"));

    public static final Item STRAW_HAT = Registry.register(
            BuiltInRegistries.ITEM,
            STRAW_HAT_KEY,
            new Item(new Item.Properties().setId(STRAW_HAT_KEY))
    );

    private ModItems() {
    }

    public static void initialize() {

        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                StrawGolemContinuation.id("straw_golem"),
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.straw-golem-continuation.straw_golem"))
                        .icon(() -> new ItemStack(STRAW_HAT))
                        .displayItems((parameters, output) -> output.accept(STRAW_HAT))
                        .build()
        );
    }
}
