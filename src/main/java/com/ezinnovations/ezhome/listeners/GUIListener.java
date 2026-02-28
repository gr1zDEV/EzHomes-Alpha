package com.ezinnovations.ezhome.listeners;

import com.ezinnovations.ezhome.EzHome;
import com.ezinnovations.ezhome.models.Home;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {
    private final EzHome plugin;

    public GUIListener(EzHome plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String rawTitle = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());
        String expectedTitle = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(plugin.parse(plugin.getConfig().getString("gui-title", "&6&lHomes")));
        if (!rawTitle.equals(expectedTitle)) {
            return;
        }

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }

        if (item.getType() != Material.LIGHT_BLUE_BED) {
            return;
        }

        String homeName = plugin.getHomeGUI().resolveHomeName(item);
        if (homeName == null || homeName.isBlank()) {
            return;
        }

        if (event.isShiftClick() && event.isLeftClick()) {
            if (plugin.getHomeManager().deleteHome(player.getUniqueId(), homeName)) {
                player.sendMessage(plugin.message("home-deleted", "name", homeName));
                plugin.scheduleRegionTask(player.getLocation(), () -> plugin.getHomeGUI().open(player));
            }
            return;
        }

        Home home = plugin.getHomeManager().getHome(player.getUniqueId(), homeName);
        if (home == null) {
            player.sendMessage(plugin.message("home-not-found", "name", homeName));
            return;
        }

        World world = Bukkit.getWorld(home.world());
        if (world == null) {
            player.sendMessage(plugin.message("home-not-found", "name", homeName));
            return;
        }

        Location target = new Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        player.sendMessage(plugin.message("teleporting", "name", home.name()));
        plugin.scheduleEntityTask(player, () -> player.teleportAsync(target).thenRun(player::closeInventory));
    }
}
