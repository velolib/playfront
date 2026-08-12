package dev.velolib.playfront.ui.widget;

import dev.velolib.playfront.config.PlayfrontConfig;
import dev.velolib.playfront.media.MediaManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NonNull;

public class MediaWidget extends AbstractWidget {

    public static final int DEFAULT_WIDTH = 200;
    public static final int DEFAULT_HEIGHT = 56;

    private static final int PADDING = 6;
    private static final int ART_SIZE = 44;
    private static final int ELEMENT_GAP = 5;
    private static final int PROGRESS_BAR_HEIGHT = 2;

    public MediaWidget(int x, int y, Component message) {
        this(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, message);
    }

    public MediaWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    private String formatTime(double seconds) {
        int totalSecs = (int) seconds;
        int mins = totalSecs / 60;
        int secs = totalSecs % 60;
        return String.format("%d:%02d", mins, secs);
    }

    public float getVisualScale() {
        Minecraft minecraft = Minecraft.getInstance();
        PlayfrontConfig config = PlayfrontConfig.INSTANCE;
        if (config != null && config.widgetScale > 0.0f) {
            int currentGuiScale = minecraft.getWindow().calculateScale(minecraft.options.guiScale().get(), minecraft.isEnforceUnicode());
            return config.widgetScale / Math.max(1, currentGuiScale);
        }
        return 1.0f; // 1.0f represents native GUI size
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        float scale = getVisualScale();
        return mouseX >= (double) this.getX()
                && mouseY >= (double) this.getY()
                && mouseX < (double) (this.getX() + this.getWidth() * scale)
                && mouseY < (double) (this.getY() + this.getHeight() * scale);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        PlayfrontConfig config = PlayfrontConfig.INSTANCE;

        float targetScale = getVisualScale();

        Matrix3x2fStack matrices = graphics.pose();
        matrices.pushMatrix();
        if (targetScale != 1.0f) {
            matrices.scale(targetScale, targetScale);
            matrices.translate(
                    this.getX() * (1.0f / targetScale - 1.0f),
                    this.getY() * (1.0f / targetScale - 1.0f)
            );
        }

        boolean showArt = config != null && config.showCoverArt;
        boolean showProgressBar = config == null || config.showProgressBar;

        int x = this.getX();
        int y = this.getY();
        int currentWidth = this.getWidth();
        int currentHeight = this.getHeight();

        int actualArtSize = showArt ? ART_SIZE : 0;
        int actualGap = showArt ? ELEMENT_GAP : 0;

        int artX = x + PADDING;
        int artY = y + PADDING;

        int contentX = artX + actualArtSize + actualGap;
        int contentWidth = Math.max(0, currentWidth - (PADDING * 2 + actualArtSize + actualGap));

        int titleY = y + PADDING;
        int artistY, timeY, barY = 0;

        artistY = titleY + font.lineHeight + ELEMENT_GAP;
        if (showProgressBar) {
            timeY = artistY + font.lineHeight + ELEMENT_GAP;
            barY = timeY + font.lineHeight + ELEMENT_GAP;
        } else {
            int totalOccupied = font.lineHeight * 2 + ELEMENT_GAP;
            int remainingSpace = ART_SIZE - totalOccupied - font.lineHeight;
            timeY = artistY + font.lineHeight + remainingSpace;
        }

        if (config != null && config.backgroundVariant != null) {
            switch (config.backgroundVariant) {
                case COLOR -> {
                    if (config.backgroundColor != null && config.backgroundColor.getAlpha() > 0) {
                        graphics.fill(x, y, x + currentWidth, y + currentHeight, config.backgroundColor.getRGB());
                    }
                    if (config.enableBorder && config.borderColor != null && config.borderColor.getAlpha() > 0) {
                        int borderColorRgb = config.borderColor.getRGB();
                        graphics.fill(x, y, x + currentWidth, y + 1, borderColorRgb);
                        graphics.fill(x, y + currentHeight - 1, x + currentWidth, y + currentHeight, borderColorRgb);
                        graphics.fill(x, y, x + 1, y + currentHeight, borderColorRgb);
                        graphics.fill(x + currentWidth - 1, y, x + currentWidth, y + currentHeight, borderColorRgb);
                    }
                }
                case BUTTON -> renderSprite(graphics, Identifier.withDefaultNamespace("widget/button"), x, y, currentWidth, currentHeight);
                case TOOLTIP -> {
                    int offset = 8;
                    renderSprite(graphics, Identifier.withDefaultNamespace("tooltip/background"), x - offset, y - offset, currentWidth + (offset * 2), currentHeight + (offset * 2));
                    renderSprite(graphics, Identifier.withDefaultNamespace("tooltip/frame"), x - offset, y - offset, currentWidth + (offset * 2), currentHeight + (offset * 2));
                }
                case NONE -> { /* Do nothing */ }
            }
        }

        if (showArt) {
            Identifier artTexture = MediaManager.currentCoverArt;
            if (artTexture != null) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        artTexture,
                        artX, artY,
                        0.0f, 0.0f,
                        actualArtSize, actualArtSize,
                        actualArtSize, actualArtSize
                );
            } else {
                graphics.fill(artX, artY, artX + actualArtSize, artY + actualArtSize, 0xFF333333);
            }
        }

        if (contentWidth > 0) {
            Component titleComp = Component.literal(MediaManager.currentTitle);
            Component artistComp = Component.literal(MediaManager.currentArtist);

            int titleRgb = config != null && config.titleColor != null ? config.titleColor.getRGB() : 0xFFFFFFFF;
            int artistRgb = config != null && config.artistColor != null ? config.artistColor.getRGB() : 0xFFAAAAAA;
            int timeIconRgb = config != null && config.timeAndIconColor != null ? config.timeAndIconColor.getRGB() : 0xFFAAAAAA;

            renderMarqueeText(graphics, font, titleComp, contentX, titleY, contentWidth, titleRgb);
            renderMarqueeText(graphics, font, artistComp, contentX, artistY, contentWidth, artistRgb);

            String statusIcon = MediaManager.isPlaying ? "▶" : "⏸";
            graphics.text(font, Component.literal(statusIcon), contentX, timeY, timeIconRgb, false);

            String timeStr = formatTime(MediaManager.currentPosition) + " / " + formatTime(MediaManager.currentDuration);
            int timeWidth = font.width(timeStr);
            graphics.text(font, Component.literal(timeStr), (contentX + contentWidth) - timeWidth, timeY, timeIconRgb, false);

            if (showProgressBar) {
                int inactiveBarRgb = config != null && config.progressBarInactiveColor != null ? config.progressBarInactiveColor.getRGB() : 0xFF444444;
                graphics.fill(contentX, barY, contentX + contentWidth, barY + PROGRESS_BAR_HEIGHT, inactiveBarRgb);

                int currentProgressWidth = (int) (contentWidth * MediaManager.currentProgress);
                int activeBarRgb = config != null && config.progressBarActiveColor != null ? config.progressBarActiveColor.getRGB() : 0xFF1DB954;
                graphics.fill(contentX, barY, contentX + currentProgressWidth, barY + PROGRESS_BAR_HEIGHT, activeBarRgb);
            }
        }

        matrices.popMatrix();
    }

    private void renderMarqueeText(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int maxWidth, int color) {
        int textWidth = font.width(text);

        if (textWidth <= maxWidth) {
            graphics.text(font, text, x, y, color, false);
        } else {
            int overflow = textWidth - maxWidth;
            long time = Util.getMillis();

            double pixelsPerSecond = 25.0;
            double pauseTimeMs = 1200.0;

            double oneWayTimeMs = (overflow / pixelsPerSecond) * 1000.0;
            double totalCycleTime = (oneWayTimeMs * 2.0) + (pauseTimeMs * 2.0);

            double cycleTime = time % totalCycleTime;
            double progress;

            if (cycleTime < pauseTimeMs) {
                progress = 0.0;
            } else if (cycleTime < pauseTimeMs + oneWayTimeMs) {
                double linear = (cycleTime - pauseTimeMs) / oneWayTimeMs;
                progress = smoothStep(linear);
            } else if (cycleTime < (pauseTimeMs * 2.0) + oneWayTimeMs) {
                progress = 1.0;
            } else {
                double linear = (cycleTime - ((pauseTimeMs * 2.0) + oneWayTimeMs)) / oneWayTimeMs;
                progress = 1.0 - smoothStep(linear);
            }

            int offset = (int) (overflow * progress);

            graphics.enableScissor(x, y, x + maxWidth, y + font.lineHeight);
            graphics.text(font, text, x - offset, y, color, false);
            graphics.disableScissor();
        }
    }

    private double smoothStep(double x) {
        x = Math.clamp(x, 0.0, 1.0);
        return x * x * (3.0 - 2.0 * x);
    }

    private void renderSprite(GuiGraphicsExtractor graphics, Identifier spriteId, int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteId, x, y, width, height);
    }

    @Override
    public void setFocused(boolean focused) {}

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public boolean isFocused() {
        return false;
    }
}