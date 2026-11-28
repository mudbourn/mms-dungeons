package info.mudbourn.mmsdungeons.corpse;

import java.util.Base64;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.GameProfile;
import com.google.common.collect.Multimap;
import com.google.common.collect.LinkedHashMultimap;
import com.mojang.serialization.Codec;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;

// The light world marker for a corpse: a downed mannequin in the owner's skin, tagged with its owner.
public final class CorpseMarkers {

    // Random Mob Sizes skips entities with this tag.
    private static final String SIZE_IGNORE_TAG = "global.ignore";
    private static final Identifier SCALE_ID = MmsDungeons.id("corpse_scale");
    private static final EquipmentSlot[] WORN_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    // The body yaw a corpse lies at, synced so the client draws it in the downed pose.
    public static AttachmentType<Float> FACING;

    static final String TAG = "mms_dungeons_corpse";
    private static final String OWNER_TAG_PREFIX = "mms_dungeons_corpse_owner_";
    private static final int MAX_DROP = 32;
    private static final double STEP = 0.5;
    private static final double SETTLE_TOLERANCE = 0.01;
    private static final int SIGNATURE_BYTES = 512;

    private CorpseMarkers() {
    }

    static void register() {
        FACING = AttachmentRegistry.<Float>builder()
            .persistent(Codec.FLOAT)
            .syncWith(ByteBufCodecs.FLOAT, AttachmentSyncPredicate.all())
            .buildAndRegister(MmsDungeons.id("corpse_facing"));
    }

    // Builds the marker on the ground under the given spot, not yet in the world.
    static Mannequin create(ServerPlayer owner, ServerLevel level, Vec3 at, float facing, List<ItemStack> worn, float scale) {
        ResolvableProfile profile = ResolvableProfile.createResolved(withoutBadSignatures(owner.getGameProfile()));
        Tag profileTag = ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile).getOrThrow();
        Mannequin body = body(level, at, facing, profileTag, Component.literal(owner.getGameProfile().name()), CollisionContext.of(owner));
        body.addTag(TAG);
        body.addTag(OWNER_TAG_PREFIX + owner.getUUID());
        dress(body, worn, scale);
        return body;
    }

    // The profile with any malformed property signature dropped, so clients do not log a failed check for every corpse that wears it.
    private static GameProfile withoutBadSignatures(GameProfile profile) {
        Multimap<String, Property> properties = LinkedHashMultimap.create();
        for (Property property : profile.properties().values()) {
            properties.put(property.name(), wellSigned(property) ? property : new Property(property.name(), property.value()));
        }
        return new GameProfile(profile.id(), profile.name(), new PropertyMap(properties));
    }

    private static boolean wellSigned(Property property) {
        if (!property.hasSignature()) {
            return true;
        }
        try {
            return Base64.getDecoder().decode(property.signature()).length == SIGNATURE_BYTES;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Puts the worn armor, head to feet, on the body for show and sizes it, apart from the base scale the settling resets.
    static void dress(Mannequin body, List<ItemStack> worn, float scale) {
        for (int i = 0; i < worn.size() && i < WORN_SLOTS.length; i++) {
            body.setItemSlot(WORN_SLOTS[i], worn.get(i).copy());
        }
        AttributeInstance size = body.getAttribute(Attributes.SCALE);
        if (size != null && scale != 1.0F) {
            size.addPermanentModifier(new AttributeModifier(SCALE_ID, scale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    // What a body shows in each armor slot, head to feet, picked by the given lookup.
    public static List<ItemStack> wornBy(Function<EquipmentSlot, ItemStack> shown) {
        List<ItemStack> worn = new ArrayList<>();
        for (EquipmentSlot slot : WORN_SLOTS) {
            worn.add(shown.apply(slot).copy());
        }
        return worn;
    }

    // A downed mannequin wearing the given profile, lying on the floor under the spot.
    static Mannequin body(ServerLevel level, Vec3 at, float facing, Tag profile, Component name, CollisionContext context) {
        CompoundTag tag = new CompoundTag();
        tag.put("profile", profile);
        tag.putString("pose", "sleeping");
        tag.putBoolean("immovable", true);
        tag.put("description", ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, Component.literal("Corpse")).getOrThrow());
        tag.putBoolean("Invulnerable", true);
        tag.putBoolean("NoGravity", true);

        Mannequin body = new Mannequin(EntityTypes.MANNEQUIN, level);
        body.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        Vec3 spot = new Vec3(at.x, floorUnder(level, at, context), at.z);
        body.snapTo(spot.x, spot.y, spot.z, facing, 0.0F);
        body.setYBodyRot(facing);
        body.setYHeadRot(facing);
        body.setAttached(FACING, facing);
        body.setCustomName(name);
        body.setCustomNameVisible(true);
        body.addTag(SIZE_IGNORE_TAG);
        return body;
    }

    static UUID ownerOf(Entity entity) {
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(OWNER_TAG_PREFIX)) {
                return UUID.fromString(tag.substring(OWNER_TAG_PREFIX.length()));
            }
        }
        return null;
    }

    // Discards the marker when its chunk is loaded; an unloaded one is cleaned up on load.
    static void remove(MinecraftServer server, CorpseData.Corpse corpse) {
        if (corpse.marker == null) {
            return;
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, corpse.dimension));
        if (level != null && level.getEntity(corpse.marker) != null) {
            level.getEntity(corpse.marker).discard();
        }
    }

    // Moves every loaded corpse marker in this level onto the floor under it, so it settles when nearby blocks change.
    static void settle(ServerLevel level) {
        for (Mannequin marker : level.getEntities(EntityTypes.MANNEQUIN, entity -> entity.entityTags().contains(TAG))) {
            Vec3 at = marker.position();
            double floor = floorUnder(level, at, CollisionContext.empty());
            if (Math.abs(floor - at.y) > SETTLE_TOLERANCE) {
                marker.setPos(at.x, floor, at.z);
            }
            AttributeInstance scale = marker.getAttribute(Attributes.SCALE);
            if (scale != null && scale.getBaseValue() != 1.0) {
                scale.setBaseValue(1.0);
                marker.addTag(SIZE_IGNORE_TAG);
            }
        }
    }

    // Height of the highest surface at most half a block above the given point, searching down from there.
    static double floorUnder(Level level, Vec3 from, CollisionContext context) {
        double ceiling = from.y + STEP;
        BlockPos pos = BlockPos.containing(from.x, ceiling, from.z);
        for (int i = 0; i < MAX_DROP && pos.getY() >= level.getMinY(); i++) {
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos, context);
            if (!shape.isEmpty()) {
                double top = pos.getY() + shape.max(Direction.Axis.Y);
                if (top <= ceiling) {
                    return top;
                }
            }
            pos = pos.below();
        }
        return from.y;
    }
}
