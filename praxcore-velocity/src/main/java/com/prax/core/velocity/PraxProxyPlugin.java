package com.prax.core.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "praxcore-velocity",
        name = "PraxCore Velocity",
        version = "1.0.0",
        authors = {"PraxDev"}
)
public class PraxProxyPlugin {

    private static final MinecraftChannelIdentifier PRAX_CHANNEL =
            MinecraftChannelIdentifier.from("prax:core");

    private static final MinecraftChannelIdentifier NEXUS_CHANNEL =
            MinecraftChannelIdentifier.from("nexus:sync");

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    // 🆕 NUEVO: Usar NexusAuthService en lugar de BackendClient
    private NexusAuthService authService;
    private TokenManager tokenManager;

    @Inject
    public PraxProxyPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Inicializando PraxCore Velocity con SDK...");

        // 🆕 NUEVO: Inicializar NexusAuthService
        String backendUrl = System.getenv().getOrDefault("BACKEND_URL", "http://localhost:3000/api/");

        int backendTimeout = Integer.parseInt(
                System.getenv().getOrDefault("BACKEND_TIMEOUT", "10000")
        );

        logger.info("🔧 Configuración del Backend:");
        logger.info("   URL: {}", backendUrl);
        logger.info("   Timeout: {}ms", backendTimeout);


        this.authService = new NexusAuthService(backendUrl, backendTimeout);
        logger.info("NexusAuthService inicializado con URL: {}", backendUrl);

        // Inicializar TokenManager (sin cambios)
        this.tokenManager = new TokenManager();
        logger.info("TokenManager inicializado");

        // Registrar canales de Plugin Messages
        server.getChannelRegistrar().register(PRAX_CHANNEL);
        server.getChannelRegistrar().register(NEXUS_CHANNEL);
        logger.info("Canales de comunicación registrados");

        // Registrar listener de mensajes
        server.getEventManager().register(this, new PluginMessageHandler(this));

        // Tarea de limpieza de tokens expirados
        server.getScheduler()
                .buildTask(this, tokenManager::cleanupExpiredSessions)
                .repeat(5, TimeUnit.MINUTES)
                .schedule();

        logger.info("PraxCore Velocity iniciado correctamente");
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        logger.info("Jugador conectándose: {} ({})", player.getUsername(), uuid);
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Validar si existe sesión activa
        if (tokenManager.sessionExists(uuid)) {
            logger.info("Sesión existente encontrada para {}", player.getUsername());
            return;
        }
        logger.info("No hay sesión existente para {}", player.getUsername());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String serverName = event.getServer().getServerInfo().getName();

        logger.info("{} conectado a servidor: {}", player.getUsername(), serverName);
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        logger.info("Jugador desconectado: {} ({})", player.getUsername(), uuid);

        // Mantener la sesión aunque se desconecte (no limpiar token)
        // El token expirará solo o el usuario hará logout explícito
    }

    // ==============================
    // GETTERS
    // ==============================

    public NexusAuthService getAuthService() {
        return authService;
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public static MinecraftChannelIdentifier getPraxChannel() {
        return PRAX_CHANNEL;
    }

    public static MinecraftChannelIdentifier getNexusChannel() {
        return NEXUS_CHANNEL;
    }
}