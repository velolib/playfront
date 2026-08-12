package dev.velolib.playfront.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.velolib.playfront.PlayfrontClient;
import dev.velolib.playfront.config.adapters.ColorTypeAdapter;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class PlayfrontConfig {
    public static final int CONFIG_VERSION = 5;

    private static final File CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("playfront.json").toFile();
    private static final File TEMP_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("playfront.json.tmp").toFile();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Color.class, new ColorTypeAdapter())
            .create();

    public static PlayfrontConfig INSTANCE;

    // --- Config Variables ---
    public int version = 5;
    public int pollingRateMs = 1000;
    public int widgetX = 10;
    public int widgetY = 10;
    public int widgetWidth = 175;
    public float widgetScale = 0.0f; // 0.0f = Follow GUI

    public boolean showCoverArt = true;
    public String fontId = "minecraft:default";
    public BackgroundVariant backgroundVariant = BackgroundVariant.COLOR;
    public Color backgroundColor = new Color(0x80000000, true);
    public boolean enableBorder = true;
    public Color borderColor = new Color(0xFF444444, true);
    // --- Element Color & Feature Options ---
    public Color titleColor = new Color(0xFFFFFFFF, true);
    public Color artistColor = new Color(0xFFAAAAAA, true);
    public Color timeAndIconColor = new Color(0xFFAAAAAA, true);
    public boolean showProgressBar = true;
    public Color progressBarActiveColor = new Color(0xFF1DB954, true);
    public Color progressBarInactiveColor = new Color(0xFF444444, true);
    public PlayfrontConfig() {
        // Default constructor for GSON
    }

    public static void load() {
        if (INSTANCE == null) {
            INSTANCE = new PlayfrontConfig();
        }

        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            PlayfrontConfig loaded = GSON.fromJson(reader, PlayfrontConfig.class);
            if (loaded != null) {
                INSTANCE = loaded;
                INSTANCE.validate();
            }
        } catch (Exception e) {
            PlayfrontClient.LOGGER.error("Failed to load config! Creating backup.", e);
            backupCorruptedConfig();
            INSTANCE = new PlayfrontConfig();
            save();
        }

        if (INSTANCE != null && INSTANCE.version < CONFIG_VERSION) {
            handleMigration(INSTANCE);
        }
    }

    private static void handleMigration(PlayfrontConfig loaded) {
        PlayfrontClient.LOGGER.info("Migrating Playfront Config from v{} to v{}", loaded.version, CONFIG_VERSION);
        loaded.version = CONFIG_VERSION;
        save();
    }

    public static void save() {
        try {
            try (FileWriter writer = new FileWriter(TEMP_FILE)) {
                GSON.toJson(INSTANCE, writer);
            }
            Files.move(TEMP_FILE.toPath(), CONFIG_FILE.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            PlayfrontClient.LOGGER.error("Critical error during config save!", e);
        }
    }

    private static void backupCorruptedConfig() {
        if (CONFIG_FILE.exists()) {
            File backup = new File(CONFIG_FILE.getAbsolutePath() + ".bak");
            CONFIG_FILE.renameTo(backup);
        }
    }

    public void validate() {
        this.pollingRateMs = Math.max(1000, this.pollingRateMs);
        this.widgetX = Math.max(0, this.widgetX);
        this.widgetY = Math.max(0, this.widgetY);
        this.widgetWidth = Math.clamp(this.widgetWidth, 120, 800);
        this.widgetScale = Math.max(0.0f, this.widgetScale);

        if (this.fontId == null || this.fontId.trim().isEmpty()) {
            this.fontId = "minecraft:default";
        }

        if (this.backgroundVariant == null) this.backgroundVariant = BackgroundVariant.COLOR;
        if (this.backgroundColor == null) this.backgroundColor = new Color(0x80000000, true);
        if (this.borderColor == null) this.borderColor = new Color(0xFF444444, true);
        if (this.titleColor == null) this.titleColor = new Color(0xFFFFFFFF, true);
        if (this.artistColor == null) this.artistColor = new Color(0xFFAAAAAA, true);
        if (this.timeAndIconColor == null) this.timeAndIconColor = new Color(0xFFAAAAAA, true);
        if (this.progressBarActiveColor == null) this.progressBarActiveColor = new Color(0xFF1DB954, true);
        if (this.progressBarInactiveColor == null) this.progressBarInactiveColor = new Color(0xFF444444, true);
    }

    // --- Background Variants & Style Options ---
    public enum BackgroundVariant {
        COLOR,
        BUTTON,
        TOOLTIP,
        NONE
    }
}