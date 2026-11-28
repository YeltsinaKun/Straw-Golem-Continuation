package xubache.com.strawgolem.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import xubache.com.strawgolem.Entity.StrawGolemEntity;
import xubache.com.strawgolem.StrawGolemRelaunched;

public final class ModEntities {

    private static final ResourceKey<EntityType<?>> STRAW_GOLEM_KEY =
            ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    StrawGolemRelaunched.id("straw_golem")
            );

    public static final EntityType<StrawGolemEntity> STRAW_GOLEM =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    STRAW_GOLEM_KEY,
                    EntityType.Builder
                            .<StrawGolemEntity>of(
                                    StrawGolemEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(0.6F,1.2F)
                            .build(STRAW_GOLEM_KEY)
            );

    private ModEntities() {
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(
                STRAW_GOLEM,
                StrawGolemEntity.createAttributes()
        );
    }
}
