package dev.velolib.playfront;

import com.mojang.blaze3d.platform.InputConstants;
import dev.velolib.playfront.config.PlayfrontConfig;
import dev.velolib.playfront.media.MediaManager;
import dev.velolib.playfront.ui.screen.EditScreen;
import dev.velolib.playfront.ui.widget.MediaWidget;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayfrontClient implements ClientModInitializer {
    public static final String MOD_ID = "playfront";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    public static final KeyMapping TOGGLE_WIDGET = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key." + MOD_ID + ".toggle_widget",
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY,
                    0
            )
    );
    public static final KeyMapping PLAY_PAUSE = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key." + MOD_ID + ".play_pause",
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY,
                    1
            )
    );
    public static final KeyMapping NEXT_TRACK = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key." + MOD_ID + ".next_track",
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY,
                    2
            )
    );
    public static final KeyMapping PREV_TRACK = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key." + MOD_ID + ".prev_track",
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY,
                    3
            )
    );
    public static final KeyMapping CYCLE_PLAYER = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key." + MOD_ID + ".cycle_player",
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY,
                    4
            )
    );

    public static MediaWidget hudWidget;

    public static boolean isWidgetVisible = true;

    public static void devLogger(String message) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            LOGGER.info("DEV - [ {} ]", message);
        }
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Playfront Client...");

        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("win")) {
            LOGGER.error("Playfront is only supported on Windows! Aborting initialization.");
            return;
        }

        PlayfrontConfig.load();

        MediaManager.startPolling();

        hudWidget = new MediaWidget(
                PlayfrontConfig.INSTANCE.widgetX,
                PlayfrontConfig.INSTANCE.widgetY,
                PlayfrontConfig.INSTANCE.widgetWidth,
                MediaWidget.DEFAULT_HEIGHT,
                Component.empty()
        );

        ClientTickEvents.END_CLIENT_TICK.register(_ -> {
            while (TOGGLE_WIDGET.consumeClick()) {
                isWidgetVisible = !isWidgetVisible;
            }
            while (PLAY_PAUSE.consumeClick()) {
                MediaManager.togglePlayPause();
            }
            while (NEXT_TRACK.consumeClick()) {
                MediaManager.nextTrack();
            }
            while (PREV_TRACK.consumeClick()) {
                MediaManager.previousTrack();
            }
            while (CYCLE_PLAYER.consumeClick()) {
                MediaManager.cyclePlayer();
            }
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("playfront", "media_widget"),
                (graphics, _) -> {
                    Minecraft client = Minecraft.getInstance();

                    if (!isWidgetVisible || client.getDebugOverlay().showDebugScreen() || client.gui.screen() instanceof EditScreen) {
                        return;
                    }

                    hudWidget.extractWidgetRenderState(graphics, 0, 0, 1.0f);
                }
        );

        ClientLifecycleEvents.CLIENT_STOPPING.register(_ -> {
            MediaManager.stopPolling();
        });
    }
}