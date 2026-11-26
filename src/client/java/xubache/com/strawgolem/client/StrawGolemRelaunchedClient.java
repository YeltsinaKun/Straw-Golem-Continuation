package xubache.com.strawgolem.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import xubache.com.strawgolem.client.render.ModEntityModelLayers;
import xubache.com.strawgolem.client.render.StrawGolemRenderer;
import xubache.com.strawgolem.registry.ModEntities;

public class StrawGolemRelaunchedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModEntityModelLayers.registerModelLayer();
		EntityRenderers.register(
				ModEntities.STRAW_GOLEM,
				StrawGolemRenderer::new
		);
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
	}
}