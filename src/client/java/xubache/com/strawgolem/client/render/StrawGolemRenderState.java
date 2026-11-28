package xubache.com.strawgolem.client.render;

import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class StrawGolemRenderState extends HoldingEntityRenderState {
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState runAnimationState = new AnimationState();
    public final AnimationState scaredAnimationState = new AnimationState();
    public float runBlend;
    public float walkBlend;
    public float harvestTicks = -1.0F;
    public float scaredBlend;
    public int textureStage;
    public boolean shivering;
    public boolean hasStrawHat;
    public boolean holdingHarvestBlock;
}
