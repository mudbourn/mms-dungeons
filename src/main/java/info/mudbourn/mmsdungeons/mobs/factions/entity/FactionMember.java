package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.corpse.MobCorpses;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.mobs.factions.Faction;
import info.mudbourn.mmsdungeons.mobs.factions.FactionClass;
import info.mudbourn.mmsdungeons.mobs.factions.FactionCover;
import info.mudbourn.mmsdungeons.mobs.factions.FactionFear;
import info.mudbourn.mmsdungeons.mobs.factions.FactionGear;
import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import info.mudbourn.mmsdungeons.mobs.factions.FactionSpawns;
import info.mudbourn.mmsdungeons.mobs.factions.FactionSupplies;
import info.mudbourn.mmsdungeons.relations.FeudAware;
import info.mudbourn.mmsdungeons.relations.FeudTargetGoal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
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
    private static final float EXHAUSTION_PER_FOOD = 4.0f;
    private static final String RANDOM_SIZE_TAG = "random_mob_sizes.checked";

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
    private static final EntityDataAccessor<String> DATA_SPELL = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_RELEASE = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_RELEASE_SEQ = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_ORIGIN = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DOWNED = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LEAF_FORM = SynchedEntityData.defineId(FactionMember.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceKey<Biome> AERIAL_HELL_CAVERN = ResourceKey.create(Registries.BIOME, MmsDungeons.id("aerial_hell_cavern"));
    private static final float LUNAR_CHANCE = 0.4f;
    private static final double RALLY_RANGE = 24.0;
    private static final int LEADER_CHECK_TICKS = 10;
    private static final int UMBRELLA_CHECK_TICKS = 20;
    // Day length and the time of day monsters start to spawn: rain darkens the sky without turning day into night.
    private static final long DAY_LENGTH = 24000L;
    private static final long NIGHTFALL = 13000L;
    // The game's own sprint boost is meant for a player's speed; a mob's speed counts twice in its stride, so this trims the boost back to a player's 30 percent.
    private static final Identifier SPRINT_TRIM = MmsDungeons.id("sprint_trim");
    private static final double SPRINT_TRIM_AMOUNT = Math.sqrt(1.3) / 1.3 - 1.0;
    private static final Identifier UMBRELLA = Identifier.fromNamespaceAndPath("mms_origins", "umbrella");
    private final Set<String> keysHeld = new HashSet<>();
    private final Set<String> keysBefore = new HashSet<>();
    private boolean softLanding;
    private boolean originChecked;
    private static final int FLORAN_WATER = 2;
    static final boolean ORIGINS = FabricLoader.getInstance().isModLoaded("mms_origins");
    public static final int CRAWL_LOOKAHEAD = 3;
    public static final EntityDimensions CRAWL_SIZE = EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.4f);
    private static final EntityDimensions CROUCH_SIZE = EntityDimensions.scalable(0.6f, 1.5f).withEyeHeight(1.27f);
    private static final float[] DROP_CHANCE = {0.06f, 0.08f, 0.1f};
    private static final int TARGET_INTERVAL = 10;
    private static final float BONUS_LOOT_CHANCE = 0.25f;
    private static final float IDLE_EAT_HEALTH = 0.75f;
    private static final int IDLE_EAT_TICKS = 20;

    private final Faction faction;
    private final FactionCaster caster = new FactionCaster(this);
    private final FactionCleric cleric = new FactionCleric(this);
    private final FactionFlight flight = new FactionFlight(this);
    private final FactionTactics tactics = new FactionTactics(this);
    private final FactionTravel travel = new FactionTravel(this);
    private static final float COLLAPSE_TICKS = 12.0f;
    private static final float BODY_TURN = 0.3f;
    private static final float HEAD_TWIST = 50.0f;
    private static final float BLOCKING_HEAD_TWIST = 15.0f;
    private static final float BACKWARD_FROM = 95.0f;
    private static final double WALKING_SQR = 0.0025;

    private final FactionDowned downedState = new FactionDowned(this);
    private final FactionPeace peace = new FactionPeace();
    private int downedSince = -1;
    private boolean reviving;
    private boolean fleeing;
    private final FactionPowers powers = new FactionPowers(this);
    private final FactionClimate climate = new FactionClimate(this);
    private ItemStack sidearm = ItemStack.EMPTY;
    private @Nullable Pose forcedPose;
    private ItemStack stowedMelee = ItemStack.EMPTY;
    private int tier = 1;
    private boolean classChosen;
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
    private float exhaustion;
    private int foodCredit;
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
        this.lookControl = new FactionLookControl(this);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FactionNavigation(this, level);
    }

    // Turns the body as a player's turns: toward where it walks or swings, and otherwise only as far as keeps the head within its limit.
    @Override
    protected void tickHeadTurn(float walkFacing) {
        float look = this.getYHeadRot();
        float target = this.yBodyRot;
        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        boolean walking = dx * dx + dz * dz > WALKING_SQR;
        // Only a member in a fight backs away from what it faces; out of one, like a downed crawler, it walks where it faces.
        boolean headFirst = this.downed() || !this.isAggressive();
        if (walking) {
            float walk = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
            float apart = Math.abs(Mth.wrapDegrees(look - walk));
            target = apart > BACKWARD_FROM && !headFirst ? walk - 180.0f : walk;
        }
        if (this.getSwingAnimation(1.0f) > 0.0f) {
            target = look;
        }
        this.yBodyRot += Mth.wrapDegrees(target - this.yBodyRot) * BODY_TURN;
        float twist = Mth.wrapDegrees(look - this.yBodyRot);
        float limit = this.isBlocking() ? BLOCKING_HEAD_TWIST : HEAD_TWIST;
        // Walking head first, the head follows the body rather than dragging it round; a downed member's always does.
        if (headFirst && (walking || this.downed())) {
            if (Math.abs(twist) > limit) {
                this.setYHeadRot(this.yBodyRot + Math.signum(twist) * limit);
            }
            return;
        }
        if (Math.abs(twist) > limit) {
            this.yBodyRot += twist - Math.signum(twist) * limit;
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new FactionDowned.DownedGoal(this));
        this.goalSelector.addGoal(1, new FactionDowned.ReviveGoal(this));
        this.goalSelector.addGoal(2, new FactionCombatGoal(this));
        this.goalSelector.addGoal(3, new FactionPeace.PeaceGoal(this));
        this.goalSelector.addGoal(4, new FactionPeace.FriendGoal(this));
        this.goalSelector.addGoal(4, new FactionWary(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, FactionMember.class));
        this.targetSelector.addGoal(1, new FactionThreatSense(this));
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
        builder.define(DATA_LEAF_FORM, false);
        builder.define(DATA_SWING, "");
        builder.define(DATA_SWING_SEQ, 0);
        builder.define(DATA_SWING_TICKS, 0);
        builder.define(DATA_SWING_UPSWING, 0.5f);
        builder.define(DATA_SWING_HAND, SWING_MAIN_HAND);
        builder.define(DATA_SPELL, "");
        builder.define(DATA_RELEASE, "");
        builder.define(DATA_RELEASE_SEQ, 0);
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

    // Sets the origin, which on the server grants its real Origins powers.
    public void setOrigin(FactionOrigin origin) {
        this.entityData.set(DATA_ORIGIN, origin.id());
        if (FactionOrigin.POWERS && origin != FactionOrigin.NONE && !this.level().isClientSide()) {
            FactionOriginPowers.grant(this, origin);
        }
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
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return this.powers.hit(level, target, struck -> super.doHurtTarget(level, struck));
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

    // The Spell Engine spell it is casting, or empty; clients play its casting animation while it lasts.
    public String castingSpell() {
        return this.entityData.get(DATA_SPELL);
    }

    void setCastingSpell(String spell) {
        this.entityData.set(DATA_SPELL, spell);
    }

    // Lets a spell go; clients play its release animation whenever the sequence number changes.
    void releaseSpell(String spell) {
        this.entityData.set(DATA_RELEASE, spell);
        this.entityData.set(DATA_RELEASE_SEQ, this.entityData.get(DATA_RELEASE_SEQ) + 1);
    }

    public String releasedSpell() {
        return this.entityData.get(DATA_RELEASE);
    }

    public int releaseSequence() {
        return this.entityData.get(DATA_RELEASE_SEQ);
    }

    // Whether it still carries food, or a quick heal such as a potion or golden apple when quick is set.
    public boolean hasSupply(boolean quick) {
        return this.findSupply(quick) >= 0;
    }

    // The hunger Origins' conditions see: the nutrition of the food in its pack, capped at a full bar of 20.
    public int foodLevel() {
        int total = 0;
        for (ItemStack stack : this.supplies) {
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (food != null) {
                total += food.nutrition() * stack.getCount();
            }
        }
        return Math.min(20, total);
    }

    // The saturation Origins' conditions see: the saturation of the food in its pack, capped at the food level as a player's is.
    public float saturation() {
        float total = 0.0f;
        for (ItemStack stack : this.supplies) {
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (food != null) {
                total += food.saturation() * stack.getCount();
            }
        }
        return Math.min(this.foodLevel(), total);
    }

    // Tires it as a player's actions do: every 4 exhaustion uses up 1 point of the nutrition of the food in its pack.
    public void exhaust(float amount) {
        this.exhaustion += amount;
        while (this.exhaustion >= EXHAUSTION_PER_FOOD) {
            this.exhaustion -= EXHAUSTION_PER_FOOD;
            if (this.foodCredit <= 0) {
                this.foodCredit = this.eatPackFood();
            }
            this.foodCredit = Math.max(0, this.foodCredit - 1);
        }
    }

    // Uses up one item of pack food and returns its nutrition, or 0 when the pack has none.
    private int eatPackFood() {
        for (int i = 0; i < this.supplies.size(); i++) {
            ItemStack stack = this.supplies.get(i);
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (food != null && food.nutrition() > 0 && !this.forbids(stack)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    this.supplies.remove(i);
                }
                return food.nutrition();
            }
        }
        return 0;
    }

    private int findSupply(boolean quick) {
        for (int i = 0; i < this.supplies.size(); i++) {
            ItemStack stack = this.supplies.get(i);
            if (!stack.isEmpty() && !FactionSupplies.water(stack) && FactionSupplies.quick(stack) == quick && (quick || FactionSupplies.meal(stack)) && !this.forbids(stack)) {
                return i;
            }
        }
        return -1;
    }

    private int findWater() {
        for (int i = 0; i < this.supplies.size(); i++) {
            if (FactionSupplies.water(this.supplies.get(i))) {
                return i;
            }
        }
        return -1;
    }

    // Drinks a water bottle from its pack, as a floran does for sun.
    public boolean startDrinkingWater() {
        return this.consume(this.consuming ? -1 : this.findWater());
    }

    public boolean consuming() {
        return this.consuming;
    }

    // Pulls one supply into the off hand and starts eating or drinking it as a player would, putting the off-hand item away meanwhile.
    public boolean startConsuming(boolean quick) {
        return this.consume(this.consuming ? -1 : this.findSupply(quick));
    }

    // The pack the member eats, drinks and refills from.
    List<ItemStack> supplies() {
        return this.supplies;
    }

    boolean consume(int index) {
        if (index < 0 || this.armless()) {
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
        if (FactionOrigin.POWERS && this.origin() != FactionOrigin.NONE) {
            FactionOriginPowers.finishUsing(this, eaten, this.getOffhandItem());
        }
        this.climate.drank(eaten);
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

    // Takes the off-hand item back out, returning an unfinished supply or a waterskin to the pack.
    private void putAwaySupply() {
        ItemStack held = this.getOffhandItem();
        if ((!this.consumed || FactionSupplies.skin(held)) && !held.isEmpty()) {
            this.supplies.add(held);
        }
        this.consuming = false;
        this.setItemSlot(EquipmentSlot.OFFHAND, this.stowedOffhand);
        this.stowedOffhand = ItemStack.EMPTY;
    }

    public boolean crawling() {
        return this.getPose() == Pose.SWIMMING;
    }

    // Holds the pose an origin power forces, as MMS Origins does for a player, and stands back up once the power lets go.
    private void tickForcedPose() {
        Pose forced = FactionOrigin.POWERS && this.origin() != FactionOrigin.NONE && !this.downed() ? FactionOriginPowers.forcedPose(this) : null;
        if (forced != null) {
            if (this.getPose() != forced) {
                this.setPose(forced);
            }
            this.forcedPose = forced;
        } else if (this.forcedPose != null) {
            if (this.getPose() == this.forcedPose && !this.downed() && !this.armless()) {
                this.setCrawling(!this.roomToStand());
            }
            this.forcedPose = null;
        }
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
        if (pose == Pose.CROUCHING) {
            return CROUCH_SIZE;
        }
        return super.getDefaultDimensions(pose);
    }

    FactionFlight flight() {
        return this.flight;
    }

    FactionTactics tactics() {
        return this.tactics;
    }

    // Whether its origin can fly, as an avian, fairy or blazeborn player can.
    public boolean winged() {
        return this.flight.winged();
    }

    // Opens or folds the wings of an elytra glide: the game's fall-flying flag, which carries it and puts it in the gliding pose.
    void setGliding(boolean gliding) {
        this.setSharedFlag(FLAG_FALL_FLYING, gliding);
    }

    // Keeps the glide for as long as the member's wings hold it, with no elytra to wear down.
    @Override
    protected void updateFallFlying() {
        this.checkFallDistanceAccumulation();
        if (!this.flight.gliding()) {
            this.setGliding(false);
        }
    }

    // A member hovering on its wings or settling to land takes no fall damage; a glide falls under the game's own elytra rules, and one saved in the air lands softly once after it loads.
    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource source) {
        if (this.softLanding) {
            this.softLanding = false;
            return false;
        }
        boolean hovering = this.flight.inAir() && !this.flight.gliding();
        return !hovering && super.causeFallDamage(fallDistance, damageMultiplier, source);
    }

    @Override
    public void setSprinting(boolean sprinting) {
        super.setSprinting(sprinting);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(SPRINT_TRIM);
        if (sprinting) {
            speed.addTransientModifier(new AttributeModifier(SPRINT_TRIM, SPRINT_TRIM_AMOUNT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    // In the air on its origin's wings, synced so the renderer can let its legs hang.
    public boolean flying() {
        return this.entityData.get(DATA_FLYING);
    }

    public void setFlying(boolean flying) {
        this.entityData.set(DATA_FLYING, flying);
    }

    // A floran folded fully into leaf form, synced so the renderer draws it as a bush.
    public boolean leafFormShown() {
        return this.entityData.get(DATA_LEAF_FORM);
    }

    void setLeafFormShown(boolean shown) {
        this.entityData.set(DATA_LEAF_FORM, shown);
    }

    // On its way to revive a downed crewmate, which it hurries to do.
    boolean reviving() {
        return this.reviving;
    }

    void setReviving(boolean reviving) {
        this.reviving = reviving;
    }

    // Running from a feared player who is chasing it, which it hurries to do.
    boolean fleeing() {
        return this.fleeing;
    }

    void setFleeing(boolean fleeing) {
        this.fleeing = fleeing;
    }

    FactionPeace peace() {
        return this.peace;
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

    // How far the fall to the ground has gone, from 0 to 1 over the first 12 ticks downed; the renderer reads it on the client.
    public float collapse(float partialTick) {
        if (!this.downed()) {
            this.downedSince = -1;
            return 0.0f;
        }
        if (this.downedSince < 0) {
            this.downedSince = this.tickCount;
        }
        return Math.min(1.0f, (this.tickCount - this.downedSince + partialTick) / COLLAPSE_TICKS);
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

    // Neutral in the Overworld by day: it starts no fights then and only answers whoever attacks its crew. At night it hunts as it does in the dungeon.
    public boolean isNeutral() {
        Level level = this.level();
        return level.dimension() == Level.OVERWORLD && !level.isThundering() && level.getOverworldClockTime() % DAY_LENGTH < NIGHTFALL;
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

    // Whether its origin's restrictions keep it from using the item, as they would a player of the origin.
    boolean forbids(ItemStack stack) {
        return FactionOrigin.POWERS && this.origin() != FactionOrigin.NONE && FactionOriginPowers.forbids(this, stack);
    }

    // In or folding into a floran's leaf form, which leaves no hands for items or weapons.
    boolean armless() {
        return this.origin() == FactionOrigin.FLORAN && (this.powers.floran().leafForm() || this.powers.floran().changing());
    }

    // Riding a Grovekeeper as its pilot.
    boolean piloting() {
        return ORIGINS && this.getVehicle() != null && FactionGrovekeeper.piloting(this);
    }

    // Followers of a living leader take the leader's targets instead of choosing their own.
    private boolean picksOwnTargets() {
        return !this.isNeutral() && this.liveLeader() == null;
    }

    // A human holds back from a player whose kit is better than its own until the player attacks it.
    @Override
    public boolean mayStartFeud(LivingEntity target) {
        return this.picksOwnTargets() && !this.cowedBy(target);
    }

    // Who a swing may catch besides its target: players it is hostile to, and mobs fighting its faction, never its own faction or allies.
    public boolean swingEnemy(LivingEntity entity) {
        if (this.considersEntityAsAlly(entity)) {
            return false;
        }
        if (entity instanceof Player player) {
            boolean hostile = !this.isNeutral() || player == this.getTarget() || player.getUUID().equals(this.provoker);
            return hostile && !player.isCreative() && !player.isSpectator();
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

    // Never turns on its own crew or the crew's summons, whoever pointed it there.
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && this.considersEntityAsAlly(target)) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Crewmates and the crew's summons cannot hurt it, so a stray swoop or arrow never sets the crew on one another.
        if (source.getEntity() != null && this.considersEntityAsAlly(source.getEntity())) {
            return false;
        }
        // As AbsolutRevive treats a downed player: only a creature's hit lands, and it finishes the member; falls, fire and the like pass over it.
        if (this.downed()) {
            if (source.getEntity() == null || amount <= 0.0f || this.isInvulnerableTo(level, source)) {
                return false;
            }
            this.downedState.finish(level, source);
            return true;
        }
        if (this.piloting() && FactionGrovekeeper.shields(this, source)) {
            return FactionGrovekeeper.takeHit(level, this, source, amount);
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            this.rally(level, attacker);
            this.powers.rallyKin(level, attacker);
        }
        return hurt;
    }

    // An attack points the whole group's idle members at the attacker.
    private void rally(ServerLevel level, LivingEntity attacker) {
        AABB area = this.getBoundingBox().inflate(RALLY_RANGE);
        for (FactionMember member : level.getEntitiesOfClass(FactionMember.class, area, member -> member.faction == this.faction)) {
            if (attacker instanceof Player) {
                member.provoker = attacker.getUUID();
                member.peace.breakWith(attacker.getUUID());
            }
            if (member.getTarget() == null) {
                member.setTarget(attacker);
            }
        }
    }

    // Gives a member saved before origins carried real powers its origin's powers, and drops the effects and armor that once stood in for them.
    private void tickOrigin() {
        if (this.originChecked) {
            return;
        }
        this.originChecked = true;
        for (Holder<MobEffect> effect : List.of(MobEffects.SLOW_FALLING, MobEffects.JUMP_BOOST, MobEffects.WATER_BREATHING, MobEffects.REGENERATION)) {
            MobEffectInstance lasting = this.getEffect(effect);
            if (lasting != null && lasting.isInfiniteDuration()) {
                this.removeEffect(effect);
            }
        }
        removeStandIn(this.getAttribute(Attributes.SCALE), "fairy_scale");
        removeStandIn(this.getAttribute(Attributes.ARMOR), "shulk_armor");
        removeStandIn(this.getAttribute(Attributes.ARMOR_TOUGHNESS), "gilded_bearing");
        FactionOrigin origin = this.origin();
        if (FactionOrigin.POWERS && origin != FactionOrigin.NONE && !FactionOriginPowers.granted(this)) {
            FactionOriginPowers.grant(this, origin);
        }
    }

    private static void removeStandIn(@Nullable AttributeInstance attribute, String name) {
        if (attribute != null) {
            attribute.removeModifier(MmsDungeons.id(name));
        }
    }

    // Holds a power key down this tick, as a player of its origin would.
    void holdKey(String key) {
        this.keysHeld.add(key);
    }

    // Sends the held keys to the origin's powers: sneaking follows the sneak key, and a key's first tick is its press.
    private void pressKeys() {
        boolean sneaking = this.keysHeld.contains(FactionOriginPowers.SNEAK);
        if (this.isShiftKeyDown() != sneaking) {
            this.setShiftKeyDown(sneaking);
        }
        if (FactionOrigin.POWERS && this.origin() != FactionOrigin.NONE) {
            for (String key : this.keysHeld) {
                FactionOriginPowers.key(this, key, !this.keysBefore.contains(key));
            }
        }
        this.keysBefore.clear();
        this.keysBefore.addAll(this.keysHeld);
        this.keysHeld.clear();
    }

    // Puts back a human that Random Mob Sizes resized before it was told to skip them, known by its stat changes: player scale, and none of those changes. An origin's own scale, such as the Enderian's, stays.
    private void undoRandomSize() {
        boolean resized = removeRandomSizeModifier(this.getAttribute(Attributes.MAX_HEALTH), "health_scale");
        resized |= removeRandomSizeModifier(this.getAttribute(Attributes.MOVEMENT_SPEED), "speed_scale");
        resized |= removeRandomSizeModifier(this.getAttribute(Attributes.ATTACK_DAMAGE), "damage_scale");
        AttributeInstance scale = this.getAttribute(Attributes.SCALE);
        if (resized && scale != null) {
            scale.setBaseValue(1.0);
            this.setHealth(Math.min(this.getHealth(), this.getMaxHealth()));
        }
    }

    private static boolean removeRandomSizeModifier(@Nullable AttributeInstance attribute, String name) {
        return attribute != null && attribute.removeModifier(Identifier.fromNamespaceAndPath("random_mob_sizes", name));
    }

    // Opens an umbrella in the off hand when the weather would hurt it, as a player of its origin would, and puts it away once it is safe. With Combat Amenities it lives on the back and trades places with the off-hand item.
    private void tickUmbrella(ServerLevel level) {
        if (this.tickCount % UMBRELLA_CHECK_TICKS != 0 || this.consuming || this.isUsingItem()) {
            return;
        }
        Item umbrella = BuiltInRegistries.ITEM.getValue(UMBRELLA);
        if (umbrella == Items.AIR) {
            return;
        }
        ItemStack held = this.getOffhandItem();
        boolean needs = !this.downed() && switch (this.origin()) {
            case PHANTOM -> level.dimension() == Level.OVERWORLD && level.isBrightOutside() && level.canSeeSky(this.blockPosition());
            case ENDERIAN, BLAZEBORN -> level.isRainingAt(this.blockPosition()) || level.isRainingAt(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ()));
            default -> false;
        };
        if (needs == held.is(umbrella)) {
            return;
        }
        EquipmentSlot back = FactionSlots.back();
        if (back == null) {
            if (needs && held.isEmpty()) {
                this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(umbrella));
                this.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
            } else if (!needs) {
                this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                this.setDropChance(EquipmentSlot.OFFHAND, DROP_CHANCE[this.faction.ordinal()]);
            }
            return;
        }
        ItemStack stowed = this.getItemBySlot(back);
        if (needs && !stowed.is(umbrella)) {
            return;
        }
        this.setItemSlot(EquipmentSlot.OFFHAND, stowed);
        this.setItemSlot(back, held);
    }

    // Returns the belt item and any back item but the umbrella to the pack so they drop with it.
    private void packSlots() {
        EquipmentSlot belt = FactionSlots.belt();
        EquipmentSlot back = FactionSlots.back();
        if (belt != null && !this.getItemBySlot(belt).isEmpty()) {
            this.supplies.add(this.getItemBySlot(belt));
            this.setItemSlot(belt, ItemStack.EMPTY);
        }
        if (back != null && !this.getItemBySlot(back).isEmpty() && !this.getItemBySlot(back).is(BuiltInRegistries.ITEM.getValue(UMBRELLA))) {
            this.supplies.add(this.getItemBySlot(back));
            this.setItemSlot(back, ItemStack.EMPTY);
        }
    }

    // Packs an umbrella on the back of a member whose origin needs one.
    private void stowUmbrella() {
        EquipmentSlot back = FactionSlots.back();
        Item umbrella = BuiltInRegistries.ITEM.getValue(UMBRELLA);
        boolean needed = this.origin() == FactionOrigin.PHANTOM || this.origin() == FactionOrigin.ENDERIAN || this.origin() == FactionOrigin.BLAZEBORN;
        if (back != null && needed && umbrella != Items.AIR && this.getItemBySlot(back).isEmpty()) {
            this.setItemSlot(back, new ItemStack(umbrella));
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.undoRandomSize();
        this.tickOrigin();
        this.tickUmbrella(level);
        this.climate.tick(level);
        this.travel.tick(level);
        this.flight.tick(level);
        this.downedState.tick(level);
        this.powers.tickAlways(level);
        this.pressKeys();
        if (!this.downed() && !this.crawling() && !this.roomToStand()) {
            this.setCrawling(true);
        } else if (!this.downed() && !this.armless() && this.crawling() && this.roomToStand() && (this.getNavigation().getPath() == null || !this.lowGapAhead(this.getNavigation().getPath(), CRAWL_LOOKAHEAD))) {
            this.setCrawling(false);
        }
        this.tickForcedPose();
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
            if (this.cowedBy(this.getTarget()) || this.dawnCalledOff(this.getTarget())) {
                this.setTarget(null);
            }
        }
    }

    // By day a night hunt ends: a player it went after is let go unless that player attacked the crew.
    private boolean dawnCalledOff(@Nullable LivingEntity target) {
        return this.isNeutral() && target instanceof Player player && !player.getUUID().equals(this.provoker);
    }

    // Whether this player has attacked its crew.
    boolean provokedBy(Player player) {
        return player.getUUID().equals(this.provoker);
    }

    // Lets go of a grudge against a player who made peace with it, and stops fighting them.
    void forgive(Player player) {
        if (this.provokedBy(player)) {
            this.provoker = null;
        }
        if (this.getTarget() == player) {
            this.setTarget(null);
        }
    }

    // Hands an offering to a human that is waiting on one after a peace sign.
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && this.peace.takeOffering(player, player.getItemInHand(hand))) {
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.mobInteract(player, hand);
    }

    // A player whose kit beats the crew's, judged by the leader when there is one, or who made peace with it, and who has not attacked the crew.
    public boolean cowedBy(@Nullable LivingEntity target) {
        if (!(target instanceof Player player) || player.getUUID().equals(this.provoker)) {
            return false;
        }
        if (this.peace.friendlyTo(player, this.level().getGameTime())) {
            return true;
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
        this.setOrigin(origin);
        this.powers.onSpawn();
        FactionSpawns.Crew crew = groupData instanceof FactionSpawns.Crew joined ? joined : null;
        if (crew != null) {
            this.leaderId = crew.leader();
        } else {
            this.leader = this.faction.led();
        }
        FactionGear.equip(this, random);
        this.supplies.addAll(FactionSupplies.roll(this.faction, this.tier, random, level, this.blockPosition()));
        this.stowUmbrella();
        if (origin == FactionOrigin.FLORAN) {
            this.supplies.add(FactionSupplies.water(FLORAN_WATER));
            this.powers.floran().spawn(random);
        }
        if (FactionOrigin.POWERS && origin != FactionOrigin.NONE) {
            FactionOriginPowers.obey(this, this.supplies);
        }
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
        this.climate.save(output);
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
        this.leader = input.getBooleanOr("Leader", false);
        this.leaderId = input.getString("LeaderId").map(UUID::fromString).orElse(null);
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            this.setCover(slot, FactionCover.upgrade(slot, input.read("Cover_" + slot.getName(), ItemStack.CODEC).orElse(ItemStack.EMPTY)));
        }
        this.thrownWeapon = input.read("ThrownWeapon", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.thrownTicks = input.getIntOr("ThrownTicks", this.thrownWeapon.isEmpty() ? 0 : 1);
        this.climate.load(input);
        this.supplies.clear();
        this.supplies.addAll(input.read("Supplies", ItemStack.CODEC.listOf()).orElse(List.of()));
        input.read("StowedOffhand", ItemStack.OPTIONAL_CODEC).ifPresent(stowed -> {
            this.supplies.add(this.getOffhandItem());
            this.setItemSlot(EquipmentSlot.OFFHAND, stowed);
        });
        if (input.getBooleanOr("Flying", false)) {
            this.flight.forceGround();
            this.softLanding = true;
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
        this.packSlots();
        this.supplies.addAll(this.powers.leftovers());
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        for (ItemStack supply : this.supplies) {
            this.spawnAtLocation(level, supply);
        }
        this.supplies.clear();
    }
}
