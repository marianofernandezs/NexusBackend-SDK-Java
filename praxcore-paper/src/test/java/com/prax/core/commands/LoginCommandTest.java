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
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests de LoginCommand - Simplificado")
class LoginCommandTest {

    @Mock
    private DataManager mockDataManager;

    @Mock
    private Player mockPlayer;

    @Mock
    private Command mockCommand;

    @Mock
    private CommandSender mockSender;

    private LoginCommand loginCommand;
    private UUID testUuid;
    private PraxCorePlugin mockPlugin;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Crear un mock más simple del plugin
        mockPlugin = mock(PraxCorePlugin.class, withSettings().lenient());
        when(mockPlugin.getDataManager()).thenReturn(mockDataManager);

        loginCommand = new LoginCommand(mockPlugin);

        testUuid = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(testUuid);
    }

    @Test
    @DisplayName("Debería rechazar comando desde consola")
    void testCommandFromConsole() {
        // Act
        boolean result = loginCommand.onCommand(mockSender, mockCommand, "login", new String[]{"password"});

        // Assert
        assertTrue(result);
        verify(mockSender).sendMessage(anyString());
    }

    @Test
    @DisplayName("Debería rechazar login sin registro")
    void testLoginNotRegistered() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUuid)).thenReturn(false);

        // Act
        boolean result = loginCommand.onCommand(mockPlayer, mockCommand, "login", new String[]{"password"});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage(contains("No estás registrado"));
    }

    @Test
    @DisplayName("Debería rechazar login sin argumentos")
    void testLoginWithoutArgs() {
        // Arrange
        when(mockDataManager.isPlayerRegistered(testUuid)).thenReturn(true);

        // Act
        boolean result = loginCommand.onCommand(mockPlayer, mockCommand, "login", new String[]{});

        // Assert
        assertFalse(result);
        verify(mockPlayer).sendMessage(contains("uso correcto"));
    }

    @Test
    @DisplayName("Debería validar contraseña con BCrypt")
    void testPasswordValidation() {
        // Arrange
        String password = "testPassword123";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        when(mockDataManager.isPlayerRegistered(testUuid)).thenReturn(true);
        when(mockDataManager.getPasswordHash(testUuid)).thenReturn(hashedPassword);
        when(mockPlugin.isAuthenticated(testUuid)).thenReturn(false);

        // Act
        boolean result = loginCommand.onCommand(mockPlayer, mockCommand, "login", new String[]{password});

        // Assert
        assertTrue(result);
        verify(mockDataManager).incrementLoginCount(testUuid);
        verify(mockPlayer).sendMessage(contains("iniciado sesión correctamente"));
    }

    @Test
    @DisplayName("Debería rechazar contraseña incorrecta")
    void testWrongPassword() {
        // Arrange
        String correctPassword = "correct123";
        String wrongPassword = "wrong123";
        String hashedPassword = BCrypt.hashpw(correctPassword, BCrypt.gensalt());

        when(mockDataManager.isPlayerRegistered(testUuid)).thenReturn(true);
        when(mockDataManager.getPasswordHash(testUuid)).thenReturn(hashedPassword);

        // Act
        boolean result = loginCommand.onCommand(mockPlayer, mockCommand, "login", new String[]{wrongPassword});

        // Assert
        assertTrue(result);
        verify(mockPlayer).sendMessage(contains("Contraseña incorrecta"));
    }
}