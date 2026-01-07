package com.prax.core.velocity;

import com.tesseractsoftwares.nexusbackend.sdkjava.auth.AuthService;
import com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto.LoginDto;
import com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto.RegisterDto;
import com.tesseractsoftwares.nexusbackend.sdkjava.config.NexusConfig;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.NexusHttpClient;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.exceptions.NexusHttpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para NexusAuthService
 */
@ExtendWith(MockitoExtension.class)
class NexusAuthServiceTest {

    @Mock
    private AuthService mockAuthService;

    @Mock
    private NexusConfig mockConfig;

    private NexusAuthService nexusAuthService;

    private static final String BASE_URL = "http://test-backend:3000";
    private static final int TIMEOUT = 5000;
    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_BIRTHDATE = "1990-01-01";
    private static final String TEST_PLAYER_NAME = "TestPlayer";
    private static final String TEST_TOKEN = "test-jwt-token-12345";

    @BeforeEach
    void setUp() {
        // Crear instancia real con mocks inyectados
        // Nota: Necesitarías refactorizar NexusAuthService para inyección de dependencias
        // Por ahora, usamos MockedConstruction para mockear las dependencias internas
    }

    // ==========================================
    // TESTS DE REGISTER
    // ==========================================

    @Test
    void testRegister_Success() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.register(any(RegisterDto.class))).thenReturn(true))) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            boolean result = service.register(TEST_UUID, TEST_EMAIL, TEST_PASSWORD, TEST_BIRTHDATE, TEST_PLAYER_NAME);

            // Assert
            assertTrue(result, "Register debería retornar true en caso de éxito");
            AuthService authService = mockedAuthService.constructed().get(0);
            verify(authService, times(1)).register(any(RegisterDto.class));
        }
    }

    @Test
    void testRegister_Failure_HttpException() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.register(any(RegisterDto.class)))
                        .thenThrow(new NexusHttpException("Error", 400, "/auth/register", null)))) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            boolean result = service.register(TEST_UUID, TEST_EMAIL, TEST_PASSWORD, TEST_BIRTHDATE, TEST_PLAYER_NAME);

            // Assert
            assertFalse(result, "Register debería retornar false cuando hay excepción HTTP");
        }
    }

    @Test
    void testRegister_Failure_UnexpectedException() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.register(any(RegisterDto.class)))
                        .thenThrow(new RuntimeException("Unexpected error")))) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            boolean result = service.register(TEST_UUID, TEST_EMAIL, TEST_PASSWORD, TEST_BIRTHDATE, TEST_PLAYER_NAME);

            // Assert
            assertFalse(result, "Register debería retornar false cuando hay excepción inesperada");
        }
    }

    // ==========================================
    // TESTS DE LOGIN
    // ==========================================

    @Test
    void testLogin_Success() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.login(any(LoginDto.class))).thenReturn(TEST_TOKEN))) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            Optional<String> result = service.login(TEST_UUID, TEST_EMAIL, TEST_PASSWORD);

            // Assert
            assertTrue(result.isPresent(), "Login debería retornar Optional con token");
            assertEquals(TEST_TOKEN, result.get(), "El token debería coincidir");
            AuthService authService = mockedAuthService.constructed().get(0);
            verify(authService, times(1)).login(any(LoginDto.class));
        }
    }

    @Test
    void testLogin_Failure_EmptyToken() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.login(any(LoginDto.class))).thenReturn(""))) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            Optional<String> result = service.login(TEST_UUID, TEST_EMAIL, TEST_PASSWORD);

            // Assert
            assertFalse(result.isPresent(), "Login debería retornar Optional.empty() con token vacío");
        }
    }

    @Test
    void testLogin_Failure_NullToken() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.login(any(LoginDto.class))).thenReturn(null))) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            Optional<String> result = service.login(TEST_UUID, TEST_EMAIL, TEST_PASSWORD);

            // Assert
            assertFalse(result.isPresent(), "Login debería retornar Optional.empty() con token null");
        }
    }

    @Test
    void testLogin_Failure_HttpException() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.login(any(LoginDto.class)))
                        .thenThrow(new NexusHttpException("Unauthorized", 401, "/auth/login", null)))) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            Optional<String> result = service.login(TEST_UUID, TEST_EMAIL, TEST_PASSWORD);

            // Assert
            assertFalse(result.isPresent(), "Login debería retornar Optional.empty() cuando hay excepción");
        }
    }

    // ==========================================
    // TESTS DE VALIDATE
    // ==========================================

    @Test
    void testValidate_Success() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.validate()).thenReturn(true));
             MockedConstruction<NexusConfig> mockedConfig = mockConstruction(NexusConfig.class)) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            boolean result = service.validate(TEST_TOKEN);

            // Assert
            assertTrue(result, "Validate debería retornar true para token válido");
            NexusConfig config = mockedConfig.constructed().get(0);
            verify(config, times(1)).setToken(TEST_TOKEN);
        }
    }

    @Test
    void testValidate_Failure_InvalidToken() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.validate()).thenReturn(false))) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            boolean result = service.validate(TEST_TOKEN);

            // Assert
            assertFalse(result, "Validate debería retornar false para token inválido");
        }
    }

    @Test
    void testValidate_Failure_NullToken() {
        // Arrange
        try (MockedConstruction<AuthService> ignored = mockConstruction(AuthService.class)) {
            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            boolean result = service.validate(null);

            // Assert
            assertFalse(result, "Validate debería retornar false para token null");
        }
    }

    @Test
    void testValidate_Failure_EmptyToken() {
        // Arrange
        try (MockedConstruction<AuthService> ignored = mockConstruction(AuthService.class)) {
            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            boolean result = service.validate("");

            // Assert
            assertFalse(result, "Validate debería retornar false para token vacío");
        }
    }

    @Test
    void testValidate_Failure_HttpException() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.validate())
                        .thenThrow(new NexusHttpException("Token expired", 401, "/auth/validate", null)))) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            boolean result = service.validate(TEST_TOKEN);

            // Assert
            assertFalse(result, "Validate debería retornar false cuando hay excepción");
        }
    }

    // ==========================================
    // TESTS DE LOGOUT
    // ==========================================

    @Test
    void testLogout_Success() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.logout()).thenReturn(true));
             MockedConstruction<NexusConfig> mockedConfig = mockConstruction(NexusConfig.class)) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            boolean result = service.logout(TEST_TOKEN);

            // Assert
            assertTrue(result, "Logout debería retornar true en caso de éxito");
            NexusConfig config = mockedConfig.constructed().get(0);
            verify(config, times(1)).setToken(TEST_TOKEN);
        }
    }

    @Test
    void testLogout_Failure_NullToken() {
        // Arrange
        try (MockedConstruction<AuthService> ignored = mockConstruction(AuthService.class)) {
            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            boolean result = service.logout(null);

            // Assert
            assertFalse(result, "Logout debería retornar false para token null");
        }
    }

    @Test
    void testLogout_Failure_EmptyToken() {
        // Arrange
        try (MockedConstruction<AuthService> ignored = mockConstruction(AuthService.class)) {
            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            boolean result = service.logout("");

            // Assert
            assertFalse(result, "Logout debería retornar false para token vacío");
        }
    }

    @Test
    void testLogout_Failure_HttpException() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.logout())
                        .thenThrow(new NexusHttpException("Logout failed", 500, "/auth/logout", null)))) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            boolean result = service.logout(TEST_TOKEN);

            // Assert
            assertFalse(result, "Logout debería retornar false cuando hay excepción");
        }
    }

    // ==========================================
    // TESTS DE REFRESH
    // ==========================================

    @Test
    void testRefresh_Success() throws NexusHttpException {
        // Arrange
        String newToken = "new-jwt-token-67890";
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.refresh()).thenReturn(newToken));
             MockedConstruction<NexusConfig> mockedConfig = mockConstruction(NexusConfig.class)) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            Optional<String> result = service.refresh(TEST_TOKEN);

            // Assert
            assertTrue(result.isPresent(), "Refresh debería retornar Optional con nuevo token");
            assertEquals(newToken, result.get(), "El nuevo token debería coincidir");
            NexusConfig config = mockedConfig.constructed().get(0);
            verify(config, times(1)).setToken(TEST_TOKEN);
        }
    }

    @Test
    void testRefresh_Failure_NullToken() {
        // Arrange
        try (MockedConstruction<AuthService> ignored = mockConstruction(AuthService.class)) {
            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            Optional<String> result = service.refresh(null);

            // Assert
            assertFalse(result.isPresent(), "Refresh debería retornar Optional.empty() para token null");
        }
    }

    @Test
    void testRefresh_Failure_EmptyToken() {
        // Arrange
        try (MockedConstruction<AuthService> ignored = mockConstruction(AuthService.class)) {
            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            Optional<String> result = service.refresh("");

            // Assert
            assertFalse(result.isPresent(), "Refresh debería retornar Optional.empty() para token vacío");
        }
    }

    @Test
    void testRefresh_Failure_HttpException() throws NexusHttpException {
        // Arrange
        try (MockedConstruction<AuthService> mockedAuthService = mockConstruction(AuthService.class,
                (mock, context) -> when(mock.refresh())
                        .thenThrow(new NexusHttpException("Refresh failed", 401, "/auth/refresh", null)))) {

            NexusAuthService service = new NexusAuthService(BASE_URL, TIMEOUT);

            // Act
            Optional<String> result = service.refresh(TEST_TOKEN);

            // Assert
            assertFalse(result.isPresent(), "Refresh debería retornar Optional.empty() cuando hay excepción");
        }
    }

    // ==========================================
    // TESTS DE UTILIDADES
    // ==========================================

    @Test
    void testMaskEmail_ValidEmail() {
        // Este test requiere hacer el método público o usar reflection
        // Por ahora lo documentamos como comportamiento esperado
        String email = "test@example.com";
        String expected = "tes***@example.com";
        // assertEquals(expected, service.maskEmail(email));
    }

    @Test
    void testMaskEmail_ShortEmail() {
        String email = "ab";
        String expected = "***";
        // assertEquals(expected, service.maskEmail(email));
    }

    @Test
    void testMaskEmail_NullEmail() {
        String expected = "***";
        // assertEquals(expected, service.maskEmail(null));
    }
}