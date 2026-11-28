package xubache.com.strawgolem.ai;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.gamerules.GameRules;
import xubache.com.strawgolem.registry.ModEntities;

public final class HarvestGameRulesTest {
    @GameTest
    public void harvestChecksRemainingCropsAfterPathBudget(GameTestHelper helper) {
        BlockPos golemPos = new BlockPos(2, 2, 2);
        BlockPos reachableCrop = new BlockPos(6, 2, 6);
        var ripeWheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE);
        for (BlockPos floor : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(6, 1, 6))) {
            helper.setBlock(floor, Blocks.STONE);
        }
        for (BlockPos shelf : BlockPos.betweenClosed(new BlockPos(1, 4, 1), new BlockPos(3, 4, 2))) {
            helper.setBlock(shelf, Blocks.FARMLAND);
            helper.setBlock(shelf.above(), ripeWheat);
        }
        helper.setBlock(reachableCrop.below(), Blocks.FARMLAND);
        helper.setBlock(reachableCrop, ripeWheat);
        var golem = helper.spawn(ModEntities.STRAW_GOLEM, golemPos);
        golem.setOnGround(true);
        var goal = new GolemHarvestGoal(golem);

        helper.assertFalse(goal.canUse(), "The first four closer crops are unreachable");
        helper.assertTrue(goal.canUse(), "The next attempt should reach the crop beyond the shelf");
        goal.start();
        var path = golem.getNavigation().getPath();
        helper.assertTrue(path != null && path.getTarget().equals(helper.absolutePos(reachableCrop)),
                "Navigation should use the path to the reachable crop");
        for (BlockPos crop : BlockPos.betweenClosed(new BlockPos(1, 5, 1), new BlockPos(3, 5, 2))) {
            helper.setBlock(crop, Blocks.AIR);
        }
        helper.setBlock(reachableCrop, Blocks.AIR);
        helper.succeed();
    }

    @GameTest(maxTicks = 50)
    public void failedScanWaitsBeforeLookingForNewCrops(GameTestHelper helper) {
        BlockPos crop = new BlockPos(3, 2, 3);
        BlockPos golemPos = crop.west();
        for (BlockPos floor : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(6, 1, 6))) {
            helper.setBlock(floor, Blocks.STONE);
        }
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        var golem = helper.spawn(ModEntities.STRAW_GOLEM, golemPos);
        golem.setNoAi(true);
        golem.setOnGround(true);
        var goal = new GolemHarvestGoal(golem);
        helper.assertFalse(goal.canUse(), "No crop is available for the initial scan");

        var ripeWheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE);
        helper.setBlock(crop, ripeWheat);
        helper.runAfterDelay(19, () -> helper.assertFalse(goal.canUse(), "Scan should wait at least 20 ticks"));
        helper.runAfterDelay(40, () -> {
            golem.setOnGround(true);
            helper.assertTrue(goal.canUse(), "Scan should resume by tick 40");
            helper.succeed();
        });
    }

    @GameTest
    public void harvestRequiresMobGriefingEvenWhenBlockDropsAreDisabled(GameTestHelper helper) {
        var level = helper.getLevel();
        var rules = level.getGameRules();
        var server = level.getServer();
        boolean originalMobGriefing = rules.get(GameRules.MOB_GRIEFING);
        boolean originalBlockDrops = rules.get(GameRules.BLOCK_DROPS);

        BlockPos crop = new BlockPos(3, 2, 3);
        BlockPos golemPos = crop.west();
        for (BlockPos floor : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(6, 1, 6))) {
            helper.setBlock(floor, Blocks.STONE);
        }
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        var ripeWheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE);
        helper.setBlock(crop, ripeWheat);
        var golem = helper.spawn(ModEntities.STRAW_GOLEM, golemPos);
        golem.setOnGround(true);
        var goal = new GolemHarvestGoal(golem);

        try {
            rules.set(GameRules.MOB_GRIEFING, true, server);
            rules.set(GameRules.BLOCK_DROPS, true, server);
            helper.assertTrue(StrawGolemHarvestHelper.isHarvestable(level, helper.absolutePos(crop)),
                    "The fixture should contain harvestable wheat");
            helper.assertTrue(goal.canUse(), "Both enabled rules should allow a reachable crop to be selected");

            rules.set(GameRules.MOB_GRIEFING, false, server);
            helper.assertFalse(goal.canUse(), "Disabled mob griefing should prevent harvest goal selection");
            StrawGolemHarvestHelper.harvest(level, golem, helper.absolutePos(crop));
            helper.assertBlockState(crop, ripeWheat);
            helper.assertFalse(golem.hasCargo(), "Harvest should leave the golem empty-handed");

            rules.set(GameRules.MOB_GRIEFING, true, server);
            rules.set(GameRules.BLOCK_DROPS, false, server);
            helper.assertTrue(new GolemHarvestGoal(golem).canUse(),
                    "Disabled block drops should still allow harvest goal selection");
            StrawGolemHarvestHelper.harvest(level, golem, helper.absolutePos(crop));
            helper.assertBlockProperty(crop, CropBlock.AGE, 0);
            helper.assertTrue(golem.hasCargo(), "Allowed harvest should yield cargo");
            helper.succeed();
        } finally {
            rules.set(GameRules.MOB_GRIEFING, originalMobGriefing, server);
            rules.set(GameRules.BLOCK_DROPS, originalBlockDrops, server);
        }
    }
}
