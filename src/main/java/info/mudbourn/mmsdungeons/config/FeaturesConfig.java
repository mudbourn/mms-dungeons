package info.mudbourn.mmsdungeons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// The JSON config for parties, corpses and the Aerial Hell portal blast, loaded once and re-saved to backfill new defaults.
public final class FeaturesConfig {

    private static final Logger LOG = LoggerFactory.getLogger("mms_dungeons");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final FeaturesConfig INSTANCE = load();

    public int inviteSeconds = 60;
    public boolean hudEnabled = true;
    public String hudHorizontal = "left";
    public String hudVertical = "middle";
    public int hudOffsetX = 4;
    public int hudOffsetY = 0;
    public int syncIntervalTicks = 5;
    public double outlineDistance = 256.0;
    public int alertSeconds = 5;
    public int alertCooldownTicks = 40;
    public double portalWarningRadius = 64.0;
    public float portalBlastPower = 6.0F;

    public static FeaturesConfig get() {
        return INSTANCE;
    }

    private static FeaturesConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("mms_dungeons_features.json");
        FeaturesConfig config = new FeaturesConfig();
        if (Files.exists(path)) {
            try {
                FeaturesConfig loaded = GSON.fromJson(Files.readString(path), FeaturesConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            } catch (IOException | RuntimeException e) {
                LOG.error("Failed to read {}, using defaults", path, e);
            }
        }
        config.normalize();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(config));
        } catch (IOException e) {
            LOG.error("Failed to write {}", path, e);
        }
        return config;
    }

    private void normalize() {
        inviteSeconds = Math.max(5, inviteSeconds);
        syncIntervalTicks = Math.max(1, syncIntervalTicks);
        outlineDistance = Math.max(0.0, outlineDistance);
        alertSeconds = Math.max(1, alertSeconds);
        alertCooldownTicks = Math.max(0, alertCooldownTicks);
        portalWarningRadius = Math.max(0.0, portalWarningRadius);
        portalBlastPower = Math.max(1.0F, portalBlastPower);
        if (hudHorizontal == null || !hudHorizontal.equals("right")) {
            hudHorizontal = "left";
        }
        if (hudVertical == null || !(hudVertical.equals("top") || hudVertical.equals("bottom"))) {
            hudVertical = "middle";
        }
    }
}
