package com.ezinnovations.ezhome.managers;

import com.ezinnovations.ezhome.EzHome;
import com.ezinnovations.ezhome.models.Home;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeManager {
    private final EzHome plugin;
    private final File dataFolder;
    private final ExecutorService ioExecutor;
    private final Map<UUID, Map<String, Home>> cache = new ConcurrentHashMap<>();

    public HomeManager(EzHome plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create data folder: " + dataFolder.getAbsolutePath());
        }
        this.ioExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "ezhome-io");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<Void> loadPlayerHomes(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            File file = playerFile(playerId);
            Map<String, Home> homes = new LinkedHashMap<>();
            if (file.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (config.isConfigurationSection("homes")) {
                    for (String name : config.getConfigurationSection("homes").getKeys(false)) {
                        String path = "homes." + name;
                        String world = config.getString(path + ".world");
                        if (world == null) {
                            continue;
                        }
                        homes.put(name.toLowerCase(), new Home(
                                name,
                                world,
                                config.getDouble(path + ".x"),
                                config.getDouble(path + ".y"),
                                config.getDouble(path + ".z"),
                                (float) config.getDouble(path + ".yaw"),
                                (float) config.getDouble(path + ".pitch")
                        ));
                    }
                }
            }
            cache.put(playerId, homes);
        }, ioExecutor);
    }

    public CompletableFuture<Void> unloadAndSave(UUID playerId) {
        Map<String, Home> homes = cache.getOrDefault(playerId, Map.of());
        return savePlayerHomes(playerId, homes).thenRun(() -> cache.remove(playerId));
    }

    public Map<String, Home> getHomes(UUID playerId) {
        return cache.computeIfAbsent(playerId, ignored -> new LinkedHashMap<>());
    }

    public Home getHome(UUID playerId, String homeName) {
        return getHomes(playerId).get(homeName.toLowerCase());
    }

    public boolean homeExists(UUID playerId, String homeName) {
        return getHomes(playerId).containsKey(homeName.toLowerCase());
    }

    public void addHome(UUID playerId, Home home) {
        Map<String, Home> homes = getHomes(playerId);
        homes.put(home.name().toLowerCase(), home);
        savePlayerHomes(playerId, homes);
    }

    public boolean deleteHome(UUID playerId, String homeName) {
        Map<String, Home> homes = getHomes(playerId);
        Home removed = homes.remove(homeName.toLowerCase());
        if (removed == null) {
            return false;
        }
        savePlayerHomes(playerId, homes);
        return true;
    }

    public int getHomeCount(UUID playerId) {
        return getHomes(playerId).size();
    }

    public CompletableFuture<Void> saveAll() {
        return CompletableFuture.allOf(cache.entrySet().stream()
                .map(entry -> savePlayerHomes(entry.getKey(), entry.getValue()))
                .toArray(CompletableFuture[]::new));
    }

    public void shutdown() {
        ioExecutor.shutdown();
    }

    private CompletableFuture<Void> savePlayerHomes(UUID playerId, Map<String, Home> homes) {
        Map<String, Home> snapshot = new LinkedHashMap<>(homes);
        return CompletableFuture.runAsync(() -> {
            File file = playerFile(playerId);
            YamlConfiguration config = new YamlConfiguration();
            for (Home home : snapshot.values()) {
                String path = "homes." + home.name();
                config.set(path + ".world", home.world());
                config.set(path + ".x", home.x());
                config.set(path + ".y", home.y());
                config.set(path + ".z", home.z());
                config.set(path + ".yaw", home.yaw());
                config.set(path + ".pitch", home.pitch());
            }
            try {
                config.save(file);
            } catch (IOException exception) {
                plugin.getLogger().severe("Failed to save homes for " + playerId + ": " + exception.getMessage());
            }
        }, ioExecutor);
    }

    private File playerFile(UUID playerId) {
        return new File(dataFolder, playerId + ".yml");
    }
}
