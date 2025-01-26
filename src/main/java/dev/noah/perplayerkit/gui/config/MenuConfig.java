package dev.noah.perplayerkit.gui.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@Getter
public class MenuConfig {

	private final FileConfiguration config;

	public MenuConfig(@NotNull JavaPlugin plugin, String fileName) {
		File file = new File(plugin.getDataFolder(), fileName);
		if (!file.exists()) {
			plugin.saveResource(fileName, false);
		}
		this.config = YamlConfiguration.loadConfiguration(file);
	}

	public static void of(JavaPlugin plugin) {
		new MenuConfig(plugin, "menus/kits_menu.yml");
		new MenuConfig(plugin, "menus/kit_editor_menu.yml");
	}
}