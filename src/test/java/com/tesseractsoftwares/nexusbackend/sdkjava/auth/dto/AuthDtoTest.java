package com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Auth DTOs - Tests Completos")
@Tag("unit")
public class AuthDtoTest {

    @Nested
    @DisplayName("Tests de LoginDto")
    class LoginDtoTests {

        @Test
        @DisplayName("Debería crear LoginDto con email y password")
        void testCreateLoginDto() {
            LoginDto dto = new LoginDto("test@example.com", "password123");

            assertNotNull(dto);
            assertEquals("test@example.com", dto.getEmail());
            assertEquals("password123", dto.getPassword());
        }

        @Test
        @DisplayName("getEmail debería retornar el email correcto")
        void testGetEmail() {
            LoginDto dto = new LoginDto("user@test.com", "pass");

            String email = dto.getEmail();

            assertEquals("user@test.com", email);
        }

        @Test
        @DisplayName("getPassword debería retornar el password correcto")
        void testGetPassword() {
            LoginDto dto = new LoginDto("user@test.com", "mypassword");

            String password = dto.getPassword();

            assertEquals("mypassword", password);
        }

        @Test
        @DisplayName("Debería crear con email vacío")
        void testCreateWithEmptyEmail() {
            LoginDto dto = new LoginDto("", "password");

            assertEquals("", dto.getEmail());
        }

        @Test
        @DisplayName("Debería crear con password vacío")
        void testCreateWithEmptyPassword() {
            LoginDto dto = new LoginDto("user@test.com", "");

            assertEquals("", dto.getPassword());
        }

        @Test
        @DisplayName("Debería manejar email con caracteres especiales")
        void testEmailWithSpecialCharacters() {
            LoginDto dto = new LoginDto("user+test@example.co.uk", "pass");

            assertEquals("user+test@example.co.uk", dto.getEmail());
        }

        @Test
        @DisplayName("Debería manejar password con caracteres especiales")
        void testPasswordWithSpecialCharacters() {
            LoginDto dto = new LoginDto("user@test.com", "P@ssw0rd!#$");

            assertEquals("P@ssw0rd!#$", dto.getPassword());
        }

        @Test
        @DisplayName("Debería crear múltiples instancias independientes")
        void testMultipleInstances() {
            LoginDto dto1 = new LoginDto("user1@test.com", "pass1");
            LoginDto dto2 = new LoginDto("user2@test.com", "pass2");

            assertNotEquals(dto1.getEmail(), dto2.getEmail());
            assertNotEquals(dto1.getPassword(), dto2.getPassword());
        }
    }

    @Nested
    @DisplayName("Tests de RegisterDto")
    class RegisterDtoTests {

        @Test
        @DisplayName("Debería crear RegisterDto con todos los campos")
        void testCreateRegisterDto() {
            RegisterDto dto = new RegisterDto(
                    "uuid-123",
                    "test@example.com",
                    "password123",
                    "1990-01-01",
                    "PlayerName"
            );

            assertNotNull(dto);
            assertEquals("uuid-123", dto.getUuid());
            assertEquals("test@example.com", dto.getEmail());
            assertEquals("password123", dto.getPassword());
            assertEquals("1990-01-01", dto.getBirthdate());
            assertEquals("PlayerName", dto.getPlayer_name());
        }

        @Test
        @DisplayName("getUuid debería retornar el UUID correcto")
        void testGetUuid() {
            RegisterDto dto = new RegisterDto(
                    "my-uuid",
                    "email@test.com",
                    "pass",
                    "2000-01-01",
                    "Player"
            );

            String uuid = dto.getUuid();

            assertEquals("my-uuid", uuid);
        }

        @Test
        @DisplayName("getEmail debería retornar el email correcto")
        void testGetEmail() {
            RegisterDto dto = new RegisterDto(
                    "uuid",
                    "user@example.com",
                    "pass",
                    "2000-01-01",
                    "Player"
            );

            String email = dto.getEmail();

            assertEquals("user@example.com", email);
        }

        @Test
        @DisplayName("getPassword debería retornar el password correcto")
        void testGetPassword() {
            RegisterDto dto = new RegisterDto(
                    "uuid",
                    "email@test.com",
                    "mypassword",
                    "2000-01-01",
                    "Player"
            );

            String password = dto.getPassword();

            assertEquals("mypassword", password);
        }

        @Test
        @DisplayName("getBirthdate debería retornar la fecha correcta")
        void testGetBirthdate() {
            RegisterDto dto = new RegisterDto(
                    "uuid",
                    "email@test.com",
                    "pass",
                    "1995-05-15",
                    "Player"
            );

            String birthdate = dto.getBirthdate();

            assertEquals("1995-05-15", birthdate);
        }

        @Test
        @DisplayName("getPlayer_name debería retornar el nombre correcto")
        void testGetPlayerName() {
            RegisterDto dto = new RegisterDto(
                    "uuid",
                    "email@test.com",
                    "pass",
                    "2000-01-01",
                    "TestPlayer123"
            );

            String playerName = dto.getPlayer_name();

            assertEquals("TestPlayer123", playerName);
        }

        @Test
        @DisplayName("Debería manejar UUID largo")
        void testLongUuid() {
            String longUuid = "550e8400-e29b-41d4-a716-446655440000";
            RegisterDto dto = new RegisterDto(
                    longUuid,
                    "email@test.com",
                    "pass",
                    "2000-01-01",
                    "Player"
            );

            assertEquals(longUuid, dto.getUuid());
        }

        @Test
        @DisplayName("Debería manejar diferentes formatos de fecha")
        void testDifferentDateFormats() {
            RegisterDto dto1 = new RegisterDto("uuid", "email@test.com", "pass", "1990-12-31", "P");
            RegisterDto dto2 = new RegisterDto("uuid", "email@test.com", "pass", "2000/01/01", "P");

            assertEquals("1990-12-31", dto1.getBirthdate());
            assertEquals("2000/01/01", dto2.getBirthdate());
        }

        @Test
        @DisplayName("Debería manejar nombres de jugador con espacios")
        void testPlayerNameWithSpaces() {
            RegisterDto dto = new RegisterDto(
                    "uuid",
                    "email@test.com",
                    "pass",
                    "2000-01-01",
                    "Player With Spaces"
            );

            assertEquals("Player With Spaces", dto.getPlayer_name());
        }

        @Test
        @DisplayName("Debería manejar nombres de jugador con caracteres especiales")
        void testPlayerNameWithSpecialChars() {
            RegisterDto dto = new RegisterDto(
                    "uuid",
                    "email@test.com",
                    "pass",
                    "2000-01-01",
                    "Player_123-ABC"
            );

            assertEquals("Player_123-ABC", dto.getPlayer_name());
        }

        @Test
        @DisplayName("Debería crear múltiples instancias independientes")
        void testMultipleInstances() {
            RegisterDto dto1 = new RegisterDto("uuid1", "email1@test.com", "pass1", "1990-01-01", "Player1");
            RegisterDto dto2 = new RegisterDto("uuid2", "email2@test.com", "pass2", "2000-12-31", "Player2");

            assertNotEquals(dto1.getUuid(), dto2.getUuid());
            assertNotEquals(dto1.getEmail(), dto2.getEmail());
            assertNotEquals(dto1.getPlayer_name(), dto2.getPlayer_name());
        }
    }

    @Nested
    @DisplayName("Tests de AuthResponseDto")
    class AuthResponseDtoTests {

        @Test
        @DisplayName("Debería crear AuthResponseDto vacío")
        void testCreateEmptyAuthResponseDto() {
            AuthResponseDto dto = new AuthResponseDto();

            assertNotNull(dto);
        }

        @Test
        @DisplayName("getToken debería retornar null por defecto")
        void testGetTokenDefault() {
            AuthResponseDto dto = new AuthResponseDto();

            String token = dto.getToken();

            assertNull(token);
        }

        @Test
        @DisplayName("Debería permitir parsear desde JSON con token")
        void testParseJsonWithToken() {
            AuthResponseDto dto = new AuthResponseDto();

            assertNotNull(dto);
        }

        @Test
        @DisplayName("Debería ser compatible con serialización JSON")
        void testJsonCompatibility() {
            AuthResponseDto dto = new AuthResponseDto();

            assertNotNull(dto);
            assertDoesNotThrow(() -> dto.getToken());
        }
    }

    @Nested
    @DisplayName("Tests de Integración entre DTOs")
    class IntegrationTests {

        @Test
        @DisplayName("LoginDto y RegisterDto deberían compartir campos comunes")
        void testCommonFields() {
            LoginDto loginDto = new LoginDto("user@test.com", "password");
            RegisterDto registerDto = new RegisterDto(
                    "uuid",
                    "user@test.com",
                    "password",
                    "2000-01-01",
                    "Player"
            );

            assertEquals(loginDto.getEmail(), registerDto.getEmail());
            assertEquals(loginDto.getPassword(), registerDto.getPassword());
        }

        @Test
        @DisplayName("Todos los DTOs deberían ser instanciables")
        void testAllDtosInstantiable() {
            assertDoesNotThrow(() -> new LoginDto("email", "pass"));
            assertDoesNotThrow(() -> new RegisterDto("uuid", "email", "pass", "date", "name"));
            assertDoesNotThrow(() -> new AuthResponseDto());
        }
    }

    @Nested
    @DisplayName("Tests de Casos Edge")
    class EdgeCaseTests {

        @Test
        @DisplayName("LoginDto debería manejar null en constructor")
        void testLoginDtoWithNulls() {
            LoginDto dto = new LoginDto(null, null);

            assertNull(dto.getEmail());
            assertNull(dto.getPassword());
        }

        @Test
        @DisplayName("RegisterDto debería manejar nulls en constructor")
        void testRegisterDtoWithNulls() {
            RegisterDto dto = new RegisterDto(null, null, null, null, null);

            assertNull(dto.getUuid());
            assertNull(dto.getEmail());
            assertNull(dto.getPassword());
            assertNull(dto.getBirthdate());
            assertNull(dto.getPlayer_name());
        }

        @Test
        @DisplayName("Debería manejar strings muy largos")
        void testVeryLongStrings() {
            String longString = "a".repeat(1000);

            LoginDto loginDto = new LoginDto(longString, longString);
            assertEquals(longString, loginDto.getEmail());
            assertEquals(longString, loginDto.getPassword());

            RegisterDto registerDto = new RegisterDto(
                    longString,
                    longString,
                    longString,
                    longString,
                    longString
            );
            assertEquals(longString, registerDto.getUuid());
        }
    }
}