package dev.velolib.playfront.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.velolib.playfront.PlayfrontClient;
import dev.velolib.playfront.ui.screen.EditScreen;
import dev.velolib.playfront.ui.widget.MediaWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PlayfrontConfigScreen {

    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("screen.playfront.config.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("screen.playfront.config.category.settings"))
                        .tooltip(Component.translatable("screen.playfront.config.category.settings.tooltip"))

                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("screen.playfront.config.group.general"))

                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.polling_rate"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.polling_rate.tooltip")))
                                        .binding(
                                                1000,
                                                () -> PlayfrontConfig.INSTANCE.pollingRateMs,
                                                newVal -> PlayfrontConfig.INSTANCE.pollingRateMs = newVal
                                        )
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(1000, 5000)
                                                .step(250))
                                        .build())

                                .option(ButtonOption.createBuilder()
                                        .name(Component.translatable("screen.playfront.config.widget_position"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.widget_position.tooltip")))
                                        .action((screen, _) -> Minecraft.getInstance().gui.setScreen(new EditScreen(screen)))
                                        .text(Component.literal("CLICK ME!"))
                                        .build())

                                .option(Option.<Float>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.widget_scale"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.widget_scale.tooltip")))
                                        .binding(
                                                0.0f,
                                                () -> PlayfrontConfig.INSTANCE.widgetScale,
                                                newVal -> PlayfrontConfig.INSTANCE.widgetScale = newVal
                                        )
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                                .range(0.0f, 4.0f)
                                                .step(0.1f)
                                                .formatValue(val -> val == 0.0f
                                                        ? Component.literal("Follow GUI")
                                                        : Component.literal(String.format("%.1fx", val))))
                                        .build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.widget_width"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.widget_width.tooltip")))
                                        .binding(
                                                MediaWidget.DEFAULT_WIDTH,
                                                () -> PlayfrontConfig.INSTANCE.widgetWidth,
                                                newVal -> {
                                                    PlayfrontConfig.INSTANCE.widgetWidth = newVal;
                                                    if (PlayfrontClient.hudWidget != null) {
                                                        PlayfrontClient.hudWidget.setWidth(newVal);
                                                    }
                                                }
                                        )
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(120, 800)
                                                .step(5))
                                        .build())

                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.show_cover_art"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.show_cover_art.tooltip")))
                                        .binding(
                                                true,
                                                () -> PlayfrontConfig.INSTANCE.showCoverArt,
                                                newVal -> PlayfrontConfig.INSTANCE.showCoverArt = newVal
                                        )
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())

                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("screen.playfront.config.group.styling"))
                                .option(Option.<PlayfrontConfig.BackgroundVariant>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.background_variant"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.background_variant.tooltip")))
                                        .binding(
                                                PlayfrontConfig.BackgroundVariant.COLOR,
                                                () -> PlayfrontConfig.INSTANCE.backgroundVariant,
                                                newVal -> PlayfrontConfig.INSTANCE.backgroundVariant = newVal
                                        )
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                                .enumClass(PlayfrontConfig.BackgroundVariant.class))
                                        .build())

                                .option(Option.<java.awt.Color>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.background_color"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.background_color.tooltip")))
                                        .binding(
                                                new java.awt.Color(0x80000000, true),
                                                () -> PlayfrontConfig.INSTANCE.backgroundColor,
                                                newVal -> PlayfrontConfig.INSTANCE.backgroundColor = newVal
                                        )
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                        .build())

                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.enable_border"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.enable_border.tooltip")))
                                        .binding(
                                                true,
                                                () -> PlayfrontConfig.INSTANCE.enableBorder,
                                                newVal -> PlayfrontConfig.INSTANCE.enableBorder = newVal
                                        )
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())

                                .option(Option.<java.awt.Color>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.border_color"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.border_color.tooltip")))
                                        .binding(
                                                new java.awt.Color(0xFF444444, true),
                                                () -> PlayfrontConfig.INSTANCE.borderColor,
                                                newVal -> PlayfrontConfig.INSTANCE.borderColor = newVal
                                        )
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                        .build())

                                .option(Option.<java.awt.Color>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.title_color"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.title_color.tooltip")))
                                        .binding(
                                                new java.awt.Color(0xFFFFFFFF, true),
                                                () -> PlayfrontConfig.INSTANCE.titleColor,
                                                newVal -> PlayfrontConfig.INSTANCE.titleColor = newVal
                                        )
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                        .build())

                                .option(Option.<java.awt.Color>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.artist_color"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.artist_color.tooltip")))
                                        .binding(
                                                new java.awt.Color(0xFFAAAAAA, true),
                                                () -> PlayfrontConfig.INSTANCE.artistColor,
                                                newVal -> PlayfrontConfig.INSTANCE.artistColor = newVal
                                        )
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                        .build())

                                .option(Option.<java.awt.Color>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.time_and_icon_color"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.time_and_icon_color.tooltip")))
                                        .binding(
                                                new java.awt.Color(0xFFAAAAAA, true),
                                                () -> PlayfrontConfig.INSTANCE.timeAndIconColor,
                                                newVal -> PlayfrontConfig.INSTANCE.timeAndIconColor = newVal
                                        )
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                        .build())

                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.show_progress_bar"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.show_progress_bar.tooltip")))
                                        .binding(
                                                true,
                                                () -> PlayfrontConfig.INSTANCE.showProgressBar,
                                                newVal -> PlayfrontConfig.INSTANCE.showProgressBar = newVal
                                        )
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())

                                .option(Option.<java.awt.Color>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.progress_bar_active_color"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.progress_bar_active_color.tooltip")))
                                        .binding(
                                                new java.awt.Color(0xFF1DB954, true),
                                                () -> PlayfrontConfig.INSTANCE.progressBarActiveColor,
                                                newVal -> PlayfrontConfig.INSTANCE.progressBarActiveColor = newVal
                                        )
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                        .build())

                                .option(Option.<java.awt.Color>createBuilder()
                                        .name(Component.translatable("screen.playfront.config.progress_bar_inactive_color"))
                                        .description(OptionDescription.of(Component.translatable("screen.playfront.config.progress_bar_inactive_color.tooltip")))
                                        .binding(
                                                new java.awt.Color(0xFF444444, true),
                                                () -> PlayfrontConfig.INSTANCE.progressBarInactiveColor,
                                                newVal -> PlayfrontConfig.INSTANCE.progressBarInactiveColor = newVal
                                        )
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                        .build())
                                .build())

                        .build())
                .save(PlayfrontConfig::save)
                .build()
                .generateScreen(parent);
    }
}