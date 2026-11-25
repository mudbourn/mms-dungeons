package info.mudbourn.mmsdungeons.mobs.kobolds.entity;

import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class AbstractKoboldZombie extends Zombie {

    private static final EntityDataAccessor<Boolean> DATA_CONVERTING = SynchedEntityData.defineId(
        AbstractKoboldZombie.class,
        EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<String> DATA_TYPE = SynchedEntityData.defineId(
        AbstractKoboldZombie.class,
        EntityDataSerializers.STRING
    );

    private int convert;

    protected AbstractKoboldZombie(EntityType<? extends AbstractKoboldZombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CONVERTING, false);
        builder.define(DATA_TYPE, ZomboType.BASE.id());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("ZomboType", this.getZomboType().id());
        output.putInt("Convert", this.convert);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setZomboType(ZomboType.byId(input.getStringOr("ZomboType", ZomboType.BASE.id())));
        this.convert = input.getIntOr("Convert", 0);
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level().isClientSide() || !this.isAlive() || this.isNoAi()) {
            return;
        }
        if (this.convert > 1) {
            this.convert--;
            if (!this.isConverting()) {
                this.entityData.set(DATA_CONVERTING, true);
            }
        } else if (this.convert == 1) {
            this.playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.0f);
            this.convertTo(this.getZomboType().cured(), ConversionParams.single(this, true, true), cured -> cured.setCanPickUpLoot(true));
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!this.level().isClientSide() && held.is(Items.GOLDEN_APPLE) && this.hasEffect(MobEffects.WEAKNESS)) {
            Difficulty difficulty = this.level().getDifficulty();
            int strength = difficulty == Difficulty.HARD ? 1 : 0;
            int ticks = switch (difficulty) {
                case EASY -> 1200;
                case NORMAL -> 2400;
                default -> 4800;
            };
            held.consume(1, player);
            player.swing(hand, held.getAttackAnimation(), true);
            this.playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.0f, 1.0f);
            this.removeEffect(MobEffects.WEAKNESS);
            this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, strength));
            this.convert = ticks;
            this.entityData.set(DATA_CONVERTING, true);
        }
        return super.mobInteract(player, hand);
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
        if (reason != EntitySpawnReason.CONVERSION) {
            ZomboType[] types = ZomboType.values();
            this.setZomboType(types[level.getRandom().nextInt(types.length)]);
            this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
            this.populateDefaultEquipmentEnchantments(level, level.getRandom(), difficulty);
        }
        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        switch (this.getZomboType()) {
            case WARRIOR -> {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(KoboldItems.KOBOLD_IRON_AXE));
                this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            case PIRATE_CAPTAIN -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(KoboldItems.KOBOLD_IRON_SWORD));
            case ENGINEER -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            case BASE, PIRATE -> this.setItemSlot(
                EquipmentSlot.MAINHAND,
                new ItemStack(random.nextFloat() >= 0.6f ? Items.CROSSBOW : KoboldItems.KOBOLD_IRON_SWORD)
            );
            default -> {
            }
        }
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
    }

    public boolean isConverting() {
        return this.entityData.get(DATA_CONVERTING);
    }

    public ZomboType getZomboType() {
        return ZomboType.byId(this.entityData.get(DATA_TYPE));
    }

    public void setZomboType(ZomboType type) {
        this.entityData.set(DATA_TYPE, type.id());
    }

    // Takes the look of the kobold this one was before it turned.
    public void setZombo(AbstractKobold kobold) {
        this.setZomboType(ZomboType.of(kobold.getType()));
    }
}
