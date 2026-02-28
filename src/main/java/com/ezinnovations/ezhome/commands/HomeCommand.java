package com.ezinnovations.ezhome.commands;

import com.ezinnovations.ezhome.EzHome;
import com.ezinnovations.ezhome.models.Home;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeCommand implements CommandExecutor, TabCompleter {
    private final EzHome plugin;

    public HomeCommand(EzHome plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.parse("&cOnly players can use this command."));
            return true;
        }

        if (args.length == 0) {
            plugin.getHomeGUI().open(player);
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.parse("&cUsage: /home [create|delete] [name]"));
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        String name = args[1].trim();

        if (name.isEmpty()) {
            player.sendMessage(plugin.parse("&cUsage: /home [create|delete] [name]"));
            return true;
        }

        switch (subCommand) {
            case "create" -> handleCreate(player, name);
            case "delete" -> handleDelete(player, name);
            default -> player.sendMessage(plugin.parse("&cUsage: /home [create|delete] [name]"));
        }
        return true;
    }

    private void handleCreate(Player player, String name) {
        int teamSlots = Math.min(2, plugin.getConfig().getInt("team-slots", 2));
        int maxHomes = Math.min(54 - teamSlots, plugin.getAllowedHomes(player));
        int currentHomes = plugin.getHomeManager().getHomeCount(player.getUniqueId());

        if (plugin.getHomeManager().homeExists(player.getUniqueId(), name)) {
            player.sendMessage(plugin.message("home-already-exists", "name", name));
            return;
        }

        if (currentHomes >= maxHomes) {
            player.sendMessage(plugin.message("home-limit-reached", "max", String.valueOf(maxHomes)));
            return;
        }

        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        Home home = new Home(name, world.getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        plugin.getHomeManager().addHome(player.getUniqueId(), home);
        player.sendMessage(plugin.message("home-created", "name", name));
    }

    private void handleDelete(Player player, String name) {
        boolean deleted = plugin.getHomeManager().deleteHome(player.getUniqueId(), name);
        if (!deleted) {
            player.sendMessage(plugin.message("home-not-found", "name", name));
            return;
        }
        player.sendMessage(plugin.message("home-deleted", "name", name));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            return List.of("create", "delete");
        }

        if (args.length == 2 && "delete".equalsIgnoreCase(args[0])) {
            return new ArrayList<>(plugin.getHomeManager().getHomes(player.getUniqueId()).keySet());
        }

        return List.of();
    }
}
