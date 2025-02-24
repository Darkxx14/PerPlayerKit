/*
 * Copyright 2022-2025 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.noah.perplayerkit.gui;

import dev.noah.perplayerkit.*;
import dev.noah.perplayerkit.gui.config.MenuConfig;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.ItemParser;
import dev.noah.perplayerkit.util.PlayerUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.slot.ClickOptions;
import org.ipvp.canvas.slot.Slot;
import org.ipvp.canvas.type.ChestMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static dev.noah.perplayerkit.gui.ItemUtil.addHideFlags;
import static dev.noah.perplayerkit.gui.ItemUtil.createItem;

public class GUI {
    private final Plugin plugin;
    private final boolean filterItemsOnImport;

    // Menu configs
    private static final MenuConfig KITS_MENU_CONFIG = new MenuConfig(PerPlayerKit.getProvidingPlugin(PerPlayerKit.class), "menus/kits_menu.yml");
    private static final MenuConfig KITS_EDITOR_MENU_CONFIG = new MenuConfig(PerPlayerKit.getProvidingPlugin(PerPlayerKit.class), "menus/kit_editor_menu.yml");


    public GUI(Plugin plugin) {
        this.plugin = plugin;
        this.filterItemsOnImport = plugin.getConfig().getBoolean("anti-exploit.import-filter", false);
    }

    public static void addLoadPublicKit(Slot slot, String id) {
        slot.setClickHandler((player, info) -> KitManager.get().loadPublicKit(player, id));
    }

    public static Menu createPublicKitMenu() {
        return ChestMenu.builder(6).title(ChatColor.BLUE + "Public Kit Room").redraw(true).build();
    }

    public void OpenKitMenu(Player p, int slot) {
        Menu menu = createKitMenu(slot);
        ConfigurationSection config = KITS_EDITOR_MENU_CONFIG.getConfig().getConfigurationSection("kit_editor_menu");

        if (config == null) throw new IllegalStateException("Configuration section 'kit_editor_menu' not found in kit_editor_menu.yml");

        // Kit Items
        if (KitManager.get().getItemStackArrayById(p.getUniqueId().toString() + slot) != null) {
            ItemStack[] kit = KitManager.get().getItemStackArrayById(p.getUniqueId().toString() + slot);
            for (int i = 0; i < 41; i++) {
                menu.getSlot(i).setItem(kit[i]);
            }
        }
        for (int i = 0; i < 41; i++) {
            allowModification(menu.getSlot(i));
        }

        // Filter
        ConfigurationSection filterSection = config.getConfigurationSection("filter");
        if (filterSection != null) {
            List<Integer> filterSlots = filterSection.getIntegerList("slots");
            ItemStack filterItem = ItemParser.parse(filterSection, null);
            for (int i : filterSlots) menu.getSlot(i).setItem(filterItem);
        }

        // Armor Items
        String[] armorTypes = {"helmet", "chestplate", "leggings", "boots", "off_hand"};
        for (String armorType : armorTypes) {
            ConfigurationSection armorSection = config.getConfigurationSection(armorType);
            if (armorSection != null) {
                List<Integer> armorSlots = armorSection.getIntegerList("slots");
                ItemStack armorItem = ItemParser.parse(armorSection, null);
                for (int i : armorSlots) menu.getSlot(i).setItem(armorItem);
            }
        }

        // Buttons
        button(menu, config, "import");
        button(menu, config, "clear_kit");
        button(menu, config, "back");

        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
    }

    public void OpenPublicKitEditor(Player p, String kitId) {
        Menu menu = createPublicKitMenu(kitId);

        if (KitManager.get().getItemStackArrayById(IDUtil.getPublicKitId(kitId)) != null) {

            ItemStack[] kit = KitManager.get().getItemStackArrayById(IDUtil.getPublicKitId(kitId));
            for (int i = 0; i < 41; i++) {
                menu.getSlot(i).setItem(kit[i]);
            }
        }
        for (int i = 0; i < 41; i++) {
            allowModification(menu.getSlot(i));
        }
        for (int i = 41; i < 54; i++) {
            menu.getSlot(i).setItem(ItemUtil.createItem(Material.BLUE_STAINED_GLASS_PANE, 1, " "));

        }
        menu.getSlot(45).setItem(createItem(Material.CHAINMAIL_BOOTS, 1, "&7&lBOOTS"));
        menu.getSlot(46).setItem(createItem(Material.CHAINMAIL_LEGGINGS, 1, "&7&lLEGGINGS"));
        menu.getSlot(47).setItem(createItem(Material.CHAINMAIL_CHESTPLATE, 1, "&7&lCHESTPLATE"));
        menu.getSlot(48).setItem(createItem(Material.CHAINMAIL_HELMET, 1, "&7&lHELMET"));
        menu.getSlot(49).setItem(createItem(Material.SHIELD, 1, "&7&lOFFHAND"));

        menu.getSlot(51).setItem(createItem(Material.CHEST, 1, "&a&lIMPORT", "&7● Import from inventory"));
        menu.getSlot(52).setItem(createItem(Material.BARRIER, 1, "&c&lCLEAR KIT", "&7● Shift click to clear"));
        menu.getSlot(53).setItem(createItem(Material.OAK_DOOR, 1, "&c&lBACK"));

        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
    }

    public void OpenECKitKenu(Player p, int slot) {
        Menu menu = createECMenu(slot);

        for (int i = 0; i < 9; i++) {
            menu.getSlot(i).setItem(ItemUtil.createItem(Material.BLUE_STAINED_GLASS_PANE, 1, " "));

        }
        for (int i = 36; i < 54; i++) {
            menu.getSlot(i).setItem(ItemUtil.createItem(Material.BLUE_STAINED_GLASS_PANE, 1, " "));

        }
        if (KitManager.get().getItemStackArrayById(p.getUniqueId() + "ec" + slot) != null) {

            ItemStack[] kit = KitManager.get().getItemStackArrayById(p.getUniqueId() + "ec" + slot);
            for (int i = 9; i < 36; i++) {
                menu.getSlot(i).setItem(kit[i - 9]);
            }
        }
        for (int i = 9; i < 36; i++) {
            allowModification(menu.getSlot(i));
        }
        menu.getSlot(51).setItem(createItem(Material.CHEST, 1, "&a&lIMPORT", "&7● Import from inventory"));
        menu.getSlot(52).setItem(createItem(Material.BARRIER, 1, "&c&lCLEAR KIT", "&7● Shift click to clear"));
        menu.getSlot(53).setItem(createItem(Material.OAK_DOOR, 1, "&c&lBACK"));
        addMainButton(menu.getSlot(53));
        addClear(menu.getSlot(52), 9, 36);
        addImportEC(menu.getSlot(51));
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
    }

    public void InspectKit(Player p, UUID target, int slot) {
        Menu menu = createInspectMenu(slot, target.toString());

        if (KitManager.get().getItemStackArrayById(target.toString() + slot) != null) {

            ItemStack[] kit = KitManager.get().getItemStackArrayById(target.toString() + slot);
            for (int i = 0; i < 41; i++) {
                menu.getSlot(i).setItem(kit[i]);
            }
        }

        for (int i = 41; i < 54; i++) {
            menu.getSlot(i).setItem(ItemUtil.createItem(Material.BLUE_STAINED_GLASS_PANE, 1, " "));

        }
        menu.getSlot(45).setItem(createItem(Material.CHAINMAIL_BOOTS, 1, "&7&lBOOTS"));
        menu.getSlot(46).setItem(createItem(Material.CHAINMAIL_LEGGINGS, 1, "&7&lLEGGINGS"));
        menu.getSlot(47).setItem(createItem(Material.CHAINMAIL_CHESTPLATE, 1, "&7&lCHESTPLATE"));
        menu.getSlot(48).setItem(createItem(Material.CHAINMAIL_HELMET, 1, "&7&lHELMET"));
        menu.getSlot(49).setItem(createItem(Material.SHIELD, 1, "&7&lOFFHAND"));

        menu.open(p);

    }

    public void OpenMainMenu(Player p) {
        Menu menu = createMainMenu(p);
        ConfigurationSection config = KITS_MENU_CONFIG.getConfig().getConfigurationSection("kit_menu");

        if (config == null) throw new IllegalStateException("Configuration section 'kit_menu' not found in kit_menu.yml");

        // Filter
        ConfigurationSection filterSection = config.getConfigurationSection("filter");
        if (filterSection != null) {
            List<Integer> filterSlots = filterSection.getIntegerList("slots");
            ItemStack filterItem = ItemParser.parse(filterSection, null);
            for (int i : filterSlots) menu.getSlot(i).setItem(filterItem);
        }

        // Kits
        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        if (kitsSection != null) {
            List<Integer> kitSlots = kitsSection.getIntegerList("slots");
            for (int i : kitSlots) {
                ItemStack kitItem = ItemParser.parse(kitsSection, Map.of("<kit>", String.valueOf(i - 8)));
                menu.getSlot(i).setItem(kitItem);
                addEditLoad(menu.getSlot(i), i - 8);
            }
        }

        // Ender chest
        ConfigurationSection ecSection = config.getConfigurationSection("ender_chests");
        if (ecSection != null) {
            List<Integer> ecSlots = ecSection.getIntegerList("slots");
            for (int i : ecSlots) {
                ItemStack ecItem = ItemParser.parse(ecSection, Map.of("<ec>", String.valueOf(i - 17)));
                menu.getSlot(i).setItem(ecItem);
                addEditLoadEC(menu.getSlot(i), i - 17);
            }
        }

        // Kit Exists
        ConfigurationSection customKitSection = config.getConfigurationSection("kit_exists");
        if (customKitSection != null) {
            List<Integer> customKitSlots = customKitSection.getIntegerList("slots");
            ItemStack kitExistsItem = ItemParser.parse(customKitSection.getConfigurationSection("exists"), null);
            ItemStack kitNotFoundItem = ItemParser.parse(customKitSection.getConfigurationSection("not_found"), null);
            for (int i = 0; i < customKitSlots.size(); i++) {
                int slot = customKitSlots.get(i);
                if (KitManager.get().getItemStackArrayById(p.getUniqueId().toString() + (i + 1)) != null) {
                    menu.getSlot(slot).setItem(kitExistsItem);
                } else {
                    menu.getSlot(slot).setItem(kitNotFoundItem);
                }
                addEdit(menu.getSlot(slot), i + 1);
            }
        }

        // Buttons
        button(menu, config, "kit_room");
        button(menu, config, "premade_kits");
        button(menu, config, "info");
        button(menu, config, "clear_inventory");
        button(menu, config, "share_kits");
        button(menu, config, "repair_items");

        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
    }

    private void button(Menu menu, @NotNull ConfigurationSection config, String key) {
        ConfigurationSection section = config.getConfigurationSection(key);
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }
        ItemStack buttonItem = ItemParser.parse(section, null);
        List<Integer> slots = section.getIntegerList("slots");

        for (int slot : slots) {
            menu.getSlot(slot).setItem(buttonItem);

            switch (key) {
                case "kit_room":
                    addKitRoom(menu.getSlot(slot));
                    break;
                case "premade_kits":
                    addPublicKitMenu(menu.getSlot(slot));
                    break;
                case "clear_inventory":
                    addClearButton(menu.getSlot(slot));
                    break;
                case "repair_items":
                    addRepairButton(menu.getSlot(slot));
                case "import":
                    addImport(menu.getSlot(slot));
                case "clear_kit":
                    addClear(menu.getSlot(slot));
                case "back":
                    addMainButton(menu.getSlot(slot));
                    break;
            }
        }
    }

    public void OpenKitRoom(Player p) {
        OpenKitRoom(p, 0);

    }

    public void OpenKitRoom(Player p, int page) {
        Menu menu = createKitRoom();
        for (int i = 0; i < 45; i++) {
            allowModification(menu.getSlot(i));
        }
        for (int i = 45; i < 54; i++) {

            menu.getSlot(i).setItem(ItemUtil.createItem(Material.BLUE_STAINED_GLASS_PANE, 1, " "));
        }
        if (KitRoomDataManager.get().getKitRoomPage(page) != null) {
            for (int i = 0; i < 45; i++) {
                menu.getSlot(i).setItem(KitRoomDataManager.get().getKitRoomPage(page)[i]);
            }
        }

        menu.getSlot(45).setItem(createItem(Material.BEACON, 1, "&3&lREFILL"));
        addKitRoom(menu.getSlot(45), page);

        if (!p.hasPermission("perplayerkit.editkitroom")) {
            menu.getSlot(53).setItem(createItem(Material.OAK_DOOR, 1, "&c&lBACK"));
            addMainButton(menu.getSlot(53));
        } else {
            menu.getSlot(53).setItem(createItem(Material.BARRIER, page + 1, "&c&lEDIT MENU", "&cSHIFT RIGHT CLICK TO SAVE"));
        }
        addKitRoom(menu.getSlot(47), 0);
        addKitRoom(menu.getSlot(48), 1);
        addKitRoom(menu.getSlot(49), 2);
        addKitRoom(menu.getSlot(50), 3);
        addKitRoom(menu.getSlot(51), 4);

        // add kit room buttons for the sections from config
        for (int i = 1; i < 6; i++) {
            menu.getSlot(46 + i).setItem(addHideFlags(createItem(Material.valueOf(plugin.getConfig().getString("kitroom.items." + i + ".material")), "&r" + plugin.getConfig().getString("kitroom.items." + i + ".name"))));
        }

        menu.getSlot(page + 47).setItem(ItemUtil.addEnchantLook(menu.getSlot(page + 47).getItem(p)));

        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);

    }

    public Menu ViewPublicKitMenu(Player p, String id) {

        ItemStack[] kit = KitManager.get().getPublicKit(id);

        if (kit == null) {
            p.sendMessage(ChatColor.RED + "Kit not found");
            if (p.hasPermission("perplayerkit.admin")) {
                p.sendMessage(ChatColor.RED + "To assign a kit to this publickit use /savepublickit <id>");
            }
            return null;
        }
        Menu menu = ChestMenu.builder(6).title(ChatColor.BLUE + "Viewing Public Kit: " + id).redraw(true).build();

        for (int i = 0; i < 54; i++) {
            menu.getSlot(i).setItem(ItemUtil.createItem(Material.BLUE_STAINED_GLASS_PANE, 1, " "));
        }


        for (int i = 9; i < 36; i++) {
            menu.getSlot(i).setItem(kit[i]);
        }
        for (int i = 0; i < 9; i++) {
            menu.getSlot(i + 36).setItem(kit[i]);
        }
        for (int i = 36; i < 41; i++) {
            menu.getSlot(i + 9).setItem(kit[i]);
        }

        menu.getSlot(52).setItem(createItem(Material.APPLE, 1, "&a&lLOAD KIT"));
        menu.getSlot(53).setItem(createItem(Material.OAK_DOOR, 1, "&c&lBACK"));
        addPublicKitMenu(menu.getSlot(53));
        addLoadPublicKit(menu.getSlot(52), id);

        // load kit button

        menu.open(p);

        return menu;
    }

    public void OpenPublicKitMenu(Player player) {
        Menu menu = createPublicKitMenu();
        for (int i = 0; i < 54; i++) {
            menu.getSlot(i).setItem(ItemUtil.createItem(Material.BLUE_STAINED_GLASS_PANE, 1, " "));
        }

        for (int i = 18; i < 36; i++) {
            menu.getSlot(i).setItem(ItemUtil.createItem(Material.BOOK, 1, "&7&lMORE KITS COMING SOON"));
        }

        List<PublicKit> publicKitList = KitManager.get().getPublicKitList();

        for (int i = 0; i < publicKitList.size(); i++) {

            if (KitManager.get().hasPublicKit(publicKitList.get(i).id)) {


                if(player.hasPermission("perplayerkit.admin")) {
                    menu.getSlot(i + 18).setItem(createItem(publicKitList.get(i).icon, 1, ChatColor.RESET+ publicKitList.get(i).name,"&7● [ADMIN] Shift click to edit"));
                }else {
                    menu.getSlot(i + 18).setItem(createItem(publicKitList.get(i).icon, 1, ChatColor.RESET+ publicKitList.get(i).name));
                }
                addPublicKitButton(menu.getSlot(i + 18), publicKitList.get(i).id);
            } else {
                if(player.hasPermission("perplayerkit.admin")){
                    menu.getSlot(i + 18).setItem(createItem(publicKitList.get(i).icon, 1, ChatColor.RESET+ publicKitList.get(i).name+ " &c&l[UNASSIGNED]","&7● Admins have not yet setup this kit yet","&7● [ADMIN] Shift click to edit"));

                }else{
                    menu.getSlot(i + 18).setItem(createItem(publicKitList.get(i).icon, 1, ChatColor.RESET+ publicKitList.get(i).name+ " &c&l[UNASSIGNED]","&7● Admins have not yet setup this kit yet"));
                }
            }

            if(player.hasPermission("perplayerkit.admin")){
              addAdminPublicKitButton(menu.getSlot(i + 18), publicKitList.get(i).id);
            }

        }

        addMainButton(menu.getSlot(53));

        menu.getSlot(53).setItem(createItem(Material.OAK_DOOR, 1, "&c&lBACK"));
        menu.open(player);

    }

    public void addClear(Slot slot) {
        slot.setClickHandler((player, info) -> {
            if (info.getClickType().isShiftClick()) {
                Menu m = info.getClickedMenu();
                for (int i = 0; i < 41; i++) {
                    m.getSlot(i).setItem((org.bukkit.inventory.ItemStack) null);
                }
            }
        });
    }

    public void addClear(Slot slot, int start, int end) {
        slot.setClickHandler((player, info) -> {
            if (info.getClickType().isShiftClick()) {
                Menu m = info.getClickedMenu();
                for (int i = start; i < end; i++) {
                    m.getSlot(i).setItem((org.bukkit.inventory.ItemStack) null);
                }
            }
        });
    }

    public void addPublicKitButton(Slot slot, String id) {
        slot.setClickHandler((player, info) -> {
            if (info.getClickType() == ClickType.LEFT) {
                KitManager.get().loadPublicKit(player, id);
            } else if (info.getClickType() == ClickType.RIGHT) {
                Menu m = ViewPublicKitMenu(player, id);
                if(m != null) {
                    m.open(player);
                }
            }

        });
    }
    public void addAdminPublicKitButton(Slot slot, String id) {
        slot.setClickHandler((player, info) -> {
            if (info.getClickType().isShiftClick()) {
                OpenPublicKitEditor(player, id);
                return;
            }
            if (info.getClickType() == ClickType.LEFT) {
                KitManager.get().loadPublicKit(player, id);
            } else if (info.getClickType() == ClickType.RIGHT) {
                Menu m = ViewPublicKitMenu(player, id);
                if(m != null) {
                    m.open(player);
                }
            }

        });
    }

    public void addMainButton(Slot slot) {
        slot.setClickHandler((player, info) -> OpenMainMenu(player));
    }

    public void addKitRoom(Slot slot) {
        slot.setClickHandler((player, info) -> {
            OpenKitRoom(player);
            BroadcastManager.get().broadcastPlayerOpenedKitRoom(player);

        });
    }

    public void addKitRoom(Slot slot, int page) {
        slot.setClickHandler((player, info) -> OpenKitRoom(player, page));
    }

    public void addPublicKitMenu(Slot slot) {
        slot.setClickHandler((player, info) -> OpenPublicKitMenu(player));
    }

    public void addKitRoomSaveButton(Slot slot, int page) {
        slot.setClickHandler((player, info) -> {
            if (info.getClickType().isRightClick() && info.getClickType().isShiftClick()) {
                ItemStack[] data = new ItemStack[45];
                for (int i = 0; i < 41; i++) {
                    data[i] = player.getInventory().getContents()[i];

                }
                KitRoomDataManager.get().setKitRoom(page, data);
                player.sendMessage("saved menu");

            }
        });
    }

    public void addRepairButton(Slot slot) {
        slot.setClickHandler((player, info) -> {
            BroadcastManager.get().broadcastPlayerRepaired(player);
            PlayerUtil.repairAll(player);

        });
    }

    public void addClearButton(Slot slot) {
        slot.setClickHandler((player, info) -> {
            if (info.getClickType().isShiftClick()) {
                player.getInventory().clear();
                player.sendMessage(ChatColor.GREEN + "Inventory cleared");
            }
        });
    }

    public void addImport(Slot slot) {
        slot.setClickHandler((player, info) -> {
            Menu m = info.getClickedMenu();
            ItemStack[] inv;
            if (filterItemsOnImport) {
                inv = ItemFilter.get().filterItemStack(player.getInventory().getContents());
            } else {
                inv = player.getInventory().getContents();
            }
            for (int i = 0; i < 41; i++) {
                m.getSlot(i).setItem(inv[i]);
            }
        });

    }

    public void addImportEC(Slot slot) {
        slot.setClickHandler((player, info) -> {
            Menu m = info.getClickedMenu();
            ItemStack[] inv;
            if (filterItemsOnImport) {
                inv = ItemFilter.get().filterItemStack(player.getEnderChest().getContents());
            } else {
                inv = player.getEnderChest().getContents();
            }
            for (int i = 0; i < 27; i++) {
                m.getSlot(i + 9).setItem(inv[i]);
            }
        });
    }

    public void addEdit(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            if (info.getClickType().isLeftClick() || info.getClickType().isRightClick()) {
                OpenKitMenu(player, i);
            }
        });
    }

    public void addEditEC(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            if (info.getClickType().isLeftClick() || info.getClickType().isRightClick()) {
                OpenECKitKenu(player, i);
            }
        });
    }

    public void addLoad(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            if (info.getClickType() == ClickType.LEFT || info.getClickType() == ClickType.SHIFT_LEFT) {
                KitManager.get().loadKit(player, i);
                info.getClickedMenu().close();

            }
        });
    }

    public void addEditLoad(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            if (info.getClickType() == ClickType.LEFT || info.getClickType() == ClickType.SHIFT_LEFT) {
                KitManager.get().loadKit(player, i);
                info.getClickedMenu().close();
            } else if (info.getClickType() == ClickType.RIGHT || info.getClickType() == ClickType.SHIFT_RIGHT) {
                OpenKitMenu(player, i);
            }
        });
    }

    public void addEditLoadEC(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            if (info.getClickType() == ClickType.LEFT || info.getClickType() == ClickType.SHIFT_LEFT) {
                KitManager.get().loadEnderchest(player, i);
                info.getClickedMenu().close();
            } else if (info.getClickType() == ClickType.RIGHT || info.getClickType() == ClickType.SHIFT_RIGHT) {
                OpenECKitKenu(player, i);
            }
        });
    }

    public Menu createKitMenu(int slot) {
        String title = Objects.requireNonNull(KITS_EDITOR_MENU_CONFIG.getConfig().getString("kit_editor_menu.title")).replace("<kit>", String.valueOf(slot));
        return ChestMenu.builder(KITS_EDITOR_MENU_CONFIG.getConfig().getInt("kit_editor_menu.rows"))
                .title(title)
                .build();
    }

    public Menu createPublicKitMenu(String id) {
        return ChestMenu.builder(6).title(ChatColor.BLUE + "Public Kit: " + id).build();
    }

    public Menu createECMenu(int slot) {
        return ChestMenu.builder(6).title(ChatColor.BLUE + "Enderchest: " + slot).build();
    }

    public Menu createInspectMenu(int slot, String s) {
        return ChestMenu.builder(6).title(ChatColor.BLUE + "Inspecting: " + s + " Slot: " + slot).build();
    }

    public Menu createMainMenu(@NotNull Player p) {
        String title = Objects.requireNonNull(KITS_MENU_CONFIG.getConfig().getString("kit_menu.title")).replace("<player>", p.getName());
        return ChestMenu.builder(KITS_MENU_CONFIG.getConfig().getInt("kit_menu.rows"))
                .title(title)
                .build();
    }

    public Menu createKitRoom() {
        return ChestMenu.builder(6).title(ChatColor.BLUE + "Kit Room").redraw(true).build();
    }

    public void allowModification(Slot slot) {
        ClickOptions options = ClickOptions.ALLOW_ALL;
        slot.setClickOptions(options);
    }
}
