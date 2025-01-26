package dev.noah.perplayerkit.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemParser {

	@Contract("null, _ -> fail")
	@SuppressWarnings("deprecation")
	public static @NotNull ItemStack parse(ConfigurationSection section, Map<String, String> placeholders) {
		if (section == null) {
			throw new IllegalArgumentException("Configuration section cannot be null");
		}

		String materialName = section.getString("material", "STONE")
				.toUpperCase().replace(" ", "_");
		Material material = Material.matchMaterial(materialName);
		if (material == null) {
			throw new IllegalArgumentException("Invalid material: " + materialName);
		}

		ItemStack itemStack = new ItemStack(material);
		ItemMeta meta = itemStack.getItemMeta();
		if (meta == null) return itemStack;

		MiniMessage mm = MiniMessage.miniMessage();

		if (section.contains("name")) {
			String name = placeholders(section.getString("name"), placeholders);
			meta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, false));
		}

		if (section.contains("lore")) {
			List<String> loreStrings = section.getStringList("lore").stream()
					.map(line -> placeholders(line, placeholders))
					.toList();
			List<Component> lore = loreStrings.stream()
					.map(line -> mm.deserialize(line).decoration(TextDecoration.ITALIC, false))
					.collect(Collectors.toList());
			meta.lore(lore);
		}

		if (section.contains("flags")) {
			for (String flagString : section.getStringList("flags")) {
				try {
					ItemFlag flag = ItemFlag.valueOf(flagString.toUpperCase().replace(" ", "_"));
					meta.addItemFlags(flag);
				} catch (IllegalArgumentException ignored) {
				}
			}
		}

		if (section.contains("enchantments")) {
			for (String enchantmentString : section.getStringList("enchantments")) {
				Enchantment enchantment = Enchantment.getByName(enchantmentString.toUpperCase().replace(" ", "_"));
				if (enchantment != null) {
					meta.addEnchant(enchantment, 1, true);
				}
			}
		}

		itemStack.setItemMeta(meta);
		return itemStack;
	}

	private static String placeholders(String text, Map<String, String> placeholders) {
		if (placeholders == null) return text;
		if (text == null) return "";
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			text = text.replace(entry.getKey(), entry.getValue());
		}
		return text;
	}
}
