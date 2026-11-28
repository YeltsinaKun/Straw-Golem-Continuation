package xubache.com.strawgolem.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import xubache.com.strawgolem.StrawGolemRelaunched;

public class ModEntityModelLayers {

    public static final ModelLayerLocation STRAW_GOLEM =
            new ModelLayerLocation(
                    StrawGolemRelaunched.id("straw_golem"),
                    "main"
            );

    public static void registerModelLayer() {
        ModelLayerRegistry.registerModelLayer(
                STRAW_GOLEM,
                StrawGolemModel::createBodyLayer
        );
    }

}
