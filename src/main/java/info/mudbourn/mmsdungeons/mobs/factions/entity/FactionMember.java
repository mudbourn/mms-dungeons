package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.corpse.MobCorpses;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.mobs.factions.Faction;
import info.mudbourn.mmsdungeons.mobs.factions.FactionClass;
import info.mudbourn.mmsdungeons.mobs.factions.FactionCover;
import info.mudbourn.mmsdungeons.mobs.factions.FactionGear;
import info.mudbourn.mmsdungeons.mobs.factions.FactionFear;
import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import info.mudbourn.mmsdungeons.mobs.factions.FactionSpawns;
import info.mudbourn.mmsdungeons.mobs.factions.FactionSupplies;
import info.mudbourn.mmsdungeons.relations.FeudAware;
import info.mudbourn.mmsdungeons.relations.FeudTargetGoal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A player-shaped human of one faction: its class, skin, gear tier, cosmetic cover and group.
public class FactionMember extends Monster implements RangedAttackMob, FeudAware {

    public static final byte ACTION_NONE = 0;
    public static final byte ACTION_CHARGE = 1;
    public static final byte ACTION_CAST = 2;
    public static final byte ACTION_SLASH = 3;
    public static final byte ACTION_SLAM = 5;
    public static final byte ACTION_SLASH_LEFT = 6;
    public static final byte ACTION_ROLL = 8;
    public static final byte ACTION_BASH = 9;
    public static final byte OFF_HAND_FLAG = 0x40;
    public static final int ROLL_TICKS = 8;
    public static final byte SWING_MAIN_HAND = 0;
    public static final byte SWING_OFF_HAND = 1;
    public static final byte SWING_TWO_HANDED = 2;
    private static final String RANDOM_SIZE_TAG = "random_mob_sizes.checked";
    private static final double FAIRY_SCALE = 0.4;

    private static final EntityDataAccessor<String> DATA_CLASS = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<ItemStack> DATA_COVER_HEAD = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_COVER_CHEST = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_COVER_LEGS = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_COVER_FEET = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_ROLL = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_SWING = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SWING_SEQ = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SWING_TICKS = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SWING_UPSWING = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_SWING_HAND = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<String> DATA_ORIGIN = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DOWNED = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceKey<Biome> AERIAL_HELL_CAVERN = ResourceKey.create(Registries.BIOME, MmsDungeons.id("aerial_hell_cavern"));
    private static final float LUNAR_CHANCE = 0.4f;
    private static final double RALLY_RANGE = 24.0;
    private static final int LEADER_CHECK_TICKS = 10;
    private static final int ORIGIN_TICKS = 20;
    public static final int CRAWL_LOOKAHEAD = 3;
    public static final EntityDimensions CRAWL_SIZE = EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.4f);
    private static final int PHANTOM_FADE_TICKS = 100;
    private static final float ENDER_BLINK_CHANCE = 0.35f;
    private static final double SHULK_ARMOR = 8.0;
    private static final float[] DROP_CHANCE = {0.06f, 0.08f, 0.1f};
    private static final int TARGET_INTERVAL = 10;
    private static final float BONUS_LOOT_CHANCE = 0.25f;
    private static final float IDLE_EAT_HEALTH = 0.75f;
    private static final int IDLE_EAT_TICKS = 20;

    private final Faction faction;
    private static final float FAIRY_FRAGILITY = 1.5f;
    private final FactionCaster caster = new FactionCaster(this);
    private final FactionCleric cleric = new FactionCleric(this);
    private final FactionFlight flight = new FactionFlight(this);
    private final FactionDowned downedState = new FactionDowned(this);
    private final FactionPowers powers = new FactionPowers(this);
    private ItemStack sidearm = ItemStack.EMPTY;
    private ItemStack stowedMelee = ItemStack.EMPTY;
    private int tier = 1;
    private boolean classChosen;
    private boolean neutral;
    private boolean leader;
    private @Nullable UUID leaderId;
    private @Nullable UUID provoker;
    private ItemStack thrownWeapon = ItemStack.EMPTY;
    private int thrownTicks;
    private @Nullable List<ItemStack> lootSink;
    private boolean killedByPlayer;
    private final List<ItemStack> supplies = new ArrayList<>();
    private ItemStack stowedOffhand = ItemStack.EMPTY;
    private boolean consuming;
    private boolean consumed;

    public FactionMember(EntityType<? extends FactionMember> type, Level level, Faction faction) {
        super(type, level);
        this.faction = faction;
        this.xpReward = 10 + 5 * faction.ordinal();
        // Humans stay player sized: the Random Mob Sizes datapack skips any mob carrying this tag.
        this.addTag(RANDOM_SIZE_TAG);
        this.setPathfindingMalus(PathType.DAMAGING, 16.0f);
        this.setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, 8.0f);
        this.setPathfindingMalus(PathType.FIRE, -1.0f);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 16.0f);
        this.setPathfindingMalus(PathType.LAVA, -1.0f);
        this.setPathfindingMalus(PathType.POWDER_SNOW, -1.0f);
        this.moveControl = new FactionMoveControl(this);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new FactionBodyControl(this);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new FactionDowned.DownedGoal(this));
        this.goalSelector.addGoal(1, new FactionDowned.ReviveGoal(this));
        this.goalSelector.addGoal(2, new FactionCombatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, FactionMember.class));
        this.targetSelector.addGoal(2, new FeudTargetGoal(this, TARGET_INTERVAL, target -> false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CLASS, FactionClass.KNIGHT.id());
        builder.define(DATA_SKIN, 0);
        builder.define(DATA_ACTION, ACTION_NONE);
        builder.define(DATA_ORIGIN, FactionOrigin.NONE.id());
        builder.define(DATA_ROLL, 0);
        builder.define(DATA_FLYING, false);
        builder.define(DATA_DOWNED, false);
        builder.define(DATA_SWING, "");
        builder.define(DATA_SWING_SEQ, 0);
        builder.define(DATA_SWING_TICKS, 0);
        builder.define(DATA_SWING_UPSWING, 0.5f);
        builder.define(DATA_SWING_HAND, SWING_MAIN_HAND);
        builder.define(DATA_COVER_HEAD, ItemStack.EMPTY);
        builder.define(DATA_COVER_CHEST, ItemStack.EMPTY);
        builder.define(DATA_COVER_LEGS, ItemStack.EMPTY);
        builder.define(DATA_COVER_FEET, ItemStack.EMPTY);
    }

    public Faction faction() {
        return this.faction;
    }

    public FactionClass factionClass() {
        return FactionClass.byId(this.entityData.get(DATA_CLASS));
    }

    public void setFactionClass(FactionClass type) {
        this.entityData.set(DATA_CLASS, type.id());
        this.classChosen = true;
    }

    public String skin() {
        int index = Math.floorMod(this.entityData.get(DATA_SKIN), this.faction.skins().size());
        return this.faction.skins().get(index);
    }

    public FactionOrigin origin() {
        return FactionOrigin.byId(this.entityData.get(DATA_ORIGIN));
    }

    // Sets the origin and the lasting effects and armor that stand in for its powers.
    public void setOrigin(FactionOrigin origin) {
        this.entityData.set(DATA_ORIGIN, origin.id());
        switch (origin) {
            case AVIAN -> this.addEffect(lasting(MobEffects.SLOW_FALLING, 0));
            case BEASTFOLK -> this.addEffect(lasting(MobEffects.JUMP_BOOST, 1));
            case FISHMAN -> this.addEffect(lasting(MobEffects.WATER_BREATHING, 0));
            case FLORAN -> this.addEffect(lasting(MobEffects.REGENERATION, 0));
            case FAIRY -> {
                AttributeInstance scale = this.getAttribute(Attributes.SCALE);
                if (scale != null && !scale.hasModifier(MmsDungeons.id("fairy_scale"))) {
                    scale.addPermanentModifier(new AttributeModifier(MmsDungeons.id("fairy_scale"), FAIRY_SCALE - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                this.addEffect(lasting(MobEffects.SLOW_FALLING, 0));
            }
            case SHULK -> {
                AttributeInstance armor = this.getAttribute(Attributes.ARMOR);
                if (armor != null && !armor.hasModifier(MmsDungeons.id("shulk_armor"))) {
                    armor.addPermanentModifier(new AttributeModifier(MmsDungeons.id("shulk_armor"), SHULK_ARMOR, AttributeModifier.Operation.ADD_VALUE));
                }
            }
            default -> {
            }
        }
    }

    private static MobEffectInstance lasting(Holder<MobEffect> effect, int amplifier) {
        return new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, false, false);
    }

    @Override
    protected Component getTypeName() {
        FactionOrigin origin = this.origin();
        if (origin == FactionOrigin.NONE) {
            return super.getTypeName();
        }
        Component originName = Component.translatable(origin.nameKey());
        return Component.translatable("entity.mms_dungeons.faction_origin", originName, super.getTypeName());
    }

    @Override
    public boolean fireImmune() {
        return this.origin() == FactionOrigin.BLAZEBORN || this.origin() == FactionOrigin.PIGLIN || super.fireImmune();
    }

    @Override
    public boolean onClimbable() {
        return super.onClimbable() || this.origin() == FactionOrigin.ARACHNID && this.horizontalCollision;
    }

    // A truffle is untouched by poison, as the MMS Origins truffle is.
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !(this.origin() == FactionOrigin.TRUFFLE && effect.is(MobEffects.POISON)) && super.canBeAffected(effect);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = this.powers.hit(level, target, struck -> super.doHurtTarget(level, struck));
        if (hit && target instanceof LivingEntity living) {
            switch (this.origin()) {
                case BLAZEBORN -> living.igniteForSeconds(4.0f);
                case ARACHNID -> living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), this);
                case TRUFFLE -> living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), this);
                default -> {
                }
            }
        }
        return hit;
    }

    public int tier() {
        return this.tier;
    }

    public byte action() {
        return this.entityData.get(DATA_ACTION);
    }

    public void setAction(byte action) {
        this.entityData.set(DATA_ACTION, action);
    }

    // Ticks left in the current combat roll, synced so the renderer can tumble the body.
    public int rollTicks() {
        return this.entityData.get(DATA_ROLL);
    }

    public void setRollTicks(int ticks) {
        this.entityData.set(DATA_ROLL, ticks);
    }

    // Starts a Better Combat swing animation that runs for the given ticks; clients replay it whenever the sequence number changes.
    public void playSwing(String animation, int ticks, float upswing, byte hand) {
        this.entityData.set(DATA_SWING, animation);
        this.entityData.set(DATA_SWING_TICKS, ticks);
        this.entityData.set(DATA_SWING_UPSWING, upswing);
        this.entityData.set(DATA_SWING_HAND, hand);
        this.entityData.set(DATA_SWING_SEQ, this.entityData.get(DATA_SWING_SEQ) + 1);
    }

    public String swingAnimation() {
        return this.entityData.get(DATA_SWING);
    }

    public int swingSequence() {
        return this.entityData.get(DATA_SWING_SEQ);
    }

    public int swingTicks() {
        return this.entityData.get(DATA_SWING_TICKS);
    }

    public float swingUpswing() {
        return this.entityData.get(DATA_SWING_UPSWING);
    }

    public byte swingHand() {
        return this.entityData.get(DATA_SWING_HAND);
    }

    // Whether it still carries food, or a quick heal such as a potion or golden apple when quick is set.
    public boolean hasSupply(boolean quick) {
        return this.findSupply(quick) >= 0;
    }

    private int findSupply(boolean quick) {
        for (int i = 0; i < this.supplies.size(); i++) {
            ItemStack stack = this.supplies.get(i);
            if (!stack.isEmpty() && FactionSupplies.quick(stack) == quick) {
                return i;
            }
        }
        return -1;
    }

    public boolean consuming() {
        return this.consuming;
    }

    // Pulls one supply into the off hand and starts eating or drinking it as a player would, putting the off-hand item away meanwhile.
    public boolean startConsuming(boolean quick) {
        int index = this.consuming ? -1 : this.findSupply(quick);
        if (index < 0) {
            return false;
        }
        ItemStack supply = this.supplies.get(index).split(1);
        if (this.supplies.get(index).isEmpty()) {
            this.supplies.remove(index);
        }
        this.stopUsingItem();
        this.stowedOffhand = this.getOffhandItem();
        this.consuming = true;
        this.consumed = false;
        this.setItemSlot(EquipmentSlot.OFFHAND, supply);
        this.startUsingItem(InteractionHand.OFF_HAND);
        this.setAction(ACTION_NONE);
        return true;
    }

    @Override
    protected void completeUsingItem() {
        if (!this.consuming) {
            super.completeUsingItem();
            return;
        }
        ItemStack eaten = this.getUseItem().copy();
        this.consumed = true;
        super.completeUsingItem();
        FoodProperties food = eaten.get(DataComponents.FOOD);
        if (food != null) {
            this.heal(food.nutrition());
        }
    }

    @Override
    public void stopUsingItem() {
        super.stopUsingItem();
        if (this.consuming) {
            this.putAwaySupply();
        }
    }

    // Takes the off-hand item back out, returning an unfinished supply to the pack.
    private void putAwaySupply() {
        ItemStack held = this.getOffhandItem();
        if (!this.consumed && !held.isEmpty()) {
            this.supplies.add(held);
        }
        this.consuming = false;
        this.setItemSlot(EquipmentSlot.OFFHAND, this.stowedOffhand);
        this.stowedOffhand = ItemStack.EMPTY;
    }

    public boolean crawling() {
        return this.getPose() == Pose.SWIMMING;
    }

    public void setCrawling(boolean crawling) {
        this.setPose(crawling ? Pose.SWIMMING : Pose.STANDING);
    }

    // Whether one of the next path nodes is a one-high gap.
    public boolean lowGapAhead(Path path, int count) {
        int end = Math.min(path.getNodeCount(), path.getNextNodeIndex() + count);
        for (int i = path.getNextNodeIndex(); i < end; i++) {
            if (this.lowGap(path.getNodePos(i))) {
                return true;
            }
        }
        return false;
    }

    // A spot to crawl under: a solid block at head height, such as a fallen log, over a foot space with nothing solid in it, so air,
    // grass, flowers and other blocks a mob walks through all count as open. Collision is read as this mob meets it.
    private boolean lowGap(BlockPos pos) {
        BlockPos head = pos.above();
        if (!this.level().hasChunkAt(pos) || !this.level().hasChunkAt(head)) {
            return false;
        }
        CollisionContext context = CollisionContext.of(this);
        boolean openFeet = this.level().getBlockState(pos).getCollisionShape(this.level(), pos, context).isEmpty();
        boolean solidHead = !this.level().getBlockState(head).getCollisionShape(this.level(), head, context).isEmpty();
        return openFeet && solidHead;
    }

    // Whether the mob would fit standing up where it is.
    public boolean roomToStand() {
        EntityDimensions standing = this.getType().getDimensions();
        return this.level().noCollision(this, standing.makeBoundingBox(this.position()).deflate(1.0E-7));
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        if (pose == Pose.SWIMMING) {
            return CRAWL_SIZE;
        }
        return super.getDefaultDimensions(pose);
    }

    FactionFlight flight() {
        return this.flight;
    }

    // Whether its origin can fly, as an avian, fairy or blazeborn player can.
    public boolean winged() {
        return this.flight.winged();
    }

    // In the air on its origin's wings, synced so the renderer can let its legs hang.
    public boolean flying() {
        return this.entityData.get(DATA_FLYING);
    }

    void setFlying(boolean flying) {
        this.entityData.set(DATA_FLYING, flying);
    }

    FactionDowned downedState() {
        return this.downedState;
    }

    // Down on the ground waiting to be revived, synced for the renderer.
    public boolean downed() {
        return this.entityData.get(DATA_DOWNED);
    }

    void setDowned(boolean downed) {
        this.entityData.set(DATA_DOWNED, downed);
    }

    FactionPowers powers() {
        return this.powers;
    }

    // The tag that marks a creature summoned for a faction, such as a floran's Grovekeeper, so its crew counts it as an ally.
    public static String summonTag(Faction faction) {
        return "mms_dungeons.summon." + faction.name().toLowerCase(Locale.ROOT);
    }

    // The ranged weapon a fighter keeps put away for targets it cannot reach with its own weapon.
    public void setSidearm(ItemStack stack) {
        this.sidearm = stack;
    }

    boolean hasSidearm() {
        return !this.sidearm.isEmpty();
    }

    boolean sidearmDrawn() {
        return !this.stowedMelee.isEmpty();
    }

    void drawSidearm() {
        if (this.sidearm.isEmpty() || this.sidearmDrawn()) {
            return;
        }
        this.stopUsingItem();
        this.stowedMelee = this.getMainHandItem();
        this.setItemSlot(EquipmentSlot.MAINHAND, this.sidearm);
        this.sidearm = ItemStack.EMPTY;
    }

    void holsterSidearm() {
        if (!this.sidearmDrawn()) {
            return;
        }
        this.stopUsingItem();
        this.sidearm = this.getMainHandItem();
        this.setItemSlot(EquipmentSlot.MAINHAND, this.stowedMelee);
        this.stowedMelee = ItemStack.EMPTY;
    }

    @Override
    public void die(DamageSource source) {
        if (this.downedState.goDown(source)) {
            return;
        }
        super.die(source);
    }

    public FactionCaster caster() {
        return this.caster;
    }

    public FactionCleric cleric() {
        return this.cleric;
    }

    public boolean isLeader() {
        return this.leader;
    }

    public boolean isNeutral() {
        return this.neutral;
    }

    public ItemStack cover(EquipmentSlot slot) {
        return this.entityData.get(coverData(slot));
    }

    public void setCover(EquipmentSlot slot, ItemStack stack) {
        this.entityData.set(coverData(slot), stack);
    }

    private static EntityDataAccessor<ItemStack> coverData(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> DATA_COVER_HEAD;
            case CHEST -> DATA_COVER_CHEST;
            case LEGS -> DATA_COVER_LEGS;
            default -> DATA_COVER_FEET;
        };
    }

    // Followers of a living leader take the leader's targets instead of choosing their own.
    private boolean picksOwnTargets() {
        return !this.neutral && this.liveLeader() == null;
    }

    // A human holds back from a player whose kit is better than its own until the player attacks it.
    @Override
    public boolean mayStartFeud(LivingEntity target) {
        return this.picksOwnTargets() && !(target instanceof Player player && FactionFear.outgears(this, player));
    }

    // Who a swing may catch besides its target: players it is hostile to, and mobs fighting its faction, never its own faction or allies.
    public boolean swingEnemy(LivingEntity entity) {
        if (this.considersEntityAsAlly(entity)) {
            return false;
        }
        if (entity instanceof Player player) {
            return !this.neutral && !player.isCreative() && !player.isSpectator();
        }
        return entity instanceof Mob other && other.getTarget() instanceof FactionMember member && member.faction == this.faction;
    }

    public @Nullable FactionMember liveLeader() {
        if (this.leaderId == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getEntity(this.leaderId) instanceof FactionMember member && member.isAlive() ? member : null;
    }

    public void joinCrew(@Nullable UUID leaderId) {
        this.leaderId = leaderId;
    }

    // Holds the main-hand weapon away while it is in flight.
    public void throwWeapon(int ticks) {
        this.thrownWeapon = this.getMainHandItem().copy();
        this.thrownTicks = ticks;
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    public boolean weaponThrown() {
        return !this.thrownWeapon.isEmpty();
    }

    @Override
    public boolean considersEntityAsAlly(Entity other) {
        return other instanceof FactionMember member && member.faction == this.faction
            || other.entityTags().contains(summonTag(this.faction))
            || super.considersEntityAsAlly(other);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof FactionMember member && member.faction == this.faction) {
            return false;
        }
        if (this.origin().noFallDamage() && source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        if (this.origin() == FactionOrigin.FAIRY) {
            amount *= FAIRY_FRAGILITY;
        }
        if (this.powers.dodge(source)) {
            return false;
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            this.rally(level, attacker);
            this.powers.rallyKin(level, attacker);
            if (this.origin() == FactionOrigin.ENDERIAN && this.random.nextFloat() < ENDER_BLINK_CHANCE) {
                this.blink();
            }
        }
        return hurt;
    }

    // An attack turns the whole group hostile and points idle members at the attacker.
    private void rally(ServerLevel level, LivingEntity attacker) {
        AABB area = this.getBoundingBox().inflate(RALLY_RANGE);
        for (FactionMember member : level.getEntitiesOfClass(FactionMember.class, area, member -> member.faction == this.faction)) {
            member.neutral = false;
            if (attacker instanceof Player) {
                member.provoker = attacker.getUUID();
            }
            if (member.getTarget() == null) {
                member.setTarget(attacker);
            }
        }
    }

    // An enderian's short teleport away from a hit.
    private void blink() {
        for (int attempt = 0; attempt < 8; attempt++) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 12.0;
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 12.0;
            if (this.randomTeleport(x, this.getY(), z, true, state -> true)) {
                return;
            }
        }
    }

    // Water hurts blazeborn and enderians, phantoms fade while idle, and each origin shows its particles.
    private void tickOrigin(ServerLevel level) {
        FactionOrigin origin = this.origin();
        if (origin == FactionOrigin.NONE || this.tickCount % ORIGIN_TICKS != 0) {
            return;
        }
        if (origin.hurtByWater() && this.isInWaterOrRain()) {
            this.hurtServer(level, this.damageSources().drown(), 1.0f);
        }
        if (origin == FactionOrigin.PHANTOM && this.getTarget() == null) {
            this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, PHANTOM_FADE_TICKS, 0, false, false));
        } else if (origin == FactionOrigin.PHANTOM) {
            this.removeEffect(MobEffects.INVISIBILITY);
        }
        if (origin.aura() != null) {
            level.sendParticles(origin.aura(), this.getX(), this.getY(1.0), this.getZ(), 3, 0.3, 0.5, 0.3, 0.01);
        }
    }

    // Puts back a human that Random Mob Sizes resized before it was told to skip them: player scale, and none of its stat changes.
    private void undoRandomSize() {
        AttributeInstance scale = this.getAttribute(Attributes.SCALE);
        if (scale == null || scale.getBaseValue() == 1.0) {
            return;
        }
        scale.setBaseValue(1.0);
        removeRandomSizeModifier(this.getAttribute(Attributes.MAX_HEALTH), "health_scale");
        removeRandomSizeModifier(this.getAttribute(Attributes.MOVEMENT_SPEED), "speed_scale");
        removeRandomSizeModifier(this.getAttribute(Attributes.ATTACK_DAMAGE), "damage_scale");
        this.setHealth(Math.min(this.getHealth(), this.getMaxHealth()));
    }

    private static void removeRandomSizeModifier(AttributeInstance attribute, String name) {
        if (attribute != null) {
            attribute.removeModifier(Identifier.fromNamespaceAndPath("random_mob_sizes", name));
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.undoRandomSize();
        this.tickOrigin(level);
        this.flight.tick(level);
        this.downedState.tick(level);
        this.powers.tickAlways(level);
        if (!this.downed() && !this.crawling() && !this.roomToStand()) {
            this.setCrawling(true);
        } else if (!this.downed() && this.crawling() && this.roomToStand() && (this.getNavigation().getPath() == null || !this.lowGapAhead(this.getNavigation().getPath(), CRAWL_LOOKAHEAD))) {
            this.setCrawling(false);
        }
        if (this.getTarget() == null && !this.downed() && !this.isUsingItem() && this.tickCount % IDLE_EAT_TICKS == 0 && this.getHealth() < this.getMaxHealth() * IDLE_EAT_HEALTH) {
            this.startConsuming(false);
        }
        if (this.thrownTicks > 0 && --this.thrownTicks == 0) {
            this.setItemSlot(EquipmentSlot.MAINHAND, this.thrownWeapon);
            this.thrownWeapon = ItemStack.EMPTY;
        }
        if (this.tickCount % LEADER_CHECK_TICKS == 0) {
            FactionMember leader = this.liveLeader();
            if (leader != null && leader.getTarget() != null && leader.getTarget() != this.getTarget()) {
                this.setTarget(leader.getTarget());
            }
            if (this.cowedBy(this.getTarget())) {
                this.setTarget(null);
            }
        }
    }

    // A player whose kit beats the crew's, judged by the leader when there is one, who has not attacked the crew.
    private boolean cowedBy(@Nullable LivingEntity target) {
        if (!(target instanceof Player player) || player.getUUID().equals(this.provoker)) {
            return false;
        }
        FactionMember leader = this.liveLeader();
        return FactionFear.outgears(leader != null ? leader : this, player);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        FactionCombatGoal.shootArrow(this, target, power);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        RandomSource random = level.getRandom();
        FactionOrigin origin = FactionOrigin.roll(this.faction, this.classChosen ? this.factionClass() : null, random);
        if (!this.classChosen) {
            this.setFactionClass(origin == FactionOrigin.NONE ? FactionClass.pick(random) : origin.pickRole(random));
        }
        this.entityData.set(DATA_SKIN, random.nextInt(this.faction.skins().size()));
        this.tier = FactionGear.rollTier(random);
        boolean inCavern = level.getBiome(this.blockPosition()).is(AERIAL_HELL_CAVERN);
        if (this.faction == Faction.GUILDSMAN && inCavern && random.nextFloat() < LUNAR_CHANCE) {
            this.tier = FactionGear.LUNAR_TIER;
        }
        this.neutral = level.getLevel().dimension() == Level.OVERWORLD;
        this.setOrigin(origin);
        this.powers.onSpawn();
        FactionSpawns.Crew crew = groupData instanceof FactionSpawns.Crew joined ? joined : null;
        if (crew != null) {
            this.leaderId = crew.leader();
        } else {
            this.leader = this.faction.led();
        }
        FactionGear.equip(this, random);
        this.supplies.addAll(FactionSupplies.roll(this.faction, this.tier, random));
        FactionCover.apply(this, random);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, DROP_CHANCE[this.faction.ordinal()]);
        }
        if (crew == null && FactionSpawns.formsCrew(reason)) {
            return FactionSpawns.formCrew(this, level, random);
        }
        return data;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Class", this.factionClass().id());
        output.putInt("Skin", this.entityData.get(DATA_SKIN));
        output.putInt("Tier", this.tier);
        output.putString("Origin", this.origin().id());
        output.putBoolean("Neutral", this.neutral);
        output.putBoolean("Leader", this.leader);
        if (this.leaderId != null) {
            output.putString("LeaderId", this.leaderId.toString());
        }
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack cover = this.cover(slot);
            if (!cover.isEmpty()) {
                output.store("Cover_" + slot.getName(), ItemStack.CODEC, cover);
            }
        }
        if (!this.thrownWeapon.isEmpty()) {
            output.store("ThrownWeapon", ItemStack.CODEC, this.thrownWeapon);
            output.putInt("ThrownTicks", this.thrownTicks);
        }
        output.store("Supplies", ItemStack.CODEC.listOf(), this.supplies);
        if (this.consuming) {
            output.store("StowedOffhand", ItemStack.OPTIONAL_CODEC, this.stowedOffhand);
        }
        output.putBoolean("Flying", this.flight.inAir());
        output.putBoolean("Downed", this.downed());
        output.putInt("DownedTicks", this.downedState.downedTicks());
        output.putBoolean("SelfRevived", this.downedState.selfRevived());
        output.putInt("ShellPotions", this.powers.shellPotions());
        output.store("Sidearm", ItemStack.OPTIONAL_CODEC, this.sidearm);
        output.store("StowedMelee", ItemStack.OPTIONAL_CODEC, this.stowedMelee);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("Class").ifPresent(id -> this.setFactionClass(FactionClass.byId(id)));
        this.entityData.set(DATA_SKIN, input.getIntOr("Skin", 0));
        this.tier = input.getIntOr("Tier", 1);
        this.entityData.set(DATA_ORIGIN, input.getStringOr("Origin", FactionOrigin.NONE.id()));
        this.neutral = input.getBooleanOr("Neutral", false);
        this.leader = input.getBooleanOr("Leader", false);
        this.leaderId = input.getString("LeaderId").map(UUID::fromString).orElse(null);
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            this.setCover(slot, FactionCover.upgrade(slot, input.read("Cover_" + slot.getName(), ItemStack.CODEC).orElse(ItemStack.EMPTY)));
        }
        this.thrownWeapon = input.read("ThrownWeapon", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.thrownTicks = input.getIntOr("ThrownTicks", this.thrownWeapon.isEmpty() ? 0 : 1);
        this.supplies.clear();
        this.supplies.addAll(input.read("Supplies", ItemStack.CODEC.listOf()).orElse(List.of()));
        input.read("StowedOffhand", ItemStack.OPTIONAL_CODEC).ifPresent(stowed -> {
            this.supplies.add(this.getOffhandItem());
            this.setItemSlot(EquipmentSlot.OFFHAND, stowed);
        });
        if (input.getBooleanOr("Flying", false)) {
            this.flight.forceGround();
        }
        this.powers.setShellPotions(input.getIntOr("ShellPotions", 0));
        this.sidearm = input.read("Sidearm", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.stowedMelee = input.read("StowedMelee", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.downedState.load(input.getBooleanOr("Downed", false), input.getIntOr("DownedTicks", 0), input.getBooleanOr("SelfRevived", false));
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        if (!DungeonDimension.isPericulum(level, this.blockPosition())) {
            super.dropAllDeathLoot(level, source);
            return;
        }
        List<ItemStack> loot = new ArrayList<>();
        this.lootSink = loot;
        super.dropAllDeathLoot(level, source);
        List<ItemStack> bonus = new ArrayList<>();
        this.lootSink = bonus;
        this.dropFromLootTable(level, source, this.killedByPlayer);
        this.lootSink = null;
        for (ItemStack stack : bonus) {
            if (this.random.nextFloat() < BONUS_LOOT_CHANCE) {
                loot.add(stack);
            }
        }
        MobCorpses.leave(level, this.position(), this.yBodyRot, "mms_dungeons:entity/factions/" + this.skin(), this.getName(), loot);
    }

    @Override
    public @Nullable ItemEntity spawnAtLocation(ServerLevel level, ItemStack stack, Vec3 offset) {
        if (this.lootSink == null) {
            return super.spawnAtLocation(level, stack, offset);
        }
        if (!stack.isEmpty()) {
            this.lootSink.add(stack);
        }
        return null;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        this.killedByPlayer = killedByPlayer;
        if (!this.thrownWeapon.isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, this.thrownWeapon);
            this.thrownWeapon = ItemStack.EMPTY;
        }
        if (this.consuming) {
            this.putAwaySupply();
        }
        this.holsterSidearm();
        this.supplies.addAll(this.powers.leftovers());
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        for (ItemStack supply : this.supplies) {
            this.spawnAtLocation(level, supply);
        }
        this.supplies.clear();
    }
}
