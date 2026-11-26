package xubache.com.strawgolem;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xubache.com.strawgolem.ai.NibbleStrawGolemGoal;
import xubache.com.strawgolem.config.StrawGolemConfig;
import xubache.com.strawgolem.registry.ModEntities;
import xubache.com.strawgolem.ai.GolemOrders;
import xubache.com.strawgolem.interaction.GolemEquipment;
import xubache.com.strawgolem.registry.ModItems;

public class StrawGolemRelaunched implements ModInitializer {
	public static final String MOD_ID = "straw-golem-relaunched";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		StrawGolemConfig.load();
		ModEntities.initialize();
		ModItems.initialize();
		GolemOrders.register();
		GolemEquipment.register();
		NibbleStrawGolemGoal.register();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
