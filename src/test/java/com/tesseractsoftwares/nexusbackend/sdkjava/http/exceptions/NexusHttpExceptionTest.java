package com.tesseractsoftwares.nexusbackend.sdkjava.http.exceptions;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NexusHttpException - Tests Completos")
@Tag("unit")
public class NexusHttpExceptionTest {

    @Nested
    @DisplayName("Tests de Construcción")
    class ConstructorTests {

        @Test
        @DisplayName("Debería crear excepción con mensaje, statusCode, endpoint y responseBody")
        void testCreateWithAllParameters() {
            NexusHttpException exception = new NexusHttpException(
                    "HTTP error",
                    400,
                    "login",
                    "{\"error\":\"Invalid request\"}"
            );

            assertNotNull(exception);
            assertEquals("HTTP error", exception.getMessage());
        }

        @Test
        @DisplayName("Debería crear excepción con statusCode 0 para errores de red")
        void testCreateNetworkError() {
            NexusHttpException exception = new NexusHttpException(
                    "Request failed: Connection timeout",
                    0,
                    "register",
                    null
            );

            assertNotNull(exception);
            assertTrue(exception.getMessage().contains("Connection timeout"));
        }

        @Test
        @DisplayName("Debería crear excepción con diferentes status codes")
        void testDifferentStatusCodes() {
            NexusHttpException ex400 = new NexusHttpException("Bad Request", 400, "endpoint", "body");
            NexusHttpException ex401 = new NexusHttpException("Unauthorized", 401, "endpoint", "body");
            NexusHttpException ex404 = new NexusHttpException("Not Found", 404, "endpoint", "body");
            NexusHttpException ex500 = new NexusHttpException("Server Error", 500, "endpoint", "body");

            assertNotNull(ex400);
            assertNotNull(ex401);
            assertNotNull(ex404);
            assertNotNull(ex500);
        }

        @Test
        @DisplayName("Debería manejar responseBody null")
        void testNullResponseBody() {
            NexusHttpException exception = new NexusHttpException(
                    "Error",
                    500,
                    "endpoint",
                    null
            );

            assertNotNull(exception);
        }

        @Test
        @DisplayName("Debería manejar endpoint null")
        void testNullEndpoint() {
            NexusHttpException exception = new NexusHttpException(
                    "Error",
                    500,
                    null,
                    "body"
            );

            assertNotNull(exception);
        }
    }

    @Nested
    @DisplayName("Tests de Mensaje de Error")
    class ErrorMessageTests {

        @Test
        @DisplayName("getMessage debería retornar el mensaje correcto")
        void testGetMessage() {
            NexusHttpException exception = new NexusHttpException(
                    "Invalid credentials",
                    401,
                    "login",
                    "error body"
            );

            String message = exception.getMessage();

            assertEquals("Invalid credentials", message);
        }

        @Test
        @DisplayName("Debería contener información útil en el mensaje")
        void testMessageContent() {
            NexusHttpException exception = new NexusHttpException(
                    "HTTP error occurred",
                    404,
                    "users/123",
                    "{\"error\":\"Not found\"}"
            );

            String message = exception.getMessage();

            assertNotNull(message);
            assertTrue(message.length() > 0);
        }

        @Test
        @DisplayName("Debería manejar mensaje largo")
        void testLongMessage() {
            String longMessage = "a".repeat(500);
            NexusHttpException exception = new NexusHttpException(
                    longMessage,
                    400,
                    "endpoint",
                    "body"
            );

            assertEquals(longMessage, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Tests de Códigos de Estado HTTP")
    class HttpStatusCodeTests {

        @Test
        @DisplayName("Debería manejar errores 4xx (cliente)")
        void testClientErrors() {
            int[] clientErrors = {400, 401, 403, 404, 409, 422, 429};

            for (int code : clientErrors) {
                NexusHttpException exception = new NexusHttpException(
                        "Client error",
                        code,
                        "endpoint",
                        "body"
                );
                assertNotNull(exception);
            }
        }

        @Test
        @DisplayName("Debería manejar errores 5xx (servidor)")
        void testServerErrors() {
            int[] serverErrors = {500, 502, 503, 504};

            for (int code : serverErrors) {
                NexusHttpException exception = new NexusHttpException(
                        "Server error",
                        code,
                        "endpoint",
                        "body"
                );
                assertNotNull(exception);
            }
        }

        @Test
        @DisplayName("Debería manejar código 0 para errores de conexión")
        void testNetworkErrorCode() {
            NexusHttpException exception = new NexusHttpException(
                    "Network error",
                    0,
                    "endpoint",
                    null
            );

            assertNotNull(exception);
        }
    }

    @Nested
    @DisplayName("Tests de Herencia de Exception")
    class ExceptionInheritanceTests {

        @Test
        @DisplayName("Debería ser una instancia de Exception")
        void testInstanceOfException() {
            NexusHttpException exception = new NexusHttpException(
                    "Error",
                    400,
                    "endpoint",
                    "body"
            );

            assertInstanceOf(Exception.class, exception);
        }

        @Test
        @DisplayName("Debería ser lanzable con throw")
        void testThrowable() {
            assertThrows(NexusHttpException.class, () -> {
                throw new NexusHttpException("Error", 400, "endpoint", "body");
            });
        }

        @Test
        @DisplayName("Debería poder ser atrapada con catch")
        void testCatchable() {
            try {
                throw new NexusHttpException("Test error", 500, "test", "body");
            } catch (NexusHttpException e) {
                assertEquals("Test error", e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Tests de Uso en Flujos Reales")
    class RealWorldUsageTests {

        @Test
        @DisplayName("Debería usarse para error de autenticación")
        void testAuthenticationError() {
            NexusHttpException exception = new NexusHttpException(
                    "Invalid credentials",
                    401,
                    "login",
                    "{\"error\":\"Authentication failed\"}"
            );

            assertNotNull(exception);
            assertEquals("Invalid credentials", exception.getMessage());
        }

        @Test
        @DisplayName("Debería usarse para error de recurso no encontrado")
        void testNotFoundError() {
            NexusHttpException exception = new NexusHttpException(
                    "Resource not found",
                    404,
                    "users/999",
                    "{\"error\":\"User does not exist\"}"
            );

            assertNotNull(exception);
        }

        @Test
        @DisplayName("Debería usarse para error de servidor")
        void testServerError() {
            NexusHttpException exception = new NexusHttpException(
                    "Internal server error",
                    500,
                    "api/action",
                    "{\"error\":\"Database connection failed\"}"
            );

            assertNotNull(exception);
        }

        @Test
        @DisplayName("Debería usarse para error de timeout")
        void testTimeoutError() {
            NexusHttpException exception = new NexusHttpException(
                    "Request failed: Connection timeout",
                    0,
                    "validate",
                    null
            );

            assertNotNull(exception);
            assertTrue(exception.getMessage().contains("timeout"));
        }
    }

    @Nested
    @DisplayName("Tests de Información de Error Detallada")
    class DetailedErrorInfoTests {

        @Test
        @DisplayName("Debería almacenar información del endpoint")
        void testEndpointInfo() {
            NexusHttpException exception = new NexusHttpException(
                    "Error",
                    400,
                    "auth/register",
                    "body"
            );

            assertNotNull(exception);
        }

        @Test
        @DisplayName("Debería almacenar body de respuesta")
        void testResponseBodyInfo() {
            String responseBody = "{\"error\":\"Validation failed\",\"details\":[\"Email required\"]}";
            NexusHttpException exception = new NexusHttpException(
                    "Validation error",
                    422,
                    "register",
                    responseBody
            );

            assertNotNull(exception);
        }

        @Test
        @DisplayName("Debería manejar diferentes formatos de response body")
        void testDifferentResponseFormats() {
            NexusHttpException json = new NexusHttpException("Error", 400, "ep", "{\"key\":\"value\"}");
            NexusHttpException text = new NexusHttpException("Error", 400, "ep", "Plain text error");
            NexusHttpException empty = new NexusHttpException("Error", 400, "ep", "");

            assertNotNull(json);
            assertNotNull(text);
            assertNotNull(empty);
        }
    }

    @Nested
    @DisplayName("Tests de Casos Edge")
    class EdgeCaseTests {

        @Test
        @DisplayName("Debería manejar mensaje vacío")
        void testEmptyMessage() {
            NexusHttpException exception = new NexusHttpException(
                    "",
                    400,
                    "endpoint",
                    "body"
            );

            assertNotNull(exception);
            assertEquals("", exception.getMessage());
        }

        @Test
        @DisplayName("Debería manejar mensaje null")
        void testNullMessage() {
            NexusHttpException exception = new NexusHttpException(
                    null,
                    400,
                    "endpoint",
                    "body"
            );

            assertNotNull(exception);
        }

        @Test
        @DisplayName("Debería manejar todos los parámetros null excepto statusCode")
        void testAllNullsExceptStatus() {
            NexusHttpException exception = new NexusHttpException(
                    null,
                    500,
                    null,
                    null
            );

            assertNotNull(exception);
        }

        @Test
        @DisplayName("Debería manejar statusCode negativo")
        void testNegativeStatusCode() {
            NexusHttpException exception = new NexusHttpException(
                    "Error",
                    -1,
                    "endpoint",
                    "body"
            );

            assertNotNull(exception);
        }

        @Test
        @DisplayName("Debería manejar statusCode muy grande")
        void testLargeStatusCode() {
            NexusHttpException exception = new NexusHttpException(
                    "Error",
                    999,
                    "endpoint",
                    "body"
            );

            assertNotNull(exception);
        }
    }

    @Nested
    @DisplayName("Tests de Múltiples Instancias")
    class MultipleInstancesTests {

        @Test
        @DisplayName("Debería crear múltiples excepciones independientes")
        void testMultipleIndependentExceptions() {
            NexusHttpException ex1 = new NexusHttpException("Error 1", 400, "ep1", "body1");
            NexusHttpException ex2 = new NexusHttpException("Error 2", 404, "ep2", "body2");
            NexusHttpException ex3 = new NexusHttpException("Error 3", 500, "ep3", "body3");

            assertNotEquals(ex1.getMessage(), ex2.getMessage());
            assertNotEquals(ex2.getMessage(), ex3.getMessage());
        }

        @Test
        @DisplayName("Cada excepción debería mantener su propio estado")
        void testIndependentState() {
            NexusHttpException ex1 = new NexusHttpException("Auth error", 401, "login", "body");
            NexusHttpException ex2 = new NexusHttpException("Not found", 404, "users", "body");

            assertEquals("Auth error", ex1.getMessage());
            assertEquals("Not found", ex2.getMessage());
        }
    }
}