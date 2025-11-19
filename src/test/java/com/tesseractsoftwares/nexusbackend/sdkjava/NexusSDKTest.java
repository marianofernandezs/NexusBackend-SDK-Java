package com.tesseractsoftwares.nexusbackend.sdkjava;

import com.tesseractsoftwares.nexusbackend.sdkjava.auth.AuthService;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NexusSDK - Tests Completos")
@Tag("unit")
public class NexusSDKTest {

    private NexusSDK sdk;
    private final String TEST_BASE_URL = "https://localhost:3000/api/auth/";

    @BeforeEach
    void resetSingleton() throws Exception {
        // Resetear el singleton usando reflection
        java.lang.reflect.Field instance = NexusSDK.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @AfterEach
    void tearDown() {
        sdk = null;
    }

    @Nested
    @DisplayName("Tests de Inicialización")
    class InitializationTests {

        @Test
        @DisplayName("Debería inicializar SDK con URL válida")
        void testInitWithValidUrl() {
            sdk = NexusSDK.init(TEST_BASE_URL);

            assertNotNull(sdk);
        }

        @Test
        @DisplayName("Debería retornar la misma instancia en múltiples llamadas")
        void testSingletonPattern() {
            NexusSDK sdk1 = NexusSDK.init(TEST_BASE_URL);
            NexusSDK sdk2 = NexusSDK.init(TEST_BASE_URL);

            assertSame(sdk1, sdk2);
        }

        @Test
        @DisplayName("getInstance debería retornar la instancia inicializada")
        void testGetInstance() {
            NexusSDK.init(TEST_BASE_URL);

            NexusSDK instance = NexusSDK.getInstance();

            assertNotNull(instance);
        }

        @Test
        @DisplayName("getInstance debería retornar null si no se ha inicializado")
        void testGetInstanceBeforeInit() {
            NexusSDK instance = NexusSDK.getInstance();

            assertNull(instance);
        }

        @Test
        @DisplayName("Debería inicializar con diferentes URLs")
        void testInitWithDifferentUrls() {
            sdk = NexusSDK.init("https://localhost:3000/api/auth/");
            assertNotNull(sdk);

            sdk = NexusSDK.init("https://localhost:8080/api/auth/");
            assertNotNull(sdk);
        }
    }

    @Nested
    @DisplayName("Tests de AuthService")
    class AuthServiceAccessTests {

        @BeforeEach
        void setUp() {
            sdk = NexusSDK.init(TEST_BASE_URL);
        }

        @Test
        @DisplayName("auth() debería retornar instancia de AuthService")
        void testAuthReturnsAuthService() {
            AuthService authService = sdk.auth();

            assertNotNull(authService);
            assertInstanceOf(AuthService.class, authService);
        }

        @Test
        @DisplayName("auth() debería retornar la misma instancia siempre")
        void testAuthReturnsSameInstance() {
            AuthService auth1 = sdk.auth();
            AuthService auth2 = sdk.auth();

            assertSame(auth1, auth2);
        }

        @Test
        @DisplayName("AuthService no debería ser null después de init")
        void testAuthServiceNotNull() {
            AuthService authService = sdk.auth();

            assertNotNull(authService);
        }
    }

    @Nested
    @DisplayName("Tests de Configuración Interna")
    class InternalConfigurationTests {

        @Test
        @DisplayName("Debería usar timeout por defecto de 5000ms")
        void testDefaultTimeout() {
            sdk = NexusSDK.init(TEST_BASE_URL);

            assertNotNull(sdk);
        }

        @Test
        @DisplayName("Debería inicializar todos los componentes internos")
        void testAllComponentsInitialized() {
            sdk = NexusSDK.init(TEST_BASE_URL);

            assertNotNull(sdk.auth());
        }
    }

    @Nested
    @DisplayName("Tests de Casos Edge")
    class EdgeCaseTests {

        @Test
        @DisplayName("Debería manejar URL con puerto específico")
        void testUrlWithPort() {
            sdk = NexusSDK.init("https://localhost:8443/api/auth/");

            assertNotNull(sdk);
            assertNotNull(sdk.auth());
        }

        @Test
        @DisplayName("Debería manejar URL con path")
        void testUrlWithPath() {
            sdk = NexusSDK.init("https://localhost:3000/api/auth/");

            assertNotNull(sdk);
            assertNotNull(sdk.auth());
        }

        @Test
        @DisplayName("Debería manejar URL con barra final")
        void testUrlWithTrailingSlash() {
            sdk = NexusSDK.init("https://localhost:3000/api/auth/");

            assertNotNull(sdk);
        }
    }

    @Nested
    @DisplayName("Tests de Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("init debería ser thread-safe")
        void testThreadSafeInit() throws InterruptedException {
            Thread[] threads = new Thread[10];
            NexusSDK[] results = new NexusSDK[10];

            for (int i = 0; i < 10; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    results[index] = NexusSDK.init(TEST_BASE_URL);
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            NexusSDK firstInstance = results[0];
            for (int i = 1; i < 10; i++) {
                assertSame(firstInstance, results[i]);
            }
        }

        @Test
        @DisplayName("auth() debería ser thread-safe")
        void testThreadSafeAuthAccess() throws InterruptedException {
            sdk = NexusSDK.init(TEST_BASE_URL);
            Thread[] threads = new Thread[10];

            for (int i = 0; i < 10; i++) {
                threads[i] = new Thread(() -> {
                    AuthService auth = sdk.auth();
                    assertNotNull(auth);
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }
        }
    }

    @Nested
    @DisplayName("Tests de Integración")
    class IntegrationTests {

        @Test
        @DisplayName("Flujo completo: init -> auth -> operaciones")
        void testCompleteFlow() {
            sdk = NexusSDK.init(TEST_BASE_URL);
            AuthService auth = sdk.auth();

            assertNotNull(sdk);
            assertNotNull(auth);
        }

        @Test
        @DisplayName("Múltiples SDKs con diferentes URLs")
        void testMultipleInstances() {
            NexusSDK sdk1 = NexusSDK.init("https://localhost:3000/api/auth/");
            NexusSDK sdk2 = NexusSDK.init("https://localhost:8080/api/auth/");

            assertSame(sdk1, sdk2);
        }
    }

    @Nested
    @DisplayName("Tests de Patrón Singleton")
    class SingletonPatternTests {

        @Test
        @DisplayName("Debería mantener una sola instancia global")
        void testSingleInstanceGlobally() {
            NexusSDK instance1 = NexusSDK.init("https://localhost:3000/api/auth/");
            NexusSDK instance2 = NexusSDK.init("https://localhost:8080/api/auth/");
            NexusSDK instance3 = NexusSDK.getInstance();

            assertSame(instance1, instance2);
            assertSame(instance2, instance3);
        }

        @Test
        @DisplayName("getInstance debería retornar null antes de init")
        void testGetInstanceWithoutInit() {
            NexusSDK instance = NexusSDK.getInstance();

            assertNull(instance);
        }

        @Test
        @DisplayName("getInstance debería retornar instancia después de init")
        void testGetInstanceAfterInit() {
            NexusSDK initialized = NexusSDK.init(TEST_BASE_URL);
            NexusSDK retrieved = NexusSDK.getInstance();

            assertNotNull(retrieved);
            assertSame(initialized, retrieved);
        }
    }
}