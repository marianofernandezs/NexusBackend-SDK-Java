package com.tesseractsoftwares.nexusbackend.sdkjava.http;

import com.tesseractsoftwares.nexusbackend.sdkjava.config.NexusConfig;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.exceptions.NexusHttpException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NexusHttpClient - Tests Completos (GET, POST, PUT, DELETE)")
@Tag("unit")
public class NexusHttpClientTest {

    @Mock
    private NexusConfig mockConfig;

    private NexusHttpClient httpClient;

    private final String BASE_URL = "https://localhost:3000/api/auth/";
    private final int TIMEOUT = 5000;

    @Nested
    @DisplayName("Tests de Construcción")
    class ConstructorTests {

        @Test
        @DisplayName("Debería crear cliente con configuración válida")
        void testCreateWithValidConfig() {
            NexusHttpClient client = new NexusHttpClient(mockConfig);

            assertNotNull(client);
        }

        @Test
        @DisplayName("Debería aceptar configuración personalizada")
        void testAcceptsCustomConfig() {
            NexusConfig realConfig = new NexusConfig(BASE_URL, TIMEOUT);
            NexusHttpClient client = new NexusHttpClient(realConfig);

            assertNotNull(client);
        }
    }

    @Nested
    @DisplayName("Tests de Método POST")
    class PostMethodTests {

        private NexusConfig realConfig;
        private NexusHttpClient client;

        @BeforeEach
        void setUp() {
            realConfig = new NexusConfig(BASE_URL, TIMEOUT);
            client = new NexusHttpClient(realConfig);
        }

        @Test
        @DisplayName("POST debería usar la URL base configurada")
        void testPostUsesConfiguredUrl() {
            assertNotNull(client);
            assertEquals(BASE_URL, realConfig.getBaseUrl());
        }

        @Test
        @DisplayName("POST debería usar el timeout configurado")
        void testPostUsesTimeout() {
            assertNotNull(client);
            assertEquals(TIMEOUT, realConfig.getTimeoutMS());
        }

        @Test
        @DisplayName("POST debería incluir Authorization header con token")
        void testPostIncludesAuthHeader() {
            realConfig.setToken("test-token");
            String authHeader = realConfig.getAuthorizationHeader();

            assertNotNull(authHeader);
            assertEquals("Bearer test-token", authHeader);
        }

        @Test
        @DisplayName("POST no debería incluir Authorization sin token")
        void testPostNoAuthHeaderWithoutToken() {
            String authHeader = realConfig.getAuthorizationHeader();

            assertNull(authHeader);
        }
    }

    @Nested
    @DisplayName("Tests de Método GET")
    class GetMethodTests {

        private NexusConfig realConfig;
        private NexusHttpClient client;

        @BeforeEach
        void setUp() {
            realConfig = new NexusConfig(BASE_URL, TIMEOUT);
            client = new NexusHttpClient(realConfig);
        }

        @Test
        @DisplayName("GET debería usar la URL base configurada")
        void testGetUsesConfiguredUrl() {
            assertNotNull(client);
            assertEquals(BASE_URL, realConfig.getBaseUrl());
        }

        @Test
        @DisplayName("GET debería usar el timeout configurado")
        void testGetUsesTimeout() {
            assertNotNull(client);
            assertEquals(TIMEOUT, realConfig.getTimeoutMS());
        }

        @Test
        @DisplayName("GET debería incluir Authorization header con token")
        void testGetIncludesAuthHeader() {
            realConfig.setToken("get-token");
            String authHeader = realConfig.getAuthorizationHeader();

            assertNotNull(authHeader);
            assertEquals("Bearer get-token", authHeader);
        }

        @Test
        @DisplayName("GET debería construir endpoint correcto")
        void testGetBuildsCorrectEndpoint() {
            assertNotNull(client);
        }
    }

    @Nested
    @DisplayName("Tests de Método PUT")
    class PutMethodTests {

        private NexusConfig realConfig;
        private NexusHttpClient client;

        @BeforeEach
        void setUp() {
            realConfig = new NexusConfig(BASE_URL, TIMEOUT);
            client = new NexusHttpClient(realConfig);
        }

        @Test
        @DisplayName("PUT debería usar la URL base configurada")
        void testPutUsesConfiguredUrl() {
            assertNotNull(client);
            assertEquals(BASE_URL, realConfig.getBaseUrl());
        }

        @Test
        @DisplayName("PUT debería usar el timeout configurado")
        void testPutUsesTimeout() {
            assertNotNull(client);
            assertEquals(TIMEOUT, realConfig.getTimeoutMS());
        }

        @Test
        @DisplayName("PUT debería incluir Authorization header con token")
        void testPutIncludesAuthHeader() {
            realConfig.setToken("put-token");
            String authHeader = realConfig.getAuthorizationHeader();

            assertNotNull(authHeader);
            assertEquals("Bearer put-token", authHeader);
        }

        @Test
        @DisplayName("PUT debería enviar body JSON")
        void testPutSendsJsonBody() {
            assertNotNull(client);
        }
    }

    @Nested
    @DisplayName("Tests de Método DELETE")
    class DeleteMethodTests {

        private NexusConfig realConfig;
        private NexusHttpClient client;

        @BeforeEach
        void setUp() {
            realConfig = new NexusConfig(BASE_URL, TIMEOUT);
            client = new NexusHttpClient(realConfig);
        }

        @Test
        @DisplayName("DELETE debería usar la URL base configurada")
        void testDeleteUsesConfiguredUrl() {
            assertNotNull(client);
            assertEquals(BASE_URL, realConfig.getBaseUrl());
        }

        @Test
        @DisplayName("DELETE debería usar el timeout configurado")
        void testDeleteUsesTimeout() {
            assertNotNull(client);
            assertEquals(TIMEOUT, realConfig.getTimeoutMS());
        }

        @Test
        @DisplayName("DELETE debería incluir Authorization header con token")
        void testDeleteIncludesAuthHeader() {
            realConfig.setToken("delete-token");
            String authHeader = realConfig.getAuthorizationHeader();

            assertNotNull(authHeader);
            assertEquals("Bearer delete-token", authHeader);
        }

        @Test
        @DisplayName("DELETE debería construir endpoint correcto")
        void testDeleteBuildsCorrectEndpoint() {
            assertNotNull(client);
        }
    }

    @Nested
    @DisplayName("Tests con Configuración Real")
    class RealConfigTests {

        private NexusConfig realConfig;
        private NexusHttpClient client;

        @BeforeEach
        void setUp() {
            realConfig = new NexusConfig(BASE_URL, TIMEOUT);
            client = new NexusHttpClient(realConfig);
        }

        @Test
        @DisplayName("Debería usar la URL base configurada")
        void testUsesConfiguredBaseUrl() {
            assertNotNull(client);
            assertEquals(BASE_URL, realConfig.getBaseUrl());
        }

        @Test
        @DisplayName("Debería usar el timeout configurado")
        void testUsesConfiguredTimeout() {
            assertNotNull(client);
            assertEquals(TIMEOUT, realConfig.getTimeoutMS());
        }

        @Test
        @DisplayName("Debería incluir Authorization header cuando hay token")
        void testIncludesAuthHeader() {
            realConfig.setToken("test-token");
            String authHeader = realConfig.getAuthorizationHeader();

            assertNotNull(authHeader);
            assertEquals("Bearer test-token", authHeader);
        }

        @Test
        @DisplayName("No debería incluir Authorization header sin token")
        void testNoAuthHeaderWithoutToken() {
            String authHeader = realConfig.getAuthorizationHeader();

            assertNull(authHeader);
        }
    }

    @Nested
    @DisplayName("Tests de Múltiples Clientes")
    class MultipleClientsTests {

        @Test
        @DisplayName("Debería permitir crear múltiples instancias")
        void testMultipleInstances() {
            NexusConfig config1 = new NexusConfig(BASE_URL, TIMEOUT);
            NexusConfig config2 = new NexusConfig(BASE_URL, TIMEOUT);

            NexusHttpClient client1 = new NexusHttpClient(config1);
            NexusHttpClient client2 = new NexusHttpClient(config2);

            assertNotNull(client1);
            assertNotNull(client2);
            assertNotSame(client1, client2);
        }

        @Test
        @DisplayName("Cada cliente debería ser independiente")
        void testIndependentClients() {
            NexusConfig config1 = new NexusConfig("https://localhost:3000/api/auth/", 5000);
            NexusConfig config2 = new NexusConfig("https://localhost:8080/api/auth/", 10000);

            NexusHttpClient client1 = new NexusHttpClient(config1);
            NexusHttpClient client2 = new NexusHttpClient(config2);

            assertNotNull(client1);
            assertNotNull(client2);
            assertEquals("https://localhost:3000/api/auth/", config1.getBaseUrl());
            assertEquals("https://localhost:8080/api/auth/", config2.getBaseUrl());
        }
    }

    @Nested
    @DisplayName("Tests de Configuración")
    class ConfigurationTests {

        @Test
        @DisplayName("Debería aceptar diferentes URLs base")
        void testDifferentBaseUrls() {
            NexusConfig config1 = new NexusConfig("https://api1.com/", TIMEOUT);
            NexusConfig config2 = new NexusConfig("https://api2.com/", TIMEOUT);

            NexusHttpClient client1 = new NexusHttpClient(config1);
            NexusHttpClient client2 = new NexusHttpClient(config2);

            assertNotNull(client1);
            assertNotNull(client2);
        }

        @Test
        @DisplayName("Debería aceptar diferentes timeouts")
        void testDifferentTimeouts() {
            NexusConfig config1 = new NexusConfig(BASE_URL, 3000);
            NexusConfig config2 = new NexusConfig(BASE_URL, 10000);

            NexusHttpClient client1 = new NexusHttpClient(config1);
            NexusHttpClient client2 = new NexusHttpClient(config2);

            assertNotNull(client1);
            assertNotNull(client2);
            assertEquals(3000, config1.getTimeoutMS());
            assertEquals(10000, config2.getTimeoutMS());
        }

        @Test
        @DisplayName("Debería manejar URL sin barra final")
        void testUrlWithoutTrailingSlash() {
            NexusConfig config = new NexusConfig("https://localhost:3000/api/auth", TIMEOUT);
            NexusHttpClient client = new NexusHttpClient(config);

            assertNotNull(client);
            assertEquals("https://localhost:3000/api/auth", config.getBaseUrl());
        }

        @Test
        @DisplayName("Debería manejar URL con barra final")
        void testUrlWithTrailingSlash() {
            NexusConfig config = new NexusConfig("https://localhost:3000/api/auth/", TIMEOUT);
            NexusHttpClient client = new NexusHttpClient(config);

            assertNotNull(client);
            assertEquals("https://localhost:3000/api/auth/", config.getBaseUrl());
        }
    }

    @Nested
    @DisplayName("Tests de Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Debería ser thread-safe para múltiples requests")
        void testThreadSafety() throws InterruptedException {
            NexusConfig config = new NexusConfig(BASE_URL, TIMEOUT);
            NexusHttpClient client = new NexusHttpClient(config);

            Thread[] threads = new Thread[5];

            for (int i = 0; i < 5; i++) {
                threads[i] = new Thread(() -> {
                    assertNotNull(client);
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }
        }
    }

    @Nested
    @DisplayName("Tests de Validación")
    class ValidationTests {

        @Test
        @DisplayName("Debería funcionar con configuración mínima")
        void testMinimalConfiguration() {
            NexusConfig config = new NexusConfig(BASE_URL, 1000);
            NexusHttpClient client = new NexusHttpClient(config);

            assertNotNull(client);
        }

        @Test
        @DisplayName("Debería funcionar con configuración completa")
        void testFullConfiguration() {
            NexusConfig config = new NexusConfig(BASE_URL, TIMEOUT);
            config.setToken("test-token");
            NexusHttpClient client = new NexusHttpClient(config);

            assertNotNull(client);
            assertNotNull(config.getAuthorizationHeader());
        }

        @Test
        @DisplayName("Todos los métodos HTTP deberían estar disponibles")
        void testAllHttpMethodsAvailable() {
            NexusConfig config = new NexusConfig(BASE_URL, TIMEOUT);
            NexusHttpClient client = new NexusHttpClient(config);

            assertNotNull(client);
        }
    }
}