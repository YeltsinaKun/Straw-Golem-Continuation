package xubache.com.strawgolem.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xubache.com.strawgolem.Entity.StrawGolemEntity;
import xubache.com.strawgolem.config.StrawGolemConfig;

import java.util.Comparator;
import java.util.EnumSet;

public final class GolemGrabGoal extends Goal {
    private static final double SEARCH_HORIZONTAL = 8.0;
    private static final double SEARCH_VERTICAL = 3.0;
    private static final double PICKUP_RANGE_SQUARED = 1.0;

    private final StrawGolemEntity golem;
    private ItemEntity target;
    private BlockPos targetPos;
    private Path initialPath;

    public GolemGrabGoal(StrawGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(this.golem.level() instanceof ServerLevel level) || this.golem.hasCargo()) {
            return false;
        }

        Vec3 origin = this.golem.position();
        AABB area = new AABB(origin, origin).inflate(SEARCH_HORIZONTAL, SEARCH_VERTICAL, SEARCH_HORIZONTAL);
        var items = level.getEntitiesOfClass(ItemEntity.class, area, item -> canPickUp(level, item));
        items.sort(Comparator.comparingDouble(this.golem::distanceToSqr));

        for (ItemEntity item : items) {
            BlockPos pos = item.blockPosition();
            boolean inReach = this.isWithinPickupRange(item);
            Path path = inReach ? null : this.golem.getNavigation().createPath(pos, 0);
            if (inReach || (path != null && path.canReach())) {
                this.target = item;
                this.targetPos = pos;
                this.initialPath = path;
                return true;
            }
        }
        return false;
    }


    private static boolean canPickUp(ServerLevel level, ItemEntity item) {
        if (item.level() != level || !item.isAlive() || item.hasPickUpDelay()) {
            return false;
        }

        BlockPos pos = item.blockPosition();
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }

        ItemStack stack = item.getItem();
        if (StrawGolemConfig.get().autoCompostSeeds
                && (StrawGolemEntity.isReserveSeed(stack.getItem()) || stack.is(Items.BONE_MEAL))) {
            return true;
        }
        return stack.is(Items.WHEAT)
                || stack.is(Items.BEETROOT)
                || stack.is(Items.CARROT)
                || stack.is(Items.POTATO)
                || stack.is(Items.POISONOUS_POTATO)
                || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.PUMPKIN)
                || stack.is(Items.MELON)
                || stack.is(Items.MELON_SLICE);
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && !this.golem.hasCargo()
                && this.golem.level() instanceof ServerLevel level
                && canPickUp(level, this.target);
    }

    @Override
    public void start() {
        if (!this.isWithinPickupRange(this.target)
                && !this.golem.getNavigation().moveTo(this.initialPath, StrawGolemEntity.WALK_SPEED)) {
            this.stop();
        }
    }

    @Override
    public void tick() {
        if (!this.canContinueToUse()) {
            this.stop();
            return;
        }

        ItemEntity item = this.target;
        if (this.isWithinPickupRange(item)) {
            ItemStack stack = item.getItem();
            int count = Math.min(stack.getCount(), stack.getMaxStackSize());
            this.golem.setCargo(stack.copyWithCount(count));

            if (count == stack.getCount()) {
                item.discard();
            } else {
                item.setItem(stack.copyWithCount(stack.getCount() - count));
            }

            this.stop();
            return;
        }

        BlockPos pos = item.blockPosition();
        var navigation = this.golem.getNavigation();
        if (!pos.equals(this.targetPos)) {
            Path path = navigation.createPath(pos, 0);
            if (path == null || !path.canReach() || !navigation.moveTo(path, StrawGolemEntity.WALK_SPEED)) {
                this.stop();
                return;
            }
            this.targetPos = pos;
        } else if (navigation.isDone()) {
            this.stop();
        }
    }

    private boolean isWithinPickupRange(ItemEntity item) {
        return this.golem.distanceToSqr(item) <= PICKUP_RANGE_SQUARED;
    }

    @Override
    public void stop() {
        if (this.target != null) {
            this.golem.getNavigation().stop();
            this.target = null;
            this.targetPos = null;
            this.initialPath = null;
        }
    }
}