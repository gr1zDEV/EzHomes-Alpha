package com.ezinnovations.ezhome.gui;

import com.ezinnovations.ezhome.EzHome;
import com.ezinnovations.ezhome.models.Home;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeGUI {
    public static final int SIZE = 54;
    private static final String HOME_KEY = "home-name";

    private final EzHome plugin;
    private final LegacyComponentSerializer legacySerializer;
    private final NamespacedKey homeNameKey;

    public HomeGUI(EzHome plugin) {
        this.plugin = plugin;
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();
        this.homeNameKey = new NamespacedKey(plugin, HOME_KEY);
    }

    public void open(Player player) {
        String title = plugin.getConfig().getString("gui.title",
                plugin.getConfig().getString("gui-title", "&6&lHomes"));
        Inventory inventory = Bukkit.createInventory(player, resolveGuiSize(), parseLegacy(title));

        List<Integer> teamSlotIndexes = resolveSlots("gui.team-slot-indexes", inventory.getSize());
        int teamSlots = Math.min(teamSlotIndexes.size(), Math.max(0, plugin.getConfig().getInt("gui.team-slots",
                plugin.getConfig().getInt("team-slots", 2))));
        if (teamSlotIndexes.isEmpty()) {
            teamSlotIndexes = defaultSequentialSlots(teamSlots, inventory.getSize());
        }

        List<Integer> homeSlotIndexes = resolveSlots("gui.home-slots", inventory.getSize());
        if (homeSlotIndexes.isEmpty()) {
            homeSlotIndexes = sequentialHomeSlots(inventory.getSize(), teamSlotIndexes);
        }

        for (Map<?, ?> itemConfig : plugin.getConfig().getMapList("gui.static-items")) {
            int slot = toInt(itemConfig.get("slot"), -1);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            inventory.setItem(slot, buildConfigItem(itemConfig));
        }

        for (int i = 0; i < teamSlots; i++) {
            inventory.setItem(teamSlotIndexes.get(i), configItem("gui.team-item", Material.RED_DYE,
                    "&c&lTeams", List.of("&7Coming Soon...")));
        }

        int maxHomesByGui = homeSlotIndexes.size();
        int allowedHomes = Math.min(maxHomesByGui, plugin.getAllowedHomes(player));
        List<Home> homes = new ArrayList<>(plugin.getHomeManager().getHomes(player.getUniqueId()).values());
        homes.sort(Comparator.comparing(Home::name, String.CASE_INSENSITIVE_ORDER));

        for (int index = 0; index < homeSlotIndexes.size(); index++) {
            int slot = homeSlotIndexes.get(index);
            if (index < homes.size()) {
                Home home = homes.get(index);
                inventory.setItem(slot, homeItem(home));
            } else if (index < allowedHomes) {
                inventory.setItem(slot, configItem("gui.empty-home-item",
                        Material.GRAY_STAINED_GLASS_PANE, "&7Empty Home Slot", List.of("&7Use /home create <name>")));
            } else {
                inventory.setItem(slot, configItem("gui.locked-home-item",
                        Material.BLACK_STAINED_GLASS_PANE, "&8Locked", List.of("&7Purchase more home slots!")));
            }
        }

        player.openInventory(inventory);
    }

    public String resolveHomeName(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        String storedName = meta.getPersistentDataContainer().get(homeNameKey, PersistentDataType.STRING);
        if (storedName != null && !storedName.isBlank()) {
            return storedName;
        }
        Component displayName = meta.displayName();
        if (displayName == null) {
            return null;
        }
        return PlainTextComponentSerializer.plainText().serialize(displayName);
    }

    private ItemStack homeItem(Home home) {
        ItemStack item = configItem("gui.home-item", Material.LIGHT_BLUE_BED,
                "&b&l{name}",
                List.of(
                        "&7World: &f{world}",
                        "&7X: &f{x} &7Y: &f{y} &7Z: &f{z}",
                        "&eClick to teleport",
                        "&cShift+Click to delete"
                ),
                "name", home.name(),
                "world", home.world(),
                "x", format(home.x()),
                "y", format(home.y()),
                "z", format(home.z()));

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(homeNameKey, PersistentDataType.STRING, home.name());
        item.setItemMeta(meta);
        return item;
    }


    private ItemStack namedItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(parseLegacy(name));
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(this::parseLegacy).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack configItem(String path, Material fallbackMaterial, String fallbackName, List<String> fallbackLore, String... placeholders) {
        Material material = material(plugin.getConfig().getString(path + ".material"), fallbackMaterial);
        String name = replacePlaceholders(plugin.getConfig().getString(path + ".name", fallbackName), placeholders);
        List<String> lore = plugin.getConfig().getStringList(path + ".lore").stream()
                .map(line -> replacePlaceholders(line, placeholders))
                .toList();
        if (lore.isEmpty()) {
            lore = fallbackLore.stream().map(line -> replacePlaceholders(line, placeholders)).toList();
        }
        return namedItem(material, name, lore);
    }

    private ItemStack buildConfigItem(Map<?, ?> itemConfig) {
        Material material = material(asString(itemConfig.get("material")), Material.BLACK_STAINED_GLASS_PANE);
        String name = asString(itemConfig.get("name"));
        if (name == null || name.isBlank()) {
            name = " ";
        }
        List<String> lore = itemConfig.get("lore") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        return namedItem(material, name, lore);
    }

    private Material material(String input, Material fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        Material parsed = Material.matchMaterial(input);
        return parsed != null ? parsed : fallback;
    }

    private int resolveGuiSize() {
        int configured = plugin.getConfig().getInt("gui.size", SIZE);
        int clamped = Math.max(9, Math.min(SIZE, configured));
        return clamped - (clamped % 9);
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value != null ? Integer.parseInt(String.valueOf(value)) : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private List<Integer> resolveSlots(String path, int inventorySize) {
        List<Integer> slots = new ArrayList<>();
        for (int rawSlot : plugin.getConfig().getIntegerList(path)) {
            if (rawSlot >= 0 && rawSlot < inventorySize && !slots.contains(rawSlot)) {
                slots.add(rawSlot);
            }
        }
        return slots;
    }

    private List<Integer> defaultSequentialSlots(int count, int inventorySize) {
        List<Integer> slots = new ArrayList<>();
        int max = Math.min(count, inventorySize);
        for (int i = 0; i < max; i++) {
            slots.add(i);
        }
        return slots;
    }

    private List<Integer> sequentialHomeSlots(int inventorySize, List<Integer> teamSlots) {
        Set<Integer> blocked = new HashSet<>(teamSlots);
        List<Integer> homeSlots = new ArrayList<>();
        for (int i = 0; i < inventorySize; i++) {
            if (!blocked.contains(i)) {
                homeSlots.add(i);
            }
        }
        return homeSlots;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String replacePlaceholders(String text, String... placeholders) {
        if (text == null) {
            return "";
        }
        String resolved = text;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            resolved = resolved.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return resolved;
    }

    private Component parseLegacy(String text) {
        return legacySerializer.deserialize(text);
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }
}
