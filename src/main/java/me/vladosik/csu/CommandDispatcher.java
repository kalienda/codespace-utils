package me.vladosik.csu;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.vladosik.csu.config.ConfigMenu;
import me.vladosik.csu.utils.Sounds;
import me.vladosik.csu.utils.Utils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.*;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class CommandDispatcher {
    private static final String[] CSU_TOP_LEVEL_ARGS = {"config", "directory"};
    private static final String[] REARRANGE_TOP_LEVEL_ARGS = {"create", "load", "list", "remove"};

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("arrangement")
                .executes(CommandDispatcher::showRearrangeHelp)
                .then(ClientCommandManager.argument("arg1", StringArgumentType.word())
                    .suggests((ctx, builder) ->
                        SharedSuggestionProvider.suggest(REARRANGE_TOP_LEVEL_ARGS, builder)
                    )
                    .executes(CommandDispatcher::executeRearrange1)
                        .then(ClientCommandManager.argument("arg2", StringArgumentType.greedyString())
                            .suggests(CommandDispatcher::suggestRearrange2)
                            .executes(CommandDispatcher::executeRearrange2)
                        )
                )
            );
            dispatcher.register(ClientCommandManager.literal("csu")
                .executes(CommandDispatcher::showCSUHelp)
                .then(ClientCommandManager.argument("arg1", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(CSU_TOP_LEVEL_ARGS, builder))
                    .executes(CommandDispatcher::executeCSU1)
                )
            );
        });
    }

    private static int executeCSU1(CommandContext<FabricClientCommandSource> ctx) {
        String arg1 = getArgument(ctx, "arg1");

        if (arg1 == null) {
            ctx.getSource().sendError(Component.translatable("csu.commands.messages.errors.missing-argument", 1, "/csu"));
            Sounds.FAIL.playOnPlayer();
            return 1;
        } else if (!(arg1.equalsIgnoreCase("config") || arg1.equalsIgnoreCase("directory"))) {
            ctx.getSource().sendError(Component.translatable("csu.commands.messages.errors.illegal-command-usage", "csu "+arg1));
            return showCSUHelp(ctx);
        } else if (arg1.equalsIgnoreCase("config")) {
            Minecraft.getInstance().pauseGame(true);
            try {
                var ignored = ConfigEntryBuilder.class;
                ConfigMenu.show(Objects.requireNonNull(Minecraft.getInstance().screen));
            } catch (NoClassDefFoundError ignored) {
                Util.getPlatform().openFile(CodespaceUtils.getConfigFile());
            }
        } else {
            Util.getPlatform().openFile(CodespaceUtils.getConfigDir());
        }
        return 0;
    }

    private static int showRearrangeHelp(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendError(Component.translatable("csu.commands.messages.errors.illegal-command-usage", "/arrangements"));
        MutableComponent base = Component.empty();
        for (int i = 0; i < REARRANGE_TOP_LEVEL_ARGS.length; ++i) {
            String argument = REARRANGE_TOP_LEVEL_ARGS[i];
            base.append(Component.literal("⇒ ").withColor(Utils.COLOR_DARK_GRAY).withStyle(Style.EMPTY.withItalic(false)));
            base.append(Component.literal("/arrangements "+argument+' ').withStyle(Style.EMPTY.withItalic(false)));
            base.append(Component.literal("[...]").withStyle(Style.EMPTY
                .withItalic(false).withColor(Utils.COLOR_GRAY)
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("csu.commands.help.arrangements."+argument)))
            ));
            if (i != REARRANGE_TOP_LEVEL_ARGS.length-1) base.append("\n");
        }
        ctx.getSource().sendFeedback(base);
        return 0;
    }

    private static int showCSUHelp(CommandContext<FabricClientCommandSource> ctx) {
        var container = FabricLoader.getInstance().getModContainer(CodespaceUtils.MOD_ID).orElseThrow();
        var version = container.getMetadata().getVersion().getFriendlyString();
        var authors = new ArrayList<>(container.getMetadata().getAuthors());
        authors.addAll(container.getMetadata().getContributors());
        var developers = authors.stream().map(Person::getName).toList();

        MutableComponent base = Component.translatable("modmenu.nameTranslation.codespace-utils").append(" v").append(Component.literal(version)).append("\n");
        base.append(Component.translatable("csu.commands.help.csu.word-authors")).append(concatenateAuthors(developers)).append("\n\n");
        var list = List.of(
            new ObjectObjectImmutablePair<>("/csu config", "csu.csu-config"),
            new ObjectObjectImmutablePair<>("/csu directory", "csu.csu-directory"),
            new ObjectObjectImmutablePair<>("/arrangements create [name]", "arrangements.create"),
            new ObjectObjectImmutablePair<>("/arrangements rearrange [file]", "arrangements.rearrange"),
            new ObjectObjectImmutablePair<>("/arrangements list", "arrangements.list"),
            new ObjectObjectImmutablePair<>("/arrangements remove <file>", "arrangements.remove")
        );
        for (int i = 0; i < list.size(); ++i) {
            var pair = list.get(i);
            base.append(Component.literal("⇒ ").withColor(Utils.COLOR_DARK_GRAY))
                .append(Component.literal(pair.left())).append(Component.literal(" [...] ").withStyle(Style.EMPTY
                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("csu.commands.help."+pair.right())))
                    .withColor(Utils.COLOR_GRAY)
                ));
            if (i != list.size()-1) base.append("\n");
        }
        ctx.getSource().sendFeedback(base);
        return 0;
    }

    private static int executeRearrange1(CommandContext<FabricClientCommandSource> ctx) {
        String arg1 = getArgument(ctx, "arg1");
        if (arg1 == null) {
            ctx.getSource().sendError(Component.translatable("csu.commands.messages.errors.missing-argument", 1, "/arrangement"));
            return showRearrangeHelp(ctx);
        }
        switch (arg1.toLowerCase()) {
            case "create" -> createArrangement(null, ctx.getSource().getClient());
            case "load" -> {
                Arrangement.primary().rearrange(false);
                Sounds.SHUFFLE.playOnPlayer();
            }
            case "list" -> listArrangements(ctx);
            case "remove" -> {
                ctx.getSource().sendError(Component.translatable("csu.commands.messages.errors.missing-argument", 2, "/rearrange remove"));
                Sounds.FAIL.playOnPlayer();
                return -1;
            }
        }
        return 0;
    }

    private static CompletableFuture<Suggestions> suggestRearrange2(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
        var arg1 = getArgument(ctx, "arg1");
        var arg2 = getArgument(ctx, "arg2");

        if (arg1 == null || !(arg1.equals("load") || arg1.equals("remove"))) return builder.buildFuture();
        Stream<String> stream = resolveArrangements();
        return SharedSuggestionProvider.suggest(arg2 == null ? stream : stream.filter(s -> s.startsWith(arg2)), builder);
    }

    private static int executeRearrange2(CommandContext<FabricClientCommandSource> ctx) {
        String arg1 = Objects.requireNonNull(getArgument(ctx, "arg1"));
        String arg2 = getArgument(ctx, "arg2");

        switch (arg1) {
            case "create" -> createArrangement(arg2, ctx.getSource().getClient());
            case "load" -> { return rearrangeExactly(ctx, arg2); }
            case "list" -> listArrangements(ctx);
            case "remove" -> removeRearrangement(ctx, arg2);
        }
        return 0;
    }

    private static int rearrangeExactly(CommandContext<FabricClientCommandSource> ctx, String filename) {
        File file = new File(CodespaceUtils.getArrangementsDir(), filename);
        if (!file.isFile()) {
            ctx.getSource().sendError(Component.translatable("csu.commands.messages.errors.file-not-found", filename));
            return -1;
        }
        JsonArray content;
        try {
            content = JsonParser.parseString(Files.readString(file.toPath())).getAsJsonArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new Arrangement(content).rearrange(false);
        Sounds.SHUFFLE.playOnPlayer();
        return 0;
    }

    private static @Nullable String getArgument(CommandContext<FabricClientCommandSource> ctx, String label) {
        try {
            return StringArgumentType.getString(ctx, label);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void createArrangement(@Nullable String filename, @NonNull Minecraft client) {
        String json = new Arrangement(Objects.requireNonNull(client.player).getInventory()).toString();

        String actualFileName;
        if (filename == null) actualFileName = UUID.randomUUID().toString().substring(0, 12)+".json";
        else actualFileName = filename.endsWith(".json") ? filename : filename+".json";

        File file = new File(CodespaceUtils.getArrangementsDir(), actualFileName);
        try {
            Files.writeString(file.toPath(), json, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        client.player.displayClientMessage(Component.translatable("csu.commands.messages.created-new-arrangement", actualFileName), false);
        Sounds.SUCCESS.playOnPlayer();
    }

    private static void listArrangements(CommandContext<FabricClientCommandSource> ctx) {
        String[] arrangements = resolveArrangements().toArray(String[]::new);
        MutableComponent base = Component.empty();

        for (int i = 0; i<arrangements.length; ++i) {
            String arrangement = arrangements[i];
            File file = new File(CodespaceUtils.getArrangementsDir(), arrangement);
            base.append(Component.literal("⇒ ").withColor(Utils.COLOR_DARK_GRAY).withStyle(Style.EMPTY.withItalic(false)));
            base.append(Component.literal(arrangement+' ').withStyle(Style.EMPTY.withItalic(false)));
            long size;
            try {
                size = Files.size(file.toPath());
            } catch (IOException e) {
                CodespaceUtils.LOGGER.error("Unexpected IOException thrown!", e);
                size = -1L;
            }
            base.append(Component.literal(size+"B ").withColor(Utils.COLOR_GRAY).withStyle(Style.EMPTY.withItalic(false)));

            MutableComponent load = Component.literal("[↓]").withStyle(Style.EMPTY
                    .withItalic(false)
                    .withColor(Utils.COLOR_BLUE)
                    .withClickEvent(new ClickEvent.SuggestCommand("/arrangement rearrange "+arrangement))
                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("csu.commands.messages.apply-arrangement-button-hover")))
            );
            MutableComponent remove = Component.literal("[✘]").withStyle(Style.EMPTY
                    .withItalic(false)
                    .withColor(Utils.COLOR_RED)
                    .withClickEvent(new ClickEvent.SuggestCommand("/arrangement remove "+arrangement))
                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("csu.commands.messages.remove-arrangement-button-hover")))
            );
            base.append(load.append(" ").append(remove));
            if (i != arrangements.length-1) base.append("\n");

            ctx.getSource().sendFeedback(base);
        }
    }

    private static void removeRearrangement(CommandContext<FabricClientCommandSource> ctx, String name) {
        File file = new File(CodespaceUtils.getArrangementsDir(), name);
        if (!file.isFile()) return;
        try {
            Utils.xOrThrow(Files.deleteIfExists(file.toPath()), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ctx.getSource().sendFeedback(Component.translatable("csu.commands.messages.removed-arrangement", name));
        Sounds.SUCCESS.playOnPlayer();
    }

    private static Stream<String> resolveArrangements() {
        try {
            return Files.list(CodespaceUtils.getArrangementsDir().toPath())
                    .map(p -> p.getFileName().toString())
                    .filter(s -> s.endsWith(".json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Component concatenateAuthors(List<String> names) {
        StringJoiner sj = new StringJoiner(", ");
        names.forEach(sj::add);
        return Component.literal(sj.toString()+'.');
    }
}
