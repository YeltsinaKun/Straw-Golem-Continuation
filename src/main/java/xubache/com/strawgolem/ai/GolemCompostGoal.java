package xubache.com.strawgolem.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import xubache.com.strawgolem.Entity.StrawGolemEntity;
import xubache.com.strawgolem.config.StrawGolemConfig;

import java.util.EnumSet;

public final class GolemCompostGoal extends Goal {
    private static final int SEARCH_HORIZONTAL = 8;
    private static final int SEARCH_VERTICAL = 3;
    private static final int SEED_INTERVAL = 10;
    private static final int READY_WAIT_LIMIT = 25;
    private static final int FAILED_PATH_DELAY = 20;

    private final StrawGolemEntity golem;
    private BlockPos target;
    private BlockPos approach;
    private long nextSeedTick;
    private long waitForReadyUntil = -1L;
    private long retryAt;

    public GolemCompostGoal(StrawGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(this.golem.level() instanceof ServerLevel level)
                || !StrawGolemConfig.get().autoCompostSeeds
                || this.golem.seedsForStorage()
                || !this.hasSeeds()
                || level.getGameTime() < this.retryAt) {
            return false;
        }

        GlobalPos previous = this.golem.getCompostSite();
        if (previous != null && (!previous.dimension().equals(level.dimension())
                || getComposter(level, previous.pos()) == null)) {
            this.golem.deliverHeldSeeds();
            return false;
        }

        for (BlockPos pos : BlockPos.withinManhattan(this.golem.blockPosition(),
                SEARCH_HORIZONTAL, SEARCH_VERTICAL, SEARCH_HORIZONTAL)) {
            BlockState state = getComposter(level, pos);
            if (state == null || state.getValue(ComposterBlock.LEVEL) == ComposterBlock.MAX_LEVEL) {
                continue;
            }

            BlockPos stand = this.canReachComposter(level, pos)
                    ? this.golem.blockPosition() : GolemDepositHelper.findApproach(level, this.golem, pos);
            if (stand != null) {
                this.target = pos.immutable();
                this.golem.setCompostSite(new GlobalPos(level.dimension(), this.target));
                this.approach = stand;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        if (this.golem.level() instanceof ServerLevel level
                && !this.canReachComposter(level, this.target)) {
            Vec3 feet = Vec3.atBottomCenterOf(this.approach);
            if (!GolemDepositHelper.isLoaded(level, this.approach)
                    || !this.golem.getNavigation().moveTo(feet.x, feet.y, feet.z, 0, StrawGolemEntity.WALK_SPEED)) {
                this.failPath(level);
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && StrawGolemConfig.get().autoCompostSeeds
                && (this.hasSeeds() || this.waitForReadyUntil >= 0L);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(this.golem.level() instanceof ServerLevel level) || !this.canContinueToUse()) {
            this.stop();
            return;
        }

        BlockState state = getComposter(level, this.target);
        if (state == null) {
            this.golem.deliverHeldSeeds();
            this.stop();
            return;
        }

        if (!this.canReachComposter(level, this.target)) {
            if (this.golem.getNavigation().isDone()) {
                this.failPath(level);
            }
            return;
        }

        this.golem.getNavigation().stop();
        Vec3 center = Vec3.atCenterOf(this.target);
        this.golem.getLookControl().setLookAt(center.x, center.y, center.z);

        long now = level.getGameTime();
        int fill = state.getValue(ComposterBlock.LEVEL);
        if (fill == ComposterBlock.READY) {
            if (!this.dropBoneMeal(level, state)) {
                this.failPath(level);
                return;
            }

            this.waitForReadyUntil = -1L;
            if (!this.hasSeeds()) {
                this.stop();
            }
            return;
        }

        if (fill == ComposterBlock.MAX_LEVEL) {
            if (now >= this.waitForReadyUntil) {
                this.stop();
            }
            return;
        }

        this.waitForReadyUntil = -1L;
        if (!this.hasSeeds()) {
            this.stop();
            return;
        }
        if (now < this.nextSeedTick) {
            return;
        }

        ItemStack seeds = this.golem.getCargo().copy();
        BlockState updated = ComposterBlock.insertItem(this.golem, state, level, seeds, this.target);
        this.golem.setCargo(seeds);
        this.nextSeedTick = now + SEED_INTERVAL;

        if (updated.getValue(ComposterBlock.LEVEL) == ComposterBlock.MAX_LEVEL) {
            this.waitForReadyUntil = now + READY_WAIT_LIMIT;
        } else if (seeds.isEmpty()) {
            this.stop();
        }
    }

    private boolean dropBoneMeal(ServerLevel level, BlockState state) {
        var composter = (ComposterBlock) state.getBlock();
        var output = composter.getContainer(state, level, this.target);
        ItemStack result = output.getItem(0).copy();
        ItemEntity drop = new ItemEntity(level, this.golem.getX(), this.golem.getY() + 0.2,
                this.golem.getZ(), result);
        drop.setDefaultPickUpDelay();
        if (!level.addFreshEntity(drop)) {
            return false;
        }

        output.removeItem(0, result.getCount());
        return true;
    }

    private boolean hasSeeds() {
        return StrawGolemEntity.isReserveSeed(this.golem.getCargo().getItem());
    }

    private boolean canReachComposter(ServerLevel level, BlockPos target) {
        return GolemDepositHelper.canInteract(level, this.golem, this.golem.getEyePosition(), target);
    }

    private static BlockState getComposter(ServerLevel level, BlockPos pos) {
        if (!GolemDepositHelper.isLoaded(level, pos)) {
            return null;
        }

        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.COMPOSTER) ? state : null;
    }

    private void failPath(ServerLevel level) {
        this.retryAt = level.getGameTime() + FAILED_PATH_DELAY;
        this.stop();
    }

    @Override
    public void stop() {
        if (this.target != null) {
            this.golem.getNavigation().stop();
            this.target = null;
            this.approach = null;
            this.waitForReadyUntil = -1L;
        }
    }
}
