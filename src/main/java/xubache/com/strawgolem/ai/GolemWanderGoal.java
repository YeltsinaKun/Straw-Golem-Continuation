package xubache.com.strawgolem.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import xubache.com.strawgolem.Entity.StrawGolemEntity;
import xubache.com.strawgolem.config.StrawGolemConfig;

public final class GolemWanderGoal extends WaterAvoidingRandomStrollGoal {
    private final StrawGolemEntity golem;
    private BlockPos startPos;

    public GolemWanderGoal(StrawGolemEntity golem, double speedModifier) {
        super(golem, speedModifier);
        this.golem = golem;
    }

    @Override
    public boolean canUse() {
        return !this.golem.hasCargo() && super.canUse();
    }

    @Override
    public void start() {
        this.startPos = this.golem.blockPosition();
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        int maxDistance = StrawGolemConfig.get().attributes.wanderMaxDistance;
        return super.canContinueToUse()
                && this.golem.blockPosition().distManhattan(this.startPos) < maxDistance;
    }
}
