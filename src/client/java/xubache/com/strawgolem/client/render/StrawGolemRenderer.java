package xubache.com.strawgolem.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xubache.com.strawgolem.Entity.StrawGolemEntity;

public class StrawGolemRenderer extends MobRenderer<
        StrawGolemEntity,
        StrawGolemRenderState,
        StrawGolemModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "straw-golem-relaunched",
                    "textures/straw_golem.png"
            );

    private static final Identifier OLD_TEXTURE = Identifier.fromNamespaceAndPath(
            "straw-golem-relaunched", "textures/straw_golem_old.png"
    );
    private static final Identifier DYING_TEXTURE = Identifier.fromNamespaceAndPath(
            "straw-golem-relaunched", "textures/straw_golem_dying.png"
    );

    public StrawGolemRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new StrawGolemModel(
                        context.bakeLayer(ModEntityModelLayers.STRAW_GOLEM)
                ),
                0.3F
        );
        this.addLayer(new RenderLayer<StrawGolemRenderState, StrawGolemModel>(this) {
            @Override
            public void submit(
                    PoseStack pose, SubmitNodeCollector collector, int light,
                    StrawGolemRenderState state, float yRot, float xRot
            ) {
                if (state.heldItem.isEmpty()) {
                    return;
                }

                pose.pushPose();
                this.getParentModel().translateToHeldItem(pose);
                pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
                pose.scale(0.5F, 0.5F, 0.5F);
                state.heldItem.submit(
                        pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor
                );
                pose.popPose();
            }
        });
    }

    @Override
    public StrawGolemRenderState createRenderState() {
        return new StrawGolemRenderState();
    }

    @Override
    protected boolean isShaking(StrawGolemRenderState state) {
        return super.isShaking(state) || state.shivering;
    }

    @Override
    public Identifier getTextureLocation(StrawGolemRenderState state) {
        return switch (state.textureStage) {
            case StrawGolemEntity.LIFE_STAGE_OLD -> OLD_TEXTURE;
            case StrawGolemEntity.LIFE_STAGE_DYING -> DYING_TEXTURE;
            default -> TEXTURE;
        };
    }

    @Override
    public void extractRenderState(
            StrawGolemEntity entity,
            StrawGolemRenderState state,
            float partialTick

    ) {
        super.extractRenderState(entity, state, partialTick);
        state.hasStrawHat = entity.hasStrawHat();
        state.shivering = entity.isShivering();
        state.textureStage = entity.getTextureStage();
        long elapsed = entity.getHarvestElapsedTicks();
        state.harvestTicks = entity.isAlive() && elapsed >= 0
                && elapsed < StrawGolemEntity.HARVEST_DURATION_TICKS
                ? elapsed + partialTick
                : -1.0F;
        ItemStack cargo = entity.getMainHandItem();
        state.holdingHarvestBlock = cargo.is(Items.PUMPKIN) || cargo.is(Items.MELON);
        this.itemModelResolver.updateForLiving(state.heldItem, cargo, ItemDisplayContext.NONE, entity);

        state.walkAnimationState.copyFrom(entity.walkAnimationState);
        state.idleAnimationState.copyFrom(entity.idleAnimationState);
        state.walkBlend = entity.getWalkBlend(partialTick);
        state.runAnimationState.copyFrom(entity.runAnimationState);
        state.runBlend = entity.getRunBlend(partialTick);
        state.scaredAnimationState.copyFrom(entity.scaredAnimationState);
        state.scaredBlend = entity.getScaredBlend(partialTick);

    }

}
