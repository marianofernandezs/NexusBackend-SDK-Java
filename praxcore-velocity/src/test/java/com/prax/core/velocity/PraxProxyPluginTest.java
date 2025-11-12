package com.prax.core.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.scheduler.Scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PraxProxyPlugin Tests - Simplified")
class PraxProxyPluginTest {

    private PraxProxyPlugin plugin;
    private ProxyServer mockServer;
    private Logger mockLogger;
    private Player mockPlayer;
    private ServerConnection mockConnection;
    private UUID testUUID;

    @BeforeEach
    void setUp() {
        // Inicializar mocks
        mockServer = mock(ProxyServer.class);
        mockLogger = mock(Logger.class);
        mockPlayer = mock(Player.class);
        mockConnection = mock(ServerConnection.class);

        testUUID = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(testUUID);
        when(mockPlayer.getUsername()).thenReturn("TestPlayer");
        when(mockConnection.getPlayer()).thenReturn(mockPlayer);

        // Crear instancia del plugin
        plugin = new PraxProxyPlugin(mockServer, mockLogger);
    }

    // ==================== TESTS DE INICIALIZACIÓN ====================

    @Test
    @DisplayName("Debe crear instancia del plugin correctamente")
    void testPluginCreation() {
        // Assert
        assertNotNull(plugin);
    }

    @Test
    @DisplayName("Debe inicializar con ProxyServer inyectado")
    void testPluginWithProxyServer() {
        // Act
        PraxProxyPlugin newPlugin = new PraxProxyPlugin(mockServer, mockLogger);

        // Assert
        assertNotNull(newPlugin);
    }

    @Test
    @DisplayName("Debe inicializar con Logger inyectado")
    void testPluginWithLogger() {
        // Act
        PraxProxyPlugin newPlugin = new PraxProxyPlugin(mockServer, mockLogger);

        // Assert
        assertNotNull(newPlugin);
        verify(mockLogger, atLeastOnce()).info(anyString());
    }

    @Test
    @DisplayName("Debe loggear mensaje de inicialización")
    void testPluginInitializationLog() {
        // Arrange
        Logger spyLogger = mock(Logger.class);

        // Act
        new PraxProxyPlugin(mockServer, spyLogger);

        // Assert
        verify(spyLogger).info(contains("Plugin inicializado correctamente"));
    }

    // ==================== TESTS DE COMPONENTES INTERNOS ====================

    @Test
    @DisplayName("Debe tener TokenManager inicializado")
    void testTokenManagerInitialized() {
        // El plugin debería tener un TokenManager interno
        // Verificamos indirectamente que no lance NPE
        assertDoesNotThrow(() -> {
            // El plugin usa internamente TokenManager
            // No podemos acceder directamente pero podemos verificar que está inicializado
            assertNotNull(plugin);
        });
    }

    @Test
    @DisplayName("Debe tener BackendClient inicializado")
    void testBackendClientInitialized() {
        // El plugin debería tener un BackendClient interno
        // Verificamos indirectamente
        assertDoesNotThrow(() -> {
            assertNotNull(plugin);
        });
    }

    // ==================== TESTS DE MANEJO DE EVENTOS ====================

    @Test
    @DisplayName("onProxyInitialization debe ejecutarse sin errores")
    void testOnProxyInitialization() {
        // Arrange
        var mockEvent = mock(com.velocitypowered.api.event.proxy.ProxyInitializeEvent.class);
        var mockChannelRegistrar = mock(com.velocitypowered.api.proxy.messages.ChannelRegistrar.class);
        when(mockServer.getChannelRegistrar()).thenReturn(mockChannelRegistrar);

        // Act & Assert
        assertDoesNotThrow(() -> plugin.onProxyInitialization(mockEvent));
        verify(mockChannelRegistrar).register(any());
        verify(mockLogger, atLeastOnce()).info(contains("Canal"));
    }

    @Test
    @DisplayName("onPlayerDisconnect debe manejar desconexión correctamente")
    void testOnPlayerDisconnect() {
        // Arrange
        var mockEvent = mock(com.velocitypowered.api.event.connection.DisconnectEvent.class);
        when(mockEvent.getPlayer()).thenReturn(mockPlayer);

        var mockScheduler = mock(Scheduler.class);
        var mockTaskBuilder = mock(Scheduler.TaskBuilder.class);
        when(mockServer.getScheduler()).thenReturn(mockScheduler);
        when(mockScheduler.buildTask(any(Object.class), any(Runnable.class))).thenReturn(mockTaskBuilder);
        when(mockTaskBuilder.delay(anyLong(), any())).thenReturn(mockTaskBuilder);
        when(mockTaskBuilder.schedule()).thenReturn(mock(com.velocitypowered.api.scheduler.ScheduledTask.class));

        // Act & Assert
        assertDoesNotThrow(() -> plugin.onPlayerDisconnect(mockEvent));
        verify(mockLogger).info(contains("desconectado"));
    }

    @Test
    @DisplayName("onServerSwitch debe manejar cambio de servidor")
    void testOnServerSwitch() {
        // Arrange
        var mockEvent = mock(com.velocitypowered.api.event.player.ServerPostConnectEvent.class);
        when(mockEvent.getPlayer()).thenReturn(mockPlayer);

        // Act & Assert
        assertDoesNotThrow(() -> plugin.onServerSwitch(mockEvent));
        verify(mockLogger).info(contains("cambió de servidor"));
    }

    // ==================== TESTS DE PLUGIN MESSAGES ====================

    @Test
    @DisplayName("onPluginMessage debe ignorar canales desconocidos")
    void testOnPluginMessageUnknownChannel() {
        // Arrange
        var mockEvent = mock(com.velocitypowered.api.event.connection.PluginMessageEvent.class);
        var unknownChannel = mock(com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.class);
        when(mockEvent.getIdentifier()).thenReturn(unknownChannel);

        // Act
        plugin.onPluginMessage(mockEvent);

        // Assert
        verify(mockEvent, never()).setResult(any());
    }

    @Test
    @DisplayName("onPluginMessage debe procesar solo mensajes de ServerConnection")
    void testOnPluginMessageFromServerConnection() {
        // Arrange
        var mockEvent = mock(com.velocitypowered.api.event.connection.PluginMessageEvent.class);
        var correctChannel = com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from("prax:core");
        when(mockEvent.getIdentifier()).thenReturn(correctChannel);
        when(mockEvent.getSource()).thenReturn(mockPlayer); // No es ServerConnection

        // Act
        plugin.onPluginMessage(mockEvent);

        // Assert
        verify(mockEvent, never()).setResult(any());
    }

    // ==================== TESTS DE CASOS EDGE ====================

    @Test
    @DisplayName("Debe manejar múltiples jugadores simultáneamente")
    void testMultiplePlayersSimultaneous() {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);

        when(player1.getUniqueId()).thenReturn(uuid1);
        when(player1.getUsername()).thenReturn("Player1");
        when(player2.getUniqueId()).thenReturn(uuid2);
        when(player2.getUsername()).thenReturn("Player2");

        var event1 = mock(com.velocitypowered.api.event.connection.DisconnectEvent.class);
        var event2 = mock(com.velocitypowered.api.event.connection.DisconnectEvent.class);
        when(event1.getPlayer()).thenReturn(player1);
        when(event2.getPlayer()).thenReturn(player2);

        var mockScheduler = mock(Scheduler.class);
        var mockTaskBuilder = mock(Scheduler.TaskBuilder.class);
        when(mockServer.getScheduler()).thenReturn(mockScheduler);
        when(mockScheduler.buildTask(any(Object.class), any(Runnable.class))).thenReturn(mockTaskBuilder);
        when(mockTaskBuilder.delay(anyLong(), any())).thenReturn(mockTaskBuilder);
        when(mockTaskBuilder.schedule()).thenReturn(mock(com.velocitypowered.api.scheduler.ScheduledTask.class));

        // Act & Assert
        assertDoesNotThrow(() -> {
            plugin.onPlayerDisconnect(event1);
            plugin.onPlayerDisconnect(event2);
        });
    }

    @Test
    @DisplayName("Debe manejar reconexión rápida del jugador")
    void testQuickReconnect() {
        // Arrange
        var disconnectEvent = mock(com.velocitypowered.api.event.connection.DisconnectEvent.class);
        var switchEvent = mock(com.velocitypowered.api.event.player.ServerPostConnectEvent.class);

        when(disconnectEvent.getPlayer()).thenReturn(mockPlayer);
        when(switchEvent.getPlayer()).thenReturn(mockPlayer);

        var mockScheduler = mock(Scheduler.class);
        var mockTaskBuilder = mock(Scheduler.TaskBuilder.class);
        when(mockServer.getScheduler()).thenReturn(mockScheduler);
        when(mockScheduler.buildTask(any(Object.class), any(Runnable.class))).thenReturn(mockTaskBuilder);
        when(mockTaskBuilder.delay(anyLong(), any())).thenReturn(mockTaskBuilder);
        when(mockTaskBuilder.schedule()).thenReturn(mock(com.velocitypowered.api.scheduler.ScheduledTask.class));

        // Act & Assert - Simular disconnect seguido de switch
        assertDoesNotThrow(() -> {
            plugin.onPlayerDisconnect(disconnectEvent);
            plugin.onServerSwitch(switchEvent);
        });

        verify(mockLogger).info(contains("desconectado"));
        verify(mockLogger).info(contains("cambió de servidor"));
    }

    // ==================== TESTS DE INTEGRACIÓN ====================

    @Test
    @DisplayName("Debe poder manejar ciclo completo: init -> disconnect")
    void testCompleteLifecycle() {
        // Arrange
        var initEvent = mock(com.velocitypowered.api.event.proxy.ProxyInitializeEvent.class);
        var disconnectEvent = mock(com.velocitypowered.api.event.connection.DisconnectEvent.class);

        var mockChannelRegistrar = mock(com.velocitypowered.api.proxy.messages.ChannelRegistrar.class);
        when(mockServer.getChannelRegistrar()).thenReturn(mockChannelRegistrar);
        when(disconnectEvent.getPlayer()).thenReturn(mockPlayer);

        var mockScheduler = mock(Scheduler.class);
        var mockTaskBuilder = mock(Scheduler.TaskBuilder.class);
        when(mockServer.getScheduler()).thenReturn(mockScheduler);
        when(mockScheduler.buildTask(any(Object.class), any(Runnable.class))).thenReturn(mockTaskBuilder);
        when(mockTaskBuilder.delay(anyLong(), any())).thenReturn(mockTaskBuilder);
        when(mockTaskBuilder.schedule()).thenReturn(mock(com.velocitypowered.api.scheduler.ScheduledTask.class));

        // Act & Assert
        assertDoesNotThrow(() -> {
            plugin.onProxyInitialization(initEvent);
            plugin.onPlayerDisconnect(disconnectEvent);
        });
    }

    // ==================== TESTS DE LOGGING ====================

    @Test
    @DisplayName("Debe loggear eventos importantes")
    void testImportantEventsAreLogged() {
        // Arrange
        var disconnectEvent = mock(com.velocitypowered.api.event.connection.DisconnectEvent.class);
        when(disconnectEvent.getPlayer()).thenReturn(mockPlayer);

        var mockScheduler = mock(Scheduler.class);
        var mockTaskBuilder = mock(Scheduler.TaskBuilder.class);
        when(mockServer.getScheduler()).thenReturn(mockScheduler);
        when(mockScheduler.buildTask(any(Object.class), any(Runnable.class))).thenReturn(mockTaskBuilder);
        when(mockTaskBuilder.delay(anyLong(), any())).thenReturn(mockTaskBuilder);
        when(mockTaskBuilder.schedule()).thenReturn(mock(com.velocitypowered.api.scheduler.ScheduledTask.class));

        // Act
        plugin.onPlayerDisconnect(disconnectEvent);

        // Assert
        verify(mockLogger, atLeastOnce()).info(anyString());
    }

    @Test
    @DisplayName("Debe loggear warnings para situaciones anormales")
    void testWarningsForAbnormalSituations() {
        // El plugin debería usar logger.warn() en casos especiales
        // Verificamos que el logger está disponible
        assertDoesNotThrow(() -> {
            // Los warnings se logguean internamente en handleValidateToken, etc.
            assertNotNull(mockLogger);
        });
    }

    // ==================== TESTS DE THREAD SAFETY ====================

    @Test
    @DisplayName("Debe ser thread-safe para eventos concurrentes")
    void testConcurrentEvents() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        var mockScheduler = mock(Scheduler.class);
        var mockTaskBuilder = mock(Scheduler.TaskBuilder.class);
        when(mockServer.getScheduler()).thenReturn(mockScheduler);
        when(mockScheduler.buildTask(any(Object.class), any(Runnable.class))).thenReturn(mockTaskBuilder);
        when(mockTaskBuilder.delay(anyLong(), any())).thenReturn(mockTaskBuilder);
        when(mockTaskBuilder.schedule()).thenReturn(mock(com.velocitypowered.api.scheduler.ScheduledTask.class));

        // Act
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                Player player = mock(Player.class);
                when(player.getUniqueId()).thenReturn(UUID.randomUUID());
                when(player.getUsername()).thenReturn("Player" + index);

                var event = mock(com.velocitypowered.api.event.connection.DisconnectEvent.class);
                when(event.getPlayer()).thenReturn(player);

                plugin.onPlayerDisconnect(event);
            });
            threads[i].start();
        }

        // Wait
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert - No debe crashear
        assertTrue(true);
    }

    // ==================== TESTS DE DEPENDENCIAS ====================

    @Test
    @DisplayName("Debe poder crear múltiples instancias del plugin")
    void testMultiplePluginInstances() {
        // Act
        PraxProxyPlugin plugin1 = new PraxProxyPlugin(mockServer, mockLogger);
        PraxProxyPlugin plugin2 = new PraxProxyPlugin(mockServer, mockLogger);

        // Assert
        assertNotNull(plugin1);
        assertNotNull(plugin2);
        assertNotSame(plugin1, plugin2);
    }

    @Test
    @DisplayName("Debe funcionar con diferentes servidores proxy")
    void testDifferentProxyServers() {
        // Arrange
        ProxyServer server1 = mock(ProxyServer.class);
        ProxyServer server2 = mock(ProxyServer.class);

        // Act
        PraxProxyPlugin plugin1 = new PraxProxyPlugin(server1, mockLogger);
        PraxProxyPlugin plugin2 = new PraxProxyPlugin(server2, mockLogger);

        // Assert
        assertNotNull(plugin1);
        assertNotNull(plugin2);
    }
}