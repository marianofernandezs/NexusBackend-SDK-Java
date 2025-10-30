package com.prax.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
// import redis.clients.jedis.Jedis; // ELIMINADO
// import redis.clients.jedis.JedisPool; // ELIMINADO

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DataManager {

    private final PraxCorePlugin plugin;
    private File customConfigFile;
    private FileConfiguration customConfig;

    // private JedisPool jedisPool; // ELIMINADO
    // private final String SESSION_KEY_PREFIX = "prax:session:"; // ELIMINADO
    // private final int SESSION_TIMEOUT_SECONDS = 15; // ELIMINADO

    public DataManager(PraxCorePlugin plugin) {
        this.plugin = plugin;
        setupFile();
        // setupRedis(); // ELIMINADO
    }

    public void setupFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        customConfigFile = new File(plugin.getDataFolder(), "players.yml");
        if (!customConfigFile.exists()) {
            try {
                customConfigFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear el archivo players.yml!");
            }
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
    }

    // TODA LA LÓGICA DE REDIS (setupRedis, shutdown, setSessionActive, etc.) HA SIDO ELIMINADA

    public FileConfiguration getConfig() {
        return customConfig;
    }

    public void saveData() {
        try {
            getConfig().save(customConfigFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("No se pudo guardar la configuración en " + customConfigFile);
        }
    }

    public boolean isPlayerRegistered(UUID playerUuid) {
        return getConfig().contains("players." + playerUuid.toString());
    }

    public String getPasswordHash(UUID playerUuid) {
        return getConfig().getString("players." + playerUuid.toString() + ".passwordHash");
    }

    public void registerPlayer(UUID playerUuid, String hashedPassword, String email, String ipAddress, String clientType, String version, String birthdate) {
        String basePath = "players." + playerUuid.toString();
        getConfig().set(basePath + ".passwordHash", hashedPassword);
        getConfig().set(basePath + ".email", email);
        getConfig().set(basePath + ".registrationIp", ipAddress);
        getConfig().set(basePath + ".clientType", clientType);
        getConfig().set(basePath + ".versionOnRegister", version);
        getConfig().set(basePath + ".birthdate", birthdate);

        saveData();
    }

    public void setFirstLoginDate(UUID playerUuid, String date) {
        getConfig().set("players." + playerUuid.toString() + ".firstLogin", date);
    }

    public void setLastLoginDate(UUID playerUuid, String datetime) {
        getConfig().set("players." + playerUuid.toString() + ".lastLogin", datetime);
    }

    public void incrementLoginCount(UUID playerUuid) {
        String path = "players." + playerUuid.toString() + ".loginCount";
        int currentCount = getConfig().getInt(path, 0);
        getConfig().set(path, currentCount + 1);
    }

    public void addPlaytime(UUID playerUuid, long sessionInMillis) {
        String path = "players." + playerUuid.toString() + ".playtime";
        long currentPlaytime = getConfig().getLong(path, 0L);
        long newPlaytime = currentPlaytime + sessionInMillis;
        getConfig().set(path, newPlaytime);
    }

    public void incrementDeaths(UUID playerUuid) {
        String path = "players." + playerUuid.toString() + ".deaths";
        int currentDeaths = getConfig().getInt(path, 0);
        getConfig().set(path, currentDeaths + 1);
    }

    public void incrementKills(UUID killerUuid) {
        String path = "players." + killerUuid.toString() + ".kills";
        int currentKills = getConfig().getInt(path, 0);
        getConfig().set(path, currentKills + 1);
    }
    public String getBirthdate (UUID playerUuid) {
        return getConfig().getString("players." + playerUuid.toString() + ".birthdate");
    }
}