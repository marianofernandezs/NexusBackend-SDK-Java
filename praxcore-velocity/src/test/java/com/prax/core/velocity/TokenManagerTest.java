package com.prax.core.velocity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenManager Tests")
class TokenManagerTest {

    private TokenManager tokenManager;
    private UUID testUUID;

    @BeforeEach
    void setUp() {
        tokenManager = new TokenManager();
        testUUID = UUID.randomUUID();
    }

    // ==================== TESTS DE CREACIÓN DE SESIÓN ====================

    @Test
    @DisplayName("Debe crear y almacenar token para nuevo jugador")
    void testCreateAndStoreTokenNewPlayer() {
        // Arrange & Act
        tokenManager.createAndStoreToken(testUUID);

        // Assert
        assertTrue(tokenManager.sessionExists(testUUID));
        assertTrue(tokenManager.validateToken(testUUID));
        assertNotNull(tokenManager.getToken(testUUID));
    }

    @Test
    @DisplayName("No debe crear token duplicado si sesión ya existe y es válida")
    void testCreateAndStoreTokenExistingValidSession() {
        // Arrange
        tokenManager.createAndStoreToken(testUUID);
        String firstToken = tokenManager.getToken(testUUID);

        // Act
        tokenManager.createAndStoreToken(testUUID);
        String secondToken = tokenManager.getToken(testUUID);

        // Assert
        assertEquals(firstToken, secondToken);
        assertTrue(tokenManager.validateToken(testUUID));
    }

    @Test
    @DisplayName("Debe crear token con formato JWT válido")
    void testTokenFormatIsValid() {
        // Arrange & Act
        tokenManager.createAndStoreToken(testUUID);
        String token = tokenManager.getToken(testUUID);

        // Assert
        assertNotNull(token);
        // JWT tiene 3 partes separadas por puntos
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "Token JWT debe tener 3 partes");
    }

    // ==================== TESTS DE VALIDACIÓN ====================

    @Test
    @DisplayName("Debe validar token existente y válido")
    void testValidateTokenSuccess() {
        // Arrange
        tokenManager.createAndStoreToken(testUUID);

        // Act
        boolean isValid = tokenManager.validateToken(testUUID);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Debe rechazar validación para UUID sin sesión")
    void testValidateTokenNoSession() {
        // Arrange
        UUID unknownUUID = UUID.randomUUID();

        // Act
        boolean isValid = tokenManager.validateToken(unknownUUID);

        // Assert
        assertFalse(isValid);
        assertFalse(tokenManager.sessionExists(unknownUUID));
    }

    @Test
    @DisplayName("Debe rechazar token inválido o corrupto")
    void testValidateTokenCorrupted() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        tokenManager.storeToken(uuid, "invalid.token.here");

        // Act
        boolean isValid = tokenManager.validateToken(uuid);

        // Assert
        assertFalse(isValid);
        assertFalse(tokenManager.sessionExists(uuid)); // Debe eliminarse
    }

    // ==================== TESTS DE ALMACENAMIENTO ====================

    @Test
    @DisplayName("storeToken debe guardar token correctamente")
    void testStoreToken() {
        // Arrange
        String customToken = "eyJhbGciOiJIUzI1NiJ9.test.signature";

        // Act
        tokenManager.storeToken(testUUID, customToken);

        // Assert
        assertTrue(tokenManager.sessionExists(testUUID));
        assertEquals(customToken, tokenManager.getToken(testUUID));
    }

    @Test
    @DisplayName("getToken debe retornar null para UUID sin sesión")
    void testGetTokenNoSession() {
        // Arrange
        UUID unknownUUID = UUID.randomUUID();

        // Act
        String token = tokenManager.getToken(unknownUUID);

        // Assert
        assertNull(token);
    }

    @Test
    @DisplayName("Debe sobrescribir token existente con storeToken")
    void testStoreTokenOverwrite() {
        // Arrange
        String firstToken = "first.token.here";
        String secondToken = "second.token.here";
        tokenManager.storeToken(testUUID, firstToken);

        // Act
        tokenManager.storeToken(testUUID, secondToken);

        // Assert
        assertEquals(secondToken, tokenManager.getToken(testUUID));
        assertNotEquals(firstToken, tokenManager.getToken(testUUID));
    }

    // ==================== TESTS DE ELIMINACIÓN ====================

    @Test
    @DisplayName("removeSession debe eliminar sesión existente")
    void testRemoveSession() {
        // Arrange
        tokenManager.createAndStoreToken(testUUID);
        assertTrue(tokenManager.sessionExists(testUUID));

        // Act
        tokenManager.removeSession(testUUID);

        // Assert
        assertFalse(tokenManager.sessionExists(testUUID));
        assertNull(tokenManager.getToken(testUUID));
    }

    @Test
    @DisplayName("removeSession debe ser seguro con UUID inexistente")
    void testRemoveSessionNonExistent() {
        // Arrange
        UUID unknownUUID = UUID.randomUUID();

        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(() -> tokenManager.removeSession(unknownUUID));
    }

    @Test
    @DisplayName("removeSession debe eliminar completamente la sesión")
    void testRemoveSessionComplete() {
        // Arrange
        tokenManager.createAndStoreToken(testUUID);

        // Act
        tokenManager.removeSession(testUUID);

        // Assert
        assertFalse(tokenManager.sessionExists(testUUID));
        assertFalse(tokenManager.validateToken(testUUID));
        assertNull(tokenManager.getToken(testUUID));
    }

    // ==================== TESTS DE EXISTENCIA ====================

    @Test
    @DisplayName("sessionExists debe retornar true para sesión activa")
    void testSessionExistsTrue() {
        // Arrange
        tokenManager.createAndStoreToken(testUUID);

        // Act & Assert
        assertTrue(tokenManager.sessionExists(testUUID));
    }

    @Test
    @DisplayName("sessionExists debe retornar false para UUID nuevo")
    void testSessionExistsFalse() {
        // Arrange
        UUID unknownUUID = UUID.randomUUID();

        // Act & Assert
        assertFalse(tokenManager.sessionExists(unknownUUID));
    }

    @Test
    @DisplayName("sessionExists debe retornar false después de removeSession")
    void testSessionExistsAfterRemoval() {
        // Arrange
        tokenManager.createAndStoreToken(testUUID);
        assertTrue(tokenManager.sessionExists(testUUID));

        // Act
        tokenManager.removeSession(testUUID);

        // Assert
        assertFalse(tokenManager.sessionExists(testUUID));
    }

    // ==================== TESTS DE LIMPIEZA ====================

    @Test
    @DisplayName("cleanupExpiredSessions debe ejecutarse sin errores")
    void testCleanupExpiredSessionsNoError() {
        // Arrange
        tokenManager.createAndStoreToken(testUUID);
        tokenManager.createAndStoreToken(UUID.randomUUID());

        // Act & Assert
        assertDoesNotThrow(() -> tokenManager.cleanupExpiredSessions());
    }

    @Test
    @DisplayName("cleanupExpiredSessions debe mantener tokens válidos")
    void testCleanupExpiredSessionsKeepsValid() {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tokenManager.createAndStoreToken(uuid1);
        tokenManager.createAndStoreToken(uuid2);

        // Act
        tokenManager.cleanupExpiredSessions();

        // Assert - Tokens recién creados deben seguir existiendo
        assertTrue(tokenManager.sessionExists(uuid1));
        assertTrue(tokenManager.sessionExists(uuid2));
    }

    @Test
    @DisplayName("cleanupExpiredSessions debe eliminar tokens corruptos")
    void testCleanupExpiredSessionsRemovesCorrupted() {
        // Arrange
        UUID validUUID = UUID.randomUUID();
        UUID corruptedUUID = UUID.randomUUID();

        tokenManager.createAndStoreToken(validUUID);
        tokenManager.storeToken(corruptedUUID, "corrupted.token.invalid");

        // Act
        tokenManager.cleanupExpiredSessions();

        // Assert
        assertTrue(tokenManager.sessionExists(validUUID));
        assertFalse(tokenManager.sessionExists(corruptedUUID));
    }

    // ==================== TESTS DE MÚLTIPLES SESIONES ====================

    @Test
    @DisplayName("Debe manejar múltiples sesiones simultáneas")
    void testMultipleSimultaneousSessions() {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        // Act
        tokenManager.createAndStoreToken(uuid1);
        tokenManager.createAndStoreToken(uuid2);
        tokenManager.createAndStoreToken(uuid3);

        // Assert
        assertTrue(tokenManager.sessionExists(uuid1));
        assertTrue(tokenManager.sessionExists(uuid2));
        assertTrue(tokenManager.sessionExists(uuid3));
        assertTrue(tokenManager.validateToken(uuid1));
        assertTrue(tokenManager.validateToken(uuid2));
        assertTrue(tokenManager.validateToken(uuid3));
    }

    @Test
    @DisplayName("Debe mantener sesiones independientes")
    void testIndependentSessions() {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tokenManager.createAndStoreToken(uuid1);
        tokenManager.createAndStoreToken(uuid2);

        // Act
        tokenManager.removeSession(uuid1);

        // Assert
        assertFalse(tokenManager.sessionExists(uuid1));
        assertTrue(tokenManager.sessionExists(uuid2)); // No debe afectarse
    }

    @Test
    @DisplayName("Cada UUID debe tener su propio token único")
    void testUniqueTokensPerUUID() {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        // Act
        tokenManager.createAndStoreToken(uuid1);
        tokenManager.createAndStoreToken(uuid2);

        String token1 = tokenManager.getToken(uuid1);
        String token2 = tokenManager.getToken(uuid2);

        // Assert
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    // ==================== TESTS DE INTEGRACIÓN ====================

    @Test
    @DisplayName("Flujo completo: crear, validar, eliminar")
    void testCompleteFlow() {
        // 1. Crear sesión
        tokenManager.createAndStoreToken(testUUID);
        assertTrue(tokenManager.sessionExists(testUUID));

        // 2. Validar sesión
        assertTrue(tokenManager.validateToken(testUUID));

        // 3. Obtener token
        String token = tokenManager.getToken(testUUID);
        assertNotNull(token);

        // 4. Eliminar sesión
        tokenManager.removeSession(testUUID);
        assertFalse(tokenManager.sessionExists(testUUID));
        assertFalse(tokenManager.validateToken(testUUID));
    }

    @Test
    @DisplayName("Debe manejar recreación de sesión después de eliminación")
    void testSessionRecreation() throws InterruptedException {
        // Arrange - Crear y eliminar
        tokenManager.createAndStoreToken(testUUID);
        String firstToken = tokenManager.getToken(testUUID);
        tokenManager.removeSession(testUUID);

        // Esperar un momento para asegurar timestamp diferente
        Thread.sleep(100);

        // Act - Recrear
        tokenManager.createAndStoreToken(testUUID);
        String secondToken = tokenManager.getToken(testUUID);

        // Assert
        assertNotNull(secondToken);
        // Los tokens pueden ser iguales si se generan en el mismo segundo
        // Lo importante es que la sesión se pueda recrear
        assertTrue(tokenManager.validateToken(testUUID));
        assertTrue(tokenManager.sessionExists(testUUID));
    }

    // ==================== TESTS DE CASOS EDGE ====================

    @Test
    @DisplayName("Debe manejar UUID null en getToken de forma segura")
    void testGetTokenWithNull() {
        // Act & Assert - ConcurrentHashMap no permite null keys
        // El método debería manejar esto o lanzar NPE
        assertThrows(NullPointerException.class, () -> {
            tokenManager.getToken(null);
        });
    }

    @Test
    @DisplayName("Debe manejar llamadas repetidas a removeSession")
    void testMultipleRemoveCalls() {
        // Arrange
        tokenManager.createAndStoreToken(testUUID);

        // Act & Assert
        tokenManager.removeSession(testUUID);
        assertDoesNotThrow(() -> tokenManager.removeSession(testUUID));
        assertDoesNotThrow(() -> tokenManager.removeSession(testUUID));
    }

    @Test
    @DisplayName("Debe manejar validación múltiple del mismo token")
    void testMultipleValidations() {
        // Arrange
        tokenManager.createAndStoreToken(testUUID);

        // Act & Assert
        assertTrue(tokenManager.validateToken(testUUID));
        assertTrue(tokenManager.validateToken(testUUID));
        assertTrue(tokenManager.validateToken(testUUID));
        assertTrue(tokenManager.sessionExists(testUUID));
    }
}