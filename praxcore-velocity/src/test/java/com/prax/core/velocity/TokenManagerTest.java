package com.prax.core.velocity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de TokenManager")
class TokenManagerTest {

    private TokenManager tokenManager;
    private UUID testUuid;

    @BeforeEach
    void setUp() {
        tokenManager = new TokenManager();
        testUuid = UUID.randomUUID();
    }

    @Test
    @DisplayName("Debería crear y almacenar token correctamente")
    void testCreateAndStoreToken() {
        // Act
        tokenManager.createAndStoreToken(testUuid);

        // Assert
        assertTrue(tokenManager.sessionExists(testUuid), "La sesión debería existir");
    }

    @Test
    @DisplayName("No debería crear token duplicado si ya existe sesión válida")
    void testDoNotCreateDuplicateToken() {
        // Arrange
        tokenManager.createAndStoreToken(testUuid);

        // Act - Intentar crear de nuevo
        tokenManager.createAndStoreToken(testUuid);

        // Assert
        assertTrue(tokenManager.sessionExists(testUuid));
        assertTrue(tokenManager.validateToken(testUuid));
    }

    @Test
    @DisplayName("Debería validar token correctamente")
    void testValidateToken() {
        // Arrange
        tokenManager.createAndStoreToken(testUuid);

        // Act
        boolean isValid = tokenManager.validateToken(testUuid);

        // Assert
        assertTrue(isValid, "El token debería ser válido");
    }

    @Test
    @DisplayName("Debería retornar false al validar token no existente")
    void testValidateNonExistentToken() {
        // Act
        boolean isValid = tokenManager.validateToken(testUuid);

        // Assert
        assertFalse(isValid, "No debería validar un token que no existe");
    }

    @Test
    @DisplayName("Debería verificar si sesión existe")
    void testSessionExists() {
        // Arrange
        tokenManager.createAndStoreToken(testUuid);

        // Act & Assert
        assertTrue(tokenManager.sessionExists(testUuid));
        assertFalse(tokenManager.sessionExists(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Debería remover sesión correctamente")
    void testRemoveSession() {
        // Arrange
        tokenManager.createAndStoreToken(testUuid);
        assertTrue(tokenManager.sessionExists(testUuid));

        // Act
        tokenManager.removeSession(testUuid);

        // Assert
        assertFalse(tokenManager.sessionExists(testUuid), "La sesión debería haber sido eliminada");
    }

    @Test
    @DisplayName("Debería manejar múltiples sesiones simultáneas")
    void testMultipleSessions() {
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
    @DisplayName("Remover una sesión no debe afectar otras sesiones")
    void testRemoveOneSessionDoesNotAffectOthers() {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tokenManager.createAndStoreToken(uuid1);
        tokenManager.createAndStoreToken(uuid2);

        // Act
        tokenManager.removeSession(uuid1);

        // Assert
        assertFalse(tokenManager.sessionExists(uuid1));
        assertTrue(tokenManager.sessionExists(uuid2));
    }

    @Test
    @DisplayName("Debería limpiar sesiones sin errores")
    void testCleanupExpiredSessions() {
        // Arrange
        tokenManager.createAndStoreToken(testUuid);

        // Act & Assert - No debería lanzar excepciones
        assertDoesNotThrow(() -> tokenManager.cleanupExpiredSessions());

        // El token recién creado no debería ser eliminado (no está expirado)
        assertTrue(tokenManager.sessionExists(testUuid));
    }
}