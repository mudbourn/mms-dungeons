package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.mobs.factions.FactionSupplies;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// What a faction human does about the weather: carries a lantern or a meal on its belt, drinks from a waterskin when hot, and refills its skins.
final class FactionClimate {

    private static final boolean THERMAL = FabricLoader.getInstance().isModLoaded("thermoo");
    private static final boolean HEAT = THERMAL && FabricLoader.getInstance().isModLoaded("scorchful");
    private static final boolean COLD = THERMAL && FabricLoader.getInstance().isModLoaded("frostiful");
    private static final int CHECK_TICKS = 20;
    private static final int AMBIENT_TICKS = 10;
    // Thermoo scale where Scorchful's first heat penalty starts and where its worse one starts.
    private static final float HOT = 0.5f;
    private static final float SCORCHING = 0.75f;
    private static final float CHILLED = -0.25f;
    private static final float WARM_ENOUGH = 0.0f;
    private static final float TOO_WARM_FOR_LIGHT = 0.25f;
    private static final int DARK = 5;
    private static final long WARD_TICKS = 2400L;
    private static final int SOURCE_DRINKS = 4;
    private static final int CAULDRON_DRINKS = 3;
    private static final int FILLS_PER_BUCKET = 2;
    private static final String FILLS_KEY = "mms_origins_waterskin_fills";

    private final FactionMember member;
    private long wardEnds;

    FactionClimate(FactionMember member) {
        this.member = member;
    }

    void tick(ServerLevel level) {
        if (this.wardEnds != 0L && level.getGameTime() >= this.wardEnds) {
            this.wardEnds = 0L;
            FactionTemperature.ward(this.member, false);
        }
        if (this.member.tickCount % AMBIENT_TICKS == 0 && (HEAT || COLD) && !this.member.isDeadOrDying()) {
            FactionAmbient.tick(this.member, level, HEAT, COLD, AMBIENT_TICKS);
        }
        if (this.member.tickCount % CHECK_TICKS != 0 || this.member.downed() || this.member.consuming() || this.member.isUsingItem()) {
            return;
        }
        float scale = THERMAL ? FactionTemperature.scale(this.member) : 0.0f;
        this.tickBelt(level, scale);
        this.tickDrink(scale);
        this.tickRefill(level);
    }

    // Cools the drinker as a player's waterskin sip does each time a sip finishes.
    void drank(ItemStack drunk) {
        if (THERMAL && FactionSupplies.skin(drunk)) {
            FactionTemperature.coolFromWaterskin(this.member);
            this.wardEnds = this.member.level().getGameTime() + WARD_TICKS;
        }
    }

    void save(ValueOutput output) {
        if (this.wardEnds != 0L) {
            output.putInt("WardTicks", (int) Math.max(0L, this.wardEnds - this.member.level().getGameTime()));
        }
    }

    void load(ValueInput input) {
        int remaining = input.getIntOr("WardTicks", 0);
        if (remaining > 0 && THERMAL) {
            this.wardEnds = this.member.level().getGameTime() + remaining;
            FactionTemperature.ward(this.member, true);
        }
    }

    // Puts a lantern on the belt when it is cold or dark and a meal there otherwise, moving items to and from the pack so none are copied.
    private void tickBelt(ServerLevel level, float scale) {
        EquipmentSlot belt = FactionSlots.belt();
        if (belt == null) {
            return;
        }
        ItemStack worn = this.member.getItemBySlot(belt);
        boolean lit = FactionSupplies.lantern(worn);
        if (this.wantsLight(level, scale, lit)) {
            int lantern = this.find(FactionSupplies::lantern, 1);
            if (!lit && lantern >= 0) {
                this.stow(belt);
                this.member.setItemSlot(belt, this.take(lantern));
            }
        } else if (lit) {
            this.stow(belt);
        } else if (worn.isEmpty()) {
            int meal = this.find(FactionSupplies::meal, 2);
            if (meal >= 0) {
                this.member.setItemSlot(belt, this.take(meal));
            }
        } else if (FactionSupplies.meal(worn) && this.find(FactionSupplies::meal, 1) < 0) {
            this.stow(belt);
        }
    }

    private boolean wantsLight(ServerLevel level, float scale, boolean lit) {
        if (scale >= TOO_WARM_FOR_LIGHT) {
            return false;
        }
        boolean dark = level.getMaxLocalRawBrightness(this.member.blockPosition()) < DARK;
        return lit ? scale < WARM_ENOUGH || dark : scale <= CHILLED || dark;
    }

    // Starts drinking a filled waterskin once hot, in a fight only when badly hot.
    private void tickDrink(float scale) {
        if (!THERMAL || scale < HOT || this.member.getTarget() != null && scale < SCORCHING) {
            return;
        }
        int skin = this.find(stack -> FactionSupplies.skin(stack) && FactionWaterskin.hasDrink(stack), 1);
        if (skin >= 0) {
            this.member.consume(skin);
        }
    }

    // Refills an empty skin from water it stands in, then a water bucket, then in the Nether from cauldron stock.
    private void tickRefill(ServerLevel level) {
        if (!FactionSupplies.SKINS) {
            return;
        }
        int index = this.find(stack -> FactionSupplies.skin(stack) && !FactionWaterskin.hasDrink(stack), 1);
        if (index < 0) {
            return;
        }
        ItemStack skin = this.member.supplies().get(index);
        int bucket = this.find(stack -> stack.is(Items.WATER_BUCKET), 1);
        int cauldron = this.find(stack -> stack.is(Items.CAULDRON), 1);
        if (this.member.isInWater()) {
            FactionWaterskin.fill(level, this.member, skin, SOURCE_DRINKS);
        } else if (bucket >= 0) {
            FactionWaterskin.fillCompletely(level, this.member, skin);
            this.stash(drained(this.take(bucket)));
        } else if (cauldron >= 0 && level.dimension() == Level.NETHER) {
            this.take(cauldron);
            FactionWaterskin.fill(level, this.member, skin, CAULDRON_DRINKS);
        }
    }

    // A water bucket with one more fill taken: the empty bucket once its fills run out.
    private static ItemStack drained(ItemStack bucket) {
        int fills = bucket.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(FILLS_KEY, 0) + 1;
        if (fills >= FILLS_PER_BUCKET) {
            return new ItemStack(Items.BUCKET);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, bucket, tag -> tag.putInt(FILLS_KEY, fills));
        return bucket;
    }

    private int find(Predicate<ItemStack> test, int minCount) {
        List<ItemStack> supplies = this.member.supplies();
        for (int i = 0; i < supplies.size(); i++) {
            ItemStack stack = supplies.get(i);
            if (!stack.isEmpty() && stack.getCount() >= minCount && test.test(stack)) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack take(int index) {
        List<ItemStack> supplies = this.member.supplies();
        ItemStack taken = supplies.get(index).split(1);
        if (supplies.get(index).isEmpty()) {
            supplies.remove(index);
        }
        return taken;
    }

    // Returns what a slot holds to the pack.
    private void stow(EquipmentSlot slot) {
        ItemStack held = this.member.getItemBySlot(slot);
        this.member.setItemSlot(slot, ItemStack.EMPTY);
        this.stash(held);
    }

    private void stash(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (ItemStack held : this.member.supplies()) {
            if (ItemStack.isSameItemSameComponents(held, stack) && held.getCount() + stack.getCount() <= held.getMaxStackSize()) {
                held.grow(stack.getCount());
                return;
            }
        }
        this.member.supplies().add(stack);
    }
}
