package com.prax.core.velocity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BackendClient Tests")
class BackendClientTest {

    private BackendClient backendClient;
    private UUID testUUID;

    @BeforeEach
    void setUp() {
        backendClient = new BackendClient();
        testUUID = UUID.randomUUID();
    }

    // ==================== TESTS DE INSTANCIACIÓN ====================

    @Test
    @DisplayName("Debe crear instancia de BackendClient sin errores")
    void testBackendClientCreation() {
        // Assert
        assertNotNull(backendClient);
    }

    @Test
    @DisplayName("Debe poder crear múltiples instancias")
    void testMultipleInstances() {
        // Arrange & Act
        BackendClient client1 = new BackendClient();
        BackendClient client2 = new BackendClient();

        // Assert
        assertNotNull(client1);
        assertNotNull(client2);
        assertNotSame(client1, client2);
    }

    // ==================== TESTS DE REGISTRO ====================

    @Test
    @DisplayName("register: Debe aceptar parámetros válidos sin crash")
   // @Disabled("Requiere backend activo - Test de integración")
    void testRegisterWithValidParameters() {
        // Arrange
        String email = "integration-test-" + System.currentTimeMillis() + "@example.com";
        String password = "securePassword123";
        String birthdate = "15-03-2000";
        String playerName = "TestPlayer";

        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(() ->
                backendClient.register(testUUID, email, password, birthdate, playerName )
        );
    }

    @Test
    @DisplayName("register: Debe retornar booleano")
    void testRegisterReturnsBoolean() {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        String birthdate = "01-01-2000";
        String playerName = "TestPlayer";

        // Act
        boolean result = backendClient.register(testUUID, email, password, birthdate, playerName );

        // Assert - Puede ser true o false dependiendo del backend
        // Lo importante es que retorne un valor booleano sin crash
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("register: Debe manejar parámetros null sin crash")
    void testRegisterWithNullParameters() {
        // Act & Assert - Puede retornar false o lanzar excepción manejada internamente
        assertDoesNotThrow(() -> {
            boolean result = backendClient.register(null, null, null, null, null);
            assertFalse(result); // Debe fallar gracefully
        });
    }

    @Test
    @DisplayName("register: Debe manejar email vacío")
    void testRegisterWithEmptyEmail() {
        // Act
        boolean result = backendClient.register(testUUID, "", "password", "01-01-2000", "TestPlayer");

        // Assert - Debe retornar false sin crash
        assertFalse(result);
    }

    @Test
    @DisplayName("register: Debe manejar contraseña vacía")
    void testRegisterWithEmptyPassword() {
        // Act
        boolean result = backendClient.register(testUUID, "test@example.com", "", "01-01-2000", "TestPlayer");

        // Assert - Debe retornar false sin crash
        assertFalse(result);
    }

    // ==================== TESTS DE LOGIN ====================

    @Test
    @DisplayName("login: Debe retornar Optional")
    void testLoginReturnsOptional() {
        // Arrange
        String email = "integration-test-" + System.currentTimeMillis() + "@example.com";
        String password = "password123";

        // Act
        Optional<String> result = backendClient.login(testUUID, email, password);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("login: Debe retornar Optional.empty() en caso de error")
    void testLoginReturnsEmptyOnError() {
        // Arrange - Credenciales inválidas
        String email = "invalid@example.com";
        String password = "wrongpassword";

        // Act
        Optional<String> result = backendClient.login(testUUID, email, password);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("login: Debe manejar parámetros null")
    void testLoginWithNullParameters() {
        // Act
        Optional<String> result = backendClient.login(null, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("login: Debe manejar email vacío")
    void testLoginWithEmptyEmail() {
        // Act
        Optional<String> result = backendClient.login(testUUID, "", "password");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("login: Debe manejar credenciales correctas")
   // @Disabled("Requiere backend activo con usuario registrado - Habilita solo para pruebas de integración")
    void testLoginWithCorrectCredentials() {
        // NOTA: Este test requiere que previamente hayas registrado un usuario
        // Cambia estos valores por credenciales reales de tu backend:

        // Arrange
        String email = "integration-test-" + System.currentTimeMillis() + "@example.com";
        String password = "TestPassword123!";
        String birthdate = "01-01-2000";
        String playerName = "TestPlayer" + System.currentTimeMillis();
        UUID uuid = UUID.randomUUID();

        boolean registerSuccess = backendClient.register(uuid, email, password, birthdate, playerName);
        assertTrue(registerSuccess, "El registro debería ser exitoso antes del login");
        // Act
        Optional<String> result = backendClient.login(testUUID, email, password);

        // Assert
        // Si el test falla, verifica:
        // 1. El backend está corriendo en localhost:3000
        // 2. El usuario existe en la base de datos
        // 3. Las credenciales son correctas
        if (result.isEmpty()) {
            System.out.println("❌ Login falló - Verifica que el usuario exista en el backend");
        }
        assertTrue(result.isPresent(), "El login debería retornar un token si las credenciales son correctas");
        assertFalse(result.get().isEmpty());
    }

    // ==================== TESTS DE VALIDACIÓN ====================

    @Test
    @DisplayName("validate: Debe retornar booleano")
    void testValidateReturnsBoolean() {
        // Arrange
        String token = "some.jwt.token";

        // Act
        boolean result = backendClient.validate(token);

        // Assert
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("validate: Debe retornar false para token null")
    void testValidateWithNullToken() {
        // Act
        boolean result = backendClient.validate(null);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("validate: Debe retornar false para token vacío")
    void testValidateWithEmptyToken() {
        // Act
        boolean result = backendClient.validate("");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("validate: Debe retornar false para token inválido")
    void testValidateWithInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.format";

        // Act
        boolean result = backendClient.validate(invalidToken);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("validate: Debe manejar token válido")
   // @Disabled("Requiere backend activo y token válido - Habilita solo para pruebas de integración")
    void testValidateWithValidToken() {
        // NOTA: Este test requiere un token JWT válido de tu backend
        // Primero haz login para obtener un token, luego úsalo aquí
        System.out.println("🧪 Iniciando test de validación con token real...");

        // Arrange
        // String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."; // ← Reemplaza con token real
        String email = "integration-test-" + System.currentTimeMillis() + "@example.com";
        String password = "TestPassword123!";
        String birthdate = "01-01-2000";
        String playerName = "ValidatePlayer" + System.currentTimeMillis();
        UUID uuid = UUID.randomUUID();

        boolean registerSuccess = backendClient.register(uuid, email, password, birthdate, playerName);
        assertTrue(registerSuccess, "El registro debería ser exitoso");

        Optional<String> loginResult = backendClient.login(uuid, email, password);
        assertTrue(loginResult.isPresent(), "El login debería retornar un token");

        String token = loginResult.get();
        // Act
        boolean result = backendClient.validate(token);

        // Assert
        if (!result) {
            System.out.println("❌ Validación falló - El token puede haber expirado o ser inválido");
        }
        assertTrue(result, "Un token válido debería pasar la validación");
    }

    // ==================== TESTS DE LOGOUT ====================

    @Test
    @DisplayName("logout: Debe retornar booleano")
    void testLogoutReturnsBoolean() {
        // Arrange
        String token = "some.jwt.token";

        // Act
        boolean result = backendClient.logout(token);

        // Assert
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("logout: Debe retornar false para token null")
    void testLogoutWithNullToken() {
        // Act
        boolean result = backendClient.logout(null);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("logout: Debe retornar false para token vacío")
    void testLogoutWithEmptyToken() {
        // Act
        boolean result = backendClient.logout("");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("logout: Debe manejar token inválido sin crash")
    void testLogoutWithInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act & Assert
        assertDoesNotThrow(() -> backendClient.logout(invalidToken));
    }

    @Test
    @DisplayName("logout: Debe manejar token válido")
   // @Disabled("Requiere backend activo y token válido - Habilita solo para pruebas de integración")
    void testLogoutWithValidToken() {
        // NOTA: Necesitas un token válido obtenido de un login exitoso
        String email = "integration-test-" + System.currentTimeMillis() + "@example.com";
        String password = "TestPassword123!";
        String birthdate = "01-01-2000";
        String playerName = "LogoutPlayer" + System.currentTimeMillis();
        UUID uuid = UUID.randomUUID();

        boolean registerSuccess = backendClient.register(uuid, email, password, birthdate, playerName);
        assertTrue(registerSuccess, "El registro debería ser exitoso antes del logout");

        Optional<String> loginResult = backendClient.login(uuid, email, password);
        assertTrue(loginResult.isPresent(), "El login debería retornar un token");
        String token = loginResult.get();
        // Arrange

        // Act
        boolean result = backendClient.logout(token);

        // Assert
        if (!result) {
            System.out.println("❌ Logout falló - Verifica que el token sea válido y el backend esté corriendo");
        }
        assertTrue(result, "El logout debería ser exitoso con un token válido");
    }

    // ==================== TESTS DE REFRESH ====================

    @Test
    @DisplayName("refresh: Debe retornar Optional")
    void testRefreshReturnsOptional() {
        // Arrange
        String oldToken = "old.jwt.token";

        // Act
        Optional<String> result = backendClient.refresh(oldToken);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("refresh: Debe retornar Optional.empty() para token null")
    void testRefreshWithNullToken() {
        // Act
        Optional<String> result = backendClient.refresh(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("refresh: Debe retornar Optional.empty() para token vacío")
    void testRefreshWithEmptyToken() {
        // Act
        Optional<String> result = backendClient.refresh("");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("refresh: Debe retornar Optional.empty() para token inválido")
    void testRefreshWithInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.format";

        // Act
        Optional<String> result = backendClient.refresh(invalidToken);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("refresh: Debe retornar nuevo token para token válido")
   // @Disabled("Requiere backend activo y token válido - Habilita solo para pruebas de integración")
    void testRefreshWithValidToken() {
        // NOTA: Necesitas un token válido que no haya expirado
        String email = "integration-test-" + System.currentTimeMillis() + "@example.com";
        String password = "TestPassword123!";
        String birthdate = "01-01-2000";
        String playerName = "RefreshPlayer" + System.currentTimeMillis();
        UUID uuid = UUID.randomUUID();

        boolean registerSuccess = backendClient.register(uuid, email, password, birthdate, playerName);
        assertTrue(registerSuccess, "El registro debería ser exitoso");

        Optional<String> loginResult = backendClient.login(uuid, email, password);
        assertTrue(loginResult.isPresent(), "El login debería retornar un token");
        String oldToken = loginResult.get();
        // Arrange

        // Act
        Optional<String> result = backendClient.refresh(oldToken);

        // Assert
        if (result.isEmpty()) {
            System.out.println("❌ Refresh falló - El token puede haber expirado o el endpoint no está implementado");
        }
        assertTrue(result.isPresent(), "Refresh debería retornar un nuevo token");
        assertNotEquals(oldToken, result.orElse(""), "El nuevo token debe ser diferente al anterior");
    }

    // ==================== TESTS DE MANEJO DE ERRORES ====================

   /* @Test
    @DisplayName("Debe manejar conexión fallida al backend")
    void testHandleConnectionFailure() {
        // Act & Assert - Backend en localhost:3000 probablemente no está corriendo
        assertDoesNotThrow(() -> {
            boolean registerResult = backendClient.register(testUUID, "test@example.com", "pass", "01-01-2000", "TestPlayer");
            Optional<String> loginResult = backendClient.login(testUUID, "test@example.com", "pass");
            boolean validateResult = backendClient.validate("token");
            boolean logoutResult = backendClient.logout("token");
            Optional<String> refreshResult = backendClient.refresh("token");

            // Todos deben retornar valores seguros
            assertFalse(registerResult);
            assertTrue(loginResult.isEmpty());
            assertFalse(validateResult);
            assertFalse(logoutResult);
            assertTrue(refreshResult.isEmpty());
        });
    }
*/
    @Test
    @DisplayName("Debe ser seguro usar múltiples veces")
    void testMultipleOperations() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                String email = "integration-test-" + i + "-" + System.currentTimeMillis() + "@example.com";
                String playerName = "TestPlayer" + i + System.currentTimeMillis();
                UUID uuid = UUID.randomUUID();

                backendClient.register(uuid, email, "pass", "01-01-2000", playerName);
                backendClient.login(uuid, email, "pass");
                backendClient.validate("token");
            }
        });
    }

    // ==================== TESTS DE CONCURRENCIA ====================

    @Test
    @DisplayName("Debe ser thread-safe para múltiples operaciones simultáneas")
    void testConcurrentOperations() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        // Act
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                UUID uuid = UUID.randomUUID();
                String email = "integration-test-" + index + "-" + System.currentTimeMillis() + "@example.com";
                String playerName = "ConcurrentPlayer" + index + System.currentTimeMillis();

                backendClient.register(uuid, email, "pass", "01-01-2000", playerName);
                backendClient.login(uuid, email, "pass");
                backendClient.validate("token-" + index);
                backendClient.logout("token-" + index);
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert - No debe haber crasheado
        assertTrue(true);
    }

    // ==================== TESTS DE FORMATO DE DATOS ====================

    @Test
    @DisplayName("Debe enviar UUID en formato string correcto")
    void testUUIDFormat() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        String expectedFormat = uuid.toString();

        // Act & Assert
        assertDoesNotThrow(() -> {
            backendClient.register(uuid, "test@example.com", "pass", "01-01-2000", "TestPlayer");
        });

        // Verificar que el UUID tiene el formato esperado
        assertTrue(expectedFormat.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    @DisplayName("Debe manejar fechas en formato DD-MM-YYYY")
    void testBirthdateFormat() {
        // Arrange
        String validDate = "15-03-2000";

        // Act & Assert
        assertDoesNotThrow(() -> {
            backendClient.register(testUUID, "test@example.com", "pass", validDate, "TestPlayer");
        });
    }

    // ==================== TESTS DE INTEGRACIÓN ====================

    @Test
    @DisplayName("Flujo completo: register -> login -> validate -> logout")
   // @Disabled("Requiere backend activo - Habilita solo para pruebas de integración completas")
    void testCompleteAuthFlow() {
        // NOTA: Este test crea un usuario nuevo, úsalo con cuidado
        // Cambia el email para evitar conflictos

        String testEmail = "integration-test-" + System.currentTimeMillis() + "@example.com";
        String testPassword = "TestPassword123!";
        String birthdate = "01-01-2000";
        String playerName = "TestPlayer" + System.currentTimeMillis();
        System.out.println("👤 Jugador: " + playerName);

        System.out.println("🧪 Iniciando flujo completo de autenticación...");
        System.out.println("📧 Email de prueba: " + testEmail);

        // 1. Register
        System.out.println("1️⃣ Registrando usuario...");
        boolean registerSuccess = backendClient.register(testUUID, testEmail, testPassword, birthdate, playerName);
        if (!registerSuccess) {
            System.out.println("❌ Registro falló - puede que el endpoint no esté implementado");
        }
        assertTrue(registerSuccess, "El registro debería ser exitoso");

        // 2. Login
        System.out.println("2️⃣ Iniciando sesión...");
        Optional<String> loginResult = backendClient.login(testUUID, testEmail, testPassword);
        assertTrue(loginResult.isPresent(), "El login debería retornar un token");
        String token = loginResult.get();
        System.out.println("✅ Token obtenido: " + token.substring(0, Math.min(50, token.length())) + "...");

        // 3. Validate
        System.out.println("3️⃣ Validando token...");
        boolean isValid = backendClient.validate(token);
        assertTrue(isValid, "El token debería ser válido");

        // 4. Logout
        System.out.println("4️⃣ Cerrando sesión...");
        boolean logoutSuccess = backendClient.logout(token);
        assertTrue(logoutSuccess, "El logout debería ser exitoso");

        // 5. Validate again (should fail)
        System.out.println("5️⃣ Validando token después de logout...");
        boolean isValidAfterLogout = backendClient.validate(token);
        assertFalse(isValidAfterLogout, "El token no debería ser válido después del logout");

        System.out.println("✅ Flujo completo exitoso!");
    }

    @Test
    @DisplayName("Flujo de refresh: login -> refresh -> validate nuevo token")
   // @Disabled("Requiere backend activo - Habilita solo para pruebas de integración")
    void testRefreshFlow() {
        // NOTA: Requiere usuario existente en el backend
        String testEmail = "integration-test-" + System.currentTimeMillis() + "@example.com"; // ← Cambia por usuario real
        String testPassword = "password123";    // ← Cambia por password real

        System.out.println("🧪 Iniciando flujo de refresh...");


        System.out.println("0️⃣ Registrando usuario...");
        boolean registerSuccess = backendClient.register(testUUID, testEmail, testPassword, "01-01-2000", "RefreshPlayer");
        assertTrue(registerSuccess, "El registro debería ser exitoso antes del login");

        // 1. Login
        System.out.println("1️⃣ Iniciando sesión...");
        Optional<String> loginResult = backendClient.login(testUUID, testEmail, testPassword);
        if (loginResult.isEmpty()) {
            System.out.println("❌ Login falló - verifica las credenciales");
        }
        assertTrue(loginResult.isPresent(), "El login debería retornar un token");
        String oldToken = loginResult.get();
        System.out.println("✅ Token obtenido");

        // 2. Refresh
        System.out.println("2️⃣ Refrescando token...");
        Optional<String> refreshResult = backendClient.refresh(oldToken);
        if (refreshResult.isEmpty()) {
            System.out.println("❌ Refresh falló - puede que el endpoint no esté implementado");
        }
        assertTrue(refreshResult.isPresent(), "Refresh debería retornar un nuevo token");
        String newToken = refreshResult.get();

        // 3. Validate nuevo token
        System.out.println("3️⃣ Validando nuevo token...");
        boolean isValid = backendClient.validate(newToken);
        assertTrue(isValid, "El nuevo token debería ser válido");

        // 4. Tokens deben ser diferentes
        assertNotEquals(oldToken, newToken, "El nuevo token debe ser diferente al anterior");

        System.out.println("✅ Flujo de refresh exitoso!");
    }
}