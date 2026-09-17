package me.vladosik.csu.config;


import me.vladosik.csu.CodespaceUtils;
import me.vladosik.csu.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class FilesOperations {
    private FilesOperations() {
    }

    public static void createFull() {
        Utils.xOrThrow(CodespaceUtils.getConfigDir().mkdir(), true);
        createConfigFile();
        Utils.xOrThrow(CodespaceUtils.getArrangementsDir().mkdir(), true);
        createTemplateArrangement();
    }

    public static void createTemplateArrangement() {
        File template = new File(CodespaceUtils.getArrangementsDir(), "default.json");
        try {
            InputStream is = Utils.getAsset("files/template_arrangement.json");
            if (is == null) throw new FileNotFoundException("Missing template arrangement resource!");
            Files.writeString(template.toPath(), new String(is.readAllBytes()), StandardOpenOption.CREATE_NEW);
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createConfigFile() {
        File configFile = new File(CodespaceUtils.getConfigDir(), "config.json");
        try {
            InputStream is = Utils.getAsset("files/config.json");
            if (is == null) throw new FileNotFoundException("Missing template config resource!");
            Files.writeString(configFile.toPath(), new String(is.readAllBytes()), StandardOpenOption.CREATE_NEW);
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void onConfigBroken(Exception e) {
        CodespaceUtils.LOGGER.error("The config file is malformed!\n{}", Utils.catchException(e));
        CodespaceUtils.LOGGER.error("Default file will be created and old one will be labeled \"broken\". Go and fix it when possible");

        try {
            Files.move(
                CodespaceUtils.getConfigFile().toPath(),
                new File(CodespaceUtils.getConfigFile().getParent(), "config.json.BROKEN").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            );
            createConfigFile();
        } catch (IOException ex) {
            CodespaceUtils.LOGGER.error("Well, I tried my best...");
            throw new RuntimeException(ex);
        }
    }
}
