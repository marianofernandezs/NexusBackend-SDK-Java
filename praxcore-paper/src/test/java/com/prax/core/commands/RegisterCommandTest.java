package com.prax.core.commands;

import com.prax.core.DataManager;
import com.prax.core.PraxCorePlugin;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RegisterCommand Tests")
class RegisterCommandTest {

    private RegisterCommand registerCommand;
    private PraxCorePlugin mockPlugin;
    private DataManager mockDataManager;
    private Player mockPlayer;
    private Command mockCommand;
    private UUID testUUID;
    private InetSocketAddress mockAddress;

    @BeforeEach
    void setUp() {
        // Inicializar mocks
        mockPlugin = mock(PraxCorePlugin.class);
        mockDataManager = mock(DataManager.class);
        mockPlayer = mock(Player.class);
        mockCommand = mock(Command.class);
        mockAddress = mock(InetSocketAddress.class);

        // UUID de prueba (Java Edition)
        testUUID = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(testUUID);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        when(mockPlugin.getDataManager()).thenReturn(mockDataManager);
        when(mockPlayer.getAddress()).thenReturn(mockAddress);
        when(mockAddress.getAddress()).thenReturn(java.net.InetAddress.getLoopbackAddress());
        when(mockPlayer.getProtocolVersion()).thenReturn(763); // Versión 1.20

        // Crear instancia del comando
        registerCommand = new RegisterCommand(mockPlugin);
    }

    @Test
    @DisplayName("Debe rechazar ejecutores que no sean jugadores")
    void testNonPlayerSender() {
        // Arrange
        var mockSender = mock(org.bukkit.command.CommandSender.class);

        // Act
        boolean result = registerCommand.onCommand(mockSender, mockCommand, "register",
                new String[]{"test@example.com", "pass123", "pass123", "15-03-2000"});

        // Assert
        assertTrue(result);
        verify(mockSender).sendMessage("Este comando solo puede ser ejecutado por un jugador.");
        verify(mockDataManager, never()).registerPlayer(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe rechazar jugadores ya registrados")
    void testAlreadyRegisteredPlayer() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"test@example.com", "pass123", "pass123", "15-03-2000"});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage("§c¡Ya estás registrado! Usa /login <contraseña> para entrar.");
        verify(mockDataManager, never()).registerPlayer(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @DisplayName("Debe rechazar cantidad incorrecta de argumentos")
    @CsvSource({
            "0, ''",
            "1, 'email@test.com'",
            "2, 'email@test.com,password'",
            "3, 'email@test.com,password,password'",
            "5, 'email@test.com,password,password,01-01-2000,extra'"
    })
    void testInvalidArgumentCount(int argCount, String argsString) {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);
        String[] args = argsString.isEmpty() ? new String[]{} : argsString.split(",");

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register", args);

        // Assert
        assertFalse(result);
        verify(mockPlayer).sendMessage(contains("Error: El uso correcto es:"));
    }

    @ParameterizedTest
    @DisplayName("Debe rechazar emails inválidos")
    @ValueSource(strings = {
            "invalid",
            "no-at-sign.com",
            "no-dot@domain"
    })
    void testInvalidEmails(String invalidEmail) {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{invalidEmail, "pass123", "pass123", "15-03-2000"});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage("§cPor favor, introduce un correo electrónico válido.");
        verify(mockDataManager, never()).registerPlayer(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe aceptar emails válidos básicos")
    void testValidBasicEmails() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);

        String[] validEmails = {
                "user@example.com",
                "test@test.co"
        };

        // Act & Assert
        for (String email : validEmails) {
            registerCommand.onCommand(mockPlayer, mockCommand, "register",
                    new String[]{email, "pass123", "pass123", "15-03-2000"});

            verify(mockDataManager, atLeastOnce()).registerPlayer(any(), anyString(), eq(email), anyString(), anyString(), anyString(), anyString());
            reset(mockDataManager, mockPlayer, mockPlugin);
            when(mockPlayer.getUniqueId()).thenReturn(testUUID);
            when(mockPlayer.getAddress()).thenReturn(mockAddress);
            when(mockAddress.getAddress()).thenReturn(java.net.InetAddress.getLoopbackAddress());
            when(mockPlayer.getProtocolVersion()).thenReturn(763);
            when(mockPlugin.getDataManager()).thenReturn(mockDataManager);
        }
    }

    @Test
    @DisplayName("Debe rechazar cuando las contraseñas no coinciden")
    void testPasswordMismatch() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"test@example.com", "password1", "password2", "15-03-2000"});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage("§cLas contraseñas no coinciden. Inténtalo de nuevo.");
        verify(mockDataManager, never()).registerPlayer(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @DisplayName("Debe rechazar formatos de fecha inválidos")
    @ValueSource(strings = {
            "2000-03-15",      // Formato incorrecto (YYYY-MM-DD)
            "15/03/2000",      // Separador incorrecto
            "15.03.2000",      // Separador incorrecto
            "32-01-2000",      // Día inválido
            "15-13-2000",      // Mes inválido
            "15-03-99",        // Año incompleto
            "invalid-date",    // No es fecha
            "15-3-2000",       // Mes sin cero
            "5-03-2000"        // Día sin cero
    })
    void testInvalidDateFormats(String invalidDate) {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"test@example.com", "pass123", "pass123", invalidDate});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage("§cFormato de fecha inválido.");
        verify(mockPlayer).sendMessage("§eUsa el formato: DD-MM-YYYY (ejemplo: 15-03-2005)");
        verify(mockDataManager, never()).registerPlayer(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe rechazar fechas de nacimiento futuras")
    void testFutureBirthdate() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);
        LocalDate futureDate = LocalDate.now().plusYears(1);
        String futureDateStr = futureDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"test@example.com", "pass123", "pass123", futureDateStr});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage("§cLa fecha de nacimiento no puede ser en el futuro.");
        verify(mockDataManager, never()).registerPlayer(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe rechazar menores de 13 años")
    void testUnderageUser() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);
        // Fecha que resulta en 12 años de edad
        LocalDate underageDate = LocalDate.now().minusYears(12).minusDays(1);
        String undageDateStr = underageDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"test@example.com", "pass123", "pass123", undageDateStr});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage("§cDebes tener al menos 13 años para registrarte.");
        verify(mockPlayer).sendMessage("§7Si crees que esto es un error, contacta a un administrador.");
        verify(mockDataManager, never()).registerPlayer(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe rechazar edades mayores a 100 años")
    void testUnrealisticAge() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);
        // Fecha que resulta en 101 años de edad
        LocalDate ancientDate = LocalDate.now().minusYears(101);
        String ancientDateStr = ancientDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"test@example.com", "pass123", "pass123", ancientDateStr});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage(contains("verifica tu fecha de nacimiento"));
        verify(mockDataManager, never()).registerPlayer(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe registrar correctamente con datos válidos (Java Edition)")
    void testSuccessfulRegistrationJavaEdition() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);
        String email = "test@example.com";
        String password = "securePassword123";
        String birthdate = "15-03-2000";

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{email, password, password, birthdate});

        // Assert
        assertTrue(result);

        // Verificar que se llamó registerPlayer con los parámetros correctos
        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> hashedPasswordCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> versionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> birthdateCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockDataManager).registerPlayer(
                uuidCaptor.capture(),
                hashedPasswordCaptor.capture(),
                emailCaptor.capture(),
                ipCaptor.capture(),
                clientTypeCaptor.capture(),
                versionCaptor.capture(),
                birthdateCaptor.capture()
        );

        assertEquals(testUUID, uuidCaptor.getValue());
        assertNotEquals(password, hashedPasswordCaptor.getValue()); // La contraseña debe estar hasheada
        assertTrue(hashedPasswordCaptor.getValue().startsWith("$2a$")); // BCrypt hash
        assertEquals(email, emailCaptor.getValue());
        assertEquals("127.0.0.1", ipCaptor.getValue());
        assertEquals("JAVA", clientTypeCaptor.getValue());
        assertEquals("763", versionCaptor.getValue());
        assertEquals(birthdate, birthdateCaptor.getValue());

        // Verificar métodos de DataManager
        verify(mockDataManager).setFirstLoginDate(eq(testUUID), anyString());
        verify(mockDataManager).incrementLoginCount(testUUID);
        verify(mockDataManager).setLastLoginDate(eq(testUUID), anyString());

        // Verificar que se envió la solicitud al proxy
        verify(mockPlugin).sendRegisterRequest(mockPlayer, email, password, birthdate);

        // Verificar mensajes
        verify(mockPlayer).sendMessage("§7Enviando solicitud de registro a PraxSuite...");
        verify(mockPlayer).sendMessage("§a¡Te has registrado exitosamente con el email " + email + "! Ahora, por favor, inicia sesión.");
    }

    @Test
    @DisplayName("Debe detectar cliente Bedrock por UUID Floodgate")
    void testBedrockClientDetection() {
        // Arrange
        UUID bedrockUUID = UUID.fromString("00000000-0000-0000-0001-123456789abc");
        when(mockPlayer.getUniqueId()).thenReturn(bedrockUUID);
        when(mockDataManager.isPlayerRegistered(bedrockUUID)).thenReturn(false);

        // Act
        registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"test@example.com", "pass123", "pass123", "15-03-2000"});

        // Assert
        ArgumentCaptor<String> clientTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockDataManager).registerPlayer(
                any(UUID.class),
                anyString(),
                anyString(),
                anyString(),
                clientTypeCaptor.capture(),
                anyString(),
                anyString()
        );

        assertEquals("BEDROCK", clientTypeCaptor.getValue());
    }

    @Test
    @DisplayName("Debe manejar dirección IP null")
    void testNullIPAddress() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);
        when(mockPlayer.getAddress()).thenReturn(null);

        // Act
        registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"test@example.com", "pass123", "pass123", "15-03-2000"});

        // Assert
        ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockDataManager).registerPlayer(
                any(UUID.class),
                anyString(),
                anyString(),
                ipCaptor.capture(),
                anyString(),
                anyString(),
                anyString()
        );

        assertEquals("IP Desconocida", ipCaptor.getValue());
    }

    @Test
    @DisplayName("Debe validar edad exactamente en el límite (13 años)")
    void testExactMinimumAge() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);
        // Exactamente 13 años hoy
        LocalDate minAgeDate = LocalDate.now().minusYears(13);
        String minAgeDateStr = minAgeDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"test@example.com", "pass123", "pass123", minAgeDateStr});

        // Assert
        assertTrue(result);
        verify(mockDataManager).registerPlayer(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(mockPlayer, never()).sendMessage(contains("al menos 13 años"));
    }

    @Test
    @DisplayName("Debe validar edad exactamente en el límite (100 años)")
    void testExactMaximumAge() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);
        // Exactamente 100 años hoy
        LocalDate maxAgeDate = LocalDate.now().minusYears(100);
        String maxAgeDateStr = maxAgeDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"test@example.com", "pass123", "pass123", maxAgeDateStr});

        // Assert
        assertTrue(result);
        verify(mockDataManager).registerPlayer(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(mockPlayer, never()).sendMessage(contains("verifica tu fecha de nacimiento"));
    }

    @Test
    @DisplayName("Debe hashear la contraseña con BCrypt antes de guardar")
    void testPasswordHashing() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);
        String plainPassword = "mySecretPassword";

        // Act
        registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"test@example.com", plainPassword, plainPassword, "15-03-2000"});

        // Assert
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockDataManager).registerPlayer(
                any(UUID.class),
                passwordCaptor.capture(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );

        String hashedPassword = passwordCaptor.getValue();
        assertNotEquals(plainPassword, hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$"));
        assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw(plainPassword, hashedPassword));
    }



    @Test
    @DisplayName("Debe verificar el orden de validaciones")
    void testValidationOrder() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);

        // Act
        registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"invalid-email", "pass1", "pass2", "invalid-date"});

        // Assert - Debe fallar primero en validación de email
        verify(mockPlayer).sendMessage("§cPor favor, introduce un correo electrónico válido.");
        verify(mockPlayer, never()).sendMessage(contains("contraseñas no coinciden"));
        verify(mockPlayer, never()).sendMessage(contains("Formato de fecha"));
    }

    @Test
    @DisplayName("Debe aceptar fecha de nacimiento de hoy hace exactamente 13 años")
    void testBirthdayTodayExactly13YearsAgo() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);
        LocalDate exactly13 = LocalDate.now().minusYears(13);
        String dateStr = exactly13.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        // Act
        boolean result = registerCommand.onCommand(mockPlayer, mockCommand, "register",
                new String[]{"test@example.com", "pass123", "pass123", dateStr});

        // Assert
        assertTrue(result);
        verify(mockDataManager).registerPlayer(any(), anyString(), anyString(), anyString(), anyString(), anyString(), eq(dateStr));
    }
}