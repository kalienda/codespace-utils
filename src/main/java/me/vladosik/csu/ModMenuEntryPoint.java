package me.vladosik.csu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.vladosik.csu.config.ConfigMenu;
import me.vladosik.csu.config.FallbackConfigScreen;

public class ModMenuEntryPoint implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return clothConfigAvailable() ? ConfigMenu::show : FallbackConfigScreen::new;
    }

    private boolean clothConfigAvailable() {
        try {
            var ignored = ConfigEntryBuilder.class;
            System.gc();
        } catch (NoClassDefFoundError e) {
            return false;
        }
        return true;
    }
}
