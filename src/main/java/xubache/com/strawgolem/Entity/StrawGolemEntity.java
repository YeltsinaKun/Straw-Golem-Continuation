package xubache.com.strawgolem.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import xubache.com.strawgolem.ai.*;
import xubache.com.strawgolem.config.StrawGolemConfig;
import net.minecraft.core.GlobalPos;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class StrawGolemEntity extends PathfinderMob {

    public static final double WALK_SPEED = 0.5D;
    public static final double RUN_SPEED = 0.8D;

    public static final int LIFE_STAGE_OLD = 1;
    public static final int LIFE_STAGE_DYING = 2;

    private static final int LIFE_STAGE_NORMAL = 0;
    private static final String LIFE_AGE_TAG = "LifeAgeTicks";
    private static final String MOISTURE_TAG = "MoistureTicks";
    private static final int SOAKED_THRESHOLD_TICKS = 100;
    private static final int MAX_MOISTURE_TICKS = 200;

    private static final EntityDataAccessor<Boolean> SHIVERING =
            SynchedEntityData.defineId(StrawGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private long lifeAgeTicks;
    private int moistureTicks;


    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState scaredAnimationState = new AnimationState();

    private float walkBlend = 0.0F;
    private float walkBlendOld = 0.0F;
    private float runBlend = 0.0F;
    private float runBlendOld = 0.0F;
    private PanicGoal panicGoal;
    private int panicGraceTicks;
    private float scaredBlend;
    private float scaredBlendOld;
    private static final String LEGACY_INVENTORY_TAG = "GolemInventory";
    private static final String PENDING_LEGACY_DROPS_TAG = "PendingLegacyDrops";
    private static final String PENDING_DROPS_TAG = "PendingDrops";
    private static final int DEFAULT_PICKUP_DELAY = 10;
    private static final int SEED_CAPACITY = 4;
    private static final List<Item> SEED_TYPES = List.of(
            Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS
    );
    private final Map<Item, Integer> seedReserve = new HashMap<>();
    private final List<ItemStack> surplusSeeds = new ArrayList<>();
    private boolean seedsForStorage;
    private GlobalPos compostSite;
    private GlobalPos depositStorage;
    private final List<PendingDrop> pendingDrops = new ArrayList<>();

    private record PendingDrop(ItemStack stack, int pickupDelay) {
        private static final Codec<PendingDrop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("stack").forGetter(PendingDrop::stack),
                Codec.intRange(0, 32767).fieldOf("pickup_delay").forGetter(PendingDrop::pickupDelay)
        ).apply(instance, PendingDrop::new));
    }



    public static final int HARVEST_CONTACT_TICK = 20;
    public static final int HARVEST_DURATION_TICKS = 39;

    private static final EntityDataAccessor<Long> HARVEST_START_TICK =
            SynchedEntityData.defineId(
                    StrawGolemEntity.class, EntityDataSerializers.LONG
            );

    private static final EntityDataAccessor<Boolean> PANIC_RUNNING =
            SynchedEntityData.defineId(
                    StrawGolemEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    public final AnimationState runAnimationState = new AnimationState();

    //constructor
    public StrawGolemEntity(
            EntityType<? extends StrawGolemEntity> entityType,
            Level level
    ) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.panicGoal = new PanicGoal(this, RUN_SPEED) {
            private int panicEndTick;
            private int nextPositionAttempt;

            @Override
            public void start() {
                super.start();

                this.panicEndTick = this.mob.tickCount + 120;
                this.nextPositionAttempt = this.mob.tickCount;

                StrawGolemEntity golem = StrawGolemEntity.this;
                if (golem.level() instanceof ServerLevel level
                        && golem.hasCargo()
                        && golem.dropStack(level, golem.getCargo(), DEFAULT_PICKUP_DELAY)) {
                    golem.setCargo(ItemStack.EMPTY);
                }
            }

            @Override
            public boolean canContinueToUse() {
                return this.mob.isAlive()
                        && this.mob.tickCount < this.panicEndTick;
            }

            @Override
            public void tick() {
                super.tick();

                if (!this.mob.getNavigation().isDone()
                        || this.mob.tickCount < this.nextPositionAttempt) {
                    return;
                }

                this.nextPositionAttempt = this.mob.tickCount + 10;

                // Thử chọn đích theo PanicGoal gốc trước,
                // nếu không được thì tìm một điểm chạy tiếp.
                if (super.canUse() || this.findRandomPosition()) {
                    this.mob.getNavigation().moveTo(
                            this.posX,
                            this.posY,
                            this.posZ,
                            this.speedModifier
                    );
                }
            }

            @Override
            public void stop() {
                super.stop();
                this.mob.getNavigation().stop();
            }
        };
        this.goalSelector.addGoal(0, this.panicGoal);
        this.goalSelector.addGoal(1, new GolemAvoidAnimalsGoal(this));
        this.goalSelector.addGoal(2, new GolemHarvestGoal(this));
        this.goalSelector.addGoal(2, new GolemGrabGoal(this));
        this.goalSelector.addGoal(2, new GolemCompostGoal(this));
        this.goalSelector.addGoal(3, new GolemDepositGoal(this));
        this.goalSelector.addGoal(4, new GolemWanderGoal(this, WALK_SPEED));

        LookAtPlayerGoal lookAtPlayer = new LookAtPlayerGoal(this, Player.class, 6.0F);
        // Yield to movement goals even when they do not claim LOOK.
        lookAtPlayer.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.goalSelector.addGoal(5, lookAtPlayer);
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.CAMPFIRE)
                || source.is(DamageTypes.LAVA)) {
            amount *= 2.0F;
        }

        return super.hurtServer(level, source, amount);
    }

    @Override
    public void tick() {
        if (this.level() instanceof ServerLevel serverLevel && this.isAlive()) {
            this.tickLifespan(serverLevel);
            if (this.isAlive() && !this.hasCargo() && !this.surplusSeeds.isEmpty()
                    && !this.isPanicRunning()) {
                this.setCargo(this.surplusSeeds.removeFirst());
            }
        }

        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            this.entityData.set(SHIVERING, this.isAlive() && this.shouldShiver(serverLevel));
            if (!this.pendingDrops.isEmpty()) {
                this.dropPendingItems(serverLevel);
            }
            boolean goalRunning = this.panicGoal != null && this.panicGoal.isRunning();

            if (goalRunning) {
                this.panicGraceTicks = 6;
            } else if (this.panicGraceTicks > 0) {
                this.panicGraceTicks--;
            }

            this.entityData.set(
                    PANIC_RUNNING,
                    goalRunning || this.panicGraceTicks > 0
            );
        } else {
            boolean moving = this.getDeltaMovement().horizontalDistance() > 0.0D;
            boolean running = moving && this.isPanicRunning();
            boolean walking = moving && !running;
            boolean panicking = this.isPanicRunning();

            this.walkBlendOld = this.walkBlend;
            this.runBlendOld = this.runBlend;
            this.scaredBlendOld = this.scaredBlend;

            this.walkBlend = Mth.approach(
                    this.walkBlend,
                    walking ? 1.0F : 0.0F,
                    0.25F
            );

            this.runBlend = Mth.approach(
                    this.runBlend,
                    running ? 1.0F : 0.0F,
                    0.25F
            );

            this.scaredBlend = Mth.approach(
                    this.scaredBlend,
                    panicking ? 1.0F : 0.0F,
                    0.25F
            );

            this.walkAnimationState.animateWhen(walking || this.walkBlendOld > 0.0F,
                    this.tickCount
            );
            this.runAnimationState.animateWhen(running || this.runBlendOld > 0.0F,
                    this.tickCount);
            this.idleAnimationState.animateWhen(!moving || this.walkBlendOld + this.runBlendOld < 1.0F,
                    this.tickCount
            );
            this.scaredAnimationState.animateWhen(panicking || this.scaredBlendOld > 0.0F,
                    this.tickCount
            );
        }
    }

    private void tickLifespan(ServerLevel level) {
        long lifespanTicks = StrawGolemConfig.get().getLifespanTicks();
        if (lifespanTicks == 0L) {
            return;
        }

        if (this.lifeAgeTicks < lifespanTicks) {
            if (this.isInWater() || this.isExposedToRain(level)) {
                this.moistureTicks = Math.min(MAX_MOISTURE_TICKS, this.moistureTicks + 1);
            } else {
                this.moistureTicks = Math.max(0, this.moistureTicks - 1);
            }

            int ageStep = this.moistureTicks >= SOAKED_THRESHOLD_TICKS ? 2 : 1;
            long previousWear = this.wornHealthPoints(this.lifeAgeTicks, lifespanTicks);
            this.lifeAgeTicks = Math.min(lifespanTicks, this.lifeAgeTicks + ageStep);
            long lost = this.wornHealthPoints(this.lifeAgeTicks, lifespanTicks) - previousWear;
            if (lost > 0 && this.lifeAgeTicks < lifespanTicks) {
                if (this.getHealth() <= lost) {
                    this.expire(level);
                    return;
                }
                this.setHealth(this.getHealth() - lost);
            }
        }
        if (this.lifeAgeTicks >= lifespanTicks) {
            this.expire(level);
        }
    }

    private long wornHealthPoints(long age, long lifespan) {
        return (long) (Math.min(age, lifespan) * (double) this.getMaxHealth() / lifespan);
    }

    private void expire(ServerLevel level) {
        this.kill(level);
        if (!this.isAlive()) {
            this.goalSelector.getAvailableGoals().forEach(WrappedGoal::stop);
            this.getNavigation().stop();
        }
    }

    public boolean repairWithWheat() {
        long lifespan = StrawGolemConfig.get().getLifespanTicks();
        boolean worn = lifespan > 0 && this.lifeAgeTicks > 0;
        if (!this.isAlive() || (this.getHealth() >= this.getMaxHealth() && !worn)) {
            return false;
        }

        this.heal(3.0F);
        if (worn) {
            long restoredTicks = Math.round(lifespan * 3.0D / this.getMaxHealth());
            this.lifeAgeTicks = this.getHealth() >= this.getMaxHealth() ? 0
                    : Math.max(0L, this.lifeAgeTicks - restoredTicks);
        }
        return true;
    }

    public int getTextureStage() {
        float health = this.getHealth();
        if (health <= 2.0F) {
            return LIFE_STAGE_DYING;
        }
        return health <= 6.0F ? LIFE_STAGE_OLD : LIFE_STAGE_NORMAL;
    }

    public float getWalkBlend(float partialTick) {
        return Mth.lerp(
                partialTick,
                this.walkBlendOld,
                this.walkBlend
        );
    }

    public float getRunBlend(float partialTick) {
        return Mth.lerp(partialTick, this.runBlendOld, this.runBlend);
    }

    public float getScaredBlend(float partialTick) {
        return Mth.lerp(
                partialTick,
                this.scaredBlendOld,
                this.scaredBlend
        );
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PANIC_RUNNING, false);
        builder.define(HARVEST_START_TICK, -1L);
        builder.define(HAS_STRAW_HAT, false);
        builder.define(SHIVERING, false);
    }

    private boolean shouldShiver(ServerLevel level) {
        BlockPos pos = this.blockPosition();
        if (level.getBiome(pos).value().coldEnoughToSnow(pos, level.getSeaLevel())) {
            return true;
        }

        return this.isExposedToRain(level);
    }

    private boolean isExposedToRain(ServerLevel level) {
        BlockPos pos = this.blockPosition();
        return !this.hasStrawHat() && level.isRainingAt(
                BlockPos.containing(pos.getX(), this.getBoundingBox().maxY, pos.getZ())
        );
    }

    public boolean isShivering() {
        return this.entityData.get(SHIVERING);
    }

    public boolean hasStrawHat() {
        return this.entityData.get(HAS_STRAW_HAT);
    }

    public boolean tryEquipStrawHat() {
        if (!(this.level() instanceof ServerLevel) || !this.isAlive() || this.hasStrawHat()) {
            return false;
        }

        this.entityData.set(HAS_STRAW_HAT, true);
        return true;
    }

    public void startHarvestAnimation() {
        this.entityData.set(HARVEST_START_TICK, this.level().getGameTime());
    }

    public void stopHarvestAnimation() {
        this.entityData.set(HARVEST_START_TICK, -1L);
    }

    public long getHarvestElapsedTicks() {
        long start = this.entityData.get(HARVEST_START_TICK);
        return start < 0L ? -1L : Math.max(0L, this.level().getGameTime() - start);
    }

    public boolean isPanicRunning() {
        return this.entityData.get(PANIC_RUNNING);
    }

    private static final String STRAW_HAT_TAG = "HasStrawHat";
    private static final EntityDataAccessor<Boolean> HAS_STRAW_HAT =
            SynchedEntityData.defineId(StrawGolemEntity.class, EntityDataSerializers.BOOLEAN);

    public GlobalPos getDepositStorage() {
        return this.depositStorage;
    }

    public void setDepositStorage(GlobalPos storage) {
        this.depositStorage = storage;
    }

    public boolean hasCargo() {
        return !this.getCargo().isEmpty();
    }

    public ItemStack getCargo() {
        return this.getMainHandItem();
    }

    public void setCargo(ItemStack stack) {
        if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(this.getCargo(), stack)) {
            this.seedsForStorage = false;
            this.compostSite = null;
        }
        this.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
    }

    public static boolean isReserveSeed(Item item) {
        return SEED_TYPES.contains(item);
    }

    public boolean consumeSeed(Item item) {
        int count = this.seedReserve.getOrDefault(item, 0);
        if (!(this.level() instanceof ServerLevel) || count == 0) {
            return false;
        }
        this.seedReserve.put(item, count - 1);
        return true;
    }

    public boolean seedsForStorage() {
        return this.seedsForStorage;
    }

    public void deliverHeldSeeds() {
        this.seedsForStorage = isReserveSeed(this.getCargo().getItem());
        this.compostSite = null;
    }

    public GlobalPos getCompostSite() {
        return this.compostSite;
    }

    public void setCompostSite(GlobalPos site) {
        this.compostSite = site;
    }

    public void storeHarvestSeeds(ItemStack stack) {
        Item item = stack.getItem();
        int count = stack.getCount();
        if (!(this.level() instanceof ServerLevel) || !isReserveSeed(item) || count <= 0) {
            return;
        }
        int stored = this.seedReserve.getOrDefault(item, 0);
        int accepted = Math.min(count, SEED_CAPACITY - stored);
        this.seedReserve.put(item, stored + accepted);
        if (count > accepted) {
            this.surplusSeeds.add(stack.copyWithCount(count - accepted));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong(LIFE_AGE_TAG, this.lifeAgeTicks);
        output.putInt(MOISTURE_TAG, this.moistureTicks);
        output.putBoolean(STRAW_HAT_TAG, this.hasStrawHat());
        if (!this.surplusSeeds.isEmpty()) {
            output.store("SurplusSeeds", ItemStack.CODEC.listOf(), this.surplusSeeds);
        }
        output.putBoolean("SeedsForStorage", this.seedsForStorage);
        if (this.compostSite != null) {
            output.store("CompostSite", GlobalPos.CODEC, this.compostSite);
        }
        if (this.depositStorage != null) {
            output.store("DepositStorage", GlobalPos.CODEC, this.depositStorage);
        }
        for (Item seed : SEED_TYPES) {
            output.putInt("SeedReserve." + seed.getDescriptionId(), this.seedReserve.getOrDefault(seed, 0));
        }
        if (!this.pendingDrops.isEmpty()) {
            output.store(PENDING_DROPS_TAG, PendingDrop.CODEC.listOf(), this.pendingDrops);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.lifeAgeTicks = Math.max(0L, input.getLongOr(LIFE_AGE_TAG, 0L));
        this.moistureTicks = Mth.clamp(
                input.getInt(MOISTURE_TAG).orElse(0), 0, MAX_MOISTURE_TICKS
        );
        long lifespan = StrawGolemConfig.get().getLifespanTicks();
        if (lifespan > 0 && this.lifeAgeTicks < lifespan) {
            float remaining = this.getMaxHealth() - this.wornHealthPoints(this.lifeAgeTicks, lifespan);
            this.setHealth(Math.min(this.getHealth(), remaining));
        }

        this.entityData.set(HAS_STRAW_HAT, input.getBooleanOr(STRAW_HAT_TAG, false));
        this.depositStorage = input.read("DepositStorage", GlobalPos.CODEC).orElse(null);
        this.seedReserve.clear();
        for (Item seed : SEED_TYPES) {
            int count = input.getInt("SeedReserve." + seed.getDescriptionId()).orElse(0);
            this.seedReserve.put(seed, Mth.clamp(count, 0, SEED_CAPACITY));
        }
        this.pendingDrops.clear();
        this.surplusSeeds.clear();
        input.read("SurplusSeeds", ItemStack.CODEC.listOf()).ifPresent(this.surplusSeeds::addAll);
        this.seedsForStorage = input.getBooleanOr("SeedsForStorage", false);
        this.compostSite = input.read("CompostSite", GlobalPos.CODEC).orElse(null);
        input.read(PENDING_DROPS_TAG, PendingDrop.CODEC.listOf())
                .ifPresent(this.pendingDrops::addAll);
        input.read(PENDING_LEGACY_DROPS_TAG, ItemStack.CODEC.listOf())
                .ifPresent(stacks -> stacks.forEach(stack ->
                        this.queuePendingDrop(stack, DEFAULT_PICKUP_DELAY)));
        input.child(LEGACY_INVENTORY_TAG).ifPresent(this::migrateLegacyInventory);
        this.setPersistenceRequired();
    }

    private void migrateLegacyInventory(ValueInput input) {
        NonNullList<ItemStack> oldItems = NonNullList.withSize(4, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, oldItems);
        for (ItemStack stack : oldItems) {
            if (stack.isEmpty()) {
                continue;
            }
            if (this.hasCargo()) {
                this.queuePendingDrop(stack, DEFAULT_PICKUP_DELAY);
            } else {
                this.setCargo(stack);
            }
        }
    }

    public boolean dropStack(ServerLevel level, ItemStack stack, int pickupDelay) {
        if (stack.isEmpty()) {
            return true;
        }

        ItemEntity drop = new ItemEntity(
                level, this.getX(), this.getY() + 0.2D, this.getZ(), stack.copy()
        );
        drop.setPickUpDelay(pickupDelay);
        return level.addFreshEntity(drop);
    }

    public void queuePendingDrop(ItemStack stack, int pickupDelay) {
        if (this.level() instanceof ServerLevel && !stack.isEmpty()) {
            this.pendingDrops.add(new PendingDrop(stack.copy(), pickupDelay));
        }
    }

    private void dropPendingItems(ServerLevel level) {
        this.pendingDrops.removeIf(drop ->
                this.dropStack(level, drop.stack(), drop.pickupDelay()));
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        ItemStack cargo = this.getCargo().copy();
        this.setCargo(ItemStack.EMPTY);
        super.dropEquipment(level);

        this.queuePendingDrop(cargo, DEFAULT_PICKUP_DELAY);
        for (var reserve : this.seedReserve.entrySet()) {
            this.queuePendingDrop(new ItemStack(reserve.getKey(), reserve.getValue()), DEFAULT_PICKUP_DELAY);
        }
        this.seedReserve.clear();
        for (ItemStack seeds : this.surplusSeeds) {
            this.queuePendingDrop(seeds, DEFAULT_PICKUP_DELAY);
        }
        this.surplusSeeds.clear();
        this.dropPendingItems(level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        var setting = xubache.com.strawgolem.config.StrawGolemConfig.get().attributes;

        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, setting.maxHealth)
                .add(Attributes.MOVEMENT_SPEED, setting.movementSpeed);
    }
}
