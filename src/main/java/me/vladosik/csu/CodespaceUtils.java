package me.vladosik.csu;

import me.vladosik.csu.config.Config;
import me.vladosik.csu.config.FilesOperations;
import me.vladosik.csu.utils.PacketManager;
import me.vladosik.csu.utils.Utils;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CodespaceUtils implements ModInitializer {
	public static final String MOD_ID = "codespace-utils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static File configDir, configFile, arrangementsDir;
	public static File getConfigDir() { return configDir; }
	public static File getArrangementsDir() { return arrangementsDir; }
	public static File getConfigFile() { return configFile; }
	private static PacketManager packetManager;
	public static PacketManager getPacketManager() { return packetManager; }
	public static Config config;

	@Override
	public void onInitialize() {
		configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "codespace-utils");
		configFile = new File(configDir, "config.json");
		arrangementsDir = new File(configDir, "arrangements");

		callbackRegistration();
		CommandDispatcher.register();

		io: {
			if (!configDir.isDirectory()) {
				FilesOperations.createFull();
				break io;
			}

			if (!configFile.isFile()) FilesOperations.createConfigFile();

			if (!arrangementsDir.isDirectory()) {
				Utils.xOrThrow(arrangementsDir.mkdir(), 1);
				FilesOperations.createTemplateArrangement();
			}
		}
		config = Config.ofConfigJSON();
		packetManager = new PacketManager(config.maxPacketsPerSecond);
	}

	private void callbackRegistration() {
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
			if (Utils.worldIsCodespace(world)) {
				Arrangement primary;
				try {
                    primary = Arrangement.primary();
                } catch (IllegalStateException e) {
                    return;
                }
				primary.rearrange(true);
			}
		});

		ClientPlayerBlockBreakEvents.AFTER.register((world, player, pos, state) -> {
			if (!config.teleportOnGlassBreak || !Utils.worldIsCodespace(world)) return;
			if ((pos.getY()-4)%7 != 0 || pos.getY() == 4) return; // Нулевой этаж на y=4, высота этажа - 7 бл.
			var playerPos = player.position();

//			float offset = player.getRotationVector().y > 0 ? 7f : -7f;
			float offset = playerPos.y < pos.getY() ? 7f : -7f;
			if (player.getBlockY() == playerPos.y && offset == 7f) offset += .4f;
			// Строка сверху - это предотвращение отката, если снапнуть игрока в сломанный блок он будет телепортирован обратно античитом
			Utils.sendDebugMessage("playerY=", playerPos.y, "; targetY=", pos.getY(), "; offset=", offset);
			config.teleportationType.move(player, playerPos.x, playerPos.y+offset, playerPos.z);
		});
	}
}
