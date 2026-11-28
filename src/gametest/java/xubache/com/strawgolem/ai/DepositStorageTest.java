package xubache.com.strawgolem.ai;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

public final class DepositStorageTest {
    @GameTest
    public void automaticScanOnlyFindsAllowedStorage(GameTestHelper helper) {
        BlockPos origin = new BlockPos(3, 2, 3);
        BlockPos chest = origin.north();
        BlockPos trappedChest = origin.south();
        BlockPos barrel = origin.east();
        BlockPos hopper = origin.west();
        BlockPos furnace = hopper.west();
        BlockPos copperChest = barrel.east();

        helper.setBlock(chest, Blocks.CHEST);
        helper.setBlock(trappedChest, Blocks.TRAPPED_CHEST);
        helper.setBlock(barrel, Blocks.BARREL);
        helper.setBlock(hopper, Blocks.HOPPER);
        helper.setBlock(furnace, Blocks.FURNACE);
        helper.setBlock(copperChest, Blocks.COPPER_CHEST);

        var level = helper.getLevel();
        var found = GolemDepositHelper.findContainers(level, helper.absolutePos(origin));
        helper.assertTrue(found.contains(helper.absolutePos(chest)), "Scan should find a chest");
        helper.assertTrue(found.contains(helper.absolutePos(trappedChest)), "Scan should find a trapped chest");
        helper.assertTrue(found.contains(helper.absolutePos(barrel)), "Scan should find a barrel");
        helper.assertFalse(found.contains(helper.absolutePos(hopper)), "Scan should skip a hopper");
        helper.assertFalse(found.contains(helper.absolutePos(furnace)), "Scan should skip a furnace");
        helper.assertFalse(found.contains(helper.absolutePos(copperChest)), "Scan should skip a copper chest");
        helper.assertTrue(GolemDepositHelper.getContainer(level, helper.absolutePos(hopper)) != null,
                "The container lookup used by manual assignment should still accept a hopper");
        helper.succeed();
    }
}
