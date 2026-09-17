package me.vladosik.csu.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import me.vladosik.csu.CodespaceUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ConfigMenu {
    private final ConfigEntryBuilder entryBuilder = ConfigEntryBuilder.create();
    private static ConfigMenu instance;

    private JsonObject changes;
    private ConfigBuilder configBuilder;
    private Config config;

    public static Screen show(@NonNull Screen parent) {
        if (instance == null) instance = new ConfigMenu();
        instance.assembleMenu();
        instance.configBuilder.setParentScreen(parent);
        return instance.configBuilder.build();
    }

    private void assembleMenu() {
        configBuilder = ConfigBuilder.create();
        config = CodespaceUtils.config.copy();
        changes = config.json.deepCopy();
        configBuilder.setTitle(Component.translatable("modmenu.nameTranslation.codespace-utils"));
        configBuilder.setAlwaysShowTabs(true);

        createCommonCategory();
        createInventoryCategory();
        createNavigationCategory();
        createDebugCategory();

        configBuilder.setSavingRunnable(() -> {
            var newCfg = new Config(changes);
            CodespaceUtils.config = newCfg;
            newCfg.write();
            CodespaceUtils.getPacketManager().setPacketsRate(CodespaceUtils.config.maxPacketsPerSecond);
            configBuilder = null;
            config = null;
            changes = null;
            System.gc();
        });
    }

    private void createCommonCategory() {
        ConfigCategory commonCategory = configBuilder.getOrCreateCategory(Component.translatable("csu.config.common.label"));

        commonCategory.addEntry(
            entryBuilder.startIntField(Component.translatable("csu.config.common.max-packets.label"), config.maxPacketsPerSecond)
                .setTooltip(Component.translatable("csu.config.common.max-packets.tooltip"))
                .setSaveConsumer(i -> changes.getAsJsonObject("common").add("max-packets-per-second", new JsonPrimitive(i)))
                .setMin(1)
                .build()
        );
    }

    private void createInventoryCategory() {
        ConfigCategory inventoryCategory = configBuilder.getOrCreateCategory(Component.translatable("csu.config.inventory.label"));
        inventoryCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("csu.config.inventory.apply-on-join.label"), config.applyArrangementOnJoin)
                        .setTooltip(Component.translatable("csu.config.inventory.apply-on-join.tooltip"))
                        .setSaveConsumer(b -> changes.getAsJsonObject("inventory").add("apply-on-join", new JsonPrimitive(b)))
                        .build()
        );
        inventoryCategory.addEntry(
                entryBuilder.startStringDropdownMenu(Component.translatable("csu.config.inventory.primary.label"), config.primaryArrangement.getName())
                        .setTooltip(Component.translatable("csu.config.inventory.primary.tooltip"))
                        .setSaveConsumer(s -> changes.getAsJsonObject("inventory").add("primary", new JsonPrimitive(s)))
                        .setSuggestionMode(true)
                        .setSelections(listArrangements())
                        .build()
        );
        inventoryCategory.addEntry(
                entryBuilder.startIntField(Component.translatable("csu.config.inventory.delay.label"), config.delayBeforeAutoApply)
                        .setTooltip(Component.translatable("csu.config.inventory.delay.tooltip"))
                        .setSaveConsumer(i -> changes.getAsJsonObject("inventory").add("delay-on-auto-apply", new JsonPrimitive(i)))
                        .setMin(0)
                        .build()
        );
    }

    private void createNavigationCategory() {
        ConfigCategory navigationCategory = configBuilder.getOrCreateCategory(Component.translatable("csu.config.navigation.label"));
        navigationCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("csu.config.navigation.glass-break.label"), config.teleportOnGlassBreak)
            .setTooltip(Component.translatable("csu.config.navigation.glass-break.tooltip"))
            .setSaveConsumer(b -> changes.getAsJsonObject("navigation").add("teleport-on-glass-break", new JsonPrimitive(b)))
            .build()
        );
        navigationCategory.addEntry(entryBuilder.startStringDropdownMenu(Component.translatable("csu.config.navigation.glasstp-method.label"), config.teleportationType.toString())
            .setTooltip(
                Component.translatable("csu.config.navigation.glasstp-method.tooltip"),
                Component.translatable("csu.config.navigation.glasstp-method.tooltip2"),
                Component.translatable("csu.config.navigation.glasstp-method.tooltip3")
            )
            .setSaveConsumer(s -> {
                if (!(s.equals("PACKET") || s.equals("COMMAND"))) s = "PACKET";
                changes.getAsJsonObject("navigation").add("teleporting-method", new JsonPrimitive(s));
            })
            .setSelections(List.of("PACKET", "COMMAND"))
            .build()
        );

        SubCategoryBuilder subCategory = entryBuilder.startSubCategory(Component.translatable("csu.config.navigation.movmultip"));
        subCategory.setExpanded(true);
        subCategory.add(entryBuilder.startBooleanToggle(Component.translatable("csu.config.navigation.movmultip.enabled.label"), config.movementMultiplier.enable())
            .setSaveConsumer(b -> changes.getAsJsonObject("navigation").getAsJsonObject("movement-multiplier").add("enable", new JsonPrimitive(b)))
            .setTooltip(Component.translatable("csu.config.navigation.movmultip.enabled.label"))
            .build()
        );
        subCategory.add(entryBuilder.startBooleanToggle(Component.translatable("csu.config.navigation.movmultip.hidden.label"), config.movementMultiplier.hidden())
            .setSaveConsumer(b -> changes.getAsJsonObject("navigation").getAsJsonObject("movement-multiplier").addProperty("hidden", b))
            .setTooltip(Component.translatable("csu.config.navigation.movmultip.hidden.tooltip"))
            .build()
        );
        subCategory.add(entryBuilder.startIntSlider(
                Component.translatable("csu.config.navigation.movmultip.movement.label"),
                config.movementMultiplier.movement(), 0, 1000
            )
            .setSaveConsumer(i ->
                changes.getAsJsonObject("navigation").getAsJsonObject("movement-multiplier").add("movement", new JsonPrimitive(i)))
            .setTooltip(Component.translatable("csu.config.navigation.movmultip.movement.tooltip"))
            .build()
        );
        subCategory.add(entryBuilder.startIntSlider(
                Component.translatable("csu.config.navigation.movmultip.sneaking.label"),
                config.movementMultiplier.sneaking(), 0, 1000
            )
            .setSaveConsumer(i -> changes.getAsJsonObject("navigation").getAsJsonObject("movement-multiplier").add("sneaking", new JsonPrimitive(i)))
            .setTooltip(Component.translatable("csu.config.navigation.movmultip.sneaking.tooltip"))
            .build()
        );
        navigationCategory.addEntry(subCategory.build());
    }

    private void createDebugCategory() {
        ConfigCategory category = configBuilder.getOrCreateCategory(Component.translatable("csu.config.debug.label"));

        category.addEntry(entryBuilder.startBooleanToggle(Component.translatable("csu.config.debug.messages.label"), config.debugMessagesEnabled)
            .setTooltip(Component.translatable("csu.config.debug.messages.tooltip"))
            .setSaveConsumer(b -> changes.getAsJsonObject("debug").addProperty("messages", b))
            .build()
        );
        category.addEntry(entryBuilder.startBooleanToggle(Component.translatable("csu.config.debug.no-queue.label"), config.debugDisablePacketQueue)
            .setTooltip(Component.translatable("csu.config.debug.no-queue.tooltip"))
            .setSaveConsumer(b -> changes.getAsJsonObject("debug").addProperty("disable-packet-queue", b))
            .build()
        );
    }

    private List<String> listArrangements() {
        File[] array = CodespaceUtils.getArrangementsDir().listFiles();
        if (array == null) throw new IllegalStateException("Arrangement directory, well, SOMETHING that is meant to be a directory, looks to be not!");
        return Arrays.stream(array)
                .map(File::getName)
                .filter(s -> s.endsWith(".json"))
                .toList();
    }

}
