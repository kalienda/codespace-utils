package me.vladosik.csu.utils;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import me.vladosik.csu.CodespaceUtils;
import me.vladosik.csu.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;

public final class Utils {
    public static final int COLOR_WHITE = 0xffffff;
    public static final int COLOR_RED = 0xff5555;
    public static final int COLOR_BLUE = 0x447fff;
    public static final int COLOR_GRAY = 0xaaaaaa;
    public static final int COLOR_DARK_GRAY = 0x555555;

    /**
     * Returns Minecraft item with given ID
     * @param id the ID of the item, e.g. {@code air}
     * @return Instance of Item
     * @throws IllegalArgumentException if there is no item with given ID in built-in registry
     */
    @NullMarked
    public static Item parseItemOrThrow(String id) throws IllegalArgumentException {
        if (id.isBlank()) return Items.AIR;
        var optional = BuiltInRegistries.ITEM.get(Identifier.withDefaultNamespace(id));
        if (optional.isEmpty()) throw new IllegalArgumentException("There is no item with ID "+id+" in built-in registry");
        return optional.get().value();
    }

    @NullMarked
    public static String catchException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return e.getClass().getName()+": "+e.getMessage()+'\n'+sw.toString().replace("\t", "  ");
    }

    public static @Nullable InputStream getAsset(String path) {
        return Utils.class.getClassLoader().getResourceAsStream("assets/codespace-utils/"+path);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static <T> T xOrThrow(@Nullable T value, @NonNull T required) throws IllegalStateException {
        if (value == null || !value.getClass().equals(required.getClass()) || value != required) throw new IllegalStateException(
            "Passed value ("+value+") does not meet required ("+required+')'
        );
        return value;
    }

    public static void onClientThread(@NonNull Consumer<Minecraft> code) {
        Minecraft.getInstance().execute(() -> code.accept(Minecraft.getInstance()));
    }

    public static void onClientThread(@NonNull Runnable code) {
        Minecraft.getInstance().execute(code);
    }

    private static void sendMessageActions(boolean bl, Object[] messages) {
        LocalPlayer lp = Minecraft.getInstance().player;
        if (!CodespaceUtils.config.debugMessagesEnabled || lp == null) return;
        StringJoiner joiner = new StringJoiner("");
        for (Object message : messages) {
            joiner.add(message != null ? message.toString() : "null");
        }
        lp.displayClientMessage(Component.literal("[csu/d] "+joiner).setStyle(Style.EMPTY.withItalic(false)), bl);
    }

    public static void sendDebugActionbar(@Nullable Object @NonNull ...messages) { sendMessageActions(true, messages); }
    public static void sendDebugMessage(@Nullable Object @NonNull ...messages) { sendMessageActions(false, messages); }
    public static void ifDebug(@NonNull Runnable actions) { if (CodespaceUtils.config.debugMessagesEnabled) actions.run(); }

    public static @Nullable JsonElement extractCustomData(@NonNull ItemStack is) {
        CustomData data = is.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        return Objects.requireNonNull(DataComponents.CUSTOM_DATA.codec()).encodeStart(JsonOps.INSTANCE, data)
                .resultOrPartial().orElse(null);
    }

    public static boolean worldIsCodespace(Level level) {
        return level.dimension().identifier().getPath().endsWith("_creativeplus_editor");
    }
}
