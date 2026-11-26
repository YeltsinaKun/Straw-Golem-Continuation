package xubache.com.strawgolem.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import xubache.com.strawgolem.Entity.StrawGolemEntity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public final class GolemDepositGoal extends Goal {
    private static final int SCAN_DELAY = 40;
    private static final int RETRY_INTERVAL = 10;
    private static final int MAX_STUCK_UPDATES = 25;
    private static final int MAX_UPDATES = 100;
    private static final double MIN_PROGRESS = 0.025;
    private static final int FAILED_CHEST_DELAY = 20;

    private final StrawGolemEntity golem;
    private final Map<GlobalPos, Long> retryAfter = new HashMap<>();
    private GlobalPos lockedChest;
    private BlockPos target;
    private BlockPos approach;
    private int scanCooldown;
    private int updates;
    private int stuckUpdates;
    private double lastDistanceSquared;

    public GolemDepositGoal(StrawGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(this.golem.level() instanceof ServerLevel level) || !this.golem.hasCargo()
                || this.golem.getHarvestElapsedTicks() >= 0) {
            return false;
        }

        long now = level.getGameTime();
        this.retryAfter.values().removeIf(until -> until <= now);

        if (this.select(level, this.golem.getDepositStorage()) || this.select(level, this.lockedChest)) {
            return true;
        }
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
            return false;
        }

        for (BlockPos pos : GolemDepositHelper.findContainers(level, this.golem.blockPosition())) {
            if (this.select(level, new GlobalPos(level.dimension(), pos))) {
                return true;
            }
        }

        this.scanCooldown = SCAN_DELAY;
        return false;
    }

    private boolean select(ServerLevel level, GlobalPos location) {
        if (location == null || !location.dimension().equals(level.dimension())
                || this.retryAfter.containsKey(location)) {
            return false;
        }

        BlockPos pos = location.pos();
        Container container = GolemDepositHelper.getContainer(level, pos);
        if (container == null) {
            return false;
        }
        if (!GolemDepositHelper.canAccept(container, this.golem.getCargo())) {
            this.avoid(level, pos);
            return false;
        }

        BlockPos stand = GolemDepositHelper.canInteract(level, this.golem, this.golem.getEyePosition(), pos)
                ? this.golem.blockPosition() : GolemDepositHelper.findApproach(level, this.golem, pos);
        if (stand == null) {
            this.avoid(level, pos);
            return false;
        }

        this.target = pos;
        this.approach = stand;
        return true;
    }

    @Override
    public void start() {
        this.golem.deliverHeldSeeds();
        this.updates = 0;
        this.stuckUpdates = 0;
        this.lastDistanceSquared = this.golem.distanceToSqr(Vec3.atCenterOf(this.target));

        if (this.golem.level() instanceof ServerLevel level
                && !GolemDepositHelper.canInteract(level, this.golem, this.golem.getEyePosition(), this.target)) {
            this.moveToApproach(level);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.golem.hasCargo();
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public void tick() {
        if (this.target == null || !(this.golem.level() instanceof ServerLevel level)) {
            return;
        }

        Container container = GolemDepositHelper.getContainer(level, this.target);
        if (container == null) {
            this.fail(level);
            return;
        }

        Vec3 center = Vec3.atCenterOf(this.target);
        this.golem.getLookControl().setLookAt(center.x, center.y, center.z);
        if (GolemDepositHelper.canInteract(level, this.golem, this.golem.getEyePosition(), this.target)) {
            if (GolemDepositHelper.deposit(container, this.golem) == 0) {
                this.fail(level);
            } else {
                this.lockedChest = new GlobalPos(level.dimension(), this.target);
                GolemDepositHelper.openChest(level, this.target);
                this.stop();
            }
            return;
        }
        if (!GolemDepositHelper.canAccept(container, this.golem.getCargo())) {
            this.fail(level);
            return;
        }

        this.updates++;
        double distanceSquared = this.golem.distanceToSqr(center);
        this.stuckUpdates = this.lastDistanceSquared - distanceSquared > MIN_PROGRESS ? 0 : this.stuckUpdates + 1;
        this.lastDistanceSquared = distanceSquared;

        if (this.updates > MAX_UPDATES || this.stuckUpdates > MAX_STUCK_UPDATES) {
            this.fail(level);
        } else if (this.updates % RETRY_INTERVAL == 0 && this.golem.getNavigation().isDone()) {
            this.moveToApproach(level);
        }
    }

    private void moveToApproach(ServerLevel level) {
        if (GolemDepositHelper.isLoaded(level, this.approach)) {
            Vec3 feet = Vec3.atBottomCenterOf(this.approach);
            this.golem.getNavigation().moveTo(feet.x, feet.y, feet.z, 0, StrawGolemEntity.WALK_SPEED);
        }
    }

    private void avoid(ServerLevel level, BlockPos pos) {
        GlobalPos location = new GlobalPos(level.dimension(), pos);
        this.retryAfter.put(location, level.getGameTime() + FAILED_CHEST_DELAY);
        if (location.equals(this.lockedChest)) {
            this.lockedChest = null;
        }
    }

    private void fail(ServerLevel level) {
        this.avoid(level, this.target);
        this.stop();
    }

    @Override
    public void stop() {
        if (this.target != null) {
            this.golem.getNavigation().stop();
            this.target = null;
            this.approach = null;
        }
    }
}
