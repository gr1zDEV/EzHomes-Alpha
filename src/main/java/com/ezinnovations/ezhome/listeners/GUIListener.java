package com.ezinnovations.ezhome.listeners;

import com.ezinnovations.ezhome.EzHome;
import com.ezinnovations.ezhome.models.Home;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

        String rawTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (plugin.getHomeGUI().isDeleteConfirmationTitle(rawTitle)) {
            handleDeleteConfirmationClick(event, player);
            return;
        }

        String configuredTitle = plugin.getConfig().getString("gui.title",
                plugin.getConfig().getString("gui-title", "&6&lHomes"));
        String expectedTitle = PlainTextComponentSerializer.plainText().serialize(plugin.parse(configuredTitle));
        if (!rawTitle.equals(expectedTitle)) {
            return;
        }

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }

        String homeName = plugin.getHomeGUI().resolveHomeName(item);
        if (homeName == null || homeName.isBlank()) {
            return;
        }

        String action = plugin.getHomeGUI().resolveAction(item);
        if ("delete".equals(action)) {
            plugin.getHomeGUI().openDeleteConfirmation(player, homeName);
            return;
        }

        if (!"teleport".equals(action)) {
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

    private void handleDeleteConfirmationClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }

        String action = plugin.getHomeGUI().resolveAction(item);
        if ("delete".equals(action)) {
            String homeName = plugin.getHomeGUI().resolveHomeName(item);
            if (homeName != null && plugin.getHomeManager().deleteHome(player.getUniqueId(), homeName)) {
                player.sendMessage(plugin.message("home-deleted", "name", homeName));
            }
            plugin.scheduleRegionTask(player.getLocation(), () -> plugin.getHomeGUI().open(player));
            return;
        }

        plugin.scheduleRegionTask(player.getLocation(), () -> plugin.getHomeGUI().open(player));
    }
}
