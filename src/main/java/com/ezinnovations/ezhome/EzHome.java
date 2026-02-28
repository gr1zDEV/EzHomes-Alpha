package com.ezinnovations.ezhome;

import com.ezinnovations.ezhome.commands.HomeCommand;
import com.ezinnovations.ezhome.gui.HomeGUI;
import com.ezinnovations.ezhome.listeners.GUIListener;
import com.ezinnovations.ezhome.managers.HomeManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.QueryOptions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EzHome extends JavaPlugin {
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private HomeManager homeManager;
    private HomeGUI homeGUI;
    private boolean folia;
    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        folia = detectFolia();
        luckPerms = resolveLuckPerms();

        homeManager = new HomeManager(this);
        homeGUI = new HomeGUI(this);

        HomeCommand homeCommand = new HomeCommand(this);
        PluginCommand command = getCommand("home");
        if (command != null) {
            command.setExecutor(homeCommand);
            command.setTabCompleter(homeCommand);
        }

        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                homeManager.loadPlayerHomes(event.getPlayer().getUniqueId());
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                homeManager.unloadAndSave(event.getPlayer().getUniqueId());
            }
        }, this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            homeManager.loadPlayerHomes(player.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        homeManager.saveAll().join();
        homeManager.shutdown();
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public HomeGUI getHomeGUI() {
        return homeGUI;
    }

    public int getAllowedHomes(Player player) {
        int highest = getConfig().getInt("default-home-limit", 1);
        for (int i = 1; i <= 54; i++) {
            if (hasPermission(player, "ezhome.homes." + i) || hasPermission(player, "ezhomes.homes." + i)) {
                highest = Math.max(highest, i);
            }
        }
        return Math.max(1, highest);
    }

    public boolean hasLuckPermsPermission(Player player, String permission) {
        if (luckPerms == null) {
            return false;
        }

        QueryOptions queryOptions = luckPerms.getContextManager().getQueryOptions(player).orElse(null);
        if (queryOptions == null) {
            return false;
        }

        CachedPermissionData permissionData = luckPerms.getPlayerAdapter(Player.class)
                .getUser(player)
                .getCachedData()
                .getPermissionData(queryOptions);
        return permissionData.checkPermission(permission).asBoolean();
    }

    public boolean hasPermission(Player player, String permission) {
        if (luckPerms != null) {
            return hasLuckPermsPermission(player, permission);
        }
        return player.hasPermission(permission);
    }

    public Component message(String key, String... placeholders) {
        String raw = getConfig().getString("messages." + key, "&cMissing message: " + key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return parse(raw);
    }

    public Component parse(String raw) {
        if (raw.contains("<") && raw.contains(">")) {
            return miniMessage.deserialize(raw);
        }
        return legacySerializer.deserialize(raw);
    }

    public void scheduleEntityTask(Entity entity, Runnable runnable) {
        if (folia) {
            entity.getScheduler().execute(this, runnable, null, 1L);
            return;
        }
        Bukkit.getScheduler().runTask(this, runnable);
    }

    public void scheduleGlobalTask(Runnable runnable) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().execute(this, runnable);
            return;
        }
        Bukkit.getScheduler().runTask(this, runnable);
    }

    public void scheduleRegionTask(Location location, Runnable runnable) {
        if (folia) {
            Bukkit.getRegionScheduler().execute(this, location, runnable);
            return;
        }
        Bukkit.getScheduler().runTask(this, runnable);
    }

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private LuckPerms resolveLuckPerms() {
        try {
            LuckPerms api = LuckPermsProvider.get();
            getLogger().info("Hooked into LuckPerms for permission checks.");
            return api;
        } catch (IllegalStateException exception) {
            getLogger().warning("LuckPerms not found. EzHome permission-based home limits will default to configured fallback.");
            return null;
        }
    }
}
