package com.tesseractsoftwares.nexusbackend.sdkjava.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NexusConfig - Tests Completos")
@Tag("unit")
public class NexusConfigTest {

    private NexusConfig config;
    private final String VALID_URL = "https://localhost:3000/api/auth/";
    private final int VALID_TIMEOUT = 30000;

    @AfterEach
    void tearDown() {
        config = null;
    }

    @Nested
    @DisplayName("Tests de Construcción")
    class ConstructorTests {

        @Test
        @DisplayName("Debería crear configuración con valores válidos")
        void testCreateWithValidValues() {
            config = new NexusConfig(VALID_URL, VALID_TIMEOUT);

            assertNotNull(config);
            assertEquals(VALID_URL, config.getBaseUrl());
            assertEquals(VALID_TIMEOUT, config.getTimeoutMS());
        }

        @Test
        @DisplayName("Debería inicializar token como null")
        void testTokenInitializedAsNull() {
            config = new NexusConfig(VALID_URL, VALID_TIMEOUT);

            assertNull(config.getToken());
        }

        @Test
        @DisplayName("Debería aceptar diferentes URLs")
        void testDifferentUrls() {
            config = new NexusConfig("https://localhost:3000/api/auth/", VALID_TIMEOUT);
            assertEquals("https://localhost:3000/api/auth/", config.getBaseUrl());

            config = new NexusConfig("https://localhost:8080/api/auth/", VALID_TIMEOUT);
            assertEquals("https://localhost:8080/api/auth/", config.getBaseUrl());
        }

        @Test
        @DisplayName("Debería aceptar diferentes timeouts")
        void testDifferentTimeouts() {
            config = new NexusConfig(VALID_URL, 5000);
            assertEquals(5000, config.getTimeoutMS());

            config = new NexusConfig(VALID_URL, 60000);
            assertEquals(60000, config.getTimeoutMS());
        }
    }

    @Nested
    @DisplayName("Tests de Gestión de Token")
    class TokenManagementTests {

        @BeforeEach
        void setUp() {
            config = new NexusConfig(VALID_URL, VALID_TIMEOUT);
        }

        @Test
        @DisplayName("Debería establecer token correctamente")
        void testSetToken() {
            String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";

            config.setToken(token);

            assertEquals(token, config.getToken());
        }

        @Test
        @DisplayName("Debería obtener token establecido")
        void testGetToken() {
            String token = "test-token-123";
            config.setToken(token);

            String retrieved = config.getToken();

            assertEquals(token, retrieved);
        }

        @Test
        @DisplayName("Debería limpiar token correctamente")
        void testClearToken() {
            config.setToken("some-token");

            config.clearToken();

            assertNull(config.getToken());
        }

        @Test
        @DisplayName("Debería permitir establecer token null")
        void testSetNullToken() {
            config.setToken("initial-token");
            config.setToken(null);

            assertNull(config.getToken());
        }

        @Test
        @DisplayName("Debería permitir actualizar token existente")
        void testUpdateToken() {
            config.setToken("old-token");
            config.setToken("new-token");

            assertEquals("new-token", config.getToken());
        }
    }

    @Nested
    @DisplayName("Tests de Authorization Header")
    class AuthorizationHeaderTests {

        @BeforeEach
        void setUp() {
            config = new NexusConfig(VALID_URL, VALID_TIMEOUT);
        }

        @Test
        @DisplayName("Debería retornar null cuando no hay token")
        void testAuthHeaderWithoutToken() {
            String authHeader = config.getAuthorizationHeader();

            assertNull(authHeader);
        }

        @Test
        @DisplayName("Debería generar header Bearer cuando hay token")
        void testAuthHeaderWithToken() {
            String token = "abc123token";
            config.setToken(token);

            String authHeader = config.getAuthorizationHeader();

            assertNotNull(authHeader);
            assertEquals("Bearer abc123token", authHeader);
        }

        @Test
        @DisplayName("Debería incluir Bearer prefix en el header")
        void testBearerPrefix() {
            config.setToken("my-token");

            String authHeader = config.getAuthorizationHeader();

            assertTrue(authHeader.startsWith("Bearer "));
        }

        @Test
        @DisplayName("Debería retornar null después de limpiar token")
        void testAuthHeaderAfterClear() {
            config.setToken("token");
            config.clearToken();

            String authHeader = config.getAuthorizationHeader();

            assertNull(authHeader);
        }

        @Test
        @DisplayName("Debería actualizar header cuando cambia el token")
        void testAuthHeaderUpdatesWithToken() {
            config.setToken("token1");
            String header1 = config.getAuthorizationHeader();

            config.setToken("token2");
            String header2 = config.getAuthorizationHeader();

            assertEquals("Bearer token1", header1);
            assertEquals("Bearer token2", header2);
            assertNotEquals(header1, header2);
        }
    }

    @Nested
    @DisplayName("Tests de Getters")
    class GetterTests {

        @Test
        @DisplayName("getBaseUrl debería retornar la URL configurada")
        void testGetBaseUrl() {
            config = new NexusConfig("https://localhost:3000/api/auth/", VALID_TIMEOUT);

            assertEquals("https://localhost:3000/api/auth/", config.getBaseUrl());
        }

        @Test
        @DisplayName("getTimeoutMS debería retornar el timeout configurado")
        void testGetTimeoutMS() {
            config = new NexusConfig(VALID_URL, 15000);

            assertEquals(15000, config.getTimeoutMS());
        }

        @Test
        @DisplayName("getToken debería retornar null inicialmente")
        void testGetTokenInitial() {
            config = new NexusConfig(VALID_URL, VALID_TIMEOUT);

            assertNull(config.getToken());
        }
    }

    @Nested
    @DisplayName("Tests de Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Debería manejar accesos concurrentes a getters")
        void testConcurrentGetters() throws InterruptedException {
            config = new NexusConfig(VALID_URL, VALID_TIMEOUT);
            config.setToken("test-token");

            Thread[] threads = new Thread[10];
            for (int i = 0; i < 10; i++) {
                threads[i] = new Thread(() -> {
                    assertEquals(VALID_URL, config.getBaseUrl());
                    assertEquals(VALID_TIMEOUT, config.getTimeoutMS());
                    assertEquals("test-token", config.getToken());
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }
        }
    }

    @Nested
    @DisplayName("Tests de Casos Edge")
    class EdgeCaseTests {

        @Test
        @DisplayName("Debería manejar URL con barra final")
        void testUrlWithTrailingSlash() {
            config = new NexusConfig("https://localhost:3000/api/auth/", VALID_TIMEOUT);

            assertEquals("https://localhost:3000/api/auth/", config.getBaseUrl());
        }

        @Test
        @DisplayName("Debería manejar timeout muy pequeño")
        void testSmallTimeout() {
            config = new NexusConfig(VALID_URL, 1);

            assertEquals(1, config.getTimeoutMS());
        }

        @Test
        @DisplayName("Debería manejar timeout muy grande")
        void testLargeTimeout() {
            config = new NexusConfig(VALID_URL, 300000);

            assertEquals(300000, config.getTimeoutMS());
        }

        @Test
        @DisplayName("Debería manejar token vacío")
        void testEmptyToken() {
            config = new NexusConfig(VALID_URL, VALID_TIMEOUT);
            config.setToken("");

            assertEquals("", config.getToken());
            assertEquals("Bearer ", config.getAuthorizationHeader());
        }

        @Test
        @DisplayName("Debería manejar token muy largo")
        void testVeryLongToken() {
            config = new NexusConfig(VALID_URL, VALID_TIMEOUT);
            String longToken = "a".repeat(1000);

            config.setToken(longToken);

            assertEquals(longToken, config.getToken());
            assertTrue(config.getAuthorizationHeader().contains(longToken));
        }
    }
}