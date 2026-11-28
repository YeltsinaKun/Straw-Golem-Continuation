package xubache.com.strawgolem.ai;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xubache.com.strawgolem.Entity.StrawGolemEntity;
import xubache.com.strawgolem.mixin.MobGoalSelectorAccessor;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

public final class NibbleStrawGolemGoal extends Goal {
    static final float SEARCH_RANGE = 6.0F;
    private static final int CHECK_INTERVAL = 10;
    private static final double APPROACH_SPEED = 1.0D;
    private static final float BITE_DAMAGE = 0.5F;

    private static final Set<EntityType<?>> NIBBLING_ANIMALS = Set.of(
            EntityType.COW, EntityType.SHEEP, EntityType.MOOSHROOM,
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
            EntityType.LLAMA, EntityType.TRADER_LLAMA
    );

    private final PathfinderMob animal;
    private long nextCheckTick;
    private StrawGolemEntity target;
    private Path approach;

    private NibbleStrawGolemGoal(PathfinderMob animal) {
        this.animal = animal;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!(entity instanceof PathfinderMob animal)
                    || !isNibblingAnimal(animal)) {
                return;
            }

            var goals = ((MobGoalSelectorAccessor) animal).strawgolem$getGoalSelector();
            if (goals.getAvailableGoals().stream()
                    .noneMatch(goal -> goal.getGoal() instanceof NibbleStrawGolemGoal)) {
                goals.addGoal(5, new NibbleStrawGolemGoal(animal));
            }
        });
    }

    static boolean isNibblingAnimal(PathfinderMob animal) {
        return NIBBLING_ANIMALS.contains(animal.getType());
    }

    @Override
    public boolean canUse() {
        if (!(this.animal.level() instanceof ServerLevel level)) {
            return false;
        }

        long now = level.getGameTime();
        if (now < this.nextCheckTick) {
            return false;
        }
        this.nextCheckTick = now + CHECK_INTERVAL + (this.animal.getId() & 7);

        var candidates = level.getEntitiesOfClass(
                StrawGolemEntity.class, this.animal.getBoundingBox().inflate(SEARCH_RANGE),
                this::canApproach
        );
        candidates.sort(Comparator.comparingDouble(this.animal::distanceToSqr));

        for (StrawGolemEntity golem : candidates) {
            if (this.canBite(golem)) {
                this.target = golem;
                return true;
            }

            Path path = this.animal.getNavigation().createPath(golem, 0);
            if (path != null && path.canReach()) {
                this.target = golem;
                this.approach = path;
                return true;
            }
        }
        return false;
    }

    private boolean canBite(StrawGolemEntity golem) {
        if (!this.animal.isWithinMeleeAttackRange(golem)) {
            return false;
        }

        double biteHeight = Math.min(this.animal.getBbHeight(), golem.getBbHeight()) * 0.5D;
        Vec3 from = this.animal.position().add(0.0D, biteHeight, 0.0D);
        Vec3 to = golem.position().add(0.0D, biteHeight, 0.0D);
        return this.animal.level().clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.animal
        )).getType() == HitResult.Type.MISS;
    }

    private boolean canApproach(StrawGolemEntity golem) {
        return golem.isAlive()
                && golem.level() == this.animal.level()
                && !golem.isPanicRunning()
                && this.animal.distanceToSqr(golem) <= SEARCH_RANGE * SEARCH_RANGE
                && this.animal.getSensing().hasLineOfSight(golem);
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && this.canApproach(this.target)
                && (!this.animal.getNavigation().isDone()
                || this.canBite(this.target));
    }

    @Override
    public void start() {
        if (this.approach != null && !this.animal.getNavigation().moveTo(this.approach, APPROACH_SPEED)) {
            this.stop();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(this.animal.level() instanceof ServerLevel level) || !this.canContinueToUse()) {
            this.stop();
            return;
        }

        this.animal.getLookControl().setLookAt(this.target);
        if (this.canBite(this.target)) {
            this.target.hurtServer(level, this.animal.damageSources().mobAttack(this.animal), BITE_DAMAGE);
            this.stop();
        }
    }

    @Override
    public void stop() {
        if (this.target != null) {
            this.animal.getNavigation().stop();
            this.target = null;
            this.approach = null;
        }
    }
}
