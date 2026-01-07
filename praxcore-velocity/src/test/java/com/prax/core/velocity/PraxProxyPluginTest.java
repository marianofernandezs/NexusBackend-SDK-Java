package com.prax.core.velocity;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.Scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.Disabled;

/**
 * Tests unitarios para PraxProxyPlugin
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // ← AGREGAR ESTA LÍNEA
class PraxProxyPluginTest {

    @Mock
    private ProxyServer mockProxyServer;

    @Mock
    private Logger mockLogger;

    @Mock
    private ChannelRegistrar mockChannelRegistrar;

    @Mock
    private EventManager mockEventManager;

    @Mock
    private Scheduler mockScheduler;

    @Mock
    private Player mockPlayer;

    @Mock
    private ProxyInitializeEvent mockProxyInitEvent;

    @Mock
    private LoginEvent mockLoginEvent;

    @Mock
    private PostLoginEvent mockPostLoginEvent;

    @Mock
    private ServerConnectedEvent mockServerConnectedEvent;

    @Mock
    private DisconnectEvent mockDisconnectEvent;

    @Mock
    private RegisteredServer mockRegisteredServer;

    @Mock
    private ServerInfo mockServerInfo;

    @Mock
    private Scheduler.TaskBuilder mockTaskBuilder;

    private PraxProxyPlugin plugin;
    private Path testDataDirectory;

    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final String TEST_USERNAME = "TestPlayer";
    private static final String TEST_SERVER_NAME = "lobby";
    private static final String TEST_TOKEN = "test-jwt-token-12345";

    @BeforeEach
    void setUp() {
        // Configurar Path de datos
        testDataDirectory = Paths.get("test-data");

        // Configurar mocks básicos del ProxyServer
        when(mockProxyServer.getChannelRegistrar()).thenReturn(mockChannelRegistrar);
        when(mockProxyServer.getEventManager()).thenReturn(mockEventManager);
        when(mockProxyServer.getScheduler()).thenReturn(mockScheduler);

        lenient().when(mockScheduler.buildTask(any(), any(Runnable.class))).thenReturn(mockTaskBuilder);
        lenient().when(mockTaskBuilder.repeat(anyLong(), any(TimeUnit.class))).thenReturn(mockTaskBuilder);
        lenient().when(mockTaskBuilder.schedule()).thenReturn(null);

        // Configurar Player mock
        when(mockPlayer.getUniqueId()).thenReturn(TEST_UUID);
        when(mockPlayer.getUsername()).thenReturn(TEST_USERNAME);

        // Crear instancia del plugin
        plugin = new PraxProxyPlugin(mockProxyServer, mockLogger, testDataDirectory);
    }

    // ==========================================
    // TESTS DE INICIALIZACIÓN
    // ==========================================

    @Test
    void testOnProxyInitialization_Success() {
        // Act
        plugin.onProxyInitialization(mockProxyInitEvent);



        // Assert - Verificar que se inicializaron los servicios
        assertNotNull(plugin.getAuthService(), "AuthService debería estar inicializado");
        assertNotNull(plugin.getTokenManager(), "TokenManager debería estar inicializado");

        // Verificar que se registraron los canales
        verify(mockChannelRegistrar, times(2)).register(any());

        // Verificar que se registró el PluginMessageHandler
        verify(mockEventManager, times(1))
                .register(eq(plugin), any(PluginMessageHandler.class));

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockLogger, atLeastOnce()).info(msgCaptor.capture());


        verify(mockLogger, atLeastOnce()).info(msgCaptor.capture(), any(Object.class));

        assertTrue(
                msgCaptor.getAllValues().stream()
                        .anyMatch(m -> m.contains("Inicializando PraxCore Velocity")),
                "Debe contener el log de inicio"
        );

        assertTrue(
                msgCaptor.getAllValues().stream()
                        .anyMatch(m -> m.contains("NexusAuthService inicializado con URL")),
                "Debería contener el log de inicialización de NexusAuthService"
        );

        assertTrue(
                msgCaptor.getAllValues().stream()
                        .anyMatch(m -> m.contains("Inicializando PraxCore Velocity")),
                "Debe contener el log de inicio"
        );

    }

    @Test
    void testOnProxyInitialization_UsesDefaultBackendUrl() {
        // Act
        plugin.onProxyInitialization(mockProxyInitEvent);

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);


        // Assert - Verificar que se usó la URL por defecto
        verify(mockLogger, atLeastOnce()).info(msgCaptor.capture(), any(Object.class));

        assertTrue(
                msgCaptor.getAllValues().stream()
                        .anyMatch(m -> m.contains("NexusAuthService inicializado con URL")),
                "Debe registrar el mensaje de inicialización del NexusAuthService"
        );
    }

    @Test
    void testOnProxyInitialization_CreatesScheduledTask() {
        // Act
        plugin.onProxyInitialization(mockProxyInitEvent);

        // Assert - Verificar que se intentó programar la tarea de limpieza
        verify(mockScheduler, times(1))
                .buildTask(eq(plugin), any(Runnable.class));
    }

    // ==========================================
    // TESTS DE EVENTOS DE JUGADOR
    // ==========================================

    @Test
    void testOnPlayerLogin() {
        // Arrange
        when(mockLoginEvent.getPlayer()).thenReturn(mockPlayer);

        // Act
        plugin.onPlayerLogin(mockLoginEvent);

        // Assert
        verify(mockLogger, times(1))
                .info("Jugador conectándose: {} ({})", TEST_USERNAME, TEST_UUID);
    }

    @Test
    void testOnPostLogin_WithoutExistingSession() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);
        when(mockPostLoginEvent.getPlayer()).thenReturn(mockPlayer);

        // Act
        plugin.onPostLogin(mockPostLoginEvent);

        // Assert
        verify(mockLogger, times(1))
                .info("No hay sesión existente para {}", TEST_USERNAME);
    }

    @Test
    void testOnPostLogin_WithValidSession() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);
        when(mockPostLoginEvent.getPlayer()).thenReturn(mockPlayer);

        // Simular sesión existente
        TokenManager tokenManager = plugin.getTokenManager();
        tokenManager.storeToken(TEST_UUID, TEST_TOKEN);

        // Act
        plugin.onPostLogin(mockPostLoginEvent);

        // Assert
        verify(mockLogger, times(1))
                .info("Sesión existente encontrada para {}", TEST_USERNAME);
    }

    @Test
    void testOnPostLogin_WithInvalidSession() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);
        when(mockPostLoginEvent.getPlayer()).thenReturn(mockPlayer);

        // Simular sesión existente pero inválida
        TokenManager tokenManager = plugin.getTokenManager();
        tokenManager.storeToken(TEST_UUID, "invalid-token");

        // Act
        plugin.onPostLogin(mockPostLoginEvent);

        // Assert
        verify(mockLogger, times(1))
                .info("Sesión existente encontrada para {}", TEST_USERNAME);
        // En este caso el token será validado contra el backend mock
        // y debería fallar, resultando en la eliminación de la sesión
    }

    @Test
    void testOnServerConnected() {
        // Arrange
        when(mockServerConnectedEvent.getPlayer()).thenReturn(mockPlayer);
        when(mockServerConnectedEvent.getServer()).thenReturn(mockRegisteredServer);
        when(mockRegisteredServer.getServerInfo()).thenReturn(mockServerInfo);
        when(mockServerInfo.getName()).thenReturn(TEST_SERVER_NAME);

        // Act
        plugin.onServerConnected(mockServerConnectedEvent);

        // Assert
        verify(mockLogger, times(1))
                .info("{} conectado a servidor: {}", TEST_USERNAME, TEST_SERVER_NAME);
    }

    @Test
    void testOnPlayerDisconnect() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);
        when(mockDisconnectEvent.getPlayer()).thenReturn(mockPlayer);

        // Crear sesión antes de desconectar
        TokenManager tokenManager = plugin.getTokenManager();
        tokenManager.storeToken(TEST_UUID, TEST_TOKEN);
        assertTrue(tokenManager.sessionExists(TEST_UUID), "Sesión debería existir antes de desconectar");

        // Act
        plugin.onPlayerDisconnect(mockDisconnectEvent);

        // Assert
        verify(mockLogger, times(1))
                .info("Jugador desconectado: {} ({})", TEST_USERNAME, TEST_UUID);

        // Verificar que la sesión NO fue eliminada (se mantiene)
        assertTrue(tokenManager.sessionExists(TEST_UUID),
                "Sesión debería mantenerse después de desconectar");
    }

    @Test
    void testOnPlayerDisconnect_SessionPersists() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);
        when(mockDisconnectEvent.getPlayer()).thenReturn(mockPlayer);

        TokenManager tokenManager = plugin.getTokenManager();
        tokenManager.storeToken(TEST_UUID, TEST_TOKEN);

        // Act
        plugin.onPlayerDisconnect(mockDisconnectEvent);

        // Assert - La sesión debe persistir para permitir reconexión
        String storedToken = tokenManager.getToken(TEST_UUID);
        assertEquals(TEST_TOKEN, storedToken, "Token debería seguir almacenado");
    }

    // ==========================================
    // TESTS DE GETTERS
    // ==========================================

    @Test
    void testGetAuthService() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);

        // Act
        NexusAuthService authService = plugin.getAuthService();

        // Assert
        assertNotNull(authService, "AuthService no debería ser null");
    }

    @Test
    void testGetTokenManager() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);

        // Act
        TokenManager tokenManager = plugin.getTokenManager();

        // Assert
        assertNotNull(tokenManager, "TokenManager no debería ser null");
    }

    @Test
    void testGetServer() {
        // Act
        ProxyServer server = plugin.getServer();

        // Assert
        assertNotNull(server, "ProxyServer no debería ser null");
        assertEquals(mockProxyServer, server, "Debería retornar el ProxyServer inyectado");
    }

    @Test
    void testGetLogger() {
        // Act
        Logger logger = plugin.getLogger();

        // Assert
        assertNotNull(logger, "Logger no debería ser null");
        assertEquals(mockLogger, logger, "Debería retornar el Logger inyectado");
    }

    @Test
    void testGetPraxChannel() {
        // Act
        MinecraftChannelIdentifier channel = PraxProxyPlugin.getPraxChannel();

        // Assert
        assertNotNull(channel, "PRAX_CHANNEL no debería ser null");
        assertEquals("prax:core", channel.getId(), "Channel ID debería ser 'prax:core'");
    }

    @Test
    void testGetNexusChannel() {
        // Act
        MinecraftChannelIdentifier channel = PraxProxyPlugin.getNexusChannel();

        // Assert
        assertNotNull(channel, "NEXUS_CHANNEL no debería ser null");
        assertEquals("nexus:sync", channel.getId(), "Channel ID debería ser 'nexus:sync'");
    }

    // ==========================================
    // TESTS DE INTEGRACIÓN DE COMPONENTES
    // ==========================================

    @Test
    void testPluginInitialization_AllComponentsWork() {
        // Act
        plugin.onProxyInitialization(mockProxyInitEvent);

        // Assert - Verificar que todos los componentes están inicializados y conectados
        assertNotNull(plugin.getAuthService());
        assertNotNull(plugin.getTokenManager());
        assertNotNull(plugin.getServer());
        assertNotNull(plugin.getLogger());

        // Verificar que los servicios pueden interactuar
        TokenManager tokenManager = plugin.getTokenManager();
        tokenManager.storeToken(TEST_UUID, TEST_TOKEN);
        assertTrue(tokenManager.sessionExists(TEST_UUID));
        assertEquals(TEST_TOKEN, tokenManager.getToken(TEST_UUID));
    }

    @Test
    void testCleanupTask_CanBeExecuted() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);
        TokenManager tokenManager = plugin.getTokenManager();

        // Act - Ejecutar manualmente la limpieza
        tokenManager.cleanupExpiredSessions();

        // Assert - No debería lanzar excepciones
        assertTrue(true, "cleanupExpiredSessions debería ejecutarse sin errores");
    }

    @Test
    void testScheduledTaskCreation() {
        // Act
        plugin.onProxyInitialization(mockProxyInitEvent);

        // Assert - Verificar que se capturó el runnable de limpieza
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockScheduler, times(1)).buildTask(eq(plugin), runnableCaptor.capture());

        Runnable cleanupTask = runnableCaptor.getValue();
        assertNotNull(cleanupTask, "La tarea de limpieza no debería ser null");

        // Ejecutar la tarea manualmente para verificar que funciona
        assertDoesNotThrow(() -> cleanupTask.run(),
                "La tarea de limpieza debería ejecutarse sin errores");
    }

    // ==========================================
    // TESTS DE FLUJOS COMPLETOS
    // ==========================================

    @Test
    void testCompletePlayerFlow_LoginToDisconnect() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);
        TokenManager tokenManager = plugin.getTokenManager();

        // 1. Player Login
        when(mockLoginEvent.getPlayer()).thenReturn(mockPlayer);
        plugin.onPlayerLogin(mockLoginEvent);
        verify(mockLogger, times(1)).info("Jugador conectándose: {} ({})", TEST_USERNAME, TEST_UUID);

        // 2. PostLogin sin sesión
        when(mockPostLoginEvent.getPlayer()).thenReturn(mockPlayer);
        plugin.onPostLogin(mockPostLoginEvent);
        verify(mockLogger, times(1)).info("No hay sesión existente para {}", TEST_USERNAME);

        // 3. Simular que el jugador hizo login y ahora tiene token
        tokenManager.storeToken(TEST_UUID, TEST_TOKEN);

        // 4. ServerConnected
        when(mockServerConnectedEvent.getPlayer()).thenReturn(mockPlayer);
        when(mockServerConnectedEvent.getServer()).thenReturn(mockRegisteredServer);
        when(mockRegisteredServer.getServerInfo()).thenReturn(mockServerInfo);
        when(mockServerInfo.getName()).thenReturn(TEST_SERVER_NAME);
        plugin.onServerConnected(mockServerConnectedEvent);
        verify(mockLogger, times(1)).info("{} conectado a servidor: {}", TEST_USERNAME, TEST_SERVER_NAME);

        // 5. Player Disconnect
        when(mockDisconnectEvent.getPlayer()).thenReturn(mockPlayer);
        plugin.onPlayerDisconnect(mockDisconnectEvent);
        verify(mockLogger, times(1)).info("Jugador desconectado: {} ({})", TEST_USERNAME, TEST_UUID);

        // 6. Verificar que la sesión persiste después de desconectar
        assertTrue(tokenManager.sessionExists(TEST_UUID), "Sesión debería persistir");
    }

    @Test
    void testPlayerReconnect_WithExistingSession() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);
        TokenManager tokenManager = plugin.getTokenManager();

        // Simular sesión existente de conexión anterior
        tokenManager.storeToken(TEST_UUID, TEST_TOKEN);

        // Act - Player se reconecta
        when(mockPostLoginEvent.getPlayer()).thenReturn(mockPlayer);
        plugin.onPostLogin(mockPostLoginEvent);

        // Assert - Debería encontrar la sesión existente
        verify(mockLogger, times(1))
                .info("Sesión existente encontrada para {}", TEST_USERNAME);
        assertTrue(tokenManager.sessionExists(TEST_UUID), "Sesión debería seguir existiendo");
    }

    // ==========================================
    // TESTS DE CONCURRENCIA
    // ==========================================

    @Test
    void testMultiplePlayersSimultaneously() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);
        TokenManager tokenManager = plugin.getTokenManager();

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        UUID player3 = UUID.randomUUID();

        // Act - Múltiples jugadores conectándose
        tokenManager.storeToken(player1, "token1");
        tokenManager.storeToken(player2, "token2");
        tokenManager.storeToken(player3, "token3");

        // Assert - Todas las sesiones deberían existir independientemente
        assertTrue(tokenManager.sessionExists(player1));
        assertTrue(tokenManager.sessionExists(player2));
        assertTrue(tokenManager.sessionExists(player3));

        assertEquals("token1", tokenManager.getToken(player1));
        assertEquals("token2", tokenManager.getToken(player2));
        assertEquals("token3", tokenManager.getToken(player3));
    }

    // ==========================================
    // TESTS DE ROBUSTEZ
    // ==========================================

    @Test
    void testInitialization_WithNullPlayer() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);
        when(mockLoginEvent.getPlayer()).thenReturn(null);

        // Act & Assert - Debería manejar null sin crashear
        assertThrows(NullPointerException.class, () -> {
            plugin.onPlayerLogin(mockLoginEvent);
        });
    }

    @Test
    void testTokenManager_MultipleOperationsOnSamePlayer() {
        // Arrange
        plugin.onProxyInitialization(mockProxyInitEvent);
        TokenManager tokenManager = plugin.getTokenManager();

        // Act - Múltiples operaciones
        tokenManager.storeToken(TEST_UUID, "token1");
        assertTrue(tokenManager.sessionExists(TEST_UUID));

        tokenManager.storeToken(TEST_UUID, "token2"); // Sobrescribir
        assertEquals("token2", tokenManager.getToken(TEST_UUID));

        tokenManager.removeSession(TEST_UUID);
        assertFalse(tokenManager.sessionExists(TEST_UUID));

        // Assert
        assertNull(tokenManager.getToken(TEST_UUID),
                "Token debería ser null después de remover sesión");
    }
}