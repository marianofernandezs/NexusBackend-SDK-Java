// Ubicación: praxcore-paper/src/main/java/com/prax/core/PluginMessageListener.java
package com.prax.core;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PluginMessageListener implements org.bukkit.plugin.messaging.PluginMessageListener {

    private final PraxCorePlugin plugin;
    public static final Map<String, Integer> serverPlayerCounts = new HashMap<>();

    public PluginMessageListener(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if ("BungeeCord".equalsIgnoreCase(channel)) {
            handleBungeeCordMessage(message);
            return;
        }
        if ("prax:core".equalsIgnoreCase(channel)) {
            handlePraxCoreMessage(player, message);
        }
        if (channel.equals("nexus:sync")) {

            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String type = in.readUTF();

            switch (type) {

                case "PLAYER_JOIN" -> {
                    String uuid = in.readUTF();
                    String name = in.readUTF();
                    String ip = in.readUTF();

                    Bukkit.getLogger().info("[NEXUS] JOIN → " + name + " (" + uuid + ") IP=" + ip);
                }

                case "PLAYER_SWITCH" -> {
                    String uuid = in.readUTF();
                    String from = in.readUTF();
                    String to = in.readUTF();

                    Bukkit.getLogger().info("[NEXUS] SWITCH → " + uuid + " " + from + " → " + to);
                }

                case "PLAYER_QUIT" -> {
                    String uuid = in.readUTF();
                    Bukkit.getLogger().info("[NEXUS] QUIT → " + uuid);
                }

                case "PLAYER_LIST" -> {
                    Bukkit.getLogger().info("[NEXUS] Lista de jugadores recibida:");

                    int serverCount = in.readInt();
                    Bukkit.getLogger().info("[NEXUS] Número de servidores: " + serverCount);

                    for (int s = 0; s < serverCount; s++) {
                        String serverName = in.readUTF();
                        int count = in.readInt();

                        Bukkit.getLogger().info(" - " + serverName + ": " + count + " jugadores");

                        for (int i = 0; i < count; i++) {
                            String uid = in.readUTF();
                            Bukkit.getLogger().info("     * " + uid);
                        }
                    }
                    Bukkit.getLogger().info("[NEXUS] Fin de la lista de jugadores");
                }
            }

            return; // Muy importante: evitar mezclar canales
        }
    }

    private void handleBungeeCordMessage(byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        if (subChannel.equals("PlayerCount")) {
            String serverName = in.readUTF();
            int playerCount = in.readInt();
            serverPlayerCounts.put(serverName, playerCount);
        }
    }

    private void handlePraxCoreMessage(Player player, byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        switch (subChannel) {
            case "ValidationResponse": {
                UUID playerUuid = UUID.fromString(in.readUTF());
                boolean isValid = in.readBoolean();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player targetPlayer = Bukkit.getPlayer(playerUuid);
                    if (targetPlayer == null || !targetPlayer.isOnline()) {
                        plugin.getLogger().warning("Respuesta de validación para jugador offline: " + playerUuid);
                        return;
                    }

                    plugin.getLogger().info("[DEBUG] [" + plugin.getServerType() + "]: ValidationResponse -> " + (isValid ? "VALIDA" : "INVALIDA"));
                    plugin.setPendingValidation(playerUuid, false);

                    if (isValid) {
                        plugin.setAuthenticated(playerUuid, true);
                        plugin.setLoginTime(playerUuid);
                        targetPlayer.sendMessage("§a¡Sesión iniciada correctamente! Bienvenido de nuevo.");
                    } else {
                        plugin.setAuthenticated(playerUuid, false);
                        if (plugin.isLobbyServer()) {
                            if (plugin.getDataManager().isPlayerRegistered(playerUuid)) {
                                targetPlayer.sendMessage("§ePor favor, inicia sesión con /login <email> <contraseña>");
                            } else {
                                targetPlayer.sendMessage("§e¡Bienvenido! Usa /register para crear una cuenta.");
                            }
                        } else {
                            targetPlayer.kickPlayer("§cTu sesión no es válida. Por favor, vuelve a conectarte.");
                        }
                    }
                });
                break;
            }

            case "RegisterResponse": {
                UUID playerUuid = UUID.fromString(in.readUTF());
                boolean success = in.readBoolean();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player targetPlayer = Bukkit.getPlayer(playerUuid);
                    if (targetPlayer == null) return;

                    if (success) {
                        targetPlayer.sendMessage("§a¡Tu cuenta se ha registrado correctamente en PraxSuite!");
                        targetPlayer.sendMessage("§7Usa /login <email> <contraseña> para iniciar sesión.");
                    } else {
                        targetPlayer.sendMessage("§cNo se pudo completar el registro. Intenta más tarde o contacta a soporte.");
                    }
                });
                break;
            }

            case "LogoutResponse": {
                UUID playerUuid = UUID.fromString(in.readUTF());
                boolean success = in.readBoolean();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player targetPlayer = Bukkit.getPlayer(playerUuid);
                    if (targetPlayer == null) return;

                    if (success) {
                        plugin.setAuthenticated(playerUuid, false);
                        plugin.removeLoginTime(playerUuid);
                        targetPlayer.sendMessage("§eHas cerrado sesión correctamente.");
                    } else {
                        targetPlayer.sendMessage("§cNo se pudo cerrar tu sesión. Intenta nuevamente.");
                    }
                });
                break;
            }

            default: {
                plugin.getLogger().warning("[PraxCore] Subcanal desconocido recibido: " + subChannel);
                break;
            }
        }
    }
}
