package info.mudbourn.mmsdungeons.mobs.orpiment.entity;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentDataSerializers;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentItems;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jspecify.annotations.Nullable;

// A friendly blob of sap that carries items, eats amber to grow, and turns hostile when struck.
public class Sap extends AbstractCubeMob implements Enemy {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        MmsDungeons.id("orpiment/sap/basic")
    );

    private static final EntityDataAccessor<Boolean> HOSTILE = SynchedEntityData.defineId(Sap.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<List<ItemStack>> CARRIED = SynchedEntityData.defineId(Sap.class, OrpimentDataSerializers.ITEMS);
    private static final int[] INVENTORY_SIZES = {4, 5, 6, 7, 8};
    private static final int MAX_GROWN_SIZE = 4;
    private static final double TEMPT_RANGE = 10.0;

    private SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZES[0]);
    private int chunksEaten;
    private int chunksToGrow;
    private List<ItemStack> splitItems = List.of();
    private int splitsLeft;

    public Sap(EntityType<? extends Sap> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.MAX_HEALTH, 3.0)
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            .add(Attributes.ARMOR, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOSTILE, false);
        builder.define(CARRIED, List.of());
    }

    public boolean isHostile() {
        return this.entityData.get(HOSTILE);
    }

    public void setHostile(boolean hostile) {
        this.entityData.set(HOSTILE, hostile);
    }

    public List<ItemStack> getCarried() {
        return this.entityData.get(CARRIED);
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(1, new TemptGoal(this));
        this.goalSelector.addGoal(3, new CubeMobAttackGoal(this) {
            @Override
            public boolean canUse() {
                return Sap.this.isHostile() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return Sap.this.isHostile() && super.canContinueToUse();
            }
        });
    }

    @Override
    protected void addTargetingGoals() {
        this.targetSelector.addGoal(
            1,
            new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                10,
                true,
                false,
                (target, level) -> this.isHostile() && this.getSize() != 1 && Math.abs(target.getY() - this.getY()) <= 4.0
            )
        );
        this.targetSelector.addGoal(
            2,
            new NearestAttackableTargetGoal<>(this, IronGolem.class, true, (target, level) -> this.isHostile() && this.getSize() != 1)
        );
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public void setSize(int size, boolean updateHealth) {
        super.setSize(size, updateHealth);
        int actual = this.getSize();
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(actual);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2);
        this.xpReward = actual;
        this.resizeInventory();
    }

    @Override
    protected void setCubeMobHealth(int size) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(size * size + 2.0);
    }

    @Override
    protected boolean canBeABaby() {
        return false;
    }

    @Override
    protected void decreaseSquish() {
        this.targetSquish *= 0.8f;
    }

    private void resizeInventory() {
        int slots = INVENTORY_SIZES[Mth.clamp(this.getSize(), 1, INVENTORY_SIZES.length) - 1];
        if (this.inventory.getContainerSize() == slots) {
            return;
        }
        SimpleContainer resized = new SimpleContainer(slots);
        for (int i = 0; i < Math.min(slots, this.inventory.getContainerSize()); i++) {
            resized.setItem(i, this.inventory.getItem(i));
        }
        this.inventory = resized;
        this.syncInventory();
    }

    private void syncInventory() {
        if (this.level().isClientSide()) {
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            items.add(this.inventory.getItem(i).copy());
        }
        this.entityData.set(CARRIED, items);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (CARRIED.equals(accessor) && this.level().isClientSide()) {
            List<ItemStack> items = this.getCarried();
            this.inventory = new SimpleContainer(Math.max(items.size(), 1));
            for (int i = 0; i < items.size(); i++) {
                this.inventory.setItem(i, items.get(i));
            }
        }
    }

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    private boolean hasItems() {
        return !this.inventory.isEmpty();
    }

    private boolean canAccept(ItemStack incoming) {
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack held = this.inventory.getItem(i);
            if (held.isEmpty() || ItemStack.isSameItemSameComponents(held, incoming) && held.getCount() < held.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private boolean addToInventory(ItemStack stack) {
        ItemStack remainder = this.inventory.addItem(stack.copy());
        boolean added = remainder.getCount() < stack.getCount();
        if (added) {
            this.syncInventory();
            this.playSound(OrpimentSounds.SAP_INSERT, this.getSoundVolume(), this.getSoundPitch());
        }
        return remainder.isEmpty();
    }

    private void fillFromLoot(ServerLevel level) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(LOOT);
        LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        this.inventory.clearContent();
        int slot = 0;
        for (ItemStack stack : table.getRandomItems(params)) {
            if (slot >= this.inventory.getContainerSize()) {
                break;
            }
            if (!stack.isEmpty()) {
                this.inventory.setItem(slot++, stack.copy());
            }
        }
        this.syncInventory();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel && this.isAlive()) {
            for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(1.0))) {
                if (item.isAlive() && !item.getItem().isEmpty() && this.addToInventory(item.getItem())) {
                    item.discard();
                }
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !this.hasItems() && super.removeWhenFarAway(distance);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player) {
            this.setHostile(true);
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.BUCKET) && this.getSize() == 2 && !this.isHostile()) {
            return this.scoop(player, hand, stack);
        }
        if (stack.is(OrpimentItems.AMBER_CHUNK) && !this.isHostile() && !player.isCrouching() && this.getSize() < MAX_GROWN_SIZE) {
            this.feed(player, stack);
            return InteractionResult.SUCCESS;
        }
        if (!stack.isEmpty() && this.canAccept(stack)) {
            if (!this.level().isClientSide()) {
                ItemStack single = stack.copyWithCount(1);
                if (this.addToInventory(single)) {
                    stack.consume(1, player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private InteractionResult scoop(Player player, InteractionHand hand, ItemStack bucket) {
        if (this.level() instanceof ServerLevel level) {
            ItemStack filled = new ItemStack(OrpimentItems.BUCKET_OF_SAP);
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            this.saveWithoutId(output);
            filled.set(DataComponents.ENTITY_DATA, TypedEntityData.of(this.getType(), output.buildResult()));
            level.playSound(null, this.getX(), this.getY(), this.getZ(), OrpimentSounds.SAP_FILL, SoundSource.NEUTRAL, 1.0f, 1.0f);
            this.discard();
            player.setItemInHand(hand, ItemUtils.createFilledResult(bucket, player, filled));
        }
        return InteractionResult.SUCCESS;
    }

    private void feed(Player player, ItemStack stack) {
        if (this.level() instanceof ServerLevel level) {
            if (this.chunksToGrow <= 0) {
                this.chunksToGrow = 7 + this.random.nextInt(8);
            }
            this.chunksEaten++;
            stack.consume(1, player);
            level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                this.getX(),
                this.getY() + this.getBbHeight() / 2.0,
                this.getZ(),
                7,
                this.getBbWidth() / 2.0,
                this.getBbHeight() / 2.0,
                this.getBbWidth() / 2.0,
                0.0
            );
            if (this.chunksEaten >= this.chunksToGrow) {
                this.chunksEaten = 0;
                this.chunksToGrow = 0;
                this.setSize(this.getSize() + 1, true);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide() && this.isTiny() && this.level() instanceof ServerLevel level) {
            for (ItemStack stack : this.inventory.removeAllItems()) {
                this.spawnAtLocation(level, stack);
            }
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && this.getSize() > 1 && this.isDeadOrDying()) {
            this.splitItems = this.inventory.removeAllItems();
            this.splitsLeft = this.getSplitCount();
        }
        super.remove(reason);
    }

    @Override
    protected int getSplitCount() {
        return this.splitsLeft > 0 ? this.splitsLeft : super.getSplitCount();
    }

    @Override
    protected void setUpSplitCube(AbstractCubeMob cube, int halfSize, float xd, float zd) {
        super.setUpSplitCube(cube, halfSize, xd, zd);
        if (cube instanceof Sap child) {
            child.setHostile(this.isHostile());
            int share = this.splitsLeft > 0 ? (int) Math.ceil((double) this.splitItems.size() / this.splitsLeft) : 0;
            for (int i = 0; i < share && !this.splitItems.isEmpty(); i++) {
                child.inventory.addItem(this.splitItems.getFirst());
                this.splitItems = this.splitItems.subList(1, this.splitItems.size());
            }
            child.syncInventory();
            this.splitsLeft--;
        }
    }

    @Override
    protected boolean canDealDamage() {
        return this.isHostile() && super.canDealDamage();
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        if (reason == EntitySpawnReason.BUCKET) {
            this.setSize(2, true);
        } else if (level.getLevel() != null) {
            this.fillFromLoot(level.getLevel());
        }
        this.setHostile(false);
        return data;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Hostile", this.isHostile());
        output.putInt("AmberChunksEaten", this.chunksEaten);
        output.putInt("ChunksToGrow", this.chunksToGrow);
        this.inventory.storeAsItemList(output.list("SapInventory", ItemStack.CODEC));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setHostile(input.getBooleanOr("Hostile", false));
        this.chunksEaten = input.getIntOr("AmberChunksEaten", 0);
        this.chunksToGrow = input.getIntOr("ChunksToGrow", 0);
        this.resizeInventory();
        this.inventory.fromItemList(input.listOrEmpty("SapInventory", ItemStack.CODEC));
        this.syncInventory();
    }

    @Override
    protected float getSoundVolume() {
        return 0.2f * this.getSize();
    }

    @Override
    protected @Nullable ParticleOptions getParticleType() {
        return new ItemParticleOption(ParticleTypes.ITEM, OrpimentItems.SAP_BALL);
    }

    @Override
    protected SoundEvent getJumpSound() {
        return OrpimentSounds.SAP_JUMP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return OrpimentSounds.SAP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return OrpimentSounds.SAP_DEATH;
    }

    @Override
    protected SoundEvent getSquishSound() {
        return OrpimentSounds.SAP_JUMP;
    }

    // Follows a player holding amber until the sap is angered.
    private static class TemptGoal extends Goal {

        private final Sap sap;
        private @Nullable Player player;

        TemptGoal(Sap sap) {
            this.sap = sap;
            this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (this.sap.isHostile()) {
                return false;
            }
            this.player = this.sap.level().getNearestPlayer(this.sap, TEMPT_RANGE);
            return this.player != null && holdsAmber(this.player);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.sap.isHostile() && this.player != null && this.player.isAlive() && holdsAmber(this.player);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.player != null && this.sap.getMoveControl() instanceof CubeMobMoveControl<?> control) {
                double dx = this.player.getX() - this.sap.getX();
                double dz = this.player.getZ() - this.sap.getZ();
                control.setDirection((float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f, false);
                control.setWantedMovement(1.2);
            }
        }

        @Override
        public void stop() {
            this.player = null;
        }

        private static boolean holdsAmber(Player player) {
            return player.getMainHandItem().is(OrpimentItems.AMBER_CHUNK) || player.getOffhandItem().is(OrpimentItems.AMBER_CHUNK);
        }
    }
}
