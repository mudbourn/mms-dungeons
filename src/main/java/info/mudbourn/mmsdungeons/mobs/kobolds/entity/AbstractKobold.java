package info.mudbourn.mmsdungeons.mobs.kobolds.entity;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldItems;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldMobs;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldPotions;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldSounds;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldTags;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

public abstract class AbstractKobold extends AgeableMob implements CrossbowAttackMob, RangedAttackMob {

    private static final EntityDataAccessor<Boolean> DATA_CHARGING = SynchedEntityData.defineId(
        AbstractKobold.class,
        EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_DIAMOND_EYES = SynchedEntityData.defineId(
        AbstractKobold.class,
        EntityDataSerializers.BOOLEAN
    );
    private static final int BREEDING_PAUSE = 18000;
    private static final int TRIDENT_COOLDOWN = 1200;
    private static final float POTION_HEALTH = 12.0f;

    private @Nullable EntityReference<Entity> thrownTrident;
    private ItemStack primary = new ItemStack(KoboldItems.KOBOLD_IRON_SWORD);
    private ItemStack secondary = ItemStack.EMPTY;
    private int cooldown;

    protected AbstractKobold(EntityType<? extends AgeableMob> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(true);
        this.getNavigation().setCanOpenDoors(true);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, LivingEntity.class, 6.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new FloatGoal(this));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!this.primary.isEmpty()) {
            output.store("Primary", ItemStack.CODEC, this.primary);
        }
        if (!this.secondary.isEmpty()) {
            output.store("Trident", ItemStack.CODEC, this.secondary);
        }
        EntityReference.store(this.thrownTrident, output, "ThrownTrident");
        output.putInt("CD", this.cooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("Primary", ItemStack.CODEC).ifPresent(stack -> this.primary = stack);
        input.read("Trident", ItemStack.CODEC).ifPresent(stack -> this.secondary = stack);
        this.thrownTrident = EntityReference.read(input, "ThrownTrident");
        this.cooldown = input.getIntOr("CD", 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGING, false);
        builder.define(DATA_DIAMOND_EYES, false);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (!(target instanceof AbstractKobold)) {
            super.setTarget(target);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        ItemStack weapon = this.getItemInHand(this.rangedHand());
        if (weapon.getItem() instanceof CrossbowItem) {
            this.performCrossbowAttack(this, 2.0f);
        } else if (weapon.is(Items.BOW)) {
            AbstractArrow arrow = ProjectileUtil.getMobArrow(this, this.getProjectile(weapon), power, weapon);
            this.shootAt(arrow, target);
            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.0f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
            this.level().addFreshEntity(arrow);
        } else if (weapon.is(Items.TRIDENT)) {
            this.secondary = weapon;
            ThrownTrident trident = new ThrownTrident(this.level(), this, weapon);
            this.shootAt(trident, target);
            this.playSound(SoundEvents.DROWNED_SHOOT, 1.0f, 1.0f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
            this.level().addFreshEntity(trident);
            this.setItemInHand(InteractionHand.MAIN_HAND, this.primary);
            this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            this.thrownTrident = EntityReference.<Entity>of(trident);
            this.cooldown = TRIDENT_COOLDOWN;
        }
    }

    private void shootAt(Projectile projectile, LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dy = target.getY(1.0 / 3.0) - projectile.getY();
        double dz = target.getZ() - this.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        projectile.shoot(dx, dy + flat * 0.2, dz, 1.6f, 14 - this.level().getDifficulty().getId() * 4);
    }

    // The hand holding a ranged weapon, off hand when the main hand holds none.
    private InteractionHand rangedHand() {
        ItemStack main = this.getMainHandItem();
        boolean ranged = main.getItem() instanceof CrossbowItem || main.is(Items.BOW) || main.is(Items.TRIDENT);
        return ranged ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    // The hand holding a matching item, off hand when the main hand holds none.
    public InteractionHand handHolding(Predicate<ItemStack> matching) {
        return matching.test(this.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(DATA_CHARGING, charging);
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_CHARGING);
    }

    @Override
    public ItemStack equipItemIfPossible(ServerLevel level, ItemStack stack) {
        EquipmentSlot slot = this.getEquipmentSlotForItem(stack);
        if (stack.is(Items.EMERALD) && this.getOffhandItem().isEmpty() || stack.is(Items.TRIDENT)) {
            slot = EquipmentSlot.OFFHAND;
        }
        ItemStack current = this.getItemBySlot(slot);
        if (!this.canReplaceCurrentItem(stack, current, slot) || !this.canHoldItem(stack)) {
            return ItemStack.EMPTY;
        }
        if (!current.isEmpty() && Math.max(this.random.nextFloat() - 0.1f, 0.0f) < this.getDropChances().byEquipment(slot)) {
            this.spawnAtLocation(level, current);
        }
        ItemStack equipped = stack;
        if (stack.is(Items.EMERALD) && stack.getCount() > 1) {
            equipped = stack.split(1);
        } else if (slot.isArmor() && stack.getCount() > 1) {
            equipped = stack.copyWithCount(1);
        }
        this.setItemSlotAndDropWhenKilled(slot, equipped);
        return equipped;
    }

    @Override
    protected boolean canReplaceCurrentItem(ItemStack drop, ItemStack held, EquipmentSlot slot) {
        if (EnchantmentHelper.hasTag(drop, EnchantmentTags.CURSE)) {
            return false;
        }
        if (drop.is(Items.TRIDENT)) {
            return this.canTakeTrident(drop, held);
        }
        if (this.isPreferredWeapon(drop)) {
            return !held.is(Items.TRIDENT) && (!this.isPreferredWeapon(held) || this.canReplaceEqualItem(drop, held));
        }
        if (this.isPreferredWeapon(held)) {
            return false;
        }
        if (drop.has(DataComponents.BLOCKS_ATTACKS)) {
            return held.has(DataComponents.BLOCKS_ATTACKS) ? this.canReplaceEqualItem(drop, held) : held.isEmpty();
        }
        if (drop.is(Items.EMERALD)) {
            return held.isEmpty() && this.typeHolder().is(KoboldTags.TRADERS);
        }
        return drop.is(KoboldTags.ARMOR) && (held.isEmpty() || this.canReplaceEqualItem(drop, held));
    }

    private boolean canTakeTrident(ItemStack drop, ItemStack held) {
        if (held.is(Items.TRIDENT) && this.canReplaceEqualItem(drop, held)) {
            this.secondary = drop;
            return true;
        }
        if (!this.secondary.isEmpty() || !this.getOffhandItem().isEmpty()) {
            return false;
        }
        this.primary = this.getMainHandItem();
        this.secondary = drop;
        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return true;
    }

    @Override
    public boolean canReplaceEqualItem(ItemStack drop, ItemStack held) {
        if (!drop.isEnchanted()) {
            if (this.isPreferredWeapon(drop) && !held.isEnchanted()) {
                return this.attackDamageWith(drop) > this.attackDamageWith(held);
            }
            return super.canReplaceEqualItem(drop, held);
        }
        if (held.isEnchanted()) {
            int dropScore = enchantScore(drop);
            int heldScore = enchantScore(held);
            if (dropScore > heldScore) {
                return true;
            }
            if (this.isPreferredWeapon(drop) && dropScore == heldScore) {
                return this.attackDamageWith(drop) > this.attackDamageWith(held);
            }
        }
        return !held.isEnchanted();
    }

    private double attackDamageWith(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return modifiers.compute(Attributes.ATTACK_DAMAGE, this.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), EquipmentSlot.MAINHAND);
    }

    // Levels of the stack's enchantments, with the ones kobolds prefer counting five times.
    public static int enchantScore(ItemStack stack) {
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(stack));
        int score = 0;
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            int level = enchantments.getLevel(enchantment);
            score += enchantment.is(KoboldTags.PREFERRED_ENCHANTMENTS) ? 5 * level : level;
        }
        return score;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isBaby() ? KoboldSounds.YIP : KoboldSounds.IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isBlocking() ? SoundEvents.SHIELD_BLOCK.value() : KoboldSounds.HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KoboldSounds.DEATH;
    }

    @Override
    public void die(DamageSource source) {
        if (source.getDirectEntity() instanceof Zombie && !this.isBaby() && this.level() instanceof ServerLevel) {
            this.playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.0f);
            if (this.getOffhandItem().is(Items.POTION)) {
                this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            }
            KoboldZombie zombold = this.convertTo(KoboldMobs.KOBOLD_ZOMBIE, ConversionParams.single(this, true, true), converted -> { });
            if (zombold != null) {
                zombold.setZombo(this);
            }
        }
        super.die(source);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand).copy();
        if (!this.isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) {
            this.playSound(KoboldSounds.PURR, 1.0f, this.isBaby() ? 1.2f : 1.0f);
            return InteractionResult.SUCCESS;
        }
        if (this.isBaby()) {
            return super.mobInteract(player, hand);
        }
        if (held.is(KoboldItems.KOBOLD_SPAWN_EGG)) {
            if (this.level() instanceof ServerLevel level) {
                KoboldMobs.KOBOLD_CHILD.spawn(level, this.blockPosition(), EntitySpawnReason.BREEDING);
            }
            player.getItemInHand(hand).consume(1, player);
            return InteractionResult.SUCCESS;
        }
        if (held.is(Items.AMETHYST_SHARD) && this.getAge() <= 0) {
            if (this.isEffectiveAi()) {
                this.feedAmethyst();
                player.getItemInHand(hand).consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (this.getMainHandItem().isEmpty() || this.getOffhandItem().isEmpty()) {
            boolean takesAxe = held.is(ItemTags.AXES) && hand != InteractionHand.MAIN_HAND && this instanceof Kobold;
            boolean takesEmerald = held.is(Items.EMERALD) && this.typeHolder().is(KoboldTags.TRADERS);
            if (takesAxe || takesEmerald) {
                if (this.isEffectiveAi()) {
                    InteractionHand free = this.getMainHandItem().isEmpty() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                    this.setItemInHand(free, takesEmerald ? held.copyWithCount(1) : held);
                    player.getItemInHand(hand).consume(1, player);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    private void feedAmethyst() {
        if (this.getHealth() < this.getMaxHealth()) {
            this.setAge(1200);
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 0));
        } else {
            this.setAge(20000);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        if (reason != EntitySpawnReason.NATURAL) {
            this.setPersistenceRequired();
        }
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        this.populateDefaultEquipmentEnchantments(level, level.getRandom(), difficulty);
        this.setBaby(this instanceof KoboldChild);
        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (this instanceof KoboldWarrior) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(KoboldItems.KOBOLD_IRON_AXE));
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        } else if (this instanceof KoboldRascal || this instanceof KoboldCaptain) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(KoboldItems.KOBOLD_IRON_SWORD));
        } else if (this instanceof KoboldEngineer) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        } else if (this instanceof Kobold) {
            Item weapon = random.nextFloat() >= 0.6f ? Items.CROSSBOW : KoboldItems.KOBOLD_IRON_SWORD;
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
        }
        this.primary = this.getMainHandItem();
        if (this.getType() == KoboldMobs.KOBOLD_PIRATE && random.nextFloat() >= 0.75f) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TRIDENT));
        }
        this.secondary = this.getOffhandItem();
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
    }

    // Replaces the main-hand weapon with depth gear, keeping the remembered primary weapon in step.
    public void setDepthWeapon(ItemStack weapon) {
        this.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        this.primary = weapon;
    }

    @Override
    public ItemStack getProjectile(ItemStack weapon) {
        if (weapon.getItem() instanceof ProjectileWeaponItem projectileWeapon) {
            Predicate<ItemStack> supported = projectileWeapon.getSupportedHeldProjectiles();
            ItemStack held = ProjectileWeaponItem.getHeldProjectile(this, supported);
            return held.isEmpty() ? new ItemStack(Items.ARROW) : held;
        }
        return super.getProjectile(weapon);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level().isClientSide() || !this.isAlive() || !this.isEffectiveAi()) {
            return;
        }
        this.checkTrident();
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (this.getHealth() < POTION_HEALTH && !this.hasEffect(MobEffects.REGENERATION)) {
            this.givePotion(PotionContents.createItemStack(Items.POTION, KoboldPotions.AMETHYST));
        }
        if (this.getAge() > 0) {
            if (this.level() instanceof ServerLevel level && this.canBreed()) {
                this.seekMate(level);
            }
            if (!this.isDiamond()) {
                this.entityData.set(DATA_DIAMOND_EYES, true);
            }
        } else if (this.isDiamond()) {
            this.entityData.set(DATA_DIAMOND_EYES, false);
        }
    }

    private void seekMate(ServerLevel level) {
        AbstractKobold mate = level.getNearestEntity(
            AbstractKobold.class,
            TargetingConditions.forNonCombat().range(8.0).ignoreLineOfSight(),
            this,
            this.getX(),
            this.getY(),
            this.getZ(),
            this.getBoundingBox().inflate(8.0)
        );
        if (Mth.nextInt(this.random, 1, 10) > 8) {
            double spread = this.random.nextGaussian() * 0.02;
            level.sendParticles(
                ParticleTypes.HEART,
                this.getRandomX(1.0),
                this.getRandomY() + 0.5,
                this.getRandomZ(1.0),
                3,
                spread,
                spread,
                spread,
                0.0
            );
        }
        if (mate == null || mate.is(this) || !mate.canBreed()) {
            return;
        }
        if (this.distanceTo(mate) >= 1.0) {
            this.getNavigation().moveTo(mate, 1.0);
            return;
        }
        KoboldChild child = this.getBreedOffspring(level, mate);
        if (child != null) {
            child.setBaby(true);
            child.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0f);
            level.addFreshEntityWithPassengers(child);
        }
        mate.setAge(BREEDING_PAUSE);
        this.setAge(BREEDING_PAUSE);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isAlive() && this.isEffectiveAi()) {
            if ((source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA)) && !this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                this.givePotion(PotionContents.createItemStack(Items.POTION, Potions.LONG_FIRE_RESISTANCE));
            } else if (source.is(DamageTypes.DROWN) && !this.hasEffect(MobEffects.WATER_BREATHING)) {
                this.givePotion(PotionContents.createItemStack(Items.POTION, Potions.LONG_WATER_BREATHING));
            }
        }
        if (this.isBlocking() && source.getDirectEntity() instanceof LivingEntity attacker) {
            this.cooldown = (int) attacker.getSecondsToDisableBlocking() * 20;
        }
        boolean ignored = source.getEntity() instanceof AbstractKobold || source.is(DamageTypes.CAMPFIRE);
        return !ignored && super.hurtServer(level, source, amount);
    }

    public boolean canBreed() {
        return this.getAge() > BREEDING_PAUSE;
    }

    @Override
    public @Nullable KoboldChild getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return KoboldMobs.KOBOLD_CHILD.create(level, EntitySpawnReason.BREEDING);
    }

    public ItemStack getPrimary() {
        return this.primary;
    }

    public ItemStack getSecondary() {
        return this.secondary;
    }

    public void updateItemData() {
        this.primary = this.getMainHandItem();
        this.secondary = this.getOffhandItem();
    }

    public void givePotion(ItemStack potion) {
        ItemStack offhand = this.getOffhandItem();
        if (offhand.isEmpty()) {
            this.setItemInHand(InteractionHand.OFF_HAND, potion);
        } else if (offhand.has(DataComponents.BLOCKS_ATTACKS)) {
            this.secondary = offhand;
            this.setItemInHand(InteractionHand.OFF_HAND, potion);
        } else if (offhand.is(Items.TRIDENT) && !this.isAggressive()) {
            this.setItemInHand(InteractionHand.MAIN_HAND, potion);
        }
    }

    private void checkTrident() {
        boolean waiting = this.cooldown > TRIDENT_COOLDOWN - 20 || !this.getOffhandItem().isEmpty();
        if (this.thrownTrident == null || waiting || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = level.getEntity(this.thrownTrident.getUUID());
        if (entity instanceof ThrownTrident trident && this.is(trident.getOwner())) {
            if (this.distanceTo(trident) < 2.0 || this.cooldown <= 1) {
                this.recallTrident(trident);
            } else if (this.distanceTo(trident) < 32.0 && this.getTarget() == null) {
                this.getNavigation().moveTo(trident, 1.0);
            }
        } else if (entity == null && this.cooldown <= 1) {
            this.recallTrident(null);
        }
    }

    private void recallTrident(@Nullable ThrownTrident trident) {
        this.primary = this.getMainHandItem();
        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        this.setItemInHand(InteractionHand.OFF_HAND, this.secondary);
        this.thrownTrident = null;
        if (this.cooldown > 1 && trident != null) {
            this.cooldown = 0;
            trident.discard();
        }
    }

    public void setCooldown(int ticks) {
        this.cooldown = ticks;
    }

    public int getCooldown() {
        return this.cooldown;
    }

    public boolean isDiamond() {
        return this.entityData.get(DATA_DIAMOND_EYES);
    }

    public boolean isPreferredWeapon(ItemStack stack) {
        return stack.is(KoboldTags.WEAPONS);
    }

    public static List<ItemStack> tradeItems(AbstractKobold kobold, String table) {
        ServerLevel level = (ServerLevel) kobold.level();
        // The trade tables use no context, and 26.3 rejects parameters the EMPTY set does not list.
        LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        return Objects.requireNonNull(level.getServer())
            .reloadableRegistries()
            .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, MmsDungeons.id("kobold/gameplay/" + table)))
            .getRandomItems(params);
    }
}
