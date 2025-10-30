package com.prax.core.commands;

import com.prax.core.DataManager;
import com.prax.core.PraxCorePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests de RegisterCommand con Fecha de Nacimiento")
class RegisterCommandTest {

    @Mock
    private PraxCorePlugin mockPlugin;

    @Mock
    private DataManager mockDataManager;

    @Mock
    private Player mockPlayer;

    @Mock
    private Command mockCommand;

    @Mock
    private CommandSender mockSender;

    private RegisterCommand registerCommand;
    private UUID testUuid;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockPlugin = mock(PraxCorePlugin.class, withSettings().lenient());
        when(mockPlugin.getDataManager()).thenReturn(mockDataManager);

        registerCommand = new RegisterCommand(mockPlugin);

        testUuid = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(testUuid);

        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), 25565);
            when(mockPlayer.getAddress()).thenReturn(address);
        } catch (Exception e) {
            when(mockPlayer.getAddress()).thenReturn(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), 25565)
            );
        }
        when(mockPlayer.getProtocolVersion()).thenReturn(763);
    }

    @Test
    @DisplayName("Debería rechazar registro sin fecha de nacimiento")
    void testRegisterWithoutBirthdate() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUuid)).thenReturn(false);

        // Act - Solo 3 argumentos (falta la fecha)
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"email@test.com", "pass123", "pass123"});

        // Assert
        assertFalse(result);
        verify(mockPlayer).sendMessage(contains("fecha_nacimiento"));
    }

    @Test
    @DisplayName("Debería rechazar formato de fecha inválido")
    void testInvalidDateFormat() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUuid)).thenReturn(false);

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"email@test.com", "pass123", "pass123", "2005-03-15"}); // Formato incorrecto

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage(contains("Formato de fecha inválido"));
    }

    @Test
    @DisplayName("Debería rechazar fecha futura")
    void testFutureDate() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUuid)).thenReturn(false);

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"email@test.com", "pass123", "pass123", "15-03-2030"}); // Fecha futura

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage(contains("no puede ser en el futuro"));
    }

    @Test
    @DisplayName("Debería rechazar edad menor a 13 años")
    void testUnderageRegistration() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUuid)).thenReturn(false);

        // Act - Fecha hace 10 años
        String recentDate = java.time.LocalDate.now().minusYears(10).format(
                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"email@test.com", "pass123", "pass123", recentDate});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage(contains("al menos 13 años"));
    }

    @Test
    @DisplayName("Debería rechazar edad mayor a 100 años")
    void testTooOldRegistration() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUuid)).thenReturn(false);

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"email@test.com", "pass123", "pass123", "01-01-1900"});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage(contains("verifica tu fecha"));
    }

    @Test
    @DisplayName("Debería registrar exitosamente con fecha válida")
    void testSuccessfulRegistrationWithBirthdate() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUuid)).thenReturn(false);

        // Act - Fecha válida (20 años)
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"email@test.com", "pass123", "pass123", "15-03-2005"});

        // Assert
        assertTrue(result);
        verify(mockDataManager).registerPlayer(
                eq(testUuid),
                anyString(), // hashedPassword
                eq("email@test.com"),
                anyString(), // IP
                eq("JAVA"),
                anyString(), // version
                eq("15-03-2005") // birthdate
        );
        verify(mockPlayer).sendMessage(contains("registrado exitosamente"));
    }

    @Test
    @DisplayName("Debería calcular edad correctamente")
    void testAgeCalculation() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUuid)).thenReturn(false);

        // Act - Persona de exactamente 18 años
        String date18YearsAgo = java.time.LocalDate.now().minusYears(18).format(
                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );

        registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"email@test.com", "pass123", "pass123", date18YearsAgo});

        // Assert
        verify(mockDataManager).registerPlayer(
                any(UUID.class),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(date18YearsAgo)
        );
    }
}