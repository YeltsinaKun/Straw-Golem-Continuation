package xubache.com.strawgolem.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import xubache.com.strawgolem.Entity.StrawGolemEntity;

public class StrawGolemModel extends EntityModel<StrawGolemRenderState> {
    private final ModelPart head;
    private final ModelPart strawHat;
    private final KeyframeAnimation legsWalk;
    private final KeyframeAnimation armsWalk;
    private final KeyframeAnimation legsIdle;
    private final KeyframeAnimation armsIdle;
    private final KeyframeAnimation legsRun;
    private final KeyframeAnimation armsRun;
    private final KeyframeAnimation armsScared;
    private final KeyframeAnimation harvestItem;
    private final KeyframeAnimation armsHoldItem;
    private final KeyframeAnimation armsHoldBlock;


    public StrawGolemModel(ModelPart root) {
        super(root);
        ModelPart upper = root.getChild("upper");
        this.head = upper.getChild("head");
        this.strawHat = this.head.getChild("hat");

        upper.getChild("body").getChild("barrel").visible = false;

        ModelPart headPart = this.head.getChild("part");
        headPart.getChild("dynamicsnow").visible = false;
        headPart.getChild("snow").visible = false;

        this.legsWalk = StrawGolemAnimations.LEGS_WALK.bake(root);
        this.armsWalk = StrawGolemAnimations.ARMS_WALK.bake(root);
        this.legsIdle = StrawGolemAnimations.LEGS_IDLE.bake(root);
        this.armsIdle = StrawGolemAnimations.ARMS_IDLE.bake(root);
        this.legsRun = StrawGolemAnimations.LEGS_RUN.bake(root);
        this.armsRun = StrawGolemAnimations.ARMS_RUN.bake(root);
        this.armsScared = StrawGolemAnimations.ARMS_SCARED.bake(root);
        this.harvestItem = StrawGolemAnimations.HARVEST_ITEM.bake(root);
        this.armsHoldItem = StrawGolemAnimations.ARMS_HOLD_ITEM.bake(root);
        this.armsHoldBlock = StrawGolemAnimations.ARMS_HOLD_BLOCK.bake(root);
    }

    @Override
    public void setupAnim(StrawGolemRenderState state) {
        super.setupAnim(state);
        this.strawHat.visible = state.hasStrawHat;
        float harvestTicks = state.harvestTicks;
        boolean holdingItem = !state.heldItem.isEmpty();
        boolean holdingHarvestBlock = holdingItem && state.holdingHarvestBlock;
        boolean playingHarvestItem = harvestTicks >= 0.0F && !holdingHarvestBlock;
        float baseWeight = 1.0F;

        if (playingHarvestItem) {
            float ticksLeft = StrawGolemEntity.HARVEST_DURATION_TICKS - harvestTicks;
            float blend = Math.min(1.0F, Math.min(harvestTicks, ticksLeft) / 3.0F);
            baseWeight = 1.0F - Math.max(0.0F, blend);
        }

        float walkWeight = state.walkBlend * baseWeight;
        float runWeight = state.runBlend * baseWeight;
        float idleWeight = Math.max(0.0F, baseWeight - walkWeight - runWeight);

        float freeArmWeight = holdingItem ? 0.0F : 1.0F;
        float scaredWeight = state.scaredBlend * baseWeight * freeArmWeight;
        float normalArmWeight = (1.0F - state.scaredBlend) * freeArmWeight;

        if (state.walkAnimationState.isStarted() && walkWeight > 0.0F) {
            long walkTime = state.walkAnimationState.getTimeInMillis(
                    state.ageInTicks
            );

            this.legsWalk.apply(walkTime, walkWeight);
            this.armsWalk.apply(walkTime, walkWeight * normalArmWeight);
        }
        if (state.runAnimationState.isStarted() && runWeight > 0.0F) {
            long runTime = state.runAnimationState.getTimeInMillis(
                    state.ageInTicks
            );

            this.legsRun.apply(runTime, runWeight);
            this.armsRun.apply(runTime, runWeight * normalArmWeight);
        }

        if (state.idleAnimationState.isStarted() && idleWeight > 0.0F) {
            long idleTime = state.idleAnimationState.getTimeInMillis(
                    state.ageInTicks
            );

            this.legsIdle.apply(idleTime, idleWeight);
            this.armsIdle.apply(idleTime, idleWeight * normalArmWeight);
        }

        if (state.scaredAnimationState.isStarted() && scaredWeight > 0.0F) {
            long scaredTime = state.scaredAnimationState.getTimeInMillis(
                    state.ageInTicks
            );

            this.armsScared.apply(scaredTime, scaredWeight);
        }

        if (playingHarvestItem) {
            this.harvestItem.apply(
                    StrawGolemAnimations.harvestClipMillis(harvestTicks), 1.0F
            );
        } else if (holdingItem) {
            KeyframeAnimation hold = holdingHarvestBlock ? this.armsHoldBlock : this.armsHoldItem;
            hold.apply((long) (state.ageInTicks * 50.0F), 1.0F);
        }

        if (harvestTicks < 0.0F) {
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD;
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD;
        }
    }

    public void translateToHeldItem(PoseStack pose) {
        this.root().translateAndRotate(pose);
        this.root().getChild("item").translateAndRotate(pose);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        //nửa dưới: tâm toạ độ ở đ cao 5 pixel tính từ ặt đất
        PartDefinition lower = root.addOrReplaceChild(
                "lower",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 19.0F, 0.0F)
        );

        lower.addOrReplaceChild(
                "leftLeg",
                CubeListBuilder.create()
                        .texOffs(8, 43)
                        .addBox(-1.0F, 1.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(2.0F, 1.0F, 0.0F)
        );

        lower.addOrReplaceChild(
                "rightLeg",
                CubeListBuilder.create()
                        .texOffs(8, 43)
                        .addBox(-1.0F, 1.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-2.0F, 1.0F, 0.0F)
        );

        //Nửa trên: giữ cấu trúc cha-con của model gốc
        PartDefinition upper = root.addOrReplaceChild(
                "upper",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 19.0F, 0.0F)
        );

        PartDefinition body = upper.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(16, 30)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 10.0F, 8.0F),
                PartPose.ZERO
        );

        PartDefinition arms = body.addOrReplaceChild(
                "arms",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, -6.0F, 0.0F)
        );

        arms.addOrReplaceChild(
                "rightArm",
                CubeListBuilder.create()
                        .texOffs(0, 39)
                        .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(-5.0F, 0.0F, 0.0F)
        );

        arms.addOrReplaceChild(
                "leftArm",
                CubeListBuilder.create()
                        .texOffs(0, 39)
                        .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(5.0F, 0.0F, 0.0F)
        );

        body.addOrReplaceChild(
                "barrel",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.25F, -8.25F, -4.5F, 8.75F, 10.5F, 8.75F),
                PartPose.ZERO
        );

        PartDefinition head = upper.addOrReplaceChild(
                "head",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, -8.0F, 0.0F)
        );

        PartDefinition part = head.addOrReplaceChild(
                "part",
                CubeListBuilder.create()
                        .texOffs(24, 23)
                        .addBox(-2.0F, -2.9F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.ZERO
        );

        part.addOrReplaceChild(
                "crown",
                CubeListBuilder.create()
                        .texOffs(22, 36)
                        .addBox(0.0F, -4.7F, -2.0F, 1.0F, 0.8F, 0.0F)
                        .texOffs(18, 36)
                        .addBox(-2.0F, -4.9F, -2.0F, 1.0F, 1.0F, 0.0F)
                        .texOffs(16, 35)
                        .addBox(-2.0F, -4.9F, -1.0F, 0.0F, 1.0F, 1.0F)
                        .texOffs(13, 35)
                        .addBox(-2.0F, -4.9F, 1.0F, 0.0F, 1.0F, 1.0F)
                        .texOffs(9, 36)
                        .addBox(-1.0F, -4.9F, 2.0F, 1.0F, 1.0F, 0.0F)
                        .texOffs(7, 36)
                        .addBox(1.0F, -4.9F, 2.0F, 1.0F, 1.0F, 0.0F)
                        .texOffs(5, 35)
                        .addBox(2.0F, -4.9F, 0.0F, 0.0F, 1.0F, 1.0F)
                        .texOffs(3, 35)
                        .addBox(2.0F, -4.9F, -2.0F, 0.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 1.0F, 0.0F)
        );

        PartDefinition dynamicSnow = part.addOrReplaceChild(
                "dynamicsnow",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.55F, -0.5F, -1.55F, 5.1F, 1.0F, 5.1F),
                PartPose.offset(0.0F, -4.4F, -1.0F)
        );

        dynamicSnow.addOrReplaceChild(
                "locator",
                CubeListBuilder.create()
                        .texOffs(0, 40)
                        .addBox(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
                PartPose.offset(0.0F, 0.4F, -2.0F)
        );

        part.addOrReplaceChild(
                "snow",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .mirror()
                        .addBox(3.95F, -15.601F, 3.3F, 1.0F, 0.1F, 1.0F)
                        .addBox(2.95F, -15.601F, -3.7F, 1.0F, 0.1F, 1.0F)
                        .mirror(false)
                        .addBox(-4.95F, -15.601F, 3.3F, 1.0F, 0.1F, 1.0F)
                        .addBox(-3.95F, -15.601F, -3.7F, 1.0F, 0.1F, 1.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F)
        );

        head.addOrReplaceChild(
                "hat",
                CubeListBuilder.create()
                        // The thin brim uses the mirrored UV layout from Blockbench.
                        .texOffs(-11, 21)
                        .mirror()
                        .addBox(-5.5F, -2.52F, -5.5F, 11.0F, 0.02F, 11.0F)
                        .mirror(false)
                        .texOffs(28, 13)
                        .addBox(-2.5F, -4.5F, -2.5F, 5.0F, 2.0F, 5.0F)
                        // Rơm nhô ra khỏi mép vành.
                        .texOffs(22, 40)
                        .addBox(-1.6F, -2.54F, -6.8F, 0.55F, 0.08F, 1.55F)
                        .addBox(1.1F, -2.52F, -6.35F, 0.4F, 0.05F, 1.1F)
                        .addBox(1.6F, -2.55F, 5.25F, 0.65F, 0.09F, 1.35F)
                        .addBox(5.25F, -2.53F, -1.0F, 1.45F, 0.07F, 0.6F)
                        .addBox(-6.7F, -2.53F, 0.6F, 1.45F, 0.07F, 0.55F)
                        .addBox(5.25F, -2.56F, 2.3F, 1.85F, 0.08F, 0.42F)
                        .addBox(-2.65F, -2.53F, 5.25F, 0.45F, 0.05F, 0.95F)
                        .addBox(-6.57F, -2.55F, -2.65F, 1.32F, 0.075F, 0.5F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "item",
                CubeListBuilder.create()
                        .texOffs(0, 40)
                        .addBox(-0.5F, 0.5F, -0.5F, 0.0F, 0.0F, 0.0F),
                PartPose.offset(0.0F, 15.5F, -5.5F)
        );

        return LayerDefinition.create(mesh, 48, 48);
    }
}
