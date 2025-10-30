package com.prax.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests de DataManager")
class DataManagerTest {

    @Mock
    private PraxCorePlugin mockPlugin;

    @Mock
    private FileConfiguration mockConfig;

    private DataManager dataManager;
    private UUID testUuid;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configurar comportamiento del plugin mock
        File mockDataFolder = new File("test-data");
        when(mockPlugin.getDataFolder()).thenReturn(mockDataFolder);

        // Crear DataManager (no llamará a setupFile en tests)
        dataManager = spy(new DataManager(mockPlugin));

        // Reemplazar el config real con nuestro mock
        doReturn(mockConfig).when(dataManager).getConfig();

        testUuid = UUID.randomUUID();
    }

    @Test
    @DisplayName("Debería verificar si un jugador está registrado")
    void testIsPlayerRegistered() {
        // Arrange
        String playerPath = "players." + testUuid.toString();
        when(mockConfig.contains(playerPath)).thenReturn(true);

        // Act
        boolean result = dataManager.isPlayerRegistered(testUuid);

        // Assert
        assertTrue(result, "El jugador debería estar registrado");
        verify(mockConfig).contains(playerPath);
    }

    @Test
    @DisplayName("Debería retornar false si jugador no está registrado")
    void testIsPlayerNotRegistered() {
        // Arrange
        String playerPath = "players." + testUuid.toString();
        when(mockConfig.contains(playerPath)).thenReturn(false);

        // Act
        boolean result = dataManager.isPlayerRegistered(testUuid);

        // Assert
        assertFalse(result, "El jugador no debería estar registrado");
    }

    @Test
    @DisplayName("Debería obtener el hash de contraseña de un jugador")
    void testGetPasswordHash() {
        // Arrange
        String expectedHash = "$2a$10$hashedPassword";
        String path = "players." + testUuid.toString() + ".passwordHash";
        when(mockConfig.getString(path)).thenReturn(expectedHash);

        // Act
        String result = dataManager.getPasswordHash(testUuid);

        // Assert
        assertEquals(expectedHash, result);
        verify(mockConfig).getString(path);
    }

    @Test
    @DisplayName("Debería registrar un jugador correctamente")
    void testRegisterPlayer() {
        // Arrange
        String hashedPassword = "$2a$10$hashedPassword";
        String email = "test@example.com";
        String ipAddress = "192.168.1.1";
        String clientType = "JAVA";
        String version = "1.20.1";
        String basePath = "players." + testUuid.toString();
        String birthdate = "15-03-2005";

        // Act
        dataManager.registerPlayer(testUuid, hashedPassword, email, ipAddress, clientType, version, birthdate);

        // Assert
        verify(mockConfig).set(basePath + ".passwordHash", hashedPassword);
        verify(mockConfig).set(basePath + ".email", email);
        verify(mockConfig).set(basePath + ".registrationIp", ipAddress);
        verify(mockConfig).set(basePath + ".clientType", clientType);
        verify(mockConfig).set(basePath + ".versionOnRegister", version);
        verify(mockConfig).set(basePath + ".birthdate", birthdate);
    }

    @Test
    @DisplayName("Debería establecer la fecha del primer login")
    void testSetFirstLoginDate() {
        // Arrange
        String date = "2024/01/15 10:30:00";
        String path = "players." + testUuid.toString() + ".firstLogin";

        // Act
        dataManager.setFirstLoginDate(testUuid, date);

        // Assert
        verify(mockConfig).set(path, date);
    }

    @Test
    @DisplayName("Debería establecer la fecha del último login")
    void testSetLastLoginDate() {
        // Arrange
        String datetime = "2024/01/15 10:30:00";
        String path = "players." + testUuid.toString() + ".lastLogin";

        // Act
        dataManager.setLastLoginDate(testUuid, datetime);

        // Assert
        verify(mockConfig).set(path, datetime);
    }

    @Test
    @DisplayName("Debería incrementar el contador de logins")
    void testIncrementLoginCount() {
        // Arrange
        String path = "players." + testUuid.toString() + ".loginCount";
        when(mockConfig.getInt(path, 0)).thenReturn(5);

        // Act
        dataManager.incrementLoginCount(testUuid);

        // Assert
        verify(mockConfig).getInt(path, 0);
        verify(mockConfig).set(path, 6);
    }

    @Test
    @DisplayName("Debería incrementar contador desde cero si no existe")
    void testIncrementLoginCountFromZero() {
        // Arrange
        String path = "players." + testUuid.toString() + ".loginCount";
        when(mockConfig.getInt(path, 0)).thenReturn(0);

        // Act
        dataManager.incrementLoginCount(testUuid);

        // Assert
        verify(mockConfig).set(path, 1);
    }

    @Test
    @DisplayName("Debería agregar tiempo de juego correctamente")
    void testAddPlaytime() {
        // Arrange
        String path = "players." + testUuid.toString() + ".playtime";
        long currentPlaytime = 3600000L; // 1 hora en ms
        long sessionTime = 1800000L; // 30 min en ms
        when(mockConfig.getLong(path, 0L)).thenReturn(currentPlaytime);

        // Act
        dataManager.addPlaytime(testUuid, sessionTime);

        // Assert
        verify(mockConfig).getLong(path, 0L);
        verify(mockConfig).set(path, 5400000L); // 1h 30min
    }

    @Test
    @DisplayName("Debería incrementar muertes del jugador")
    void testIncrementDeaths() {
        // Arrange
        String path = "players." + testUuid.toString() + ".deaths";
        when(mockConfig.getInt(path, 0)).thenReturn(10);

        // Act
        dataManager.incrementDeaths(testUuid);

        // Assert
        verify(mockConfig).getInt(path, 0);
        verify(mockConfig).set(path, 11);
    }

    @Test
    @DisplayName("Debería incrementar kills del jugador")
    void testIncrementKills() {
        // Arrange
        String path = "players." + testUuid.toString() + ".kills";
        when(mockConfig.getInt(path, 0)).thenReturn(25);

        // Act
        dataManager.incrementKills(testUuid);

        // Assert
        verify(mockConfig).getInt(path, 0);
        verify(mockConfig).set(path, 26);
    }
    @Test
    @DisplayName("Debería obtener fecha de nacimiento del jugador")
    void testGetBirthdate() {
        String expectedBirthdate = "15-03-2005";
        String path = "players." + testUuid.toString() + ".birthdate";
        when(mockConfig.getString(path)).thenReturn(expectedBirthdate);

        String result = dataManager.getBirthdate(testUuid);

        assertEquals(expectedBirthdate, result);
        verify(mockConfig).getString(path);
    }
}