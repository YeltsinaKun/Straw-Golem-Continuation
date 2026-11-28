package xubache.com.strawgolem.ai;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import xubache.com.strawgolem.Entity.StrawGolemEntity;
import xubache.com.strawgolem.config.StrawGolemConfig;
import xubache.com.strawgolem.registry.ModEntities;

public final class GolemAttributeSaveTest {
    @GameTest
    public void textureStageScalesWithMaxHealth(GameTestHelper helper) {
        var golem = new StrawGolemEntity(ModEntities.STRAW_GOLEM, helper.getLevel());
        for (float maxHealth : new float[]{4.0F, 12.0F, 40.0F}) {
            golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
            float oldThreshold = maxHealth / 2.0F;
            float dyingThreshold = maxHealth / 6.0F;

            golem.setHealth(Math.nextUp(oldThreshold));
            helper.assertValueEqual(golem.getTextureStage(), 0, "Normal stage above half health");
            golem.setHealth(oldThreshold);
            helper.assertValueEqual(golem.getTextureStage(), StrawGolemEntity.LIFE_STAGE_OLD, "Old stage at half health");
            golem.setHealth(Math.nextUp(dyingThreshold));
            helper.assertValueEqual(golem.getTextureStage(), StrawGolemEntity.LIFE_STAGE_OLD, "Old stage above one sixth");
            golem.setHealth(dyingThreshold);
            helper.assertValueEqual(golem.getTextureStage(), StrawGolemEntity.LIFE_STAGE_DYING, "Dying stage at one sixth");
        }
        helper.succeed();
    }

    @GameTest
    public void loadedAttributesUseConfig(GameTestHelper helper) {
        var level = helper.getLevel();
        var config = StrawGolemConfig.get();
        double maxHealth = config.attributes.maxHealth;
        double movementSpeed = config.attributes.movementSpeed;
        boolean lifespanEnabled = config.lifespanEnabled;

        var golem = new StrawGolemEntity(ModEntities.STRAW_GOLEM, level);
        golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0D);
        golem.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        golem.setHealth(18.0F);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        golem.saveWithoutId(output);
        var saved = output.buildResult();

        try {
            config.attributes.maxHealth = 12.0D;
            config.attributes.movementSpeed = 0.25D;
            config.lifespanEnabled = false;
            golem.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
            helper.assertValueEqual(golem.getAttributeBaseValue(Attributes.MAX_HEALTH), 12.0D, "Max health base");
            helper.assertValueEqual(golem.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), 0.25D, "Movement speed base");
            helper.assertValueEqual(golem.getHealth(), 12.0F, "Health should be capped even with lifespan disabled");

            config.attributes.maxHealth = 24.0D;
            golem.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
            helper.assertValueEqual(golem.getAttributeBaseValue(Attributes.MAX_HEALTH), 24.0D, "Increased max health");
            helper.assertValueEqual(golem.getHealth(), 18.0F, "Increasing max health should preserve saved health");
            helper.succeed();
        } finally {
            config.attributes.maxHealth = maxHealth;
            config.attributes.movementSpeed = movementSpeed;
            config.lifespanEnabled = lifespanEnabled;
        }
    }
}
