package dev.velolib.playfront.media;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import dev.velolib.playfront.config.PlayfrontConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaManager {

    public static volatile String currentTitle = "No Media Playing";
    public static volatile String currentArtist = "";
    public static volatile float currentProgress = 0.0f;

    public static volatile double currentPosition = 0.0;
    public static volatile double currentDuration = 0.0;
    public static volatile boolean isPlaying = false;
    public static volatile String currentAppId = "default";

    public static volatile long lastPollTime = System.currentTimeMillis();

    public static Identifier currentCoverArt = null;
    private static DynamicTexture dynamicTexture = null;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static File tempExe = null;

    public static double getInterpolatedPosition() {
        if (!isPlaying) {
            return currentPosition;
        }
        double secondsSincePoll = (System.currentTimeMillis() - lastPollTime) / 1000.0;
        double estimated = currentPosition + secondsSincePoll;
        return (currentDuration > 0) ? Math.min(estimated, currentDuration) : estimated;
    }

    public static String getCleanAppName(String appId) {
        if (appId == null || appId.isEmpty() || appId.equalsIgnoreCase("default")) {
            return "System Default";
        }
        String lower = appId.toLowerCase();
        if (lower.contains("spotify")) return "Spotify";
        if (lower.contains("chrome")) return "Google Chrome";
        if (lower.contains("msedge") || lower.contains("edge")) return "Microsoft Edge";
        if (lower.contains("firefox")) return "Firefox";
        if (lower.contains("itunes") || lower.contains("apple")) return "Apple Music";
        if (lower.contains("foobar")) return "foobar2000";
        if (lower.contains("vlc")) return "VLC Media Player";

        int lastDot = appId.lastIndexOf('.');
        if (lastDot != -1 && lastDot < appId.length() - 1) {
            String name = appId.substring(lastDot + 1);
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return appId;
    }

    public static float getInterpolatedProgress() {
        if (currentDuration <= 0) return 0.0f;
        return (float) Math.min(1.0, Math.max(0.0, getInterpolatedPosition() / currentDuration));
    }

    public static void startPolling() {
        extractWindowsBridge();

        // Grab the polling rate from config, default to 1000ms if config isn't loaded yet
        long pollingRate = 1000L;
        if (PlayfrontConfig.INSTANCE != null && PlayfrontConfig.INSTANCE.pollingRateMs > 0) {
            pollingRate = PlayfrontConfig.INSTANCE.pollingRateMs;
        }

        scheduler.scheduleAtFixedRate(() -> {
            try { pollWindowsSMTC(); } catch (Exception e) { e.printStackTrace(); }
        }, 0, pollingRate, TimeUnit.MILLISECONDS);
    }

    public static void stopPolling() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }


    public static void play() { sendCommandAsync("play"); }
    public static void pause() { sendCommandAsync("pause"); }
    public static void togglePlayPause() { sendCommandAsync("toggle"); }
    public static void nextTrack() { sendCommandAsync("next"); }
    public static void previousTrack() { sendCommandAsync("prev"); }

    public static void cyclePlayer() {
        if (tempExe == null || !tempExe.exists()) return;

        scheduler.execute(() -> {
            try {
                Process process = new ProcessBuilder(tempExe.getAbsolutePath(), "cycle", currentAppId).start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                String newAppId = reader.readLine();
                process.waitFor();

                if (newAppId != null && !newAppId.trim().isEmpty()) {
                    currentAppId = newAppId.trim();

                    pollWindowsSMTC();

                    Minecraft.getInstance().execute(() -> {
                        String cleanName = getCleanAppName(currentAppId);
                        Minecraft.getInstance().gui.toastManager().addToast(
                                new SystemToast(
                                        SystemToast.SystemToastId.NARRATOR_TOGGLE,
                                        Component.literal("Playfront"),
                                        Component.literal("Active Player: " + cleanName)
                                )
                        );
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void sendCommandAsync(String action) {
        if (tempExe == null || !tempExe.exists()) return;

        scheduler.execute(() -> {
            try {
                Process process = new ProcessBuilder(tempExe.getAbsolutePath(), action, currentAppId).start();
                process.waitFor();

                Thread.sleep(100);
                pollWindowsSMTC();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private static void pollWindowsSMTC() throws Exception {
        if (tempExe == null || !tempExe.exists()) return;

        String requestedAppId = currentAppId;

        Process process = new ProcessBuilder(tempExe.getAbsolutePath(), "info", requestedAppId).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        process.waitFor();

        if (output.contains("\t")) {
            String[] parts = output.split("\t");
            if (parts.length >= 8) {
                String newTitle = parts[0];
                String newArtist = parts[1];
                currentProgress = Float.parseFloat(parts[2].replace(",", "."));
                String artPath = parts[3];

                currentPosition = Double.parseDouble(parts[4].replace(",", "."));
                currentDuration = Double.parseDouble(parts[5].replace(",", "."));
                isPlaying = Boolean.parseBoolean(parts[6]);

                lastPollTime = System.currentTimeMillis();

                String activeAppId = parts[7].trim();

                boolean needsArtUpdate = false;

                if (!activeAppId.equals(requestedAppId)) {
                    currentAppId = activeAppId;
                    needsArtUpdate = true;
                }

                if (needsArtUpdate || !newTitle.equals(currentTitle) || !newArtist.equals(currentArtist)) {
                    currentTitle = newTitle;
                    currentArtist = newArtist;

                    if (!artPath.isEmpty()) {
                        File artFile = new File(artPath);
                        if (artFile.exists()) {
                            Minecraft.getInstance().execute(() -> {
                                try {
                                    if (dynamicTexture != null) {
                                        dynamicTexture.close();
                                    }
                                    if (currentCoverArt != null) {
                                        Minecraft.getInstance().getTextureManager().release(currentCoverArt);
                                    }

                                    NativeImage image;
                                    try (FileInputStream fis = new FileInputStream(artFile)) {
                                        image = NativeImage.read(fis); // Fast native read (PNG/JPG)
                                    } catch (Exception formatException) {
                                        // Fallback for weird formats (WebP, BMP, etc.)
                                        java.awt.image.BufferedImage bufferedImage = javax.imageio.ImageIO.read(artFile);
                                        if (bufferedImage == null) throw new Exception("Image format not recognized");

                                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                                        javax.imageio.ImageIO.write(bufferedImage, "png", baos);
                                        image = NativeImage.read(new java.io.ByteArrayInputStream(baos.toByteArray()));
                                    }

                                    dynamicTexture = new DynamicTexture(() -> "playfront_cover_art", image);
                                    currentCoverArt = Identifier.fromNamespaceAndPath("playfront", "cover_art");
                                    Minecraft.getInstance().getTextureManager().register(currentCoverArt, dynamicTexture);
                                } catch (Exception e) {
                                    currentCoverArt = null;
                                }
                            });
                        }
                    } else {
                        currentCoverArt = null;
                    }
                }
            }
        }
    }

    private static void extractWindowsBridge() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) return;

        try {
            InputStream in = MediaManager.class.getResourceAsStream("/assets/playfront/PlayfrontSMTC.exe");
            if (in == null) return;

            tempExe = File.createTempFile("playfront_smtc", ".exe");
            tempExe.deleteOnExit();
            Files.copy(in, tempExe.toPath(), StandardCopyOption.REPLACE_EXISTING);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}