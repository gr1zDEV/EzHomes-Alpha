package com.ezinnovations.ezhome.gui;

import com.ezinnovations.ezhome.EzHome;
import com.ezinnovations.ezhome.models.Home;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HomeGUI {
    public static final int SIZE = 54;

    private final EzHome plugin;
    private final LegacyComponentSerializer legacySerializer;

    public HomeGUI(EzHome plugin) {
        this.plugin = plugin;
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();
    }

    public void open(Player player) {
        String title = plugin.getConfig().getString("gui-title", "&6&lHomes");
        Inventory inventory = Bukkit.createInventory(player, SIZE, parseLegacy(title));

        int teamSlots = Math.min(2, plugin.getConfig().getInt("team-slots", 2));
        for (int i = 0; i < teamSlots; i++) {
            inventory.setItem(i, namedItem(Material.RED_DYE, "&c&lTeams", List.of("&7Coming Soon...")));
        }

        int maxHomesByGui = SIZE - teamSlots;
        int allowedHomes = Math.min(maxHomesByGui, plugin.getAllowedHomes(player));
        List<Home> homes = new ArrayList<>(plugin.getHomeManager().getHomes(player.getUniqueId()).values());
        homes.sort(Comparator.comparing(Home::name, String.CASE_INSENSITIVE_ORDER));

        int homeStart = teamSlots;

        for (int slot = homeStart; slot < SIZE; slot++) {
            int index = slot - homeStart;
            if (index < homes.size()) {
                Home home = homes.get(index);
                inventory.setItem(slot, homeItem(home));
            } else if (index < allowedHomes) {
                inventory.setItem(slot, namedItem(Material.GRAY_STAINED_GLASS_PANE, "&7Empty Home Slot", List.of("&7Use /home create <name>")));
            } else {
                inventory.setItem(slot, namedItem(Material.BLACK_STAINED_GLASS_PANE, "&8Locked", List.of("&7Purchase more home slots!")));
            }
        }

        if (teamSlots < 2) {
            for (int i = teamSlots; i < 2; i++) {
                inventory.setItem(i, blankPane());
            }
        }

        player.openInventory(inventory);
    }

    public String resolveHomeName(ItemStack stack) {
        if (stack == null || stack.getType() != Material.LIGHT_BLUE_BED || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        Component displayName = meta.displayName();
        if (displayName == null) {
            return null;
        }
        return PlainTextComponentSerializer.plainText().serialize(displayName);
    }

    private ItemStack homeItem(Home home) {
        return namedItem(Material.LIGHT_BLUE_BED,
                "&b&l" + home.name(),
                List.of(
                        "&7World: &f" + home.world(),
                        "&7X: &f" + format(home.x()) + " &7Y: &f" + format(home.y()) + " &7Z: &f" + format(home.z()),
                        "&eClick to teleport",
                        "&cShift+Click to delete"
                ));
    }

    private ItemStack blankPane() {
        return namedItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
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

    private Component parseLegacy(String text) {
        return legacySerializer.deserialize(text);
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }
}
