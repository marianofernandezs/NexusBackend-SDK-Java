package com.prax.core.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Plugin(id = "praxcore-velocity", name = "PraxCore Velocity", version = "1.1.0")
public class PraxProxyPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final TokenManager tokenManager;
    private final BackendClient backendClient;
    private final Map<UUID, ScheduledTask> pendingDisconnections = new ConcurrentHashMap<>();

    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("prax:core");

    @Inject
    public PraxProxyPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.tokenManager = new TokenManager();
        this.backendClient = new BackendClient();

        logger.info("==========================================");
        logger.info("[PraxProxy] Plugin inicializado correctamente.");
        logger.info("==========================================");
    }

    // ============================================================
    // EVENTOS PRINCIPALES
    // ============================================================

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(CHANNEL);
        logger.info("[PraxProxy] Canal 'prax:core' registrado con éxito.");
        logger.info("[PraxProxy] Sistema de autenticación PraxSuite listo.");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())) return;
        if (!(event.getSource() instanceof ServerConnection)) return;

        ServerConnection connection = (ServerConnection) event.getSource();
        Player player = connection.getPlayer();
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());

        String subChannel = in.readUTF();
        logger.info("[PLUGIN_MSG] Subcanal recibido: " + subChannel + " de " + player.getUsername());

        switch (subChannel) {
            case "CreateSession":
                handleCreateSession(connection, in);
                break;
            case "ValidateToken":
                handleValidateToken(connection, in);
                break;
            case "LogoutSession":
                handleLogoutSession(connection, in);
                break;
            case "RefreshSession":
                handleRefreshSession(connection, in);
                break;
            case "RegisterRequest":
                handleRegisterRequest(connection, in);
                break;
            default:
                logger.warn("[PLUGIN_MSG] Subcanal desconocido: " + subChannel);
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    @Subscribe
    public void onServerSwitch(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        logger.info("[SWITCH] " + player.getUsername() + " cambió de servidor");

        ScheduledTask pendingTask = pendingDisconnections.remove(playerUuid);
        if (pendingTask != null) {
            pendingTask.cancel();
            logger.info("[SWITCH] Tarea de limpieza cancelada para " + playerUuid);
        }
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        logger.info("[DISCONNECT] " + player.getUsername() + " desconectado. Eliminación de sesión en 30s...");

        ScheduledTask task = server.getScheduler()
                .buildTask(this, () -> {
                    if (server.getPlayer(playerUuid).isEmpty()) {
                        tokenManager.removeSession(playerUuid);
                        logger.info("[CLEANUP] Sesión eliminada para " + player.getUsername());
                    }
                })
                .delay(30, TimeUnit.SECONDS)
                .schedule();

        pendingDisconnections.put(playerUuid, task);
    }

    // ============================================================
    // MANEJO DE SUBCANALES
    // ============================================================

    private void handleCreateSession(ServerConnection connection, ByteArrayDataInput in) {
        try {
            UUID uuid = UUID.fromString(in.readUTF());
            String email = in.readUTF();
            String password = in.readUTF();

            logger.info("[SESSION] Creando sesión para " + email + " (" + uuid + ")");

            Optional<String> tokenOpt = backendClient.login(uuid, email, password);

            if (tokenOpt.isPresent()) {
                String token = tokenOpt.get();
                tokenManager.storeToken(uuid, token);
                sendValidationResponse(connection, uuid, true);
                logger.info("[SESSION] Login exitoso. Token almacenado.");
            } else {
                sendValidationResponse(connection, uuid, false);
                logger.warn("[SESSION] Error en login. Backend devolvió null o fallo autenticación.");
            }

        } catch (Exception e) {
            logger.error("[SESSION] Error procesando CreateSession: " + e.getMessage());
        }
    }

    private void handleValidateToken(ServerConnection connection, ByteArrayDataInput in) {
        try {
            UUID playerUuid = UUID.fromString(in.readUTF());
            String token = tokenManager.getToken(playerUuid);

            if (token == null) {
                logger.warn("[VALIDATE] No se encontró token activo para " + playerUuid);
                sendValidationResponse(connection, playerUuid, false);
                return;
            }

            boolean isValid = backendClient.validate(token);
            sendValidationResponse(connection, playerUuid, isValid);
            logger.info("[VALIDATE] Token validado: " + (isValid ? "VÁLIDO" : "INVÁLIDO"));

        } catch (Exception e) {
            logger.error("[VALIDATE] Error validando token: " + e.getMessage());
        }
    }

    private void handleLogoutSession(ServerConnection connection, ByteArrayDataInput in) {
        try {
            UUID playerUuid = UUID.fromString(in.readUTF());
            String token = tokenManager.getToken(playerUuid);

            if (token == null) {
                logger.warn("[LOGOUT] No se encontró token activo para " + playerUuid);
                sendLogoutResponse(connection, playerUuid, false);
                return;
            }

            boolean success = backendClient.logout(token);
            if (success) tokenManager.removeSession(playerUuid);

            sendLogoutResponse(connection, playerUuid, success);
            logger.info("[LOGOUT] Sesión cerrada para " + playerUuid + ": " + success);

        } catch (Exception e) {
            logger.error("[LOGOUT] Error al cerrar sesión: " + e.getMessage());
        }
    }

    private void handleRefreshSession(ServerConnection connection, ByteArrayDataInput in) {
        try {
            UUID playerUuid = UUID.fromString(in.readUTF());
            String oldToken = tokenManager.getToken(playerUuid);

            if (oldToken == null) {
                logger.warn("[REFRESH] No existe token previo para " + playerUuid);
                sendValidationResponse(connection, playerUuid, false);
                return;
            }

            Optional<String> newTokenOpt = backendClient.refresh(oldToken);
            if (newTokenOpt.isPresent()) {
                String newToken = newTokenOpt.get();
                tokenManager.storeToken(playerUuid, newToken);
                sendValidationResponse(connection, playerUuid, true);
                logger.info("[REFRESH] Token actualizado correctamente.");
            } else {
                sendValidationResponse(connection, playerUuid, false);
                logger.warn("[REFRESH] Error al refrescar token para " + playerUuid);
            }

        } catch (Exception e) {
            logger.error("[REFRESH] Error en RefreshSession: " + e.getMessage());
        }
    }
    private void handleRegisterRequest(ServerConnection connection, ByteArrayDataInput in) {
        try {
            UUID uuid = UUID.fromString(in.readUTF());
            String email = in.readUTF();
            String password = in.readUTF();
            String birthdate = in.readUTF();
            String playerName = in.readUTF();

            logger.info("[REGISTER] Nueva solicitud de registro desde Paper: " + email);

            boolean success = backendClient.register(uuid, email, password, birthdate, playerName);

            // Enviar respuesta de vuelta
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("RegisterResponse");
            out.writeUTF(uuid.toString());
            out.writeBoolean(success);
            connection.sendPluginMessage(CHANNEL, out.toByteArray());

            logger.info("[REGISTER] Respuesta enviada a Paper: " + (success ? "OK" : "FAIL"));
        } catch (Exception e) {
            logger.error("[REGISTER] Error procesando RegisterRequest: " + e.getMessage());
        }
    }


    // ============================================================
    // UTILIDADES DE RESPUESTA
    // ============================================================

    private void sendValidationResponse(ServerConnection connection, UUID playerUuid, boolean isValid) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidationResponse");
        out.writeUTF(playerUuid.toString());
        out.writeBoolean(isValid);
        connection.sendPluginMessage(CHANNEL, out.toByteArray());
    }

    private void sendLogoutResponse(ServerConnection connection, UUID playerUuid, boolean success) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("LogoutResponse");
        out.writeUTF(playerUuid.toString());
        out.writeBoolean(success);
        connection.sendPluginMessage(CHANNEL, out.toByteArray());
    }
}
