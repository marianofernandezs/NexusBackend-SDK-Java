// Ubicación: praxcore-paper/src/main/java/com/prax/core/PraxCorePlugin.java
package com.prax.core;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.prax.core.commands.LoginCommand;
import com.prax.core.commands.LogoutCommand;
import com.prax.core.commands.RegisterCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PraxCorePlugin extends JavaPlugin {

    private DataManager dataManager;
    private String serverType;
    private final Set<UUID> authenticatedPlayers = new HashSet<>();
    private final Set<UUID> pendingValidationPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Long> playerLoginTimes = new HashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.serverType = this.getConfig().getString("server-type", "backend");
        this.dataManager = new DataManager(this);

        // CRÍTICO: Registrar canales PRIMERO
        getLogger().info("[DEBUG] Registrando canales de Plugin Message...");
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "prax:core");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "prax:core", new PluginMessageListener(this));
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new PluginMessageListener(this));
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "nexus:sync", new PluginMessageListener(this));
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "nexus:sync");
        getLogger().info("[DEBUG] Canales registrados correctamente");

        // Después registrar eventos
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStatsListener(this), this);

        if (isLobbyServer()) {
            getCommand("register").setExecutor(new RegisterCommand(this));
            getCommand("login").setExecutor(new LoginCommand(this));
            getCommand("logout").setExecutor(new LogoutCommand(this));
            getServer().getPluginManager().registerEvents(new SelectorListener(this), this);
            getLogger().info("Modo Lobby detectado");
        } else {
            getLogger().info("Modo Backend (" + serverType + ") detectado");
        }
    }

    // --- MÉTODOS DE ESTADO DEL JUGADOR (Sin cambios) ---
    public boolean isLobbyServer() { return "lobby".equalsIgnoreCase(serverType); }
    public String getServerType() { return this.serverType; }
    public boolean isAuthenticated(UUID playerUuid) { return authenticatedPlayers.contains(playerUuid); }
    public boolean isPendingValidation(UUID playerUuid) { return pendingValidationPlayers.contains(playerUuid); }

    public void setAuthenticated(UUID playerUuid, boolean authenticated) {
        if (authenticated) {
            authenticatedPlayers.add(playerUuid);
        } else {
            authenticatedPlayers.remove(playerUuid);
        }
        pendingValidationPlayers.remove(playerUuid);
    }

    public void setPendingValidation(UUID playerUuid, boolean pending) {
        if (pending) {
            pendingValidationPlayers.add(playerUuid);
        } else {
            pendingValidationPlayers.remove(playerUuid);
        }
    }

    public void sendCreateSessionAuth(Player player, String email, String password) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("CreateSession");
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(email);
            out.writeUTF(password);

            // Verificar que el canal está registrado
            if (!this.getServer().getMessenger().isOutgoingChannelRegistered(this, "prax:core")) {
                getLogger().severe("[ERROR] Canal 'prax:core' no está registrado!");
                return;
            }

            player.sendPluginMessage(this, "prax:core", out.toByteArray());
            getLogger().info("[DEBUG] Mensaje CreateSession enviado al proxy para " + player.getName());

        } catch (Exception e) {
            getLogger().severe("[ERROR] Error enviando CreateSession: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void sendRegisterRequest(Player player, String email, String password, String birthdate) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("RegisterRequest");
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(email);
            out.writeUTF(password);
            out.writeUTF(birthdate);
            out.writeUTF(player.getName());


            if (!this.getServer().getMessenger().isOutgoingChannelRegistered(this, "prax:core")) {
                getLogger().severe("[ERROR] Canal 'prax:core' no está registrado!");
                return;
            }

            player.sendPluginMessage(this, "prax:core", out.toByteArray());
            player.sendMessage("§7Solicitud enviada, esperando confirmación del backend...");
            getLogger().info("[DEBUG] RegisterRequest enviado para " + player.getName());

        } catch (Exception e) {
            getLogger().severe("[ERROR] Error enviando RegisterRequest: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void sendLogoutRequest(Player player) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("LogoutSession");
            out.writeUTF(player.getUniqueId().toString());

            if (!this.getServer().getMessenger().isOutgoingChannelRegistered(this, "prax:core")) {
                getLogger().severe("[ERROR] Canal 'prax:core' no está registrado!");
                return;
            }

            player.sendPluginMessage(this, "prax:core", out.toByteArray());
            getLogger().info("[DEBUG] LogoutSession enviado al proxy para " + player.getName());
        } catch (Exception e) {
            getLogger().severe("[ERROR] Error enviando LogoutSession: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendValidateTokenMessage(Player player) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("ValidateToken");
            out.writeUTF(player.getUniqueId().toString());

            if (!this.getServer().getMessenger().isOutgoingChannelRegistered(this, "prax:core")) {
                getLogger().severe("[ERROR] Canal 'prax:core' no está registrado!");
                return;
            }

            player.sendPluginMessage(this, "prax:core", out.toByteArray());
            getLogger().info("[DEBUG] Mensaje ValidateToken enviado al proxy para " + player.getName());

        } catch (Exception e) {
            getLogger().severe("[ERROR] Error enviando ValidateToken: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- MÉTODOS DE GESTIÓN DE DATOS Y TIEMPO (Sin cambios) ---
    public DataManager getDataManager() { return this.dataManager; }
    public void setLoginTime(UUID playerUuid) { playerLoginTimes.put(playerUuid, System.currentTimeMillis()); }
    public Long getLoginTime(UUID playerUuid) { return playerLoginTimes.get(playerUuid); }
    public void removeLoginTime(UUID playerUuid) { playerLoginTimes.remove(playerUuid); }
}