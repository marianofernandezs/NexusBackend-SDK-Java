package com.prax.core;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PluginMessageListener - Advanced Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PluginMessageListenerAdvancedTest {

    private PluginMessageListener listener;
    private PraxCorePlugin mockPlugin;
    private Player mockPlayer;
    private UUID testUUID;
    private Logger mockLogger;
    private MockedStatic<Bukkit> mockedBukkit;
    private Server mockServer;
    private BukkitScheduler mockScheduler;
    private DataManager mockDataManager;

    @BeforeEach
    void setUp() {
        // Limpiar serverPlayerCounts antes de cada test
        PluginMessageListener.serverPlayerCounts.clear();

        // Inicializar mocks
        mockPlugin = mock(PraxCorePlugin.class);
        mockPlayer = mock(Player.class);
        mockLogger = mock(Logger.class);
        mockServer = mock(Server.class);
        mockScheduler = mock(BukkitScheduler.class);
        mockDataManager = mock(DataManager.class);

        testUUID = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(testUUID);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        when(mockPlayer.isOnline()).thenReturn(true);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getServerType()).thenReturn("lobby");
        when(mockPlugin.getDataManager()).thenReturn(mockDataManager);
        when(mockPlugin.isLobbyServer()).thenReturn(true);

        // Mock Bukkit estático
        mockedBukkit = mockStatic(Bukkit.class);
        mockedBukkit.when(Bukkit::getServer).thenReturn(mockServer);
        mockedBukkit.when(Bukkit::getScheduler).thenReturn(mockScheduler);
        mockedBukkit.when(Bukkit::getLogger).thenReturn(mockLogger);
        mockedBukkit.when(() -> Bukkit.getPlayer(testUUID)).thenReturn(mockPlayer);

        when(mockServer.getScheduler()).thenReturn(mockScheduler);

        // Configurar scheduler para ejecutar tareas inmediatamente
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return null;
        }).when(mockScheduler).runTask(eq(mockPlugin), any(Runnable.class));

        listener = new PluginMessageListener(mockPlugin);
    }

    @AfterEach
    void tearDown() {
        if (mockedBukkit != null) {
            mockedBukkit.close();
        }
        PluginMessageListener.serverPlayerCounts.clear();
    }

    // ==================== TESTS AVANZADOS DE BUNGEE CORD ====================

    @Test
    @DisplayName("BungeeCord: Debe manejar PlayerCount con nombre de servidor vacío")
    void testBungeeCordPlayerCountEmptyServerName() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF("");
        out.writeInt(0);

        // Act
        listener.onPluginMessageReceived("BungeeCord", mockPlayer, out.toByteArray());

        // Assert
        assertTrue(PluginMessageListener.serverPlayerCounts.containsKey(""));
        assertEquals(0, PluginMessageListener.serverPlayerCounts.get(""));
    }

    @Test
    @DisplayName("BungeeCord: Debe manejar PlayerCount con valor negativo")
    void testBungeeCordPlayerCountNegativeValue() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF("test-server");
        out.writeInt(-5);

        // Act
        listener.onPluginMessageReceived("BungeeCord", mockPlayer, out.toByteArray());

        // Assert
        assertEquals(-5, PluginMessageListener.serverPlayerCounts.get("test-server"));
    }

    @Test
    @DisplayName("BungeeCord: Debe manejar PlayerCount con valor máximo de Integer")
    void testBungeeCordPlayerCountMaxValue() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF("large-server");
        out.writeInt(Integer.MAX_VALUE);

        // Act
        listener.onPluginMessageReceived("BungeeCord", mockPlayer, out.toByteArray());

        // Assert
        assertEquals(Integer.MAX_VALUE, PluginMessageListener.serverPlayerCounts.get("large-server"));
    }

    @Test
    @DisplayName("BungeeCord: Debe manejar canal con case insensitive")
    void testBungeeCordCaseInsensitive() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF("server1");
        out.writeInt(10);

        // Act - Probar diferentes variaciones de case
        listener.onPluginMessageReceived("bungeecord", mockPlayer, out.toByteArray());

        // Assert
        assertEquals(10, PluginMessageListener.serverPlayerCounts.get("server1"));
    }

    // ==================== TESTS AVANZADOS DE VALIDATION RESPONSE ====================

    @Test
    @DisplayName("ValidationResponse: Debe manejar validación exitosa en servidor lobby")
    void testValidationResponseSuccessfulInLobby() {
        // Arrange
        when(mockPlugin.isLobbyServer()).thenReturn(true);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidationResponse");
        out.writeUTF(testUUID.toString());
        out.writeBoolean(true);

        // Act
        listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray());

        // Assert
        verify(mockPlugin).setPendingValidation(testUUID, false);
        verify(mockPlugin).setAuthenticated(testUUID, true);
        verify(mockPlugin).setLoginTime(testUUID);
        verify(mockPlayer).sendMessage(contains("¡Sesión iniciada correctamente!"));
    }

    @Test
    @DisplayName("ValidationResponse: Debe manejar validación fallida con jugador registrado en lobby")
    void testValidationResponseFailedRegisteredPlayerInLobby() {
        // Arrange
        when(mockPlugin.isLobbyServer()).thenReturn(true);
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidationResponse");
        out.writeUTF(testUUID.toString());
        out.writeBoolean(false);

        // Act
        listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray());

        // Assert
        verify(mockPlugin).setPendingValidation(testUUID, false);
        verify(mockPlugin).setAuthenticated(testUUID, false);
        verify(mockPlayer).sendMessage(contains("/login"));
    }

    @Test
    @DisplayName("ValidationResponse: Debe manejar validación fallida con jugador no registrado en lobby")
    void testValidationResponseFailedUnregisteredPlayerInLobby() {
        // Arrange
        when(mockPlugin.isLobbyServer()).thenReturn(true);
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidationResponse");
        out.writeUTF(testUUID.toString());
        out.writeBoolean(false);

        // Act
        listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray());

        // Assert
        verify(mockPlugin).setAuthenticated(testUUID, false);
        verify(mockPlayer).sendMessage(contains("/register"));
    }

    @Test
    @DisplayName("ValidationResponse: Debe kickear jugador en servidor backend con validación fallida")
    void testValidationResponseFailedInBackend() {
        // Arrange
        when(mockPlugin.isLobbyServer()).thenReturn(false);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidationResponse");
        out.writeUTF(testUUID.toString());
        out.writeBoolean(false);

        // Act
        listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray());

        // Assert
        verify(mockPlayer).kickPlayer(contains("Tu sesión no es válida"));
    }

    @Test
    @DisplayName("ValidationResponse: Debe manejar jugador offline gracefully")
    void testValidationResponsePlayerOffline() {
        // Arrange
        when(mockPlayer.isOnline()).thenReturn(false);
        mockedBukkit.when(() -> Bukkit.getPlayer(testUUID)).thenReturn(null);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidationResponse");
        out.writeUTF(testUUID.toString());
        out.writeBoolean(true);

        // Act
        listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray());

        // Assert
        verify(mockLogger).warning(contains("Respuesta de validación para jugador offline"));
        verify(mockPlugin, never()).setAuthenticated(any(), anyBoolean());
    }

    @Test
    @DisplayName("ValidationResponse: Debe loguear correctamente el tipo de servidor")
    void testValidationResponseLogsServerType() {
        // Arrange
        when(mockPlugin.getServerType()).thenReturn("survival");

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidationResponse");
        out.writeUTF(testUUID.toString());
        out.writeBoolean(true);

        // Act
        listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray());

        // Assert
        verify(mockLogger).info(contains("[survival]"));
        verify(mockLogger).info(contains("VALIDA"));
    }

    // ==================== TESTS AVANZADOS DE REGISTER RESPONSE ====================

    @Test
    @DisplayName("RegisterResponse: Debe manejar registro exitoso")
    void testRegisterResponseSuccess() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("RegisterResponse");
        out.writeUTF(testUUID.toString());
        out.writeBoolean(true);

        // Act
        listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray());

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockPlayer, times(2)).sendMessage(messageCaptor.capture());

        assertTrue(messageCaptor.getAllValues().get(0).contains("registrado correctamente"));
        assertTrue(messageCaptor.getAllValues().get(1).contains("/login"));
    }

    @Test
    @DisplayName("RegisterResponse: Debe manejar registro fallido")
    void testRegisterResponseFailure() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("RegisterResponse");
        out.writeUTF(testUUID.toString());
        out.writeBoolean(false);

        // Act
        listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray());

        // Assert
        verify(mockPlayer).sendMessage(contains("No se pudo completar el registro"));
    }

    @Test
    @DisplayName("RegisterResponse: Debe ignorar respuesta si jugador está offline")
    void testRegisterResponsePlayerNull() {
        // Arrange
        UUID offlineUUID = UUID.randomUUID();
        mockedBukkit.when(() -> Bukkit.getPlayer(offlineUUID)).thenReturn(null);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("RegisterResponse");
        out.writeUTF(offlineUUID.toString());
        out.writeBoolean(true);

        // Act
        listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray());

        // Assert
        verify(mockPlayer, never()).sendMessage(anyString());
    }

    // ==================== TESTS AVANZADOS DE LOGOUT RESPONSE ====================

    @Test
    @DisplayName("LogoutResponse: Debe manejar logout exitoso")
    void testLogoutResponseSuccess() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("LogoutResponse");
        out.writeUTF(testUUID.toString());
        out.writeBoolean(true);

        // Act
        listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray());

        // Assert
        verify(mockPlugin).setAuthenticated(testUUID, false);
        verify(mockPlugin).removeLoginTime(testUUID);
        verify(mockPlayer).sendMessage(contains("cerrado sesión correctamente"));
    }

    @Test
    @DisplayName("LogoutResponse: Debe manejar logout fallido")
    void testLogoutResponseFailure() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("LogoutResponse");
        out.writeUTF(testUUID.toString());
        out.writeBoolean(false);

        // Act
        listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray());

        // Assert
        verify(mockPlugin, never()).setAuthenticated(any(), anyBoolean());
        verify(mockPlugin, never()).removeLoginTime(any());
        verify(mockPlayer).sendMessage(contains("No se pudo cerrar tu sesión"));
    }

    @Test
    @DisplayName("LogoutResponse: Debe ignorar respuesta si jugador está offline")
    void testLogoutResponsePlayerOffline() {
        // Arrange
        UUID offlineUUID = UUID.randomUUID();
        mockedBukkit.when(() -> Bukkit.getPlayer(offlineUUID)).thenReturn(null);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("LogoutResponse");
        out.writeUTF(offlineUUID.toString());
        out.writeBoolean(true);

        // Act
        listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray());

        // Assert
        verify(mockPlugin, never()).setAuthenticated(any(), anyBoolean());
    }

    // ==================== TESTS DE NEXUS:SYNC CHANNEL ====================

    @Test
    @DisplayName("Nexus: Debe procesar mensaje PLAYER_JOIN correctamente")
    void testNexusPlayerJoin() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PLAYER_JOIN");
        out.writeUTF(testUUID.toString());
        out.writeUTF("TestPlayer");
        out.writeUTF("127.0.0.1");

        // Act
        listener.onPluginMessageReceived("nexus:sync", mockPlayer, out.toByteArray());

        // Assert
        verify(mockLogger).info(contains("[NEXUS] JOIN"));
        verify(mockLogger).info(contains("TestPlayer"));
        verify(mockLogger).info(contains(testUUID.toString()));
    }

    @Test
    @DisplayName("Nexus: Debe procesar mensaje PLAYER_SWITCH correctamente")
    void testNexusPlayerSwitch() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PLAYER_SWITCH");
        out.writeUTF(testUUID.toString());
        out.writeUTF("lobby");
        out.writeUTF("survival");

        // Act
        listener.onPluginMessageReceived("nexus:sync", mockPlayer, out.toByteArray());

        // Assert
        verify(mockLogger).info(contains("[NEXUS] SWITCH"));
        verify(mockLogger).info(contains("lobby"));
        verify(mockLogger).info(contains("survival"));
    }

    @Test
    @DisplayName("Nexus: Debe procesar mensaje PLAYER_QUIT correctamente")
    void testNexusPlayerQuit() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PLAYER_QUIT");
        out.writeUTF(testUUID.toString());

        // Act
        listener.onPluginMessageReceived("nexus:sync", mockPlayer, out.toByteArray());

        // Assert
        verify(mockLogger).info(contains("[NEXUS] QUIT"));
        verify(mockLogger).info(contains(testUUID.toString()));
    }

    @Test
    @DisplayName("Nexus: Debe procesar mensaje PLAYER_LIST con múltiples servidores")
    void testNexusPlayerList() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PLAYER_LIST");
        out.writeInt(2); // 2 servidores

        // Servidor 1
        out.writeUTF("lobby");
        out.writeInt(2); // 2 jugadores
        out.writeUTF(UUID.randomUUID().toString());
        out.writeUTF(UUID.randomUUID().toString());

        // Servidor 2
        out.writeUTF("survival");
        out.writeInt(1); // 1 jugador
        out.writeUTF(UUID.randomUUID().toString());

        // Act
        listener.onPluginMessageReceived("nexus:sync", mockPlayer, out.toByteArray());

        // Assert
        verify(mockLogger, atLeast(5)).info(anyString());
        verify(mockLogger).info(contains("Número de servidores: 2"));
        verify(mockLogger).info(contains("lobby"));
        verify(mockLogger).info(contains("survival"));
    }

    @Test
    @DisplayName("Nexus: Debe procesar PLAYER_LIST vacía")
    void testNexusPlayerListEmpty() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PLAYER_LIST");
        out.writeInt(0); // Sin servidores

        // Act
        listener.onPluginMessageReceived("nexus:sync", mockPlayer, out.toByteArray());

        // Assert
        verify(mockLogger).info(contains("Número de servidores: 0"));
        verify(mockLogger).info(contains("Fin de la lista de jugadores"));
    }

    // ==================== TESTS DE CONCURRENCIA ====================

    @Test
    @DisplayName("Concurrencia: Múltiples mensajes PlayerCount simultáneos")
    void testConcurrentPlayerCountUpdates() {
        // Arrange
        String[] servers = {"server1", "server2", "server3", "server4", "server5"};
        int[] counts = {10, 20, 30, 40, 50};

        // Act - Simular múltiples actualizaciones
        for (int i = 0; i < servers.length; i++) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PlayerCount");
            out.writeUTF(servers[i]);
            out.writeInt(counts[i]);
            listener.onPluginMessageReceived("BungeeCord", mockPlayer, out.toByteArray());
        }

        // Assert - Verificar que todos los valores estén correctos
        for (int i = 0; i < servers.length; i++) {
            assertEquals(counts[i], PluginMessageListener.serverPlayerCounts.get(servers[i]));
        }
    }

    // ==================== TESTS DE EDGE CASES ====================

    @Test
    @DisplayName("Edge: UUID inválido en ValidationResponse")
    void testValidationResponseInvalidUUID() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidationResponse");
        out.writeUTF("invalid-uuid");
        out.writeBoolean(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                listener.onPluginMessageReceived("prax:core", mockPlayer, out.toByteArray())
        );
    }

    @Test
    @DisplayName("Edge: Mensaje vacío en canal prax:core")
    void testEmptyMessagePraxCore() {
        // Arrange
        byte[] emptyMessage = new byte[0];

        // Act & Assert
        assertThrows(Exception.class, () ->
                listener.onPluginMessageReceived("prax:core", mockPlayer, emptyMessage)
        );
    }

    @Test
    @DisplayName("Edge: Case sensitivity en canales")
    void testChannelCaseSensitivity() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidationResponse");
        out.writeUTF(testUUID.toString());
        out.writeBoolean(true);

        // Act - Diferentes variaciones de case
        assertDoesNotThrow(() ->
                listener.onPluginMessageReceived("PRAX:CORE", mockPlayer, out.toByteArray())
        );

        assertDoesNotThrow(() ->
                listener.onPluginMessageReceived("Prax:Core", mockPlayer, out.toByteArray())
        );
    }

    // ==================== TESTS DE INTEGRACIÓN ====================

    @Test
    @DisplayName("Integración: Flujo completo de login en lobby")
    void testCompleteLoginFlowInLobby() {
        // Arrange
        when(mockPlugin.isLobbyServer()).thenReturn(true);
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);

        // Act 1: Validación fallida inicialmente
        ByteArrayDataOutput out1 = ByteStreams.newDataOutput();
        out1.writeUTF("ValidationResponse");
        out1.writeUTF(testUUID.toString());
        out1.writeBoolean(false);
        listener.onPluginMessageReceived("prax:core", mockPlayer, out1.toByteArray());

        // Assert 1
        verify(mockPlayer).sendMessage(contains("/login"));
        verify(mockPlugin).setAuthenticated(testUUID, false);

        // Act 2: Login exitoso
        ByteArrayDataOutput out2 = ByteStreams.newDataOutput();
        out2.writeUTF("ValidationResponse");
        out2.writeUTF(testUUID.toString());
        out2.writeBoolean(true);
        listener.onPluginMessageReceived("prax:core", mockPlayer, out2.toByteArray());

        // Assert 2
        verify(mockPlugin).setAuthenticated(testUUID, true);
        verify(mockPlugin).setLoginTime(testUUID);
        verify(mockPlayer).sendMessage(contains("¡Sesión iniciada correctamente!"));
    }

    @Test
    @DisplayName("Integración: Múltiples jugadores con diferentes estados")
    void testMultiplePlayersWithDifferentStates() {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        Player player3 = mock(Player.class);

        when(player1.getUniqueId()).thenReturn(uuid1);
        when(player2.getUniqueId()).thenReturn(uuid2);
        when(player3.getUniqueId()).thenReturn(uuid3);
        when(player1.isOnline()).thenReturn(true);
        when(player2.isOnline()).thenReturn(true);
        when(player3.isOnline()).thenReturn(true);

        mockedBukkit.when(() -> Bukkit.getPlayer(uuid1)).thenReturn(player1);
        mockedBukkit.when(() -> Bukkit.getPlayer(uuid2)).thenReturn(player2);
        mockedBukkit.when(() -> Bukkit.getPlayer(uuid3)).thenReturn(player3);

        // Act - Player1: Validación exitosa
        ByteArrayDataOutput out1 = ByteStreams.newDataOutput();
        out1.writeUTF("ValidationResponse");
        out1.writeUTF(uuid1.toString());
        out1.writeBoolean(true);
        listener.onPluginMessageReceived("prax:core", player1, out1.toByteArray());

        // Player2: Registro exitoso
        ByteArrayDataOutput out2 = ByteStreams.newDataOutput();
        out2.writeUTF("RegisterResponse");
        out2.writeUTF(uuid2.toString());
        out2.writeBoolean(true);
        listener.onPluginMessageReceived("prax:core", player2, out2.toByteArray());

        // Player3: Logout exitoso
        ByteArrayDataOutput out3 = ByteStreams.newDataOutput();
        out3.writeUTF("LogoutResponse");
        out3.writeUTF(uuid3.toString());
        out3.writeBoolean(true);
        listener.onPluginMessageReceived("prax:core", player3, out3.toByteArray());

        // Assert
        verify(mockPlugin).setAuthenticated(uuid1, true);
        verify(player1).sendMessage(contains("¡Sesión iniciada correctamente!"));
        verify(player2, times(2)).sendMessage(anyString());
        verify(mockPlugin).setAuthenticated(uuid3, false);
    }
}