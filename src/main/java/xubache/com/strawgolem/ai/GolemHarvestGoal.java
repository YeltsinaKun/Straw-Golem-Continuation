package xubache.com.strawgolem.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xubache.com.strawgolem.Entity.StrawGolemEntity;
import xubache.com.strawgolem.config.StrawGolemConfig;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class GolemHarvestGoal extends Goal {

    private static final double REACH_SQUARED = 4.0D;
    private static final int MIN_FAILED_SCAN_DELAY = 20;
    private static final int MAX_FAILED_SCAN_DELAY = 40;
    private static final int MAX_PATH_ATTEMPTS = 4;
    private static final int FAILED_PATH_TTL = 100;
    private final StrawGolemEntity golem;
    private final Map<BlockPos, Long> failedUntil = new HashMap<>();
    private PriorityQueue<BlockPos> crops = new PriorityQueue<>();
    private BlockPos target;
    private Path targetPath;
    private long nextScanTick;

    public GolemHarvestGoal(StrawGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.golem.hasCargo() || !(this.golem.level() instanceof ServerLevel level)
                || !StrawGolemHarvestHelper.canHarvest(level)) {
            return false;
        }

        boolean needsScan = this.crops.isEmpty();
        long now = level.getGameTime();
        if (needsScan && now < this.nextScanTick) {
            return false;
        }

        StrawGolemConfig config = StrawGolemConfig.get();
        int horizontalRange = config.harvestHorizontalRange;
        BlockPos origin = this.golem.blockPosition();
        PriorityQueue<BlockPos> ordered = new PriorityQueue<>(
                Comparator.comparingInt(origin::distManhattan)
        );

        if (needsScan) {
            this.failedUntil.values().removeIf(until -> until <= now);
            for (BlockPos pos : BlockPos.withinManhattan(
                    origin, horizontalRange, config.harvestScanVerticalRange, horizontalRange
            )) {
                if (isInTargetRange(origin, pos, horizontalRange, config.harvestTargetVerticalRange)
                        && !this.failedUntil.containsKey(pos)
                        && StrawGolemHarvestHelper.isHarvestable(level, pos)) {
                    ordered.add(pos.immutable());
                }
            }
        } else {
            ordered.addAll(this.crops);
        }
        this.crops = ordered;

        int pathAttempts = 0;
        while (pathAttempts < MAX_PATH_ATTEMPTS && !this.crops.isEmpty()) {
            BlockPos candidate = this.crops.remove();
            if (!isInTargetRange(
                    origin, candidate, horizontalRange, config.harvestTargetVerticalRange
            ) || !StrawGolemHarvestHelper.isHarvestable(level, candidate)) {
                continue;
            }

            var state = level.getBlockState(candidate);
            Path path;
            if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
                Set<BlockPos> approaches = loadedApproaches(level, candidate);
                if (approaches.isEmpty()) {
                    continue;
                }
                path = this.golem.getNavigation().createPath(approaches, 0);
            } else {
                path = this.golem.getNavigation().createPath(
                        candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D, 0
                );
            }
            pathAttempts++;
            if (path != null && path.canReach()) {
                this.target = candidate;
                this.targetPath = path;
                return true;
            }
            this.failedUntil.put(candidate, now + FAILED_PATH_TTL);
        }
        if (this.crops.isEmpty()) {
            int delayCount = MAX_FAILED_SCAN_DELAY - MIN_FAILED_SCAN_DELAY + 1;
            this.nextScanTick = now + MIN_FAILED_SCAN_DELAY + Math.floorMod(this.golem.getId(), delayCount);
        }
        return false;
    }

    private static boolean isInTargetRange(
            BlockPos origin, BlockPos pos, int horizontalRange, int verticalRange
    ) {
        long dx = (long) pos.getX() - origin.getX();
        int dy = pos.getY() - origin.getY();
        long dz = (long) pos.getZ() - origin.getZ();

        return Math.abs(dy) <= verticalRange
                && dx * dx + dz * dz <= (long) horizontalRange * horizontalRange;
    }

    private static Set<BlockPos> loadedApproaches(ServerLevel level, BlockPos pos) {
        Set<BlockPos> approaches = new HashSet<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos stand = pos.relative(direction);
            if (level.getChunkSource().hasChunk(stand.getX() >> 4, stand.getZ() >> 4)) {
                approaches.add(stand);
            }
        }
        return approaches;
    }

    @Override
    public void start() {
        if (!this.isWithinReach()
                && !this.golem.getNavigation().moveTo(this.targetPath, StrawGolemEntity.WALK_SPEED)) {
            this.stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null;
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.target == null || !(this.golem.level() instanceof ServerLevel level)) {
            return;
        }

        long elapsed = this.golem.getHarvestElapsedTicks();
        if (elapsed < 0L) {
            if (this.golem.hasCargo() || !StrawGolemHarvestHelper.isHarvestable(level, this.target)) {
                this.stop();
                return;
            }

            if (!this.isWithinReach()) {
                if (this.golem.getNavigation().isDone()) {
                    this.stop();
                }
                return;
            }

            this.golem.getNavigation().stop();
            this.golem.startHarvestAnimation();
            elapsed = 0L;
        }

        if (elapsed >= StrawGolemEntity.HARVEST_DURATION_TICKS) {
            this.stop();
            return;
        }

        Vec3 center = Vec3.atCenterOf(this.target);
        this.golem.getLookControl().setLookAt(center.x, center.y, center.z, 180.0F, 180.0F);

        if (elapsed == StrawGolemEntity.HARVEST_CONTACT_TICK && this.isWithinReach()) {
            StrawGolemHarvestHelper.harvest(level, this.golem, this.target);
        }
    }

    private boolean isWithinReach() {
        return new AABB(this.target).distanceToSqr(this.golem.getEyePosition()) <= REACH_SQUARED;
    }

    @Override
    public void stop() {
        if (this.target == null) {
            return;
        }

        this.golem.getNavigation().stop();
        this.golem.stopHarvestAnimation();
        this.target = null;
        this.targetPath = null;
    }
}
