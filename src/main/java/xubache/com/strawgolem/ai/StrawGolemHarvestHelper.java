package xubache.com.strawgolem.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gamerules.GameRules;
import xubache.com.strawgolem.Entity.StrawGolemEntity;
import xubache.com.strawgolem.config.StrawGolemConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StrawGolemHarvestHelper {
    private static final TagKey<Item> SEEDS = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath("c", "seeds")
    );

    private StrawGolemHarvestHelper() {
    }

    static boolean isHarvestable(ServerLevel level, BlockPos pos) {
        return getRipeCrop(level, pos) != null;
    }

    static boolean canHarvest(ServerLevel level) {
        return level.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    static void harvest(ServerLevel level, StrawGolemEntity golem, BlockPos pos) {
        if (golem.level() != level || golem.hasCargo() || !canHarvest(level)) {
            return;
        }

        BlockState state = getRipeCrop(level, pos);
        if (state == null) {
            return;
        }

        List<ItemStack> loot = Block.getDrops(state, level, pos, null, golem, ItemStack.EMPTY);
        if (StrawGolemConfig.get().vanillaHarvesting) {
            List<ItemStack> drops = new ArrayList<>(loot.size());
            for (ItemStack drop : loot) {
                drops.add(drop.copy());
            }
            applyVanillaHarvest(level, golem, pos, state, drops);
        } else {
            ItemStack cargo = state.is(Blocks.MELON)
                    ? new ItemStack(Items.MELON)
                    : selectSingleDrop(loot);
            level.setBlock(pos, resetBlock(state), Block.UPDATE_ALL);
            golem.setCargo(cargo);
        }
    }

    static void applyVanillaHarvest(ServerLevel level, StrawGolemEntity golem, BlockPos pos,
                                    BlockState state, List<ItemStack> drops) {
        if (state.is(Blocks.MELON)) {
            drops.clear();
            drops.add(new ItemStack(Items.MELON));
        }

        Item plantingItem = plantingItem(state);
        BlockState replacement;
        if (plantingItem == null) {
            replacement = resetBlock(state);
        } else if (consumeFromLoot(drops, plantingItem) || golem.consumeSeed(plantingItem)) {
            replacement = state.getBlock().defaultBlockState();
        } else {
            replacement = Blocks.AIR.defaultBlockState();
        }

        level.setBlock(pos, replacement, Block.UPDATE_ALL);
        distributeDrops(level, golem, drops);
    }

    static ItemStack selectSingleDrop(List<ItemStack> drops) {
        return drops.stream()
                .filter(stack -> {
                    Item item = stack.getItem();
                    return !(item instanceof BlockItem)
                            || !(StrawGolemEntity.isReserveSeed(item) || stack.is(SEEDS))
                            || item.components().has(DataComponents.FOOD);
                })
                .findFirst()
                .orElseGet(() -> drops.isEmpty() ? ItemStack.EMPTY : drops.getFirst());
    }

    private static boolean consumeFromLoot(List<ItemStack> drops, Item item) {
        for (ItemStack stack : drops) {
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void distributeDrops(ServerLevel level, StrawGolemEntity golem, List<ItemStack> drops) {
        ItemStack cargo = ItemStack.EMPTY;
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) {
                continue;
            }

            if (StrawGolemEntity.isReserveSeed(stack.getItem())) {
                golem.storeHarvestSeeds(stack);
            } else if (cargo.isEmpty()) {
                cargo = stack;
            } else if (ItemStack.isSameItemSameComponents(cargo, stack)
                    && stack.getCount() <= cargo.getMaxStackSize() - cargo.getCount()) {
                cargo.grow(stack.getCount());
            } else {
                dropAtGolem(level, golem, stack, 10);
            }
        }
        golem.setCargo(cargo);
    }

    private static void dropAtGolem(ServerLevel level, StrawGolemEntity golem, ItemStack stack, int delay) {
        if (!golem.dropStack(level, stack, delay)) {
            golem.queuePendingDrop(stack, delay);
        }
    }

    private static Item plantingItem(BlockState state) {
        if (state.is(Blocks.WHEAT)) {
            return Items.WHEAT_SEEDS;
        }
        if (state.is(Blocks.BEETROOTS)) {
            return Items.BEETROOT_SEEDS;
        }
        if (state.is(Blocks.CARROTS)) {
            return Items.CARROT;
        }
        if (state.is(Blocks.POTATOES)) {
            return Items.POTATO;
        }
        return null;
    }

    private static BlockState resetBlock(BlockState state) {
        IntegerProperty age = ageProperty(state);
        return age == null ? Blocks.AIR.defaultBlockState()
                : state.setValue(age, state.getBlock().defaultBlockState().getValue(age));
    }

    private static IntegerProperty ageProperty(BlockState state) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age")) {
                return age;
            }
        }
        return null;
    }

    private static BlockState getRipeCrop(ServerLevel level, BlockPos pos) {
        if (!isLoaded(level, pos)) {
            return null;
        }

        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos stemPos = pos.relative(direction);
                if (isLoaded(level, stemPos)) {
                    BlockState stem = level.getBlockState(stemPos);
                    if (stem.getBlock() instanceof AttachedStemBlock
                            && stem.getValue(AttachedStemBlock.FACING) == direction.getOpposite()) {
                        return state;
                    }
                }
            }
            return null;
        }

        if (!(state.getBlock() instanceof CropBlock) && !state.is(Blocks.SWEET_BERRY_BUSH)) {
            return null;
        }

        IntegerProperty age = ageProperty(state);
        return age != null && state.getValue(age).equals(Collections.max(age.getPossibleValues()))
                && resetBlock(state).canSurvive(level, pos) ? state : null;
    }

    private static boolean isLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }
}
