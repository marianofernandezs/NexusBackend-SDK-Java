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
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("LoginCommand Tests")
class LoginCommandTest {

    private LoginCommand loginCommand;
    private PraxCorePlugin mockPlugin;
    private DataManager mockDataManager;
    private Player mockPlayer;
    private Command mockCommand;
    private UUID testUUID;

    @BeforeEach
    void setUp() {
        // Inicializar mocks
        mockPlugin = mock(PraxCorePlugin.class);
        mockDataManager = mock(DataManager.class);
        mockPlayer = mock(Player.class);
        mockCommand = mock(Command.class);

        // UUID de prueba
        testUUID = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(testUUID);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        when(mockPlugin.getDataManager()).thenReturn(mockDataManager);

        // Crear instancia del comando
        loginCommand = new LoginCommand(mockPlugin);
    }

    @Test
    @DisplayName("Debe rechazar ejecutores que no sean jugadores")
    void testNonPlayerSender() {
        // Arrange
        var mockSender = mock(org.bukkit.command.CommandSender.class);

        // Act
        boolean result = loginCommand.onCommand(mockSender, mockCommand, "login", new String[]{"email@test.com", "password"});

        // Assert
        assertTrue(result);
        verify(mockSender).sendMessage("Este comando solo puede ser ejecutado por un jugador.");
        verify(mockPlugin, never()).sendCreateSessionAuth(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe rechazar jugadores no registrados")
    void testUnregisteredPlayer() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);

        // Act
        boolean result = loginCommand.onCommand(mockPlayer, mockCommand, "login",
                new String[]{"email@test.com", "password123"});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage("§cNo estás registrado. Usa /register <email> <contraseña> <contraseña> <fecha_nacimiento> para crear una cuenta.");
        verify(mockPlugin, never()).sendCreateSessionAuth(any(), anyString(), anyString());
    }

    @ParameterizedTest
    @DisplayName("Debe rechazar formatos de comando incorrectos")
    @CsvSource({
            "0, ''",
            "1, 'email@test.com'",
            "3, 'email@test.com,password,extra'"
    })
    void testInvalidCommandFormat(int argCount, String argsString) {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);
        String[] args = argsString.isEmpty() ? new String[]{} : argsString.split(",");

        // Act
        boolean result = loginCommand.onCommand(mockPlayer, mockCommand, "login", args);

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage("§cUso correcto: /login <email> <contraseña>");
        verify(mockPlugin, never()).sendCreateSessionAuth(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe informar si el jugador ya está autenticado")
    void testAlreadyAuthenticated() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(true);

        // Act
        boolean result = loginCommand.onCommand(mockPlayer, mockCommand, "login",
                new String[]{"email@test.com", "password123"});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage("§eYa estás autenticado.");
        verify(mockPlugin, never()).sendCreateSessionAuth(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe procesar login correctamente con credenciales válidas")
    void testSuccessfulLogin() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(false);
        String email = "usuario@test.com";
        String password = "password123";

        // Act
        boolean result = loginCommand.onCommand(mockPlayer, mockCommand, "login",
                new String[]{email, password});

        // Assert
        assertTrue(result);

        // Verificar mensajes en orden
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockPlayer).sendMessage(messageCaptor.capture());
        assertEquals("§7Validando tus credenciales con PraxSuite...", messageCaptor.getValue());

        // Verificar que se envió la solicitud de autenticación
        verify(mockPlugin).sendCreateSessionAuth(mockPlayer, email, password);
    }

    @Test
    @DisplayName("Debe manejar emails con diferentes formatos válidos")
    void testVariousEmailFormats() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(false);

        String[] emails = {
                "test@example.com",
                "user.name@domain.co.uk",
                "user+tag@gmail.com",
                "123@numbers.com",
                "a@b.c"
        };

        // Act & Assert
        for (String email : emails) {
            loginCommand.onCommand(mockPlayer, mockCommand, "login",
                    new String[]{email, "password"});
            verify(mockPlugin).sendCreateSessionAuth(mockPlayer, email, "password");
            reset(mockPlugin);
            when(mockPlugin.getDataManager()).thenReturn(mockDataManager);
        }
    }

    @Test
    @DisplayName("Debe manejar contraseñas con espacios y caracteres especiales")
    void testPasswordsWithSpecialCharacters() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(false);

        String email = "test@example.com";
        String complexPassword = "P@ssw0rd!2024#Complex";

        // Act
        boolean result = loginCommand.onCommand(mockPlayer, mockCommand, "login",
                new String[]{email, complexPassword});

        // Assert
        assertTrue(result);
        verify(mockPlugin).sendCreateSessionAuth(mockPlayer, email, complexPassword);
    }

    @Test
    @DisplayName("Debe verificar el orden correcto de validaciones")
    void testValidationOrder() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(false);

        // Act
        loginCommand.onCommand(mockPlayer, mockCommand, "login",
                new String[]{"email@test.com", "password"});

        // Assert - Verificar orden de llamadas
        var inOrder = inOrder(mockDataManager, mockPlugin, mockPlayer);
        inOrder.verify(mockDataManager).isPlayerRegistered(testUUID);
        inOrder.verify(mockPlugin).isAuthenticated(testUUID);
        inOrder.verify(mockPlayer).sendMessage(anyString());
        inOrder.verify(mockPlugin).sendCreateSessionAuth(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe funcionar con diferentes labels del comando")
    void testDifferentCommandLabels() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(false);
        String[] args = {"test@example.com", "pass"};

        // Act & Assert
        assertTrue(loginCommand.onCommand(mockPlayer, mockCommand, "login", args));
        assertTrue(loginCommand.onCommand(mockPlayer, mockCommand, "LOGIN", args));
        assertTrue(loginCommand.onCommand(mockPlayer, mockCommand, "l", args));

        verify(mockPlugin, times(3)).sendCreateSessionAuth(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe manejar intentos de login consecutivos")
    void testConsecutiveLoginAttempts() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(false);

        // Act - Primer intento
        loginCommand.onCommand(mockPlayer, mockCommand, "login",
                new String[]{"test@example.com", "wrong_pass"});

        // Segundo intento (simular que sigue sin autenticar)
        loginCommand.onCommand(mockPlayer, mockCommand, "login",
                new String[]{"test@example.com", "correct_pass"});

        // Assert
        verify(mockPlugin, times(2)).sendCreateSessionAuth(eq(mockPlayer),
                eq("test@example.com"), anyString());
    }

    @Test
    @DisplayName("Debe prevenir login cuando ya está autenticado incluso con credenciales válidas")
    void testPreventDoubleAuthentication() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(true);

        // Act
        loginCommand.onCommand(mockPlayer, mockCommand, "login",
                new String[]{"test@example.com", "password"});

        // Assert
        verify(mockPlugin, never()).sendCreateSessionAuth(any(), anyString(), anyString());
        verify(mockPlayer).sendMessage("§eYa estás autenticado.");
    }

    @Test
    @DisplayName("Debe capturar correctamente los argumentos del comando")
    void testArgumentCapture() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(true);
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(false);

        String expectedEmail = "capture@test.com";
        String expectedPassword = "capturePass123";

        // Act
        loginCommand.onCommand(mockPlayer, mockCommand, "login",
                new String[]{expectedEmail, expectedPassword});

        // Assert
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockPlugin).sendCreateSessionAuth(eq(mockPlayer),
                emailCaptor.capture(), passwordCaptor.capture());

        assertEquals(expectedEmail, emailCaptor.getValue());
        assertEquals(expectedPassword, passwordCaptor.getValue());
    }

    @Test
    @DisplayName("Debe manejar caso cuando DataManager devuelve null")
    void testNullDataManager() {
        // Arrange
        when(mockPlugin.getDataManager()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            loginCommand.onCommand(mockPlayer, mockCommand, "login",
                    new String[]{"test@example.com", "password"});
        });
    }

    @Test
    @DisplayName("Debe validar que el jugador esté registrado antes de validar argumentos")
    void testRegistrationCheckBeforeArgumentValidation() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUUID)).thenReturn(false);

        // Act - Enviar con argumentos válidos pero jugador no registrado
        loginCommand.onCommand(mockPlayer, mockCommand, "login",
                new String[]{"test@example.com", "password"});

        // Assert - No debe validar formato de argumentos si no está registrado
        verify(mockPlayer, never()).sendMessage("§cUso correcto: /login <email> <contraseña>");
        verify(mockPlayer).sendMessage(contains("No estás registrado"));
    }
}