package com.tesseractsoftwares.nexusbackend.sdkjava.players.dto;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerDto - Tests Completos")
@Tag("unit")
public class PlayerDtoTest {

    @Nested
    @DisplayName("Tests de Construcción")
    class ConstructorTests {

        @Test
        @DisplayName("Debería crear PlayerDto con todos los campos")
        void testCreateWithAllFields() {
            PlayerDto dto = new PlayerDto(
                    "uuid-123",
                    "player@test.com",
                    "1995-05-15",
                    "TestPlayer",
                    true,
                    "server-01"
            );

            assertNotNull(dto);
            assertEquals("uuid-123", dto.getUuid());
            assertEquals("player@test.com", dto.getEmail());
            assertEquals("1995-05-15", dto.getBirthdate());
            assertEquals("TestPlayer", dto.getPlayer_name());
            assertTrue(dto.isOnline());
            assertEquals("server-01", dto.getServer_connected());
        }

        @Test
        @DisplayName("Debería crear con jugador offline")
        void testCreateWithOfflinePlayer() {
            PlayerDto dto = new PlayerDto(
                    "uuid-456",
                    "offline@test.com",
                    "2000-01-01",
                    "OfflinePlayer",
                    false,
                    null
            );

            assertFalse(dto.isOnline());
            assertNull(dto.getServer_connected());
        }

        @Test
        @DisplayName("Debería crear con jugador online")
        void testCreateWithOnlinePlayer() {
            PlayerDto dto = new PlayerDto(
                    "uuid-789",
                    "online@test.com",
                    "1990-12-31",
                    "OnlinePlayer",
                    true,
                    "lobby-01"
            );

            assertTrue(dto.isOnline());
            assertEquals("lobby-01", dto.getServer_connected());
        }
    }

    @Nested
    @DisplayName("Tests de Getters")
    class GetterTests {

        private PlayerDto dto;

        @BeforeEach
        void setUp() {
            dto = new PlayerDto(
                    "test-uuid",
                    "test@example.com",
                    "1995-06-20",
                    "PlayerTest",
                    true,
                    "survival-01"
            );
        }

        @Test
        @DisplayName("getUuid debería retornar el UUID correcto")
        void testGetUuid() {
            assertEquals("test-uuid", dto.getUuid());
        }

        @Test
        @DisplayName("getEmail debería retornar el email correcto")
        void testGetEmail() {
            assertEquals("test@example.com", dto.getEmail());
        }

        @Test
        @DisplayName("getBirthdate debería retornar la fecha correcta")
        void testGetBirthdate() {
            assertEquals("1995-06-20", dto.getBirthdate());
        }

        @Test
        @DisplayName("getPlayer_name debería retornar el nombre correcto")
        void testGetPlayerName() {
            assertEquals("PlayerTest", dto.getPlayer_name());
        }

        @Test
        @DisplayName("isOnline debería retornar el estado online")
        void testIsOnline() {
            assertTrue(dto.isOnline());
        }

        @Test
        @DisplayName("getServer_connected debería retornar el servidor")
        void testGetServerConnected() {
            assertEquals("survival-01", dto.getServer_connected());
        }
    }

    @Nested
    @DisplayName("Tests de Estados Online/Offline")
    class OnlineOfflineTests {

        @Test
        @DisplayName("Jugador online debería tener servidor conectado")
        void testOnlinePlayerHasServer() {
            PlayerDto dto = new PlayerDto(
                    "uuid",
                    "email@test.com",
                    "2000-01-01",
                    "Player",
                    true,
                    "creative-02"
            );

            assertTrue(dto.isOnline());
            assertNotNull(dto.getServer_connected());
            assertEquals("creative-02", dto.getServer_connected());
        }

        @Test
        @DisplayName("Jugador offline no debería tener servidor")
        void testOfflinePlayerNoServer() {
            PlayerDto dto = new PlayerDto(
                    "uuid",
                    "email@test.com",
                    "2000-01-01",
                    "Player",
                    false,
                    null
            );

            assertFalse(dto.isOnline());
            assertNull(dto.getServer_connected());
        }

        @Test
        @DisplayName("Jugador puede estar online sin servidor")
        void testOnlinePlayerWithoutServer() {
            PlayerDto dto = new PlayerDto(
                    "uuid",
                    "email@test.com",
                    "2000-01-01",
                    "Player",
                    true,
                    null
            );

            assertTrue(dto.isOnline());
            assertNull(dto.getServer_connected());
        }
    }

    @Nested
    @DisplayName("Tests de Validación de Datos")
    class DataValidationTests {

        @Test
        @DisplayName("Debería manejar UUID largo")
        void testLongUuid() {
            String longUuid = "550e8400-e29b-41d4-a716-446655440000";
            PlayerDto dto = new PlayerDto(
                    longUuid,
                    "test@test.com",
                    "2000-01-01",
                    "Player",
                    false,
                    null
            );

            assertEquals(longUuid, dto.getUuid());
        }

        @Test
        @DisplayName("Debería manejar emails válidos")
        void testValidEmails() {
            PlayerDto dto1 = new PlayerDto("uuid", "user@domain.com", "2000-01-01", "P", false, null);
            PlayerDto dto2 = new PlayerDto("uuid", "user+tag@domain.co.uk", "2000-01-01", "P", false, null);

            assertEquals("user@domain.com", dto1.getEmail());
            assertEquals("user+tag@domain.co.uk", dto2.getEmail());
        }

        @Test
        @DisplayName("Debería manejar diferentes formatos de fecha")
        void testDifferentDateFormats() {
            PlayerDto dto1 = new PlayerDto("uuid", "email", "1990-12-31", "P", false, null);
            PlayerDto dto2 = new PlayerDto("uuid", "email", "2000/01/01", "P", false, null);

            assertEquals("1990-12-31", dto1.getBirthdate());
            assertEquals("2000/01/01", dto2.getBirthdate());
        }

        @Test
        @DisplayName("Debería manejar nombres con espacios")
        void testPlayerNameWithSpaces() {
            PlayerDto dto = new PlayerDto(
                    "uuid",
                    "email@test.com",
                    "2000-01-01",
                    "Player With Spaces",
                    false,
                    null
            );

            assertEquals("Player With Spaces", dto.getPlayer_name());
        }

        @Test
        @DisplayName("Debería manejar nombres con caracteres especiales")
        void testPlayerNameWithSpecialChars() {
            PlayerDto dto = new PlayerDto(
                    "uuid",
                    "email@test.com",
                    "2000-01-01",
                    "Player_123-XYZ",
                    false,
                    null
            );

            assertEquals("Player_123-XYZ", dto.getPlayer_name());
        }

        @Test
        @DisplayName("Debería manejar diferentes nombres de servidor")
        void testDifferentServerNames() {
            PlayerDto dto1 = new PlayerDto("uuid", "email", "2000-01-01", "P", true, "lobby-01");
            PlayerDto dto2 = new PlayerDto("uuid", "email", "2000-01-01", "P", true, "survival-main");
            PlayerDto dto3 = new PlayerDto("uuid", "email", "2000-01-01", "P", true, "creative_build");

            assertEquals("lobby-01", dto1.getServer_connected());
            assertEquals("survival-main", dto2.getServer_connected());
            assertEquals("creative_build", dto3.getServer_connected());
        }
    }

    @Nested
    @DisplayName("Tests de Casos Edge")
    class EdgeCaseTests {

        @Test
        @DisplayName("Debería manejar valores null en campos opcionales")
        void testNullOptionalFields() {
            PlayerDto dto = new PlayerDto(
                    "uuid",
                    "email@test.com",
                    "2000-01-01",
                    "Player",
                    false,
                    null
            );

            assertNotNull(dto);
            assertNull(dto.getServer_connected());
        }

        @Test
        @DisplayName("Debería manejar strings vacíos")
        void testEmptyStrings() {
            PlayerDto dto = new PlayerDto(
                    "",
                    "",
                    "",
                    "",
                    false,
                    ""
            );

            assertEquals("", dto.getUuid());
            assertEquals("", dto.getEmail());
            assertEquals("", dto.getBirthdate());
            assertEquals("", dto.getPlayer_name());
            assertEquals("", dto.getServer_connected());
        }

        @Test
        @DisplayName("Debería manejar valores null en constructor")
        void testNullValues() {
            PlayerDto dto = new PlayerDto(null, null, null, null, false, null);

            assertNull(dto.getUuid());
            assertNull(dto.getEmail());
            assertNull(dto.getBirthdate());
            assertNull(dto.getPlayer_name());
            assertNull(dto.getServer_connected());
        }
    }

    @Nested
    @DisplayName("Tests de Múltiples Instancias")
    class MultipleInstancesTests {

        @Test
        @DisplayName("Debería crear múltiples instancias independientes")
        void testMultipleIndependentInstances() {
            PlayerDto dto1 = new PlayerDto("uuid1", "email1", "1990-01-01", "Player1", true, "server1");
            PlayerDto dto2 = new PlayerDto("uuid2", "email2", "2000-12-31", "Player2", false, null);

            assertNotEquals(dto1.getUuid(), dto2.getUuid());
            assertNotEquals(dto1.getEmail(), dto2.getEmail());
            assertNotEquals(dto1.getPlayer_name(), dto2.getPlayer_name());
            assertNotEquals(dto1.isOnline(), dto2.isOnline());
        }

        @Test
        @DisplayName("Cada instancia debería mantener su propio estado")
        void testIndependentState() {
            PlayerDto online = new PlayerDto("uuid1", "email1", "2000-01-01", "Online", true, "lobby");
            PlayerDto offline = new PlayerDto("uuid2", "email2", "2000-01-01", "Offline", false, null);

            assertTrue(online.isOnline());
            assertFalse(offline.isOnline());
            assertNotNull(online.getServer_connected());
            assertNull(offline.getServer_connected());
        }
    }

    @Nested
    @DisplayName("Tests de Serialización JSON")
    class JsonCompatibilityTests {

        @Test
        @DisplayName("Debería ser compatible con serialización JSON")
        void testJsonCompatibility() {
            PlayerDto dto = new PlayerDto(
                    "uuid-123",
                    "test@example.com",
                    "1995-05-15",
                    "TestPlayer",
                    true,
                    "server-01"
            );

            assertDoesNotThrow(() -> {
                dto.getUuid();
                dto.getEmail();
                dto.getBirthdate();
                dto.getPlayer_name();
                dto.isOnline();
                dto.getServer_connected();
            });
        }
    }
}