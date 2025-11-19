package com.tesseractsoftwares.nexusbackend.sdkjava.auth;

import com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto.LoginDto;
import com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto.RegisterDto;
import com.tesseractsoftwares.nexusbackend.sdkjava.config.NexusConfig;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.NexusHttpClient;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.exceptions.NexusHttpException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Tests Completos")
@Tag("unit")
public class AuthServiceTest {

    @Mock
    private NexusHttpClient mockHttpClient;

    @Mock
    private NexusConfig mockConfig;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(mockHttpClient, mockConfig);
    }

    @Nested
    @DisplayName("Tests de Register")
    class RegisterTests {

        @Test
        @DisplayName("Debería registrar usuario exitosamente")
        void testRegisterSuccess() throws NexusHttpException {
            RegisterDto dto = new RegisterDto(
                    "uuid-123",
                    "test@example.com",
                    "password123",
                    "1990-01-01",
                    "TestPlayer"
            );

            when(mockHttpClient.post(eq("auth/register"), anyString())).thenReturn("{}");

            assertDoesNotThrow(() -> authService.register(dto));

            verify(mockHttpClient, times(1)).post(eq("auth/register"), anyString());
        }

        @Test
        @DisplayName("Debería enviar datos correctos en el JSON")
        void testRegisterSendsCorrectData() throws NexusHttpException {
            RegisterDto dto = new RegisterDto(
                    "uuid-456",
                    "user@test.com",
                    "pass456",
                    "1995-05-15",
                    "Player123"
            );

            when(mockHttpClient.post(eq("auth/register"), anyString())).thenReturn("{}");

            authService.register(dto);

            verify(mockHttpClient).post(eq("auth/register"), contains("user@test.com"));
            verify(mockHttpClient).post(eq("auth/register"), contains("pass456"));
        }

        @Test
        @DisplayName("Debería propagar excepción si el registro falla")
        void testRegisterThrowsException() throws NexusHttpException {
            RegisterDto dto = new RegisterDto(
                    "uuid",
                    "test@test.com",
                    "pass",
                    "2000-01-01",
                    "Player"
            );

            when(mockHttpClient.post(eq("auth/register"), anyString()))
                    .thenThrow(new NexusHttpException("Registration failed", 400, "register", "error"));

            assertThrows(NexusHttpException.class, () -> authService.register(dto));
        }
    }

    @Nested
    @DisplayName("Tests de Login")
    class LoginTests {

        @Test
        @DisplayName("Debería hacer login exitosamente y retornar token")
        void testLoginSuccess() throws NexusHttpException {
            LoginDto dto = new LoginDto("user@example.com", "password123");
            String jsonResponse = "{\"token\":\"abc123token\"}";

            when(mockHttpClient.post(eq("auth/login"), anyString())).thenReturn(jsonResponse);
            doNothing().when(mockConfig).setToken(anyString());

            String token = authService.login(dto);

            assertEquals("abc123token", token);
            verify(mockHttpClient, times(1)).post(eq("auth/login"), anyString());
            verify(mockConfig, times(1)).setToken("abc123token");
        }

        @Test
        @DisplayName("Debería establecer token en config después de login")
        void testLoginSetsTokenInConfig() throws NexusHttpException {
            LoginDto dto = new LoginDto("test@test.com", "pass");
            String jsonResponse = "{\"token\":\"my-auth-token\"}";

            when(mockHttpClient.post(eq("auth/login"), anyString())).thenReturn(jsonResponse);
            doNothing().when(mockConfig).setToken(anyString());

            authService.login(dto);

            verify(mockConfig).setToken("my-auth-token");
        }

        @Test
        @DisplayName("Debería enviar credenciales en el body")
        void testLoginSendsCredentials() throws NexusHttpException {
            LoginDto dto = new LoginDto("email@test.com", "password456");
            String jsonResponse = "{\"token\":\"token\"}";

            when(mockHttpClient.post(eq("auth/login"), anyString())).thenReturn(jsonResponse);
            doNothing().when(mockConfig).setToken(anyString());

            authService.login(dto);

            verify(mockHttpClient).post(eq("auth/login"), contains("email@test.com"));
            verify(mockHttpClient).post(eq("auth/login"), contains("password456"));
        }

        @Test
        @DisplayName("Debería manejar respuesta sin token")
        void testLoginWithoutToken() throws NexusHttpException {
            LoginDto dto = new LoginDto("user@test.com", "pass");
            String jsonResponse = "{\"token\":null}";

            when(mockHttpClient.post(eq("auth/login"), anyString())).thenReturn(jsonResponse);

            String token = authService.login(dto);

            assertNull(token);
            verify(mockConfig, never()).setToken(anyString());
        }

        @Test
        @DisplayName("Debería propagar excepción si login falla")
        void testLoginThrowsException() throws NexusHttpException {
            LoginDto dto = new LoginDto("user@test.com", "wrongpass");

            when(mockHttpClient.post(eq("auth/login"), anyString()))
                    .thenThrow(new NexusHttpException("Invalid credentials", 401, "login", "error"));

            assertThrows(NexusHttpException.class, () -> authService.login(dto));
        }
    }

    @Nested
    @DisplayName("Tests de Validate")
    class ValidateTests {

        @Test
        @DisplayName("Debería retornar true cuando validación es exitosa")
        void testValidateSuccess() throws NexusHttpException {
            when(mockHttpClient.post(eq("auth/validate"), eq("{}"))).thenReturn("{\"success\":true}");

            boolean result = authService.validate();

            assertTrue(result);
            verify(mockHttpClient, times(1)).post("auth/validate", "{}");
        }

        @Test
        @DisplayName("Debería retornar true cuando respuesta contiene 'success'")
        void testValidateWithSuccessInResponse() throws NexusHttpException {
            when(mockHttpClient.post(eq("auth/validate"), eq("{}"))).thenReturn("success");

            boolean result = authService.validate();

            assertTrue(result);
        }

        @Test
        @DisplayName("Debería retornar true cuando respuesta contiene 'true'")
        void testValidateWithTrueInResponse() throws NexusHttpException {
            when(mockHttpClient.post(eq("auth/validate"), eq("{}"))).thenReturn("true");

            boolean result = authService.validate();

            assertTrue(result);
        }

        @Test
        @DisplayName("Debería retornar false cuando validación falla")
        void testValidateFails() throws NexusHttpException {
            when(mockHttpClient.post(eq("auth/validate"), eq("{}"))).thenReturn("invalid");

            boolean result = authService.validate();

            assertFalse(result);
        }

        @Test
        @DisplayName("Debería propagar excepción si request falla")
        void testValidateThrowsException() throws NexusHttpException {
            when(mockHttpClient.post(eq("auth/validate"), eq("{}")))
                    .thenThrow(new NexusHttpException("Validation failed", 401, "validate", "error"));

            assertThrows(NexusHttpException.class, () -> authService.validate());
        }
    }

    @Nested
    @DisplayName("Tests de Logout")
    class LogoutTests {

        @Test
        @DisplayName("Debería hacer logout exitosamente")
        void testLogoutSuccess() throws NexusHttpException {
            when(mockHttpClient.post(eq("auth/logout"), eq("{}"))).thenReturn("{}");
            doNothing().when(mockConfig).clearToken();

            assertDoesNotThrow(() -> authService.logout());

            verify(mockHttpClient, times(1)).post("auth/logout", "{}");
            verify(mockConfig, times(1)).clearToken();
        }

        @Test
        @DisplayName("Debería limpiar token después de logout")
        void testLogoutClearsToken() throws NexusHttpException {
            when(mockHttpClient.post(eq("auth/logout"), eq("{}"))).thenReturn("{}");
            doNothing().when(mockConfig).clearToken();

            authService.logout();

            verify(mockConfig).clearToken();
        }

        @Test
        @DisplayName("Debería propagar excepción si logout falla")
        void testLogoutThrowsException() throws NexusHttpException {
            when(mockHttpClient.post(eq("auth/logout"), eq("{}")))
                    .thenThrow(new NexusHttpException("Logout failed", 500, "logout", "error"));

            assertThrows(NexusHttpException.class, () -> authService.logout());
        }
    }

    @Nested
    @DisplayName("Tests de Refresh Token")
    class RefreshTokenTests {

        @Test
        @DisplayName("Debería refrescar token exitosamente")
        void testRefreshSuccess() throws NexusHttpException {
            String jsonResponse = "{\"token\":\"new-refreshed-token\"}";

            when(mockHttpClient.post(eq("auth/refresh"), eq("{}"))).thenReturn(jsonResponse);
            doNothing().when(mockConfig).setToken(anyString());

            String newToken = authService.refresh();

            assertEquals("new-refreshed-token", newToken);
            verify(mockHttpClient, times(1)).post("auth/refresh", "{}");
            verify(mockConfig, times(1)).setToken("new-refreshed-token");
        }

        @Test
        @DisplayName("Debería actualizar token en config después de refresh")
        void testRefreshUpdatesConfig() throws NexusHttpException {
            String jsonResponse = "{\"token\":\"updated-token\"}";

            when(mockHttpClient.post(eq("auth/refresh"), eq("{}"))).thenReturn(jsonResponse);
            doNothing().when(mockConfig).setToken(anyString());

            authService.refresh();

            verify(mockConfig).setToken("updated-token");
        }

        @Test
        @DisplayName("Debería manejar respuesta sin token en refresh")
        void testRefreshWithoutToken() throws NexusHttpException {
            String jsonResponse = "{\"token\":null}";

            when(mockHttpClient.post(eq("auth/refresh"), eq("{}"))).thenReturn(jsonResponse);

            String token = authService.refresh();

            assertNull(token);
            verify(mockConfig, never()).setToken(anyString());
        }

        @Test
        @DisplayName("Debería propagar excepción si refresh falla")
        void testRefreshThrowsException() throws NexusHttpException {
            when(mockHttpClient.post(eq("auth/refresh"), eq("{}")))
                    .thenThrow(new NexusHttpException("Refresh failed", 401, "refresh", "error"));

            assertThrows(NexusHttpException.class, () -> authService.refresh());
        }
    }

    @Nested
    @DisplayName("Tests de Integración de Flujo Completo")
    class IntegrationFlowTests {

        @Test
        @DisplayName("Flujo completo: Login -> Validate -> Logout")
        void testCompleteAuthFlow() throws NexusHttpException {
            LoginDto loginDto = new LoginDto("user@test.com", "password");
            when(mockHttpClient.post(eq("auth/login"), anyString()))
                    .thenReturn("{\"token\":\"auth-token\"}");
            when(mockHttpClient.post(eq("auth/validate"), eq("{}")))
                    .thenReturn("{\"success\":true}");
            when(mockHttpClient.post(eq("auth/logout"), eq("{}")))
                    .thenReturn("{}");
            doNothing().when(mockConfig).setToken(anyString());
            doNothing().when(mockConfig).clearToken();

            String token = authService.login(loginDto);
            boolean isValid = authService.validate();
            authService.logout();

            assertNotNull(token);
            assertTrue(isValid);
            verify(mockConfig).setToken("auth-token");
            verify(mockConfig).clearToken();
        }

        @Test
        @DisplayName("Flujo: Login -> Refresh -> Logout")
        void testLoginRefreshLogout() throws NexusHttpException {
            when(mockHttpClient.post(eq("auth/login"), anyString()))
                    .thenReturn("{\"token\":\"initial-token\"}");
            when(mockHttpClient.post(eq("auth/refresh"), eq("{}")))
                    .thenReturn("{\"token\":\"refreshed-token\"}");
            when(mockHttpClient.post(eq("auth/logout"), eq("{}")))
                    .thenReturn("{}");
            doNothing().when(mockConfig).setToken(anyString());
            doNothing().when(mockConfig).clearToken();

            authService.login(new LoginDto("user@test.com", "pass"));
            String newToken = authService.refresh();
            authService.logout();

            assertEquals("refreshed-token", newToken);
            verify(mockConfig).setToken("initial-token");
            verify(mockConfig).setToken("refreshed-token");
            verify(mockConfig).clearToken();
        }
    }
}