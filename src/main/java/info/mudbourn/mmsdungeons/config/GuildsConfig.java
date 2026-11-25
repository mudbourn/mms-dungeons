package info.mudbourn.mmsdungeons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// The JSON config for guilds, quests and contracts, loaded once and re-saved to backfill new defaults.
public final class GuildsConfig {

    private static final Logger LOG = LoggerFactory.getLogger("mms_dungeons");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final GuildsConfig INSTANCE = load();

    public long foundingCostPice = 50000L;
    public int questExpiryDays = 7;
    public int contractExpiryDays = 7;
    public int bountiesPerParty = 3;
    public int bountiesPerGuild = 50;
    public int contractCommissionPercent = 15;
    public int otherGuildPostingPercent = 13;
    public int otherGuildOwnPercent = 15;
    public int noGuildPostingPercent = 26;
    public int finishedRetentionDays = 1;
    public int maxProofCount = 2304;
    public int boardReach = 8;
    public int maxNameLength = 24;
    public int maxCharterLength = 600;

    public static GuildsConfig get() {
        return INSTANCE;
    }

    private static GuildsConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("mms_dungeons_guilds.json");
        GuildsConfig config = new GuildsConfig();
        if (Files.exists(path)) {
            try {
                GuildsConfig loaded = GSON.fromJson(Files.readString(path), GuildsConfig.class);
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
        foundingCostPice = Math.max(0L, foundingCostPice);
        questExpiryDays = Math.max(1, questExpiryDays);
        contractExpiryDays = Math.max(1, contractExpiryDays);
        bountiesPerParty = Math.max(1, bountiesPerParty);
        bountiesPerGuild = Math.max(1, bountiesPerGuild);
        contractCommissionPercent = clampPercent(contractCommissionPercent);
        otherGuildPostingPercent = clampPercent(otherGuildPostingPercent);
        otherGuildOwnPercent = clampPercent(otherGuildOwnPercent);
        noGuildPostingPercent = clampPercent(noGuildPostingPercent);
        finishedRetentionDays = Math.max(0, finishedRetentionDays);
        maxProofCount = Math.max(1, maxProofCount);
        boardReach = Math.max(2, boardReach);
        maxNameLength = Math.max(3, maxNameLength);
        maxCharterLength = Math.max(0, maxCharterLength);
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
