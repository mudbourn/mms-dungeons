package info.mudbourn.mmsdungeons.client.compat.wthit;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Mimic;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IModInfo;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.ITooltipComponent;
import mcp.mobius.waila.api.IWailaClientPlugin;
import mcp.mobius.waila.api.IWailaConfig;
import mcp.mobius.waila.api.WailaConstants;
import mcp.mobius.waila.api.component.ItemComponent;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

// Makes WTHIT show a closed Mimic as a plain chest: the chest's name, icon and mod line. The health and
// attribute lines are held back by WthitAttributesMixin. Once the Mimic opens, WTHIT shows it as itself.
public final class MimicDisguisePlugin implements IWailaClientPlugin {

    // Runs after WTHIT's own entity name, icon and mod line, so these replace them.
    private static final int PRIORITY = 5000;

    @Override
    public void register(IClientRegistrar registrar) {
        registrar.icon(Disguise.INSTANCE, Mimic.class, PRIORITY);
        registrar.head(Disguise.INSTANCE, Mimic.class, PRIORITY);
        registrar.tail(Disguise.INSTANCE, Mimic.class, PRIORITY);
    }

    // Whether WTHIT is looking at a Mimic that still passes for a chest.
    public static boolean disguised(Object entity) {
        return entity instanceof Mimic mimic && !mimic.isRevealed();
    }

    private enum Disguise implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public @Nullable ITooltipComponent getIcon(IEntityAccessor accessor, IPluginConfig config) {
            return disguised(accessor.getEntity()) ? new ItemComponent(Blocks.CHEST) : null;
        }

        @Override
        public void appendHead(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
            if (!disguised(accessor.getEntity())) {
                return;
            }
            IWailaConfig.Formatter formatter = IWailaConfig.get().getFormatter();
            tooltip.setLine(WailaConstants.OBJECT_NAME_TAG, formatter.blockName(Blocks.CHEST.getName()));
            if (config.getBoolean(WailaConstants.CONFIG_SHOW_REGISTRY)) {
                tooltip.setLine(WailaConstants.REGISTRY_NAME_TAG, formatter.registryName(Blocks.CHEST.builtInRegistryHolder().key().identifier()));
            }
        }

        @Override
        public void appendTail(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
            if (disguised(accessor.getEntity()) && config.getBoolean(WailaConstants.CONFIG_SHOW_MOD_NAME)) {
                tooltip.setLine(WailaConstants.MOD_NAME_TAG, IWailaConfig.get().getFormatter().modName(IModInfo.get(Blocks.CHEST).getName()));
            }
        }
    }
}
