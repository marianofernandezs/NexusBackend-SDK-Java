package com.tesseractsoftwares.nexusbackend.sdkjava.integration;

import com.tesseractsoftwares.nexusbackend.sdkjava.NexusSDK;
import com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto.LoginDto;
import com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto.RegisterDto;
import com.tesseractsoftwares.nexusbackend.sdkjava.players.PlayerService;
import com.tesseractsoftwares.nexusbackend.sdkjava.players.dto.PlayerDto;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de Integración - Conexión con Backend")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BackendIntegrationTest {

    private static final String BASE_URL = "http://localhost:3000/api/";
    private static NexusSDK sdk;
    private static String authToken;

    // UUID válido para PostgreSQL
    private static final String testPlayerUuid = UUID.randomUUID().toString();

    @BeforeAll
    static void setupIntegration() {
        System.out.println("=================================================");
        System.out.println(" TESTS DE INTEGRACIÓN - VERIFICANDO BACKEND");
        System.out.println("=================================================");
        System.out.println("Base URL: " + BASE_URL);
        System.out.println("=================================================\n");

        sdk = NexusSDK.init(BASE_URL);
        assertNotNull(sdk);
    }

    // ============================================================================================
    // 1. REGISTER
    // ============================================================================================
    @Test
    @Order(1)
    @DisplayName("Register Success")
    void testRegister() {
        RegisterDto dto = new RegisterDto(
                testPlayerUuid,
                "integration@test.com",
                "123456",
                "2000-01-01",
                "IntegrationTest"
        );

        try {
            boolean ok = sdk.auth().register(dto);
            assertTrue(ok);
        } catch (Exception e) {
            fail("Error al registrar: " + e.getMessage());
        }
    }

    // ============================================================================================
    // 2. LOGIN
    // ============================================================================================
    @Test
    @Order(2)
    @DisplayName("Login Success")
    void testLogin() {
        LoginDto login = new LoginDto("integration@test.com", "123456");
        try {
            authToken = sdk.auth().login(login);
            assertNotNull(authToken);
            assertTrue(authToken.length() > 10);
        } catch (Exception e) {
            fail("Error al hacer login: " + e.getMessage());
        }
    }

    // ============================================================================================
    // 3. VALIDATE TOKEN
    // ============================================================================================
    @Test
    @Order(3)
    @DisplayName("Validate Token Success")
    void testValidate() {
        try {
            boolean ok = sdk.auth().validate();
            assertTrue(ok);
        } catch (Exception e) {
            fail("Error al validar token: " + e.getMessage());
        }
    }

    // ============================================================================================
    // 4. REFRESH TOKEN
    // ============================================================================================
    @Test
    @Order(4)
    @DisplayName("Refresh Token Success")
    void testRefresh() {
        try {
            String newToken = sdk.auth().refresh();
            assertNotNull(newToken);
            assertTrue(newToken.length() > 10);
            authToken = newToken;
        } catch (Exception e) {
            fail("Error al refrescar token: " + e.getMessage());
        }
    }

    // ============================================================================================
    // 5. GET PLAYER
    // ============================================================================================
    @Test
    @Order(5)
    @DisplayName("Get Player Success")
    void testGetPlayer() {
        try {
            PlayerDto dto = sdk.players().getByUuid(testPlayerUuid);
            assertNotNull(dto);
        } catch (Exception e) {
            fail("Error al obtener jugador: " + e.getMessage());
        }
    }

    // ============================================================================================
    // 6. UPDATE PLAYER
    // ============================================================================================
    @Test
    @Order(6)
    @DisplayName("Update Player Success")
    void testUpdatePlayer() {
        try {
            PlayerDto updated = new PlayerDto(
                    testPlayerUuid,
                    "integration@test.com",
                    "2000-01-01",
                    "IntegrationUpdated",
                    false,
                    "lobby1"
            );

            PlayerDto result = sdk.players().update(testPlayerUuid, updated);
            assertNotNull(result);

        } catch (Exception e) {
            fail("Error al actualizar jugador: " + e.getMessage());
        }
    }

    // ============================================================================================
    // 7. DELETE PLAYER
    // ============================================================================================
    @Test
    @Order(7)
    @DisplayName("Delete Player Success")
    void testDeletePlayer() {
        try {
            boolean ok = sdk.players().delete(testPlayerUuid);
            assertTrue(ok);
        } catch (Exception e) {
            fail("Error al eliminar jugador: " + e.getMessage());
        }
    }

    // ============================================================================================
    // 8. LOGOUT — SIEMPRE AL FINAL
    // ============================================================================================
    @Test
    @Order(8)
    @DisplayName("Logout Success")
    void testLogout() {
        try {
            boolean ok = sdk.auth().logout();
            assertTrue(ok);
        } catch (Exception e) {
            fail("Error al hacer logout: " + e.getMessage());
        }
    }
}
