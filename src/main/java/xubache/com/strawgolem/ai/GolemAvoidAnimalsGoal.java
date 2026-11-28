package xubache.com.strawgolem.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.phys.Vec3;
import xubache.com.strawgolem.Entity.StrawGolemEntity;

public final class GolemAvoidAnimalsGoal extends AvoidEntityGoal<PathfinderMob> {
    private static final int CHECK_INTERVAL = 5;
    private long nextCheckTick;

    public GolemAvoidAnimalsGoal(StrawGolemEntity golem) {
        super(golem, PathfinderMob.class, NibbleStrawGolemGoal.SEARCH_RANGE,
                StrawGolemEntity.WALK_SPEED, StrawGolemEntity.RUN_SPEED,
                candidate -> canThreaten(golem, candidate));
    }

    @Override
    public boolean canUse() {
        long now = this.mob.level().getGameTime();
        if (now < this.nextCheckTick) {
            return false;
        }
        this.nextCheckTick = now + CHECK_INTERVAL + (this.mob.getId() & 3);
        return super.canUse() && this.path.canReach();
    }

    @Override
    public void stop() {
        super.stop();
        this.pathNav.stop();
        this.path = null;
    }

    private static boolean canThreaten(StrawGolemEntity golem, LivingEntity candidate) {
        if (!(candidate instanceof PathfinderMob animal) || !NibbleStrawGolemGoal.isNibblingAnimal(animal)) {
            return false;
        }

        Vec3 forward = Vec3.directionFromRotation(0.0F, golem.getYHeadRot());
        Vec3 toAnimal = animal.position().subtract(golem.position());
        if (forward.dot(toAnimal) <= 0.0D || !golem.getSensing().hasLineOfSight(animal)) {
            return false;
        }

        // Probe reachability without changing the animal's active navigation.
        var approach = new GroundPathNavigation(animal, animal.level()).createPath(golem, 0);
        return approach != null && approach.canReach();
    }
}
