package com.prax.core.velocity.integration;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.prax.core.velocity.NexusAuthService;
import com.prax.core.velocity.PluginMessageHandler;
import com.prax.core.velocity.PraxProxyPlugin;
import com.prax.core.velocity.TokenManager;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test de integración completo para el flujo de autenticación usando el SDK NexusAuthService.
 *
 * Flujo principal:
 *  1. Paper envía RegisterRequest → Velocity (PluginMessageHandler) → Backend vía NexusAuthService
 *  2. Paper envía CreateSession (Login) → Velocity → Backend → TokenManager guarda token
 *  3. Paper envía ValidateToken → Velocity valida con Backend usando el token del TokenManager
 *  4. Paper envía LogoutSession → Velocity → Backend → TokenManager elimina sesión
 *
 * ⚠ Requiere que el backend esté corriendo en TEST_BACKEND_URL.
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthenticationFlowIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFlowIntegrationTest.class);

    // ==============================
    // CONFIGURACIÓN BACKEND
    // ==============================

    /**
     * URL del backend de pruebas.
     * Se puede overridear con la env var BACKEND_URL si se desea.
     */
    private static final String TEST_BACKEND_URL =
            System.getenv().getOrDefault("BACKEND_URL", "http://localhost:3000/api/");

    /**
     * Timeout en milisegundos para las llamadas HTTP del SDK.
     */
    private static final int TIMEOUT_MS = 10_000;

    // ==============================
    // DATOS DE PRUEBA
    // ==============================

    private static UUID TEST_UUID = UUID.randomUUID();

    @BeforeEach
    void regenerateTestUUID() {
        TEST_UUID = UUID.randomUUID();
    }


    /**
     * Email de prueba único por corrida de suite, pero compartido entre tests.
     * Evita problemas de UNIQUE en la BD al ejecutar varias veces la suite.
     */
    private String TEST_EMAIL;

    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String TEST_BIRTHDATE = "01-01-1990";
    private static final String TEST_PLAYER_NAME = "IntegrationTestPlayer";

    private static final MinecraftChannelIdentifier PRAX_CHANNEL =
            MinecraftChannelIdentifier.from("prax:core");

    // ==============================
    // MOCKS DE VELOCITY
    // ==============================

    @Mock
    private PraxProxyPlugin mockPlugin;

    @Mock
    private Player mockPlayer;

    @Mock
    private ServerConnection mockServerConnection;

    @Mock
    private RegisteredServer mockRegisteredServer;

    @Mock
    private PluginMessageEvent mockEvent;

    // ==============================
    // OBJETOS REALES
    // ==============================

    private NexusAuthService authService;
    private TokenManager tokenManager;
    private PluginMessageHandler handler;

    private String currentToken;

    // ==============================
    // SETUP / TEARDOWN
    // ==============================

    @BeforeEach
    void setUp() {

        TEST_EMAIL = "test-"
                + UUID.randomUUID().toString().replace("-", "")
                + "-"
                + System.nanoTime()
                + "@praxsuite.test";

        logger.info("=".repeat(80));
        logger.info("Iniciando test de integración con backend: {}", TEST_BACKEND_URL);
        logger.info("Email de prueba: {}", TEST_EMAIL);
        logger.info("=".repeat(80));


        // Configurar mocks básicos
        lenient().when(mockPlugin.getLogger()).thenReturn(logger);

        lenient().when(mockPlayer.getUniqueId()).thenReturn(TEST_UUID);
        lenient().when(mockPlayer.getUsername()).thenReturn(TEST_PLAYER_NAME);

        lenient().when(mockServerConnection.getPlayer()).thenReturn(mockPlayer);
        lenient().when(mockPlayer.getCurrentServer()).thenReturn(Optional.of(mockServerConnection));
        lenient().when(mockServerConnection.getServer()).thenReturn(mockRegisteredServer);

        // Inicializar servicios reales
        authService = new NexusAuthService(TEST_BACKEND_URL, TIMEOUT_MS);
        tokenManager = new TokenManager();

        when(mockPlugin.getAuthService()).thenReturn(authService);
        when(mockPlugin.getTokenManager()).thenReturn(tokenManager);

        handler = new PluginMessageHandler(mockPlugin);

        logger.info("✓ Servicios NexusAuthService y TokenManager inicializados");
    }

    @AfterEach
    void tearDown() {
        logger.info("Iniciando limpieza de sesión de prueba...");

        try {
            if (currentToken != null) {
                logger.info("Intentando logout explícito en backend...");
                authService.logout(currentToken);
            }
        } catch (Exception e) {
            logger.warn("Error al hacer logout de limpieza: {}", e.getMessage());
        }

        if (tokenManager.sessionExists(TEST_UUID)) {
            logger.info("Removiendo sesión en TokenManager para UUID {}", TEST_UUID);
            tokenManager.removeSession(TEST_UUID);
        }

        currentToken = null;

        logger.info("Limpieza finalizada");
        logger.info("=".repeat(80));
        logger.info("");
    }

    // ==============================
    // TESTS DE FLUJO COMPLETO
    // ==============================

    @Test
    @Order(1)
    @Tag("integration")
    @DisplayName("Flujo completo: Register → Login → Validate → Logout")
    void testCompleteAuthenticationFlow() throws InterruptedException {
        logger.info("\n" + "=".repeat(80));
        logger.info("TEST #1: Flujo completo de autenticación");
        logger.info("=".repeat(80));

        // ===== PASO 1: REGISTRO =====
        logger.info("\n--- PASO 1: Register ---");
        boolean registrationSuccess = executeRegistration();
        assertTrue(registrationSuccess, "El registro debería ser exitoso");
        logger.info("✓ Registro completado exitosamente para {}", TEST_EMAIL);

        // Pequeño delay para asegurarnos que el backend persiste la data
        TimeUnit.MILLISECONDS.sleep(500);

        // ===== PASO 2: LOGIN =====
        logger.info("\n--- PASO 2: Login ---");
        boolean loginSuccess = executeLogin();
        assertTrue(loginSuccess, "El login debería ser exitoso");
        assertNotNull(currentToken, "El token debería estar presente después del login");
        assertTrue(tokenManager.sessionExists(TEST_UUID), "Debería existir sesión en TokenManager");
        logger.info("✓ Login completado exitosamente. Token (primeros 20): {}",
                currentToken.substring(0, Math.min(20, currentToken.length())));

        TimeUnit.MILLISECONDS.sleep(500);

        // ===== PASO 3: VALIDACIÓN =====
        logger.info("\n--- PASO 3: ValidateToken ---");
        boolean validationSuccess = executeValidation();
        assertTrue(validationSuccess, "La validación debería ser exitosa");
        logger.info("✓ Validación del token completada exitosamente");

        TimeUnit.MILLISECONDS.sleep(500);

        // ===== PASO 4: LOGOUT =====
        logger.info("\n--- PASO 4: LogoutSession ---");
        boolean logoutSuccess = executeLogout();
        assertTrue(logoutSuccess, "El logout debería ser exitoso");
        assertFalse(tokenManager.sessionExists(TEST_UUID), "No debería existir sesión después del logout");
        logger.info("✓ Logout completado exitosamente");

        logger.info("\n" + "=".repeat(80));
        logger.info("✓✓✓ FLUJO COMPLETO EJECUTADO EXITOSAMENTE ✓✓✓");
        logger.info("=".repeat(80));
    }

    @Test
    @Order(2)
    @Tag("integration")
    @DisplayName("Login con credenciales inválidas debe fallar y no crear sesión")
    void testLoginWithInvalidCredentials() {
        logger.info("\n--- TEST #2: Login con credenciales inválidas ---");

        // Preparar mensaje de CreateSession con password incorrecta
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("CreateSession");
        out.writeUTF(TEST_UUID.toString());
        out.writeUTF(TEST_EMAIL);
        out.writeUTF("WrongPassword123!");

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());

        clearInvocations(mockServerConnection);

        // Ejecutar handler
        handler.onPluginMessage(mockEvent);

        // No debería haberse creado sesión
        assertFalse(tokenManager.sessionExists(TEST_UUID),
                "No debería existir sesión en TokenManager para credenciales inválidas");

        // Capturar respuesta enviada a Paper
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, atLeastOnce())
                .sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        byte[] response = getLastMessage(messageCaptor);
        ByteArrayDataInput in = ByteStreams.newDataInput(response);
        String responseType = in.readUTF();
        String uuid = in.readUTF();
        boolean success = in.readBoolean();

        assertEquals("ValidationResponse", responseType, "Para CreateSession se espera ValidationResponse");
        assertEquals(TEST_UUID.toString(), uuid);
        assertFalse(success, "El login con credenciales inválidas debe ser rechazado");

        logger.info("✓ Login inválido rechazado correctamente, no se creó sesión");
    }

    @Test
    @Order(3)
    @Tag("integration")
    @DisplayName("Validación sin sesión previa debe fallar")
    void testValidationWithoutSession() {
        logger.info("\n--- TEST #3: ValidateToken sin sesión previa ---");

        // Asegurarse que no haya sesión
        tokenManager.removeSession(TEST_UUID);

        // Preparar mensaje de ValidateToken
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidateToken");
        out.writeUTF(TEST_UUID.toString());

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());

        clearInvocations(mockServerConnection);

        // Ejecutar
        handler.onPluginMessage(mockEvent);

        // Capturar respuesta
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, atLeastOnce())
                .sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        byte[] response = getLastMessage(messageCaptor);
        ByteArrayDataInput in = ByteStreams.newDataInput(response);

        String responseType = in.readUTF();
        String uuid = in.readUTF();
        boolean success = in.readBoolean();

        assertEquals("ValidationResponse", responseType);
        assertEquals(TEST_UUID.toString(), uuid);
        assertFalse(success, "La validación sin sesión debe fallar");

        logger.info("✓ Validación sin sesión rechazada correctamente");
    }

    @Test
    @Order(4)
    @Tag("integration")
    @DisplayName("Múltiples validaciones consecutivas deberían funcionar con la misma sesión")
    void testMultipleValidations() throws InterruptedException {
        logger.info("\n--- TEST #4: Múltiples ValidateToken consecutivos ---");

        // Registrar y loguear primero
        logger.info("Preparando sesión: Register + Login...");
        boolean registrationSuccess = executeRegistration();
        assertTrue(registrationSuccess, "El registro previo debería ser exitoso");

        TimeUnit.MILLISECONDS.sleep(400);

        boolean loginSuccess = executeLogin();
        assertTrue(loginSuccess, "El login previo debería ser exitoso");
        assertTrue(tokenManager.sessionExists(TEST_UUID), "Debe existir sesión antes de validar múltiples veces");

        TimeUnit.MILLISECONDS.sleep(400);

        // Ejecutar varias validaciones seguidas
        for (int i = 1; i <= 3; i++) {
            logger.info("→ Validación #{}", i);
            boolean validationSuccess = executeValidation();
            assertTrue(validationSuccess, "La validación #" + i + " debería ser exitosa");
            TimeUnit.MILLISECONDS.sleep(200);
        }

        logger.info("✓ Todas las validaciones consecutivas fueron exitosas");

        // Cleanup explícito
        boolean logoutSuccess = executeLogout();
        assertTrue(logoutSuccess, "El logout después de validaciones múltiples debería ser exitoso");
    }

    // ==============================
    // TEST DE CONECTIVIDAD BÁSICA
    // ==============================

    @Test
    @Tag("connectivity")
    @DisplayName("Conectividad básica: el backend debe responder al registro vía SDK")
    void testBackendConnectivity() {
        logger.info("\n--- TEST: Conectividad básica con backend usando NexusAuthService.register ---");

        UUID randomUuid = UUID.randomUUID();
        String email = "connectivity-" + System.currentTimeMillis() + "@praxsuite.test";

        try {
            boolean result = authService.register(
                    randomUuid,
                    email,
                    TEST_PASSWORD,
                    TEST_BIRTHDATE,
                    "ConnectivityTestPlayer"
            );

            logger.info("Backend respondió al registro de conectividad con resultado: {}", result);
            // No afirmamos true/false estrictamente, solo que no lance excepción.
        } catch (Exception e) {
            fail("Backend no está disponible o no responde correctamente: " + e.getMessage());
        }
    }

    // ==============================
    // MÉTODOS AUXILIARES
    // ==============================

    /**
     * Ejecuta el flujo de registro mediante PluginMessageHandler como si viniera de Paper.
     */
    private boolean executeRegistration() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("RegisterRequest");
        out.writeUTF(TEST_UUID.toString());
        out.writeUTF(TEST_EMAIL);
        out.writeUTF(TEST_PASSWORD);
        out.writeUTF(TEST_BIRTHDATE);
        out.writeUTF(TEST_PLAYER_NAME);

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());

        clearInvocations(mockServerConnection);

        handler.onPluginMessage(mockEvent);

        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, atLeastOnce())
                .sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        byte[] response = getLastMessage(messageCaptor);
        ByteArrayDataInput in = ByteStreams.newDataInput(response);

        String responseType = in.readUTF(); // "RegisterResponse"
        String uuid = in.readUTF();
        boolean success = in.readBoolean();

        logger.info("🔍 DEBUG - Response Type: {}", responseType);
        logger.info("🔍 DEBUG - UUID: {}", uuid);
        logger.info("🔍 DEBUG - Success: {}", success);

        if (!success) {
            logger.error("❌ REGISTRO FALLÓ para email: {}", TEST_EMAIL);
            logger.error("❌ UUID usado: {}", TEST_UUID);
            // Esto te ayudará a ver si es problema de email duplicado
        }

        assertEquals("RegisterResponse", responseType, "Se esperaba RegisterResponse");
        assertEquals(TEST_UUID.toString(), uuid, "El UUID de respuesta debe coincidir");

        return success;
    }

    /**
     * Ejecuta el flujo de login (CreateSession).
     * Si el login es exitoso, actualiza currentToken desde el TokenManager.
     */
    private boolean executeLogin() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("CreateSession");
        out.writeUTF(TEST_UUID.toString());
        out.writeUTF(TEST_EMAIL);
        out.writeUTF(TEST_PASSWORD);

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());

        clearInvocations(mockServerConnection);

        handler.onPluginMessage(mockEvent);

        // Después del handler, si el login es exitoso, el TokenManager debería tener el token
        if (tokenManager.sessionExists(TEST_UUID)) {
            currentToken = tokenManager.getToken(TEST_UUID);
        }

        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, atLeastOnce())
                .sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        byte[] response = getLastMessage(messageCaptor);
        ByteArrayDataInput in = ByteStreams.newDataInput(response);

        String responseType = in.readUTF(); // "ValidationResponse" para CreateSession
        String uuid = in.readUTF();
        boolean success = in.readBoolean();

        assertEquals("ValidationResponse", responseType, "Para login se espera ValidationResponse");
        assertEquals(TEST_UUID.toString(), uuid, "El UUID de respuesta debe coincidir");

        return success;
    }

    /**
     * Ejecuta el flujo de ValidateToken usando el TokenManager.
     */
    private boolean executeValidation() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidateToken");
        out.writeUTF(TEST_UUID.toString());

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());

        clearInvocations(mockServerConnection);

        handler.onPluginMessage(mockEvent);

        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, atLeastOnce())
                .sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        byte[] response = getLastMessage(messageCaptor);
        ByteArrayDataInput in = ByteStreams.newDataInput(response);

        String responseType = in.readUTF(); // "ValidationResponse"
        String uuid = in.readUTF();
        boolean success = in.readBoolean();

        assertEquals("ValidationResponse", responseType, "Para ValidateToken se espera ValidationResponse");
        assertEquals(TEST_UUID.toString(), uuid, "El UUID de respuesta debe coincidir");

        return success;
    }

    /**
     * Ejecuta el flujo de LogoutSession.
     */
    private boolean executeLogout() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("LogoutSession");
        out.writeUTF(TEST_UUID.toString());

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());

        clearInvocations(mockServerConnection);

        handler.onPluginMessage(mockEvent);

        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, atLeastOnce())
                .sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        byte[] response = getLastMessage(messageCaptor);
        ByteArrayDataInput in = ByteStreams.newDataInput(response);

        String responseType = in.readUTF(); // "LogoutResponse"
        String uuid = in.readUTF();
        boolean success = in.readBoolean();

        assertEquals("LogoutResponse", responseType, "Para LogoutSession se espera LogoutResponse");
        assertEquals(TEST_UUID.toString(), uuid, "El UUID de respuesta debe coincidir");

        return success;
    }

    /**
     * Devuelve el último mensaje enviado por el mock de ServerConnection.
     * Es útil si el handler envía más de un mensaje en algunas ramas.
     */
    private byte[] getLastMessage(ArgumentCaptor<byte[]> captor) {
        List<byte[]> all = captor.getAllValues();
        if (all.isEmpty()) {
            fail("No se capturó ningún mensaje de plugin");
        }
        return all.get(all.size() - 1);
    }
}
