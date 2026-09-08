package com.afwid.client.config;

import com.afwid.AfwDebugChatMode;
import com.afwid.api.AfwDamageBehavior;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

public final class AfwConfigScreen extends Screen {
    private final Screen parent;
    private SettingsList settingsList;

    public AfwConfigScreen(Screen parent) {
        super(Text.translatable("config.animationframework.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (this.client == null) {
            return;
        }

        int listTop = 32;
        int listBottomPadding = 36;
        this.settingsList = new SettingsList(this.client, this.width, this.height - listTop - listBottomPadding, listTop);
        this.addDrawableChild(this.settingsList);

        AfwClientConfig config = AfwClientConfig.get();
        this.settingsList.addEntryRow(new SettingsList.RowEntry(
                Text.translatable("config.animationframework.debug_chat"),
                () -> Text.translatable("config.animationframework.debug_chat.value." + config.debugChatMode().id()),
                button -> {
                    config.setDebugChatMode(nextDebugChatMode(config.debugChatMode()));
                    button.setMessage(Text.translatable("config.animationframework.debug_chat.value." + config.debugChatMode().id()));
                }));
        this.settingsList.addEntryRow(new SettingsList.RowEntry(
                Text.translatable("config.animationframework.damage_behavior"),
                () -> Text.translatable("config.animationframework.damage_behavior.value." + config.defaultDamageBehavior().id()),
                button -> {
                    config.setDefaultDamageBehavior(nextDamageBehavior(config.defaultDamageBehavior()));
                    button.setMessage(Text.translatable("config.animationframework.damage_behavior.value." + config.defaultDamageBehavior().id()));
                }));
        this.settingsList.addEntryRow(new SettingsList.RowEntry(
                Text.translatable("config.animationframework.ignore_attackers"),
                () -> Text.translatable(config.ignoreAttackers() ? "options.on" : "options.off"),
                button -> {
                    config.setIgnoreAttackers(!config.ignoreAttackers());
                    button.setMessage(Text.translatable(config.ignoreAttackers() ? "options.on" : "options.off"));
                }));
        this.settingsList.addEntryRow(new SettingsList.RowEntry(
                Text.translatable("config.animationframework.lock_perspective"),
                () -> Text.translatable(config.lockPerspective() ? "options.on" : "options.off"),
                button -> {
                    config.setLockPerspective(!config.lockPerspective());
                    button.setMessage(Text.translatable(config.lockPerspective() ? "options.on" : "options.off"));
                }));

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                .dimensions(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void close() {
        AfwClientConfig.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
    }

    private static AfwDebugChatMode nextDebugChatMode(AfwDebugChatMode current) {
        if (current == null) {
            return AfwDebugChatMode.SETUP_ERRORS;
        }
        return switch (current) {
            case ALL -> AfwDebugChatMode.SETUP_WARNINGS_ERRORS;
            case SETUP_WARNINGS_ERRORS -> AfwDebugChatMode.SETUP_ERRORS;
            case SETUP_ERRORS -> AfwDebugChatMode.ERRORS_ONLY;
            case ERRORS_ONLY -> AfwDebugChatMode.ALL;
        };
    }

    private static AfwDamageBehavior nextDamageBehavior(AfwDamageBehavior current) {
        if (current == null) {
            return AfwDamageBehavior.STOP_ON_DAMAGE;
        }
        return switch (current) {
            case STOP_ON_DAMAGE -> AfwDamageBehavior.IGNORE_DAMAGE;
            case IGNORE_DAMAGE -> AfwDamageBehavior.BLOCK_DAMAGE;
            case BLOCK_DAMAGE -> AfwDamageBehavior.STOP_ON_DAMAGE;
        };
    }

    private static final class SettingsList extends ElementListWidget<SettingsList.RowEntry> {
        private SettingsList(MinecraftClient client, int width, int height, int top) {
            super(client, width, height, top, 24);
            this.centerListVertically = false;
        }

        private void addEntryRow(RowEntry entry) {
            super.addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return 360;
        }

        @Override
        public int getRowLeft() {
            return (this.width - this.getRowWidth()) / 2;
        }

        private static final class RowEntry extends ElementListWidget.Entry<RowEntry> {
            private final TextWidget labelWidget;
            private final ButtonWidget valueButton;

            private RowEntry(Text label, java.util.function.Supplier<Text> valueSupplier,
                             java.util.function.Consumer<ButtonWidget> action) {
                this.labelWidget = new TextWidget(0, 0, 220, 20, label, MinecraftClient.getInstance().textRenderer);
                this.valueButton = ButtonWidget.builder(valueSupplier.get(), action::accept)
                        .dimensions(0, 0, 130, 20)
                        .build();
            }

            @Override
            public List<? extends Element> children() {
                return List.of(this.labelWidget, this.valueButton);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of(this.labelWidget, this.valueButton);
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                this.labelWidget.setPosition(x, y + 2);
                this.valueButton.setPosition(x + entryWidth - 130, y + 2);
                this.labelWidget.render(context, mouseX, mouseY, tickDelta);
                this.valueButton.render(context, mouseX, mouseY, tickDelta);
            }
        }
    }
}
