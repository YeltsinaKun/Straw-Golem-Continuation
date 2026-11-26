package xubache.com.strawgolem.client.render;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import xubache.com.strawgolem.Entity.StrawGolemEntity;

public class StrawGolemAnimations {

    private static final float[] WALK_TIMES = {
            0.0F, 0.25F, 0.5F, 0.75F, 1.0F
    };

    private static final float[] RUN_TIMES = {
            0.0F, 0.0833F, 0.1667F, 0.25F,
            0.3333F, 0.4167F, 0.5F, 0.5833F,
            0.6667F, 0.75F, 0.8333F, 0.9167F, 1.0F
    };

    private static Keyframe rotation(
            float time, float x, float y, float z
    ) {
        return new Keyframe(
                time,
                KeyframeAnimations.degreeVec(x, y, z),
                AnimationChannel.Interpolations.LINEAR
        );
    }

    private static Keyframe position(
            float time, float x, float y, float z
    ) {
        return new Keyframe(
                time,
                KeyframeAnimations.posVec(x, y, z),
                AnimationChannel.Interpolations.LINEAR
        );
    }

    private static float phaseValue(int index, float amount) {
        return switch (index % 4) {
            case 1 -> amount;
            case 3 -> -amount;
            default -> 0.0F;
        };
    }

    private static AnimationChannel swingX(
            float angle, float[] times
    ) {
        return swingXZ(angle, 0.0F, times);
    }

    private static AnimationChannel swingXZ(
            float angle, float zAngle, float[] times
    ) {
        Keyframe[] frames = new Keyframe[times.length];

        for (int i = 0; i < times.length; i++) {
            frames[i] = rotation(
                    times[i],
                    phaseValue(i, angle),
                    0.0F,
                    zAngle
            );
        }

        return new AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                frames
        );
    }

    private static AnimationChannel bobY(
            float amount, float[] times
    ) {
        Keyframe[] frames = new Keyframe[times.length];

        for (int i = 0; i < times.length; i++) {
            frames[i] = position(
                    times[i],
                    0.0F,
                    phaseValue(i, amount),
                    0.0F
            );
        }

        return new AnimationChannel(
                AnimationChannel.Targets.POSITION,
                frames
        );
    }

    private static AnimationChannel idleArmZ(float angle) {
        return new AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                rotation(0.0F, 0.0F, 0.0F, 0.0F),
                rotation(0.5F, 0.0F, 0.0F, angle),
                rotation(1.0F, 0.0F, 0.0F, 0.0F),
                rotation(1.5F, 0.0F, 0.0F, angle),
                rotation(2.0F, 0.0F, 0.0F, 0.0F)
        );
    }

    public static final AnimationDefinition LEGS_IDLE =
            AnimationDefinition.Builder.withLength(2.0F)
                    .looping()
                    .addAnimation("body", new AnimationChannel(
                            AnimationChannel.Targets.POSITION,
                            position(0.0F, 0.0F, 0.0F, 0.0F),
                            position(1.0F, 0.0F, 0.1F, 0.0F),
                            position(2.0F, 0.0F, 0.0F, 0.0F)
                    ))
                    .build();

    public static final AnimationDefinition ARMS_IDLE =
            AnimationDefinition.Builder.withLength(2.0F)
                    .looping()
                    .addAnimation("arms", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            rotation(0.0F, 0.0F, 0.0F, 0.0F),
                            rotation(0.25F, -3.0F, 0.0F, 0.0F),
                            rotation(1.75F, 3.0F, 0.0F, 0.0F),
                            rotation(2.0F, 0.0F, 0.0F, 0.0F)
                    ))
                    .addAnimation("rightArm", idleArmZ(2.0F))
                    .addAnimation("leftArm", idleArmZ(-2.0F))
                    .build();

    public static final AnimationDefinition LEGS_WALK =
            AnimationDefinition.Builder.withLength(1.0F)
                    .looping()
                    .addAnimation(
                            "rightLeg", swingX(25.0F, WALK_TIMES)
                    )
                    .addAnimation(
                            "leftLeg", swingX(-25.0F, WALK_TIMES)
                    )
                    .addAnimation(
                            "body", bobY(0.08F, WALK_TIMES)
                    )
                    .addAnimation(
                            "head", bobY(-0.04F, WALK_TIMES)
                    )
                    .build();

    public static final AnimationDefinition ARMS_WALK =
            AnimationDefinition.Builder.withLength(1.0F)
                    .looping()
                    .addAnimation(
                            "rightArm", swingX(-15.0F, WALK_TIMES)
                    )
                    .addAnimation(
                            "leftArm", swingX(15.0F, WALK_TIMES)
                    )
                    .build();

    public static final AnimationDefinition LEGS_RUN =
            AnimationDefinition.Builder.withLength(1.0F)
                    .looping()
                    .addAnimation(
                            "rightLeg", swingX(35.0F, RUN_TIMES)
                    )
                    .addAnimation(
                            "leftLeg", swingX(-35.0F, RUN_TIMES)
                    )
                    .addAnimation(
                            "body", bobY(0.1F, RUN_TIMES)
                    )
                    .addAnimation(
                            "head", bobY(-0.04F, RUN_TIMES)
                    )
                    .build();

    public static final AnimationDefinition ARMS_RUN =
            AnimationDefinition.Builder.withLength(1.0F)
                    .looping()
                    .addAnimation(
                            "rightArm", swingX(-22.5F, RUN_TIMES)
                    )
                    .addAnimation(
                            "leftArm", swingX(22.5F, RUN_TIMES)
                    )
                    .build();

    public static final AnimationDefinition ARMS_SCARED =
            AnimationDefinition.Builder.withLength(1.0F)
                    .looping()
                    .addAnimation("arms", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            rotation(0.0F, -180.0F, 0.0F, 0.0F),
                            rotation(1.0F, -180.0F, 0.0F, 0.0F)
                    ))
                    .addAnimation(
                            "rightArm",
                            swingXZ(20.0F, 12.5F, RUN_TIMES)
                    )
                    .addAnimation(
                            "leftArm",
                            swingXZ(-20.0F, -12.5F, RUN_TIMES)
                    )
                    .build();
    private static final float HARVEST_CLIP_SECONDS = 1.3333F;
    private static final float HARVEST_LIFT_SECONDS = 0.7083F;

    public static long harvestClipMillis(float elapsedTicks) {
        float contact = StrawGolemEntity.HARVEST_CONTACT_TICK;
        if (elapsedTicks <= contact) {
            return Math.round(1000.0F * HARVEST_LIFT_SECONDS * elapsedTicks / contact);
        }
        float progress = (elapsedTicks - contact)
                / (StrawGolemEntity.HARVEST_DURATION_TICKS - contact);
        float seconds = HARVEST_LIFT_SECONDS
                + (HARVEST_CLIP_SECONDS - HARVEST_LIFT_SECONDS) * progress;
        return Math.round(1000.0F * seconds);
    }

    private static AnimationChannel rotations(Keyframe... frames) {
        return new AnimationChannel(AnimationChannel.Targets.ROTATION, frames);
    }

    private static AnimationChannel positions(Keyframe... frames) {
        return new AnimationChannel(AnimationChannel.Targets.POSITION, frames);
    }

    public static final AnimationDefinition HARVEST_ITEM =
            AnimationDefinition.Builder.withLength(HARVEST_CLIP_SECONDS)
                    .addAnimation("head", rotations(
                            rotation(0.0F, 0.0F, 0.0F, 0.0F),
                            rotation(0.25F, 0.0F, 0.0F, 0.0F),
                            rotation(0.4583F, 12.5F, 0.0F, 0.0F),
                            rotation(0.625F, 17.5F, 0.0F, 0.0F),
                            rotation(HARVEST_LIFT_SECONDS, 17.5F, 0.0F, 0.0F),
                            rotation(0.875F, 12.5F, 0.0F, 0.0F),
                            rotation(HARVEST_CLIP_SECONDS, 0.0F, 0.0F, 0.0F)
                    ))
                    .addAnimation("rightArm", rotations(
                            rotation(0.0F, 0.0F, 0.0F, 0.0F),
                            rotation(0.25F, 0.0F, 0.0F, 0.0F),
                            rotation(0.5F, 0.0F, -5.0F, -7.5F),
                            rotation(0.625F, 0.0F, -5.0F, -12.5F),
                            rotation(HARVEST_LIFT_SECONDS, 0.0F, -5.0F, -12.5F),
                            rotation(0.8333F, 0.0F, -5.0F, -12.5F)
                    ))
                    .addAnimation("rightArm", positions(
                            position(0.5F, -0.15F, 0.0F, 0.0F),
                            position(0.625F, -0.15F, 0.15F, -0.1F),
                            position(HARVEST_LIFT_SECONDS, -0.15F, 0.15F, -0.1F),
                            position(0.8333F, -0.15F, 0.15F, -0.1F)
                    ))
                    .addAnimation("leftArm", rotations(
                            rotation(0.0F, 0.0F, 0.0F, 0.0F),
                            rotation(0.25F, 0.0F, 0.0F, 0.0F),
                            rotation(0.5F, 0.0F, 5.0F, 7.5F),
                            rotation(0.625F, 0.0F, 5.0F, 12.5F),
                            rotation(HARVEST_LIFT_SECONDS, 0.0F, 5.0F, 12.5F),
                            rotation(0.8333F, 0.0F, 5.0F, 12.5F)
                    ))
                    .addAnimation("leftArm", positions(
                            position(0.5F, 0.15F, 0.0F, 0.0F),
                            position(0.625F, 0.15F, -0.15F, -0.1F),
                            position(HARVEST_LIFT_SECONDS, 0.15F, -0.15F, -0.1F),
                            position(0.8333F, 0.15F, -0.15F, -0.1F)
                    ))
                    .addAnimation("upper", rotations(
                            rotation(0.0F, 0.0F, 0.0F, 0.0F),
                            rotation(0.4583F, 27.5F, 0.0F, 0.0F),
                            rotation(0.625F, 32.5F, 0.0F, 0.0F),
                            rotation(HARVEST_LIFT_SECONDS, 32.5F, 0.0F, 0.0F),
                            rotation(0.875F, 27.5F, 0.0F, 0.0F),
                            rotation(HARVEST_CLIP_SECONDS, 0.0F, 0.0F, 0.0F)
                    ))
                    .addAnimation("upper", positions(
                            position(0.0F, 0.0F, 0.0F, 0.0F),
                            position(0.4583F, 0.0F, -0.25F, -0.25F),
                            position(0.875F, 0.0F, -0.25F, -0.25F),
                            position(HARVEST_CLIP_SECONDS, 0.0F, 0.0F, 0.0F)
                    ))
                    .addAnimation("arms", rotations(
                            rotation(0.0F, 0.0F, 0.0F, 0.0F),
                            rotation(0.5F, -50.0F, 0.0F, 0.0F)
                    ))
                    .addAnimation("arms", positions(
                            position(0.0F, 0.0F, 0.0F, 0.0F),
                            position(0.5F, 0.0F, -0.25F, -0.75F)
                    ))
                    .addAnimation("item", rotations(
                            rotation(0.0F, 0.0F, 0.0F, 0.01F)
                    ))
                    .addAnimation("item", positions(
                            position(HARVEST_LIFT_SECONDS, 0.0F, -5.0F, 0.0F),
                            position(0.9583F, 0.0F, -4.0F, -1.0F),
                            position(1.0F, 0.0F, -3.82093F, -0.95523F),
                            position(1.0417F, 0.0F, -3.41424F, -0.85356F),
                            position(1.0833F, 0.0F, -2.88831F, -0.72208F),
                            position(1.125F, 0.0F, -2.30228F, -0.57557F),
                            position(1.1667F, 0.0F, -1.69772F, -0.42443F),
                            position(1.2083F, 0.0F, -1.11169F, -0.27792F),
                            position(1.25F, 0.0F, -0.58576F, -0.14644F),
                            position(1.2917F, 0.0F, -0.17907F, -0.04477F),
                            position(HARVEST_CLIP_SECONDS, 0.0F, 0.0F, 0.0F)
                    ))
                    .build();

    public static final AnimationDefinition ARMS_HOLD_ITEM =
            AnimationDefinition.Builder.withLength(1.0F)
                    .looping()
                    .addAnimation("rightArm", rotations(
                            rotation(0.0F, 0.0F, -5.0F, -12.5F)
                    ))
                    .addAnimation("rightArm", positions(
                            position(0.0F, -0.15F, 0.15F, -0.1F)
                    ))
                    .addAnimation("leftArm", rotations(
                            rotation(0.0F, 0.0F, 5.0F, 12.5F)
                    ))
                    .addAnimation("leftArm", positions(
                            position(0.0F, 0.15F, -0.15F, -0.1F)
                    ))
                    .addAnimation("arms", rotations(
                            rotation(0.0F, -50.0F, 0.0F, 0.0F),
                            rotation(0.5F, -48.0F, 0.0F, 0.0F),
                            rotation(1.0F, -50.0F, 0.0F, 0.0F)
                    ))
                    .addAnimation("arms", positions(
                            position(0.0F, 0.0F, -0.25F, -0.75F),
                            position(0.5F, 0.0F, -0.25F, -0.75F),
                            position(1.0F, 0.0F, -0.25F, -0.75F)
                    ))
                    .addAnimation("item", positions(
                            position(0.0F, 0.0F, 0.0F, -0.1F),
                            position(0.4F, 0.0F, -0.1F, 0.0F),
                            position(1.0F, 0.0F, 0.0F, -0.1F)
                    ))
                    .build();

    private static Keyframe uniformScale(float time, float scale) {
        return new Keyframe(
                time,
                KeyframeAnimations.scaleVec(scale, scale, scale),
                AnimationChannel.Interpolations.LINEAR
        );
    }

    public static final AnimationDefinition ARMS_HOLD_BLOCK =
            AnimationDefinition.Builder.withLength(1.0F)
                    .looping()
                    .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.SCALE,
                            uniformScale(0.0F, 0.96F),
                            uniformScale(0.5F, 0.95F),
                            uniformScale(1.0F, 0.96F)
                    ))
                    .addAnimation("arms", positions(
                            position(0.0F, 0.0F, 0.15F, 0.0F),
                            position(0.5F, 0.0F, 0.1F, 0.0F),
                            position(1.0F, 0.0F, 0.15F, 0.0F)
                    ))
                    .addAnimation("arms", rotations(
                            rotation(0.0F, -180.0F, 0.0F, 0.0F),
                            rotation(1.0F, -180.0F, 0.0F, 0.0F)
                    ))
                    .addAnimation("rightArm", positions(
                            position(0.0F, 0.25F, 0.15F, -0.1F),
                            position(1.0F, 0.25F, 0.15F, -0.1F)
                    ))
                    .addAnimation("rightArm", rotations(
                            rotation(0.0F, 0.0F, 0.0F, -9.0F),
                            rotation(1.0F, 0.0F, 0.0F, -9.0F)
                    ))
                    .addAnimation("leftArm", positions(
                            position(0.0F, -0.25F, -0.15F, -0.1F),
                            position(1.0F, -0.25F, -0.15F, -0.1F)
                    ))
                    .addAnimation("leftArm", rotations(
                            rotation(0.0F, 0.0F, 0.0F, 9.0F),
                            rotation(1.0F, 0.0F, 0.0F, 9.0F)
                    ))
                    .addAnimation("item", positions(
                            position(0.0F, 0.0F, 11.0F, 5.5F),
                            position(0.5F, 0.0F, 10.9F, 5.5F),
                            position(1.0F, 0.0F, 11.0F, 5.5F)
                    ))
                    .build();
}
