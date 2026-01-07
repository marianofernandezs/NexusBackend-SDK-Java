package com.prax.core.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Manejador de mensajes de plugin entre Paper y Velocity
 * Actualizado para usar NexusAuthService (SDK) en lugar de BackendClient
 */
public class PluginMessageHandler {

    private final PraxProxyPlugin plugin;
    private final Logger logger;
    private final NexusAuthService authService;
    private final TokenManager tokenManager;

    public PluginMessageHandler(PraxProxyPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.authService = plugin.getAuthService();
        this.tokenManager = plugin.getTokenManager();
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // Solo procesar mensajes del canal prax:core
        if (!event.getIdentifier().equals(PraxProxyPlugin.getPraxChannel())) {
            return;
        }

        // Solo procesar mensajes de servidores backend
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        ServerConnection serverConnection = (ServerConnection) event.getSource();
        Player player = serverConnection.getPlayer();

        // Leer el mensaje
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subChannel = in.readUTF();

        logger.info("[PluginMessage] Recibido: {} de {}", subChannel, player.getUsername());

        // Enrutar según el subcanal
        switch (subChannel) {
            case "CreateSession":
                handleCreateSession(player, in);
                break;

            case "ValidateToken":
                handleValidateToken(player, in);
                break;

            case "RegisterRequest":
                handleRegisterRequest(player, in);
                break;

            case "LogoutSession":
                handleLogoutSession(player, in);
                break;

            default:
                logger.warn("[PluginMessage] Subcanal desconocido: {}", subChannel);
                break;
        }
    }

    // ==============================
    // 🔹 CREATE SESSION (LOGIN)
    // ==============================
    private void handleCreateSession(Player player, ByteArrayDataInput in) {
        try {
            UUID playerUuid = UUID.fromString(in.readUTF());
            String email = in.readUTF();
            String password = in.readUTF();

            logger.info("[CreateSession] Procesando login para {}", player.getUsername());

            //Usa AuthService del SDK
            Optional<String> tokenOpt = authService.login(playerUuid, email, password);

            boolean success = tokenOpt.isPresent();

            if (success) {
                String token = tokenOpt.get();

                // Almacenar token en TokenManager
                tokenManager.storeToken(playerUuid, token);

                logger.info("[CreateSession] Login exitoso para {}", player.getUsername());
            } else {
                logger.warn("[CreateSession] Login fallido para {}", player.getUsername());
            }

            // Enviar respuesta a Paper
            sendValidationResponse(player, playerUuid, success);

        } catch (Exception e) {
            logger.error("[CreateSession] Error procesando login: {}", e.getMessage(), e);
            sendValidationResponse(player, player.getUniqueId(), false);
        }
    }

    // ==============================
    // 🔹 VALIDATE TOKEN
    // ==============================
    private void handleValidateToken(Player player, ByteArrayDataInput in) {
        try {
            UUID playerUuid = UUID.fromString(in.readUTF());

            logger.info("[ValidateToken] Validando sesión para {}", player.getUsername());

            boolean isValid = false;

            // Verificar si existe token en TokenManager
            if (tokenManager.sessionExists(playerUuid)) {
                String token = tokenManager.getToken(playerUuid);

                // 🆕 NUEVO: Validar con AuthService del SDK
                isValid = authService.validate(token);

                if (!isValid) {
                    logger.warn("[ValidateToken] Token inválido, limpiando sesión");
                    tokenManager.removeSession(playerUuid);
                }
            } else {
                logger.info("[ValidateToken] No existe sesión para {}", player.getUsername());
            }

            // Enviar respuesta a Paper
            sendValidationResponse(player, playerUuid, isValid);

        } catch (Exception e) {
            logger.error("[ValidateToken] Error validando token: {}", e.getMessage(), e);
            sendValidationResponse(player, player.getUniqueId(), false);
        }
    }

    // ==============================
    // 🔹 REGISTER REQUEST
    // ==============================
    private void handleRegisterRequest(Player player, ByteArrayDataInput in) {
        try {
            UUID playerUuid = UUID.fromString(in.readUTF());
            String email = in.readUTF();
            String password = in.readUTF();
            String birthdate = in.readUTF();
            String playerName = in.readUTF();

            logger.info("[RegisterRequest] Procesando registro para {}", playerName);

            // Usa AuthService del SDK para registro
            boolean success = authService.register(playerUuid, email, password, birthdate, playerName);

            if (success) {
                logger.info("[RegisterRequest] Registro exitoso para {}", playerName);
            } else {
                logger.warn("[RegisterRequest] Registro fallido para {}", playerName);
            }

            // Enviar respuesta a Paper
            sendRegisterResponse(player, playerUuid, success);

        } catch (Exception e) {
            logger.error("[RegisterRequest] Error procesando registro: {}", e.getMessage(), e);
            sendRegisterResponse(player, player.getUniqueId(), false);
        }
    }

    // ==============================
    // 🔹 LOGOUT SESSION
    // ==============================
    private void handleLogoutSession(Player player, ByteArrayDataInput in) {
        try {
            UUID playerUuid = UUID.fromString(in.readUTF());

            logger.info("[LogoutSession] Procesando logout para {}", player.getUsername());

            boolean success = false;

            // Verificar si existe sesión
            if (tokenManager.sessionExists(playerUuid)) {
                String token = tokenManager.getToken(playerUuid);

                //Hacer logout con AuthService del SDK
                success = authService.logout(token);

                if (success) {
                    // Limpiar sesión local
                    tokenManager.removeSession(playerUuid);
                    logger.info("[LogoutSession] Logout exitoso para {}", player.getUsername());
                } else {
                    logger.warn("[LogoutSession] Logout fallido en backend para {}", player.getUsername());
                }
            } else {
                logger.warn("[LogoutSession] No existe sesión para {}", player.getUsername());
                success = true; // No hay sesión que cerrar, considerarlo exitoso
            }

            // Enviar respuesta a Paper
            sendLogoutResponse(player, playerUuid, success);

        } catch (Exception e) {
            logger.error("[LogoutSession] Error procesando logout: {}", e.getMessage(), e);
            sendLogoutResponse(player, player.getUniqueId(), false);
        }
    }

    // ==============================
    // 🔹 MÉTODOS AUXILIARES - ENVIAR RESPUESTAS
    // ==============================

    private void sendValidationResponse(Player player, UUID playerUuid, boolean isValid) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("ValidationResponse");
            out.writeUTF(playerUuid.toString());
            out.writeBoolean(isValid);

            player.getCurrentServer().ifPresent(server -> {
                server.sendPluginMessage(PraxProxyPlugin.getPraxChannel(), out.toByteArray());
                logger.info("[Response] ValidationResponse enviada: {}", isValid);
            });
        } catch (Exception e) {
            logger.error("[Response] Error enviando ValidationResponse: {}", e.getMessage(), e);
        }
    }

    private void sendRegisterResponse(Player player, UUID playerUuid, boolean success) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("RegisterResponse");
            out.writeUTF(playerUuid.toString());
            out.writeBoolean(success);

            player.getCurrentServer().ifPresent(server -> {
                server.sendPluginMessage(PraxProxyPlugin.getPraxChannel(), out.toByteArray());
                logger.info("[Response] RegisterResponse enviada: {}", success);
            });
        } catch (Exception e) {
            logger.error("[Response] Error enviando RegisterResponse: {}", e.getMessage(), e);
        }
    }

    private void sendLogoutResponse(Player player, UUID playerUuid, boolean success) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("LogoutResponse");
            out.writeUTF(playerUuid.toString());
            out.writeBoolean(success);

            player.getCurrentServer().ifPresent(server -> {
                server.sendPluginMessage(PraxProxyPlugin.getPraxChannel(), out.toByteArray());
                logger.info("[Response] LogoutResponse enviada: {}", success);
            });
        } catch (Exception e) {
            logger.error("[Response] Error enviando LogoutResponse: {}", e.getMessage(), e);
        }
    }
}