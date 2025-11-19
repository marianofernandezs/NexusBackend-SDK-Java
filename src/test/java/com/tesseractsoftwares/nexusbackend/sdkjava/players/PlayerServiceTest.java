package com.tesseractsoftwares.nexusbackend.sdkjava.players;

import com.tesseractsoftwares.nexusbackend.sdkjava.http.NexusHttpClient;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.exceptions.NexusHttpException;
import com.tesseractsoftwares.nexusbackend.sdkjava.players.dto.PlayerDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService - Tests Completos")
@Tag("unit")
public class PlayerServiceTest {

    @Mock
    private NexusHttpClient mockHttpClient;

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(mockHttpClient);
    }

    @Nested
    @DisplayName("Tests de Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Debería crear PlayerService con HttpClient")
        void testCreateWithHttpClient() {
            PlayerService service = new PlayerService(mockHttpClient);

            assertNotNull(service);
        }
    }

    @Nested
    @DisplayName("Tests de getByUuid()")
    class GetByUuidTests {

        @Test
        @DisplayName("Debería obtener jugador por UUID exitosamente")
        void testGetByUuidSuccess() throws NexusHttpException {
            String uuid = "player-uuid-123";
            String jsonResponse = "{\"uuid\":\"player-uuid-123\",\"email\":\"player@test.com\"," +
                    "\"birthdate\":\"1995-05-15\",\"player_name\":\"TestPlayer\"," +
                    "\"online\":true,\"server_connected\":\"lobby-01\"}";

            when(mockHttpClient.get("players/" + uuid)).thenReturn(jsonResponse);

            PlayerDto result = playerService.getByUuid(uuid);

            assertNotNull(result);
            assertEquals("player-uuid-123", result.getUuid());
            assertEquals("player@test.com", result.getEmail());
            assertEquals("1995-05-15", result.getBirthdate());
            assertEquals("TestPlayer", result.getPlayer_name());
            assertTrue(result.isOnline());
            assertEquals("lobby-01", result.getServer_connected());

            verify(mockHttpClient, times(1)).get("players/" + uuid);
        }

        @Test
        @DisplayName("Debería construir endpoint correcto con UUID")
        void testBuildsCorrectEndpoint() throws NexusHttpException {
            String uuid = "test-uuid-456";
            String jsonResponse = "{\"uuid\":\"test-uuid-456\",\"email\":\"test@test.com\"," +
                    "\"birthdate\":\"2000-01-01\",\"player_name\":\"Player\"," +
                    "\"online\":false,\"server_connected\":null}";

            when(mockHttpClient.get("players/" + uuid)).thenReturn(jsonResponse);

            playerService.getByUuid(uuid);

            verify(mockHttpClient).get("players/test-uuid-456");
        }

        @Test
        @DisplayName("Debería obtener jugador offline")
        void testGetOfflinePlayer() throws NexusHttpException {
            String uuid = "offline-player";
            String jsonResponse = "{\"uuid\":\"offline-player\",\"email\":\"offline@test.com\"," +
                    "\"birthdate\":\"1990-12-31\",\"player_name\":\"OfflinePlayer\"," +
                    "\"online\":false,\"server_connected\":null}";

            when(mockHttpClient.get("players/" + uuid)).thenReturn(jsonResponse);

            PlayerDto result = playerService.getByUuid(uuid);

            assertFalse(result.isOnline());
            assertNull(result.getServer_connected());
        }

        @Test
        @DisplayName("Debería obtener jugador online")
        void testGetOnlinePlayer() throws NexusHttpException {
            String uuid = "online-player";
            String jsonResponse = "{\"uuid\":\"online-player\",\"email\":\"online@test.com\"," +
                    "\"birthdate\":\"2000-06-15\",\"player_name\":\"OnlinePlayer\"," +
                    "\"online\":true,\"server_connected\":\"survival-01\"}";

            when(mockHttpClient.get("players/" + uuid)).thenReturn(jsonResponse);

            PlayerDto result = playerService.getByUuid(uuid);

            assertTrue(result.isOnline());
            assertEquals("survival-01", result.getServer_connected());
        }

        @Test
        @DisplayName("Debería propagar excepción cuando falla GET")
        void testGetThrowsException() throws NexusHttpException {
            String uuid = "nonexistent-player";

            when(mockHttpClient.get("players/" + uuid))
                    .thenThrow(new NexusHttpException("Player not found", 404, "players/" + uuid, "error"));

            assertThrows(NexusHttpException.class, () -> playerService.getByUuid(uuid));
        }

        @Test
        @DisplayName("Debería manejar UUIDs con diferentes formatos")
        void testDifferentUuidFormats() throws NexusHttpException {
            String uuid1 = "simple-uuid";
            String uuid2 = "550e8400-e29b-41d4-a716-446655440000";

            String json = "{\"uuid\":\"\",\"email\":\"test@test.com\"," +
                    "\"birthdate\":\"2000-01-01\",\"player_name\":\"P\"," +
                    "\"online\":false,\"server_connected\":null}";

            when(mockHttpClient.get(anyString())).thenReturn(json);

            playerService.getByUuid(uuid1);
            playerService.getByUuid(uuid2);

            verify(mockHttpClient).get("players/simple-uuid");
            verify(mockHttpClient).get("players/550e8400-e29b-41d4-a716-446655440000");
        }
    }

    @Nested
    @DisplayName("Tests de update()")
    class UpdateTests {

        @Test
        @DisplayName("Debería actualizar jugador exitosamente")
        void testUpdateSuccess() throws NexusHttpException {
            String uuid = "player-uuid-123";
            PlayerDto updateDto = new PlayerDto(
                    "player-uuid-123",
                    "updated@test.com",
                    "1995-05-15",
                    "UpdatedPlayer",
                    true,
                    "creative-01"
            );

            String jsonResponse = "{\"uuid\":\"player-uuid-123\",\"email\":\"updated@test.com\"," +
                    "\"birthdate\":\"1995-05-15\",\"player_name\":\"UpdatedPlayer\"," +
                    "\"online\":true,\"server_connected\":\"creative-01\"}";

            when(mockHttpClient.put(eq("players/" + uuid), anyString())).thenReturn(jsonResponse);

            PlayerDto result = playerService.update(uuid, updateDto);

            assertNotNull(result);
            assertEquals("updated@test.com", result.getEmail());
            assertEquals("UpdatedPlayer", result.getPlayer_name());
            assertTrue(result.isOnline());
            assertEquals("creative-01", result.getServer_connected());

            verify(mockHttpClient, times(1)).put(eq("players/" + uuid), anyString());
        }

        @Test
        @DisplayName("Debería serializar DTO a JSON correctamente")
        void testSerializesDto() throws NexusHttpException {
            String uuid = "player-uuid";
            PlayerDto dto = new PlayerDto(
                    "player-uuid",
                    "test@test.com",
                    "2000-01-01",
                    "TestPlayer",
                    false,
                    null
            );

            String jsonResponse = "{\"uuid\":\"player-uuid\",\"email\":\"test@test.com\"," +
                    "\"birthdate\":\"2000-01-01\",\"player_name\":\"TestPlayer\"," +
                    "\"online\":false,\"server_connected\":null}";

            when(mockHttpClient.put(eq("players/" + uuid), anyString())).thenReturn(jsonResponse);

            playerService.update(uuid, dto);

            verify(mockHttpClient).put(eq("players/" + uuid), contains("test@test.com"));
            verify(mockHttpClient).put(eq("players/" + uuid), contains("TestPlayer"));
        }

        @Test
        @DisplayName("Debería actualizar estado online del jugador")
        void testUpdateOnlineStatus() throws NexusHttpException {
            String uuid = "player-uuid";
            PlayerDto dto = new PlayerDto(
                    uuid,
                    "player@test.com",
                    "1990-01-01",
                    "Player",
                    true,
                    "lobby-main"
            );

            String jsonResponse = "{\"uuid\":\"player-uuid\",\"email\":\"player@test.com\"," +
                    "\"birthdate\":\"1990-01-01\",\"player_name\":\"Player\"," +
                    "\"online\":true,\"server_connected\":\"lobby-main\"}";

            when(mockHttpClient.put(eq("players/" + uuid), anyString())).thenReturn(jsonResponse);

            PlayerDto result = playerService.update(uuid, dto);

            assertTrue(result.isOnline());
            assertEquals("lobby-main", result.getServer_connected());
        }

        @Test
        @DisplayName("Debería actualizar jugador a offline")
        void testUpdateToOffline() throws NexusHttpException {
            String uuid = "player-uuid";
            PlayerDto dto = new PlayerDto(
                    uuid,
                    "player@test.com",
                    "1990-01-01",
                    "Player",
                    false,
                    null
            );

            String jsonResponse = "{\"uuid\":\"player-uuid\",\"email\":\"player@test.com\"," +
                    "\"birthdate\":\"1990-01-01\",\"player_name\":\"Player\"," +
                    "\"online\":false,\"server_connected\":null}";

            when(mockHttpClient.put(eq("players/" + uuid), anyString())).thenReturn(jsonResponse);

            PlayerDto result = playerService.update(uuid, dto);

            assertFalse(result.isOnline());
            assertNull(result.getServer_connected());
        }

        @Test
        @DisplayName("Debería propagar excepción cuando falla UPDATE")
        void testUpdateThrowsException() throws NexusHttpException {
            String uuid = "player-uuid";
            PlayerDto dto = new PlayerDto(uuid, "email", "date", "name", false, null);

            when(mockHttpClient.put(eq("players/" + uuid), anyString()))
                    .thenThrow(new NexusHttpException("Update failed", 500, "players/" + uuid, "error"));

            assertThrows(NexusHttpException.class, () -> playerService.update(uuid, dto));
        }

        @Test
        @DisplayName("Debería construir endpoint correcto en UPDATE")
        void testUpdateBuildsCorrectEndpoint() throws NexusHttpException {
            String uuid = "test-uuid-789";
            PlayerDto dto = new PlayerDto(uuid, "email@test.com", "2000-01-01", "Player", false, null);

            String jsonResponse = "{\"uuid\":\"test-uuid-789\",\"email\":\"email@test.com\"," +
                    "\"birthdate\":\"2000-01-01\",\"player_name\":\"Player\"," +
                    "\"online\":false,\"server_connected\":null}";

            when(mockHttpClient.put(eq("players/" + uuid), anyString())).thenReturn(jsonResponse);

            playerService.update(uuid, dto);

            verify(mockHttpClient).put(eq("players/test-uuid-789"), anyString());
        }
    }

    @Nested
    @DisplayName("Tests de delete()")
    class DeleteTests {

        @Test
        @DisplayName("Debería eliminar jugador exitosamente")
        void testDeleteSuccess() throws NexusHttpException {
            String uuid = "player-to-delete";

            when(mockHttpClient.delete("players/" + uuid)).thenReturn("{}");

            assertDoesNotThrow(() -> playerService.delete(uuid));

            verify(mockHttpClient, times(1)).delete("players/" + uuid);
        }

        @Test
        @DisplayName("Debería construir endpoint correcto en DELETE")
        void testDeleteBuildsCorrectEndpoint() throws NexusHttpException {
            String uuid = "uuid-123";

            when(mockHttpClient.delete("players/" + uuid)).thenReturn("{}");

            playerService.delete(uuid);

            verify(mockHttpClient).delete("players/uuid-123");
        }

        @Test
        @DisplayName("Debería propagar excepción cuando falla DELETE")
        void testDeleteThrowsException() throws NexusHttpException {
            String uuid = "nonexistent-player";

            when(mockHttpClient.delete("players/" + uuid))
                    .thenThrow(new NexusHttpException("Player not found", 404, "players/" + uuid, "error"));

            assertThrows(NexusHttpException.class, () -> playerService.delete(uuid));
        }

        @Test
        @DisplayName("Debería eliminar con diferentes UUIDs")
        void testDeleteDifferentUuids() throws NexusHttpException {
            when(mockHttpClient.delete(anyString())).thenReturn("{}");

            playerService.delete("uuid-1");
            playerService.delete("uuid-2");
            playerService.delete("550e8400-e29b-41d4-a716-446655440000");

            verify(mockHttpClient).delete("players/uuid-1");
            verify(mockHttpClient).delete("players/uuid-2");
            verify(mockHttpClient).delete("players/550e8400-e29b-41d4-a716-446655440000");
        }
    }

    @Nested
    @DisplayName("Tests de Integración de Flujo Completo")
    class IntegrationFlowTests {

        @Test
        @DisplayName("Flujo completo: Get -> Update -> Get")
        void testGetUpdateGetFlow() throws NexusHttpException {
            String uuid = "player-uuid";

            String initialJson = "{\"uuid\":\"player-uuid\",\"email\":\"old@test.com\"," +
                    "\"birthdate\":\"1990-01-01\",\"player_name\":\"OldName\"," +
                    "\"online\":false,\"server_connected\":null}";

            String updatedJson = "{\"uuid\":\"player-uuid\",\"email\":\"new@test.com\"," +
                    "\"birthdate\":\"1990-01-01\",\"player_name\":\"NewName\"," +
                    "\"online\":true,\"server_connected\":\"lobby\"}";

            when(mockHttpClient.get("players/" + uuid))
                    .thenReturn(initialJson)
                    .thenReturn(updatedJson);

            when(mockHttpClient.put(eq("players/" + uuid), anyString())).thenReturn(updatedJson);

            PlayerDto initial = playerService.getByUuid(uuid);
            assertEquals("old@test.com", initial.getEmail());

            PlayerDto updateDto = new PlayerDto(uuid, "new@test.com", "1990-01-01", "NewName", true, "lobby");
            PlayerDto updated = playerService.update(uuid, updateDto);
            assertEquals("new@test.com", updated.getEmail());

            PlayerDto fetched = playerService.getByUuid(uuid);
            assertEquals("new@test.com", fetched.getEmail());

            verify(mockHttpClient, times(2)).get("players/" + uuid);
            verify(mockHttpClient, times(1)).put(eq("players/" + uuid), anyString());
        }

        @Test
        @DisplayName("Flujo completo: Get -> Delete")
        void testGetDeleteFlow() throws NexusHttpException {
            String uuid = "player-to-remove";

            String json = "{\"uuid\":\"player-to-remove\",\"email\":\"test@test.com\"," +
                    "\"birthdate\":\"2000-01-01\",\"player_name\":\"Player\"," +
                    "\"online\":false,\"server_connected\":null}";

            when(mockHttpClient.get("players/" + uuid)).thenReturn(json);
            when(mockHttpClient.delete("players/" + uuid)).thenReturn("{}");

            PlayerDto player = playerService.getByUuid(uuid);
            assertNotNull(player);

            playerService.delete(uuid);

            verify(mockHttpClient).get("players/" + uuid);
            verify(mockHttpClient).delete("players/" + uuid);
        }
    }

    @Nested
    @DisplayName("Tests de Casos Edge")
    class EdgeCaseTests {

        @Test
        @DisplayName("Debería manejar respuesta JSON vacía en DELETE")
        void testDeleteEmptyResponse() throws NexusHttpException {
            String uuid = "player-uuid";

            when(mockHttpClient.delete("players/" + uuid)).thenReturn("");

            assertDoesNotThrow(() -> playerService.delete(uuid));
        }

        @Test
        @DisplayName("Debería manejar UUID vacío")
        void testEmptyUuid() throws NexusHttpException {
            String emptyUuid = "";
            String json = "{\"uuid\":\"\",\"email\":\"test@test.com\"," +
                    "\"birthdate\":\"2000-01-01\",\"player_name\":\"Player\"," +
                    "\"online\":false,\"server_connected\":null}";

            when(mockHttpClient.get("players/")).thenReturn(json);

            PlayerDto result = playerService.getByUuid(emptyUuid);

            assertNotNull(result);
            verify(mockHttpClient).get("players/");
        }

        @Test
        @DisplayName("Debería manejar UUID muy largo")
        void testVeryLongUuid() throws NexusHttpException {
            String longUuid = "a".repeat(100);
            String json = "{\"uuid\":\"" + longUuid + "\",\"email\":\"test@test.com\"," +
                    "\"birthdate\":\"2000-01-01\",\"player_name\":\"Player\"," +
                    "\"online\":false,\"server_connected\":null}";

            when(mockHttpClient.get("players/" + longUuid)).thenReturn(json);

            PlayerDto result = playerService.getByUuid(longUuid);

            assertNotNull(result);
        }
    }
}