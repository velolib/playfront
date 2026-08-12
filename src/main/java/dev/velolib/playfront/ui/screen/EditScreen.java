package dev.velolib.playfront.ui.screen;

import dev.velolib.playfront.PlayfrontClient;
import dev.velolib.playfront.config.PlayfrontConfig;
import dev.velolib.playfront.ui.widget.MediaWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class EditScreen extends Screen {
    private static final int INITIAL_DELAY_TICKS = 5; // 250ms at 20 TPS
    private final Screen parent;
    private final Set<Integer> pressedKeys = new HashSet<>();
    private MediaWidget mediaWidget;
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int keyRepeatDelay = 0;

    public EditScreen(Screen parent) {
        super(Component.translatable("screen.playfront.widget_positioning"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int width = PlayfrontConfig.INSTANCE.widgetWidth;
        int height = MediaWidget.DEFAULT_HEIGHT;

        int startX = PlayfrontConfig.INSTANCE.widgetX;
        int startY = PlayfrontConfig.INSTANCE.widgetY;

        this.mediaWidget = new MediaWidget(startX, startY, width, height, Component.empty());

        // Use our new helper method to apply bounds instantly on load
        this.setWidgetPositionBounded(startX, startY);

        // Save bounds correction instantly if it was off-screen
        PlayfrontConfig.INSTANCE.widgetX = this.mediaWidget.getX();
        PlayfrontConfig.INSTANCE.widgetY = this.mediaWidget.getY();

        this.addRenderableWidget(this.mediaWidget);

        this.addRenderableWidget(Button.builder(Component.translatable("playfront.ui.confirm"), _ -> {
            PlayfrontConfig.INSTANCE.widgetX = this.mediaWidget.getX();
            PlayfrontConfig.INSTANCE.widgetY = this.mediaWidget.getY();
            PlayfrontConfig.save();

            if (PlayfrontClient.hudWidget != null) {
                PlayfrontClient.hudWidget.setPosition(PlayfrontConfig.INSTANCE.widgetX, PlayfrontConfig.INSTANCE.widgetY);
            }

            Minecraft.getInstance().gui.setScreen(this.parent);
        }).bounds(this.width / 2 - 105, this.height - 30, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("playfront.ui.cancel"), _ -> Minecraft.getInstance().gui.setScreen(this.parent)).bounds(this.width / 2 + 5, this.height - 30, 100, 20).build());
    }

    private void setWidgetPositionBounded(int targetX, int targetY) {
        if (this.mediaWidget != null) {
            // Get the true visual scale bounds
            float scale = this.mediaWidget.getVisualScale();
            int scaledWidth = (int) (this.mediaWidget.getWidth() * scale);
            int scaledHeight = (int) (this.mediaWidget.getHeight() * scale);

            // Restrict X and Y to the screen edges
            int newX = Math.clamp(targetX, 0, this.width - scaledWidth);
            int newY = Math.clamp(targetY, 0, this.height - scaledHeight);

            this.mediaWidget.setPosition(newX, newY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (event.button() == 0 && this.mediaWidget != null) {
            if (this.mediaWidget.isMouseOver(mouseX, mouseY)) {
                this.isDragging = true;
                this.dragOffsetX = (int) (mouseX - this.mediaWidget.getX());
                this.dragOffsetY = (int) (mouseY - this.mediaWidget.getY());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (this.isDragging && this.mediaWidget != null) {
            this.setWidgetPositionBounded(
                    (int) (event.x() - this.dragOffsetX),
                    (int) (event.y() - this.dragOffsetY)
            );
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (event.button() == 0 && this.isDragging) {
            this.isDragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();

        boolean arrowKey =
                keyCode == GLFW.GLFW_KEY_UP ||
                        keyCode == GLFW.GLFW_KEY_DOWN ||
                        keyCode == GLFW.GLFW_KEY_LEFT ||
                        keyCode == GLFW.GLFW_KEY_RIGHT;

        if (!arrowKey) {
            return super.keyPressed(event);
        }

        // Ignore keyboard repeat events.
        // The first physical press is handled here.
        if (this.pressedKeys.add(keyCode)) {
            int dx = 0;
            int dy = 0;

            if (keyCode == GLFW.GLFW_KEY_UP) dy = -1;
            else if (keyCode == GLFW.GLFW_KEY_DOWN) dy = 1;
            else if (keyCode == GLFW.GLFW_KEY_LEFT) dx = -1;
            else if (keyCode == GLFW.GLFW_KEY_RIGHT) dx = 1;

            // Immediate 1-pixel movement
            this.setWidgetPositionBounded(
                    this.mediaWidget.getX() + dx,
                    this.mediaWidget.getY() + dy
            );

            // Start the initial repeat delay
            this.keyRepeatDelay = INITIAL_DELAY_TICKS;

            return true;
        }

        return true;
    }

    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();

        this.pressedKeys.remove(keyCode);

        // Reset the delay when the key is released
        if (this.pressedKeys.isEmpty()) {
            this.keyRepeatDelay = 0;
        }

        return super.keyReleased(event);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.pressedKeys.isEmpty()) {
            return;
        }

        // Wait before starting continuous movement
        if (this.keyRepeatDelay > 0) {
            this.keyRepeatDelay--;
            return;
        }

        int dx = 0;
        int dy = 0;

        if (this.pressedKeys.contains(GLFW.GLFW_KEY_UP)) {
            dy -= 1;
        }

        if (this.pressedKeys.contains(GLFW.GLFW_KEY_DOWN)) {
            dy += 1;
        }

        if (this.pressedKeys.contains(GLFW.GLFW_KEY_LEFT)) {
            dx -= 1;
        }

        if (this.pressedKeys.contains(GLFW.GLFW_KEY_RIGHT)) {
            dx += 1;
        }

        if (dx != 0 || dy != 0) {
            this.setWidgetPositionBounded(
                    this.mediaWidget.getX() + dx,
                    this.mediaWidget.getY() + dy
            );
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Draw perfectly centered text exactly 9 pixels above the Confirm/Cancel buttons
        int textY = this.height - 48;
        graphics.centeredText(Minecraft.getInstance().font, Component.translatable("screen.playfront.widget_positioning.info"), this.width / 2, textY, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }
}