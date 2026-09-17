package me.vladosik.csu.config;

import me.vladosik.csu.CodespaceUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/// Срисовал с {@link net.minecraft.client.gui.screens.DisconnectedScreen}<br>
/// Не могу не сказать насколько тяжело было вслепую эту хуйню писать не зная как лэйауты работают
public class FallbackConfigScreen extends Screen {
    private final Screen parent;
    private final LinearLayout layout = LinearLayout.vertical();

    public FallbackConfigScreen() {
        this(null);
    }

    public FallbackConfigScreen(Screen parent) {
        super(Component.translatable("csu.fallback-config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        layout.defaultCellSetting().alignHorizontallyCenter().padding(10);

        layout.addChild(string(Component.translatable("csu.fallback-config.title")));
        layout.defaultCellSetting().padding(5);
        layout.addChild(string(Component.translatable("csu.fallback-config.subtitle")));

        layout.defaultCellSetting().padding(3);
        layout.addChild(button(Component.literal("Modrinth"), button -> Util.getPlatform().openUri("https://modrinth.com/mod/cloth-config")));
        layout.addChild(button(Component.literal("Curseforge"), button -> Util.getPlatform().openUri("https://www.curseforge.com/minecraft/mc-mods/cloth-config")));

        layout.defaultCellSetting().padding(0);
        layout.addChild(string(Component.translatable("csu.fallback-config.manual-edit.line1")));
        layout.addChild(string(Component.translatable("csu.fallback-config.manual-edit.line2")));
        layout.addChild(string(Component.translatable("csu.fallback-config.manual-edit.line3")));

        layout.defaultCellSetting().padding(10);
        layout.addChild(button(Component.translatable("csu.fallback-config.open-file-button"), button -> Util.getPlatform().openFile(CodespaceUtils.getConfigFile())));
        layout.defaultCellSetting().padding(1);
        layout.addChild(button(CommonComponents.GUI_BACK, button -> onClose()));

        layout.arrangeElements();
        layout.visitWidgets(super::addRenderableWidget);
        repositionElements();
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    protected void repositionElements() {
        FrameLayout.centerInRectangle(layout, getRectangle());
    }

    private StringWidget string(Component component) {
        return new StringWidget(component, font);
    }

    private Button button(Component text, Button.OnPress does) {
        return Button.builder(text, does).size(220, 20).build();
    }
}
