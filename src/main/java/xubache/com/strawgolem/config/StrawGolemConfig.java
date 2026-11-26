package xubache.com.strawgolem.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class StrawGolemConfig {

    private static final Logger LOGGER =
            LoggerFactory.getLogger("strawgolem");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setStrictness(Strictness.STRICT)
            .create();

    private static StrawGolemConfig current = new StrawGolemConfig();

    public AttributeSettings attributes = new AttributeSettings();

    @SerializedName("vanilla_harvesting")
    public boolean vanillaHarvesting = true;

    @SerializedName("auto_compost_seeds")
    public boolean autoCompostSeeds = false;

    @SerializedName("lifespan_enabled")
    public boolean lifespanEnabled = false;

    @SerializedName("lifespan_seconds")
    public int lifespanSeconds = 4800;

    @SerializedName("harvest_horizontal_range")
    public int harvestHorizontalRange = 24;

    @SerializedName("harvest_scan_vertical_range")
    public int harvestScanVerticalRange = 12;

    @SerializedName("harvest_target_vertical_range")
    public int harvestTargetVerticalRange = 3;

    @SerializedName("deposit_horizontal_range")
    public int depositHorizontalRange = 32;

    @SerializedName("deposit_vertical_range")
    public int depositVerticalRange = 8;

    public static final class AttributeSettings {
        @SerializedName("max_health")
        public double maxHealth = 12.0;

        @SerializedName("movement_speed")
        public double movementSpeed = 0.25;

        @SerializedName("wander_max_distance")
        public int wanderMaxDistance = 24;
    }
    public static StrawGolemConfig get() {
        return current;
    }

    public long getLifespanTicks() {
        return this.lifespanEnabled ? this.lifespanSeconds * 20L : 0L;
    }

    public static void load() {
        Path path = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("strawgolem.json");

        try {
            Files.createDirectories(path.getParent());
            StrawGolemConfig loaded;

            if (Files.exists(path)) {
                String json = Files.readString(
                        path,
                        StandardCharsets.UTF_8
                );

                loaded = GSON.fromJson(json, StrawGolemConfig.class);
            } else {
                loaded = new StrawGolemConfig();

                Files.writeString(
                        path,
                        GSON.toJson(loaded),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE
                );
            }

            if (loaded == null || loaded.attributes == null) {
                throw new IllegalArgumentException(
                        "Config and attributes must not be null."
                );
            }

            validateRange(
                    "max_health",
                    loaded.attributes.maxHealth,
                    1.0,
                    1024.0
            );

            validateRange(
                    "wander_max_distance",
                    loaded.attributes.wanderMaxDistance,
                    1,
                    Integer.MAX_VALUE
            );

            validateRange("lifespan_seconds", loaded.lifespanSeconds, 1, Integer.MAX_VALUE);
            validateRange("harvest_horizontal_range", loaded.harvestHorizontalRange, 0, 32);
            validateRange("harvest_scan_vertical_range", loaded.harvestScanVerticalRange, 0, 16);
            validateRange("harvest_target_vertical_range", loaded.harvestTargetVerticalRange,0, loaded.harvestScanVerticalRange);
            validateRange("deposit_horizontal_range", loaded.depositHorizontalRange, 0, 64);
            validateRange("deposit_vertical_range", loaded.depositVerticalRange, 0, 32);

            current = loaded;
            LOGGER.info("Loaded Straw Golem config from {}", path);

        } catch (IOException | JsonParseException | IllegalArgumentException e) {
            current = new StrawGolemConfig();

            LOGGER.error(
                    "Cannot load config at {}. Using defaults; "
                    + "any exitsting config file is left unchanged.",
                    path,
                    e
            );
        }
    }

    private static void validateRange(
            String name,
            double value,
            double min,
            double max
    ) {
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(
                    name + "must be between " + min + " and " + max
            );
        }
    }
}
