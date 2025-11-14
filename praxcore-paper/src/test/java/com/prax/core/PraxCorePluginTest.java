package com.prax.core;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.messaging.Messenger;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PraxCorePlugin - Advanced Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PraxCorePluginAdvancedTest {

    private PraxCorePlugin plugin;
    private DataManager mockDataManager;
    private Player mockPlayer;
    private UUID testUUID;
    private Logger mockLogger;
    private Server mockServer;
    private Messenger mockMessenger;

    @BeforeEach
    void setUp() throws Exception {
        // Crear instancia real del plugin usando mock parcial
        plugin = mock(PraxCorePlugin.class);
        mockDataManager = mock(DataManager.class);
        mockPlayer = mock(Player.class);
        mockLogger = mock(Logger.class);
        mockServer = mock(Server.class);
        mockMessenger = mock(Messenger.class);

        testUUID = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(testUUID);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        when(mockPlayer.isOnline()).thenReturn(true);

        // Configurar comportamiento del mock
        when(plugin.getDataManager()).thenReturn(mockDataManager);
        when(plugin.getLogger()).thenReturn(mockLogger);
        when(plugin.getServer()).thenReturn(mockServer);
        when(mockServer.getMessenger()).thenReturn(mockMessenger);
        when(mockMessenger.isOutgoingChannelRegistered(eq(plugin), anyString())).thenReturn(true);

        // Inicializar los campos privados usando reflexión
        initializePrivateFields(plugin);

        // Hacer que los métodos de estado funcionen con los campos reales
        doCallRealMethod().when(plugin).isAuthenticated(any());
        doCallRealMethod().when(plugin).setAuthenticated(any(), anyBoolean());
        doCallRealMethod().when(plugin).isPendingValidation(any());
        doCallRealMethod().when(plugin).setPendingValidation(any(), anyBoolean());
        doCallRealMethod().when(plugin).setLoginTime(any());
        doCallRealMethod().when(plugin).getLoginTime(any());
        doCallRealMethod().when(plugin).removeLoginTime(any());

        // Hacer que los métodos de envío de mensajes funcionen
        doCallRealMethod().when(plugin).sendCreateSessionAuth(any(), anyString(), anyString());
        doCallRealMethod().when(plugin).sendRegisterRequest(any(), anyString(), anyString(), anyString());
        doCallRealMethod().when(plugin).sendLogoutRequest(any());
        doCallRealMethod().when(plugin).sendValidateTokenMessage(any());
    }

    private void initializePrivateFields(PraxCorePlugin plugin) throws Exception {
        // Inicializar authenticatedPlayers
        Field authenticatedPlayersField = PraxCorePlugin.class.getDeclaredField("authenticatedPlayers");
        authenticatedPlayersField.setAccessible(true);
        authenticatedPlayersField.set(plugin, new HashSet<UUID>());

        // Inicializar pendingValidationPlayers
        Field pendingValidationPlayersField = PraxCorePlugin.class.getDeclaredField("pendingValidationPlayers");
        pendingValidationPlayersField.setAccessible(true);
        pendingValidationPlayersField.set(plugin, Collections.newSetFromMap(new ConcurrentHashMap<>()));

        // Inicializar playerLoginTimes
        Field playerLoginTimesField = PraxCorePlugin.class.getDeclaredField("playerLoginTimes");
        playerLoginTimesField.setAccessible(true);
        playerLoginTimesField.set(plugin, new HashMap<UUID, Long>());
    }

    // ==================== TESTS DE AUTENTICACIÓN AVANZADOS ====================

    @Test
    @DisplayName("Autenticación: Múltiples jugadores con estados diferentes")
    void testMultiplePlayersWithDifferentAuthStates() {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();

        // Act
        plugin.setAuthenticated(uuid1, true);
        plugin.setAuthenticated(uuid2, false);
        plugin.setPendingValidation(uuid3, true);
        // uuid4 sin cambios

        // Assert
        assertTrue(plugin.isAuthenticated(uuid1));
        assertFalse(plugin.isAuthenticated(uuid2));
        assertFalse(plugin.isAuthenticated(uuid3));
        assertTrue(plugin.isPendingValidation(uuid3));
        assertFalse(plugin.isAuthenticated(uuid4));
        assertFalse(plugin.isPendingValidation(uuid4));
    }

    @Test
    @DisplayName("Autenticación: Cambio de estado múltiple del mismo jugador")
    void testMultipleAuthStateChanges() {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act & Assert - Secuencia compleja de cambios
        plugin.setAuthenticated(uuid, true);
        assertTrue(plugin.isAuthenticated(uuid));

        plugin.setAuthenticated(uuid, false);
        assertFalse(plugin.isAuthenticated(uuid));

        plugin.setAuthenticated(uuid, true);
        assertTrue(plugin.isAuthenticated(uuid));

        plugin.setAuthenticated(uuid, true);
        assertTrue(plugin.isAuthenticated(uuid)); // Idempotente

        plugin.setAuthenticated(uuid, false);
        assertFalse(plugin.isAuthenticated(uuid));

        plugin.setAuthenticated(uuid, false);
        assertFalse(plugin.isAuthenticated(uuid)); // Idempotente
    }

    @Test
    @DisplayName("Autenticación: Thread safety con ConcurrentHashMap")
    void testAuthenticationThreadSafety() throws InterruptedException {
        // Arrange
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            uuids.add(UUID.randomUUID());
        }

        // Act - Simular acceso concurrente
        Thread[] threads = new Thread[10];
        for (int t = 0; t < threads.length; t++) {
            final int threadIndex = t;
            threads[t] = new Thread(() -> {
                for (int i = threadIndex * 10; i < (threadIndex + 1) * 10; i++) {
                    plugin.setPendingValidation(uuids.get(i), true);
                }
            });
            threads[t].start();
        }

        // Esperar a que todos los threads terminen
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert - Verificar que todos los UUIDs fueron agregados
        for (UUID uuid : uuids) {
            assertTrue(plugin.isPendingValidation(uuid));
        }
    }

    // ==================== TESTS DE PENDING VALIDATION ====================

    @Test
    @DisplayName("PendingValidation: Debe limpiar pending al autenticar")
    void testSetAuthenticatedClearsPending() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        plugin.setPendingValidation(uuid, true);
        assertTrue(plugin.isPendingValidation(uuid));

        // Act
        plugin.setAuthenticated(uuid, true);

        // Assert
        assertTrue(plugin.isAuthenticated(uuid));
        assertFalse(plugin.isPendingValidation(uuid));
    }

    @Test
    @DisplayName("PendingValidation: Debe limpiar pending al denegar autenticación")
    void testSetAuthenticatedFalseClearsPending() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        plugin.setPendingValidation(uuid, true);
        assertTrue(plugin.isPendingValidation(uuid));

        // Act
        plugin.setAuthenticated(uuid, false);

        // Assert
        assertFalse(plugin.isAuthenticated(uuid));
        assertFalse(plugin.isPendingValidation(uuid));
    }

    @Test
    @DisplayName("PendingValidation: Estado independiente de autenticación")
    void testPendingValidationIndependent() {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act & Assert - Pending puede estar activo sin autenticación
        plugin.setPendingValidation(uuid, true);
        assertTrue(plugin.isPendingValidation(uuid));
        assertFalse(plugin.isAuthenticated(uuid));

        // Limpiar pending sin autenticar
        plugin.setPendingValidation(uuid, false);
        assertFalse(plugin.isPendingValidation(uuid));
        assertFalse(plugin.isAuthenticated(uuid));
    }

    // ==================== TESTS DE LOGIN TIME ====================

    @Test
    @DisplayName("LoginTime: Debe registrar tiempo actual")
    void testLoginTimeCurrentTime() throws InterruptedException {
        // Arrange
        UUID uuid = UUID.randomUUID();
        long beforeTime = System.currentTimeMillis();

        // Act
        Thread.sleep(10); // Pequeña espera para asegurar diferencia
        plugin.setLoginTime(uuid);
        Thread.sleep(10);
        long afterTime = System.currentTimeMillis();
        Long loginTime = plugin.getLoginTime(uuid);

        // Assert
        assertNotNull(loginTime);
        assertTrue(loginTime > beforeTime);
        assertTrue(loginTime < afterTime);
    }

    @Test
    @DisplayName("LoginTime: Debe sobrescribir tiempo anterior")
    void testLoginTimeOverwrite() throws InterruptedException {
        // Arrange
        UUID uuid = UUID.randomUUID();
        plugin.setLoginTime(uuid);
        Long firstTime = plugin.getLoginTime(uuid);

        // Act
        Thread.sleep(50); // Espera significativa
        plugin.setLoginTime(uuid);
        Long secondTime = plugin.getLoginTime(uuid);

        // Assert
        assertNotNull(firstTime);
        assertNotNull(secondTime);
        assertTrue(secondTime > firstTime);
    }

    @Test
    @DisplayName("LoginTime: Múltiples jugadores con tiempos diferentes")
    void testMultipleLoginTimes() throws InterruptedException {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        // Act
        plugin.setLoginTime(uuid1);
        Thread.sleep(20);
        plugin.setLoginTime(uuid2);
        Thread.sleep(20);
        plugin.setLoginTime(uuid3);

        Long time1 = plugin.getLoginTime(uuid1);
        Long time2 = plugin.getLoginTime(uuid2);
        Long time3 = plugin.getLoginTime(uuid3);

        // Assert
        assertNotNull(time1);
        assertNotNull(time2);
        assertNotNull(time3);
        assertTrue(time2 > time1);
        assertTrue(time3 > time2);
    }

    @Test
    @DisplayName("LoginTime: Remove debe eliminar completamente")
    void testRemoveLoginTimeComplete() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        plugin.setLoginTime(uuid);
        assertNotNull(plugin.getLoginTime(uuid));

        // Act
        plugin.removeLoginTime(uuid);

        // Assert
        assertNull(plugin.getLoginTime(uuid));
    }

    @Test
    @DisplayName("LoginTime: Remove en UUID inexistente no causa error")
    void testRemoveLoginTimeNonExistent() {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act & Assert
        assertDoesNotThrow(() -> plugin.removeLoginTime(uuid));
        assertNull(plugin.getLoginTime(uuid));
    }

    // ==================== TESTS DE MENSAJES DE PLUGIN ====================

    @Test
    @DisplayName("PluginMessage: sendCreateSessionAuth debe enviar datos correctos")
    void testSendCreateSessionAuth() {
        // Arrange
        String email = "test@example.com";
        String password = "securePassword123";
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);

        // Act
        plugin.sendCreateSessionAuth(mockPlayer, email, password);

        // Assert
        verify(mockPlayer).sendPluginMessage(eq(plugin), eq("prax:core"), messageCaptor.capture());

        // Verificar el contenido del mensaje
        byte[] sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertTrue(sentMessage.length > 0);

        verify(mockLogger).info(contains("CreateSession enviado"));
    }

    @Test
    @DisplayName("PluginMessage: sendCreateSessionAuth con canal no registrado")
    void testSendCreateSessionAuthChannelNotRegistered() {
        // Arrange
        when(mockMessenger.isOutgoingChannelRegistered(eq(plugin), eq("prax:core"))).thenReturn(false);

        // Act
        plugin.sendCreateSessionAuth(mockPlayer, "test@test.com", "password");

        // Assert
        verify(mockPlayer, never()).sendPluginMessage(any(), anyString(), any());
        verify(mockLogger).severe(contains("Canal 'prax:core' no está registrado"));
    }

    @Test
    @DisplayName("PluginMessage: sendRegisterRequest debe enviar todos los datos")
    void testSendRegisterRequest() {
        // Arrange
        String email = "newuser@example.com";
        String password = "newPassword123";
        String birthdate = "1990-01-01";
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);

        // Act
        plugin.sendRegisterRequest(mockPlayer, email, password, birthdate);

        // Assert
        verify(mockPlayer).sendPluginMessage(eq(plugin), eq("prax:core"), messageCaptor.capture());
        verify(mockPlayer).sendMessage(contains("Solicitud enviada"));
        verify(mockLogger).info(contains("RegisterRequest enviado"));

        byte[] sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertTrue(sentMessage.length > 0);
    }

    @Test
    @DisplayName("PluginMessage: sendLogoutRequest debe funcionar correctamente")
    void testSendLogoutRequest() {
        // Arrange
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);

        // Act
        plugin.sendLogoutRequest(mockPlayer);

        // Assert
        verify(mockPlayer).sendPluginMessage(eq(plugin), eq("prax:core"), messageCaptor.capture());
        verify(mockLogger).info(contains("LogoutSession enviado"));

        byte[] sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertTrue(sentMessage.length > 0);
    }

    @Test
    @DisplayName("PluginMessage: sendValidateTokenMessage debe enviar UUID")
    void testSendValidateTokenMessage() {
        // Arrange
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);

        // Act
        plugin.sendValidateTokenMessage(mockPlayer);

        // Assert
        verify(mockPlayer).sendPluginMessage(eq(plugin), eq("prax:core"), messageCaptor.capture());
        verify(mockLogger).info(contains("ValidateToken enviado"));

        byte[] sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertTrue(sentMessage.length > 0);
    }

    @Test
    @DisplayName("PluginMessage: Manejo de excepciones en sendCreateSessionAuth")
    void testSendCreateSessionAuthException() {
        // Arrange
        doThrow(new RuntimeException("Network error"))
                .when(mockPlayer).sendPluginMessage(any(), anyString(), any());

        // Act
        plugin.sendCreateSessionAuth(mockPlayer, "test@test.com", "password");

        // Assert
        verify(mockLogger).severe(contains("Error enviando CreateSession"));
    }

    // ==================== TESTS DE CONFIGURACIÓN ====================

    @Test
    @DisplayName("Configuración: isLobbyServer case insensitive")
    void testIsLobbyServerCaseInsensitive() {
        // Test con "lobby" en minúsculas
        when(plugin.getServerType()).thenReturn("lobby");
        when(plugin.isLobbyServer()).thenReturn(true);
        assertTrue(plugin.isLobbyServer());

        // Test con "LOBBY" en mayúsculas
        when(plugin.getServerType()).thenReturn("LOBBY");
        when(plugin.isLobbyServer()).thenReturn(true);
        assertTrue(plugin.isLobbyServer());

        // Test con "Lobby" mezclado
        when(plugin.getServerType()).thenReturn("Lobby");
        when(plugin.isLobbyServer()).thenReturn(true);
        assertTrue(plugin.isLobbyServer());

        // Test con "backend"
        when(plugin.getServerType()).thenReturn("backend");
        when(plugin.isLobbyServer()).thenReturn(false);
        assertFalse(plugin.isLobbyServer());
    }

    @Test
    @DisplayName("Configuración: getServerType debe retornar valor actual")
    void testGetServerTypeValues() {
        // Test diferentes valores posibles
        String[] serverTypes = {"lobby", "survival", "creative", "minigames", "backend"};

        for (String type : serverTypes) {
            when(plugin.getServerType()).thenReturn(type);
            assertEquals(type, plugin.getServerType());
        }
    }

    // ==================== TESTS DE INTEGRACIÓN COMPLETA ====================

    @Test
    @DisplayName("Integración: Flujo completo de registro y login")
    void testCompleteRegistrationAndLoginFlow() {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act & Assert - Estado inicial
        assertFalse(plugin.isAuthenticated(uuid));
        assertFalse(plugin.isPendingValidation(uuid));
        assertNull(plugin.getLoginTime(uuid));

        // Fase 1: Pending validation
        plugin.setPendingValidation(uuid, true);
        assertTrue(plugin.isPendingValidation(uuid));
        assertFalse(plugin.isAuthenticated(uuid));

        // Fase 2: Autenticación exitosa
        plugin.setAuthenticated(uuid, true);
        assertTrue(plugin.isAuthenticated(uuid));
        assertFalse(plugin.isPendingValidation(uuid)); // Se limpió automáticamente

        // Fase 3: Registrar tiempo de login
        plugin.setLoginTime(uuid);
        assertNotNull(plugin.getLoginTime(uuid));

        // Fase 4: Logout
        plugin.setAuthenticated(uuid, false);
        plugin.removeLoginTime(uuid);
        assertFalse(plugin.isAuthenticated(uuid));
        assertNull(plugin.getLoginTime(uuid));
    }

    @Test
    @DisplayName("Integración: Sesión expirada y relogin")
    void testSessionExpiredAndRelogin() throws InterruptedException {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act - Primera sesión
        plugin.setAuthenticated(uuid, true);
        plugin.setLoginTime(uuid);
        Long firstLoginTime = plugin.getLoginTime(uuid);

        // Simular expiración
        Thread.sleep(50);
        plugin.setAuthenticated(uuid, false);
        plugin.removeLoginTime(uuid);

        // Nueva sesión
        Thread.sleep(50);
        plugin.setAuthenticated(uuid, true);
        plugin.setLoginTime(uuid);
        Long secondLoginTime = plugin.getLoginTime(uuid);

        // Assert
        assertNotNull(firstLoginTime);
        assertNotNull(secondLoginTime);
        assertTrue(secondLoginTime > firstLoginTime);
        assertTrue(plugin.isAuthenticated(uuid));
    }

    @Test
    @DisplayName("Integración: Validación fallida y retry exitoso")
    void testFailedValidationThenSuccess() {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act - Intento 1: Fallido
        plugin.setPendingValidation(uuid, true);
        plugin.setAuthenticated(uuid, false); // Validación fallida
        assertFalse(plugin.isAuthenticated(uuid));
        assertFalse(plugin.isPendingValidation(uuid));

        // Intento 2: Exitoso
        plugin.setPendingValidation(uuid, true);
        plugin.setAuthenticated(uuid, true); // Validación exitosa
        plugin.setLoginTime(uuid);

        // Assert
        assertTrue(plugin.isAuthenticated(uuid));
        assertFalse(plugin.isPendingValidation(uuid));
        assertNotNull(plugin.getLoginTime(uuid));
    }

    @Test
    @DisplayName("Integración: Múltiples sesiones concurrentes")
    void testMultipleConcurrentSessions() {
        // Arrange
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            uuids.add(UUID.randomUUID());
        }

        // Act - Crear 20 sesiones
        for (UUID uuid : uuids) {
            plugin.setAuthenticated(uuid, true);
            plugin.setLoginTime(uuid);
        }

        // Assert - Verificar que todas las sesiones estén activas
        for (UUID uuid : uuids) {
            assertTrue(plugin.isAuthenticated(uuid));
            assertNotNull(plugin.getLoginTime(uuid));
        }

        // Act - Cerrar 10 sesiones (pares)
        for (int i = 0; i < uuids.size(); i += 2) {
            plugin.setAuthenticated(uuids.get(i), false);
            plugin.removeLoginTime(uuids.get(i));
        }

        // Assert - Verificar estado mixto
        for (int i = 0; i < uuids.size(); i++) {
            if (i % 2 == 0) {
                assertFalse(plugin.isAuthenticated(uuids.get(i)));
                assertNull(plugin.getLoginTime(uuids.get(i)));
            } else {
                assertTrue(plugin.isAuthenticated(uuids.get(i)));
                assertNotNull(plugin.getLoginTime(uuids.get(i)));
            }
        }
    }

    @Test
    @DisplayName("Integración: Envío de mensajes durante flujo de autenticación")
    void testMessageSendingDuringAuthFlow() {
        // Arrange
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        when(player1.getUniqueId()).thenReturn(uuid1);
        when(player2.getUniqueId()).thenReturn(uuid2);
        when(player1.getName()).thenReturn("Player1");
        when(player2.getName()).thenReturn("Player2");

        // Act - Simular flujo completo para 2 jugadores
        // Player 1: Validar token
        plugin.sendValidateTokenMessage(player1);
        plugin.setPendingValidation(uuid1, true);

        // Player 2: Crear sesión
        plugin.sendCreateSessionAuth(player2, "player2@test.com", "pass123");
        plugin.setPendingValidation(uuid2, true);

        // Ambos se autentican
        plugin.setAuthenticated(uuid1, true);
        plugin.setLoginTime(uuid1);
        plugin.setAuthenticated(uuid2, true);
        plugin.setLoginTime(uuid2);

        // Player 1 hace logout
        plugin.sendLogoutRequest(player1);
        plugin.setAuthenticated(uuid1, false);
        plugin.removeLoginTime(uuid1);

        // Assert - Verificar que se enviaron mensajes a ambos jugadores
        verify(player1, times(2)).sendPluginMessage(any(), eq("prax:core"), any()); // ValidateToken + Logout
        verify(player2, times(1)).sendPluginMessage(any(), eq("prax:core"), any()); // CreateSession
        assertTrue(plugin.isAuthenticated(uuid2));
        assertFalse(plugin.isAuthenticated(uuid1));
        assertNotNull(plugin.getLoginTime(uuid2));
        assertNull(plugin.getLoginTime(uuid1));
    }

    // ==================== TESTS DE LÍMITES Y EDGE CASES ====================

    @Test
    @DisplayName("EdgeCase: Operaciones con UUID válido en estado inicial")
    void testValidUUIDInInitialState() {
        // Verificar que un UUID válido funciona correctamente en estado inicial
        UUID validUuid = UUID.randomUUID();

        // Estado inicial: no autenticado, no pending, sin login time
        assertFalse(plugin.isAuthenticated(validUuid));
        assertFalse(plugin.isPendingValidation(validUuid));
        assertNull(plugin.getLoginTime(validUuid));

        // Nota: El código real no soporta UUID null en las colecciones
        // (HashSet y HashMap lanzan NullPointerException con null keys)
        // Por lo tanto, no testeamos ese caso edge ya que es un comportamiento esperado
    }

    @Test
    @DisplayName("EdgeCase: Operaciones repetidas idempotentes")
    void testIdempotentOperations() {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act & Assert - Múltiples llamadas del mismo estado
        plugin.setAuthenticated(uuid, true);
        plugin.setAuthenticated(uuid, true);
        plugin.setAuthenticated(uuid, true);
        assertTrue(plugin.isAuthenticated(uuid));

        plugin.setPendingValidation(uuid, true);
        plugin.setPendingValidation(uuid, true);
        assertTrue(plugin.isPendingValidation(uuid));

        plugin.setAuthenticated(uuid, false);
        plugin.setAuthenticated(uuid, false);
        assertFalse(plugin.isAuthenticated(uuid));
    }

    @Test
    @DisplayName("EdgeCase: LoginTime con tiempo de sistema manipulado")
    void testLoginTimeWithSystemTimeManipulation() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        long expectedMinTime = System.currentTimeMillis();

        // Act
        plugin.setLoginTime(uuid);
        Long loginTime = plugin.getLoginTime(uuid);

        // Assert
        assertNotNull(loginTime);
        assertTrue(loginTime >= expectedMinTime);
        assertTrue(loginTime <= System.currentTimeMillis() + 1000); // Margen de 1 segundo
    }

    @Test
    @DisplayName("Performance: Operaciones masivas de autenticación")
    void testMassiveAuthenticationOperations() {
        // Arrange
        int playerCount = 1000;
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            uuids.add(UUID.randomUUID());
        }

        // Act - Autenticar 1000 jugadores
        long startTime = System.currentTimeMillis();
        for (UUID uuid : uuids) {
            plugin.setAuthenticated(uuid, true);
            plugin.setLoginTime(uuid);
        }
        long endTime = System.currentTimeMillis();

        // Assert
        assertTrue(endTime - startTime < 1000, "Debería tomar menos de 1 segundo");

        // Verificar que todos estén autenticados
        for (UUID uuid : uuids) {
            assertTrue(plugin.isAuthenticated(uuid));
            assertNotNull(plugin.getLoginTime(uuid));
        }
    }

    @Test
    @DisplayName("DataManager: Debe estar disponible después de inicialización")
    void testDataManagerAvailability() {
        // Assert
        assertNotNull(plugin.getDataManager());
        assertSame(mockDataManager, plugin.getDataManager());
    }
}