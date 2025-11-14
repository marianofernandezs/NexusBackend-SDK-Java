package com.prax.core.commands;

import com.prax.core.PraxCorePlugin;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("LogoutCommand Tests")
class LogoutCommandTest {

    private LogoutCommand logoutCommand;
    private PraxCorePlugin mockPlugin;
    private Player mockPlayer;
    private Command mockCommand;
    private UUID testUUID;

    @BeforeEach
    void setUp() {
        // Inicializar mocks
        mockPlugin = mock(PraxCorePlugin.class);
        mockPlayer = mock(Player.class);
        mockCommand = mock(Command.class);

        // UUID de prueba
        testUUID = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(testUUID);
        when(mockPlayer.getName()).thenReturn("TestPlayer");

        // Crear instancia del comando
        logoutCommand = new LogoutCommand(mockPlugin);
    }

    @Test
    @DisplayName("Debe rechazar ejecutores que no sean jugadores")
    void testNonPlayerSender() {
        // Arrange
        var mockSender = mock(org.bukkit.command.CommandSender.class);

        // Act
        boolean result = logoutCommand.onCommand(mockSender, mockCommand, "logout", new String[]{});

        // Assert
        assertTrue(result, "El comando debe retornar true");
        verify(mockSender).sendMessage("Este comando solo puede ser ejecutado por un jugador.");
        verify(mockPlugin, never()).sendLogoutRequest(any());
    }

    @Test
    @DisplayName("Debe informar si el jugador no tiene sesión activa")
    void testLogoutWhenNotAuthenticated() {
        // Arrange
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(false);

        // Act
        boolean result = logoutCommand.onCommand(mockPlayer, mockCommand, "logout", new String[]{});

        // Assert
        assertTrue(result, "El comando debe retornar true");
        verify(mockPlayer).sendMessage("§eNo tienes una sesión activa.");
        verify(mockPlugin, never()).sendLogoutRequest(any());
    }

    @Test
    @DisplayName("Debe procesar logout correctamente cuando el jugador está autenticado")
    void testSuccessfulLogout() {
        // Arrange
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(true);

        // Act
        boolean result = logoutCommand.onCommand(mockPlayer, mockCommand, "logout", new String[]{});

        // Assert
        assertTrue(result, "El comando debe retornar true");

        // Verificar mensajes en orden
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockPlayer, times(1)).sendMessage(messageCaptor.capture());
        assertEquals("§7Cerrando tu sesión...", messageCaptor.getValue());

        // Verificar que se envió la solicitud de logout
        verify(mockPlugin).sendLogoutRequest(mockPlayer);
    }

    @Test
    @DisplayName("Debe ignorar argumentos adicionales")
    void testLogoutWithExtraArguments() {
        // Arrange
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(true);
        String[] args = {"extra", "arguments", "ignored"};

        // Act
        boolean result = logoutCommand.onCommand(mockPlayer, mockCommand, "logout", args);

        // Assert
        assertTrue(result, "El comando debe retornar true");
        verify(mockPlugin).sendLogoutRequest(mockPlayer);
    }

    @Test
    @DisplayName("Debe manejar múltiples intentos de logout consecutivos")
    void testMultipleLogoutAttempts() {
        // Arrange
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(true);

        // Act - Primer logout
        logoutCommand.onCommand(mockPlayer, mockCommand, "logout", new String[]{});

        // Simular que ahora no está autenticado
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(false);

        // Act - Segundo logout
        logoutCommand.onCommand(mockPlayer, mockCommand, "logout", new String[]{});

        // Assert
        verify(mockPlugin, times(1)).sendLogoutRequest(mockPlayer);
        verify(mockPlayer).sendMessage("§eNo tienes una sesión activa.");
    }

    @Test
    @DisplayName("Debe verificar el estado de autenticación antes de procesar")
    void testAuthenticationCheckOrder() {
        // Arrange
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(false);

        // Act
        logoutCommand.onCommand(mockPlayer, mockCommand, "logout", new String[]{});

        // Assert - Verificar el orden de las llamadas
        var inOrder = inOrder(mockPlugin, mockPlayer);
        inOrder.verify(mockPlugin).isAuthenticated(testUUID);
        inOrder.verify(mockPlayer).sendMessage("§eNo tienes una sesión activa.");
        inOrder.verify(mockPlugin, never()).sendLogoutRequest(any());
    }

    @Test
    @DisplayName("Debe funcionar con diferentes labels del comando")
    void testDifferentCommandLabels() {
        // Arrange
        when(mockPlugin.isAuthenticated(testUUID)).thenReturn(true);

        // Act & Assert - Probar diferentes labels
        assertTrue(logoutCommand.onCommand(mockPlayer, mockCommand, "logout", new String[]{}));
        assertTrue(logoutCommand.onCommand(mockPlayer, mockCommand, "LOGOUT", new String[]{}));
        assertTrue(logoutCommand.onCommand(mockPlayer, mockCommand, "desconectar", new String[]{}));

        verify(mockPlugin, times(3)).sendLogoutRequest(mockPlayer);
    }

    @Test
    @DisplayName("Debe manejar correctamente cuando el plugin es null (caso extremo)")
    void testNullPluginHandling() {
        // Arrange
        LogoutCommand commandWithNullPlugin = new LogoutCommand(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            commandWithNullPlugin.onCommand(mockPlayer, mockCommand, "logout", new String[]{});
        });
    }
}