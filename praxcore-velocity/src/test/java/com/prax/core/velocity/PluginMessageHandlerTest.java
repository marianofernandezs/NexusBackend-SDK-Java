package com.prax.core.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.Disabled;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para PluginMessageHandler
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PluginMessageHandlerTest {

    @Mock
    private PraxProxyPlugin mockPlugin;

    @Mock
    private Logger mockLogger;

    @Mock
    private NexusAuthService mockAuthService;

    @Mock
    private TokenManager mockTokenManager;

    @Mock
    private Player mockPlayer;

    @Mock
    private ServerConnection mockServerConnection;

    @Mock
    private RegisteredServer mockRegisteredServer;

    @Mock
    private PluginMessageEvent mockEvent;

    private PluginMessageHandler handler;

    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final String TEST_USERNAME = "TestPlayer";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_BIRTHDATE = "1990-01-01";
    private static final String TEST_TOKEN = "test-jwt-token";
    private static final MinecraftChannelIdentifier PRAX_CHANNEL =
            MinecraftChannelIdentifier.from("prax:core");

    @BeforeEach
    void setUp() {
        // Configurar mocks básicos (lenient para evitar UnnecessaryStubbing)
        lenient().when(mockPlugin.getLogger()).thenReturn(mockLogger);
        lenient().when(mockPlugin.getAuthService()).thenReturn(mockAuthService);
        lenient().when(mockPlugin.getTokenManager()).thenReturn(mockTokenManager);
        lenient().when(mockPlayer.getUniqueId()).thenReturn(TEST_UUID);
        lenient().when(mockPlayer.getUsername()).thenReturn(TEST_USERNAME);
        lenient().when(mockServerConnection.getPlayer()).thenReturn(mockPlayer);
        lenient().when(mockPlayer.getCurrentServer()).thenReturn(Optional.of(mockServerConnection));
        lenient().when(mockServerConnection.getServer()).thenReturn(mockRegisteredServer);

        // Crear handler
        handler = new PluginMessageHandler(mockPlugin);
    }

    // ==========================================
    // TESTS DE FILTRADO DE MENSAJES
    // ==========================================

    @Test
    void testOnPluginMessage_IgnoresWrongChannel() {
        // Arrange
        MinecraftChannelIdentifier wrongChannel = MinecraftChannelIdentifier.from("wrong:channel");
        when(mockEvent.getIdentifier()).thenReturn(wrongChannel);

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockAuthService, never()).login(any(), any(), any());
        verify(mockAuthService, never()).validate(any());
    }

    @Test
    void testOnPluginMessage_IgnoresNonServerConnection() {
        // Arrange
        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockPlayer); // No es ServerConnection

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockAuthService, never()).login(any(), any(), any());
    }

    // ==========================================
    // TESTS DE CREATE SESSION (LOGIN)
    // ==========================================

    @Test
    void testHandleCreateSession_Success() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("CreateSession");
        out.writeUTF(TEST_UUID.toString());
        out.writeUTF(TEST_EMAIL);
        out.writeUTF(TEST_PASSWORD);

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());
        when(mockAuthService.login(any(), eq(TEST_EMAIL), eq(TEST_PASSWORD)))
                .thenReturn(Optional.of(TEST_TOKEN));

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockAuthService, times(1)).login(TEST_UUID, TEST_EMAIL, TEST_PASSWORD);
        verify(mockTokenManager, times(1)).storeToken(TEST_UUID, TEST_TOKEN);

        // Verificar que se envió la respuesta
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, times(1)).sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        // Verificar contenido de la respuesta
        ByteArrayDataInput response = ByteStreams.newDataInput(messageCaptor.getValue());
        assertEquals("ValidationResponse", response.readUTF());
        assertEquals(TEST_UUID.toString(), response.readUTF());
        assertTrue(response.readBoolean());
    }

    @Test
    void testHandleCreateSession_Failure() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("CreateSession");
        out.writeUTF(TEST_UUID.toString());
        out.writeUTF(TEST_EMAIL);
        out.writeUTF(TEST_PASSWORD);

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());
        when(mockAuthService.login(any(), eq(TEST_EMAIL), eq(TEST_PASSWORD)))
                .thenReturn(Optional.empty());

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockAuthService, times(1)).login(TEST_UUID, TEST_EMAIL, TEST_PASSWORD);
        verify(mockTokenManager, never()).storeToken(any(), any());

        // Verificar respuesta negativa
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, times(1)).sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        ByteArrayDataInput response = ByteStreams.newDataInput(messageCaptor.getValue());
        assertEquals("ValidationResponse", response.readUTF());
        assertEquals(TEST_UUID.toString(), response.readUTF());
        assertFalse(response.readBoolean());
    }

    // ==========================================
    // TESTS DE VALIDATE TOKEN
    // ==========================================

    @Test
    void testHandleValidateToken_Success() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidateToken");
        out.writeUTF(TEST_UUID.toString());

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());
        when(mockTokenManager.sessionExists(TEST_UUID)).thenReturn(true);
        when(mockTokenManager.getToken(TEST_UUID)).thenReturn(TEST_TOKEN);
        when(mockAuthService.validate(TEST_TOKEN)).thenReturn(true);

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockTokenManager, times(1)).sessionExists(TEST_UUID);
        verify(mockTokenManager, times(1)).getToken(TEST_UUID);
        verify(mockAuthService, times(1)).validate(TEST_TOKEN);
        verify(mockTokenManager, never()).removeSession(TEST_UUID);

        // Verificar respuesta
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, times(1)).sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        ByteArrayDataInput response = ByteStreams.newDataInput(messageCaptor.getValue());
        assertEquals("ValidationResponse", response.readUTF());
        assertEquals(TEST_UUID.toString(), response.readUTF());
        assertTrue(response.readBoolean());
    }

    @Test
    void testHandleValidateToken_InvalidToken() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidateToken");
        out.writeUTF(TEST_UUID.toString());

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());
        when(mockTokenManager.sessionExists(TEST_UUID)).thenReturn(true);
        when(mockTokenManager.getToken(TEST_UUID)).thenReturn(TEST_TOKEN);
        when(mockAuthService.validate(TEST_TOKEN)).thenReturn(false);

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockAuthService, times(1)).validate(TEST_TOKEN);
        verify(mockTokenManager, times(1)).removeSession(TEST_UUID);

        // Verificar respuesta negativa
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, times(1)).sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        ByteArrayDataInput response = ByteStreams.newDataInput(messageCaptor.getValue());
        assertEquals("ValidationResponse", response.readUTF());
        assertEquals(TEST_UUID.toString(), response.readUTF());
        assertFalse(response.readBoolean());
    }

    @Test
    void testHandleValidateToken_NoSession() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidateToken");
        out.writeUTF(TEST_UUID.toString());

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());
        when(mockTokenManager.sessionExists(TEST_UUID)).thenReturn(false);

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockAuthService, never()).validate(any());

        // Verificar respuesta negativa
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, times(1)).sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        ByteArrayDataInput response = ByteStreams.newDataInput(messageCaptor.getValue());
        assertEquals("ValidationResponse", response.readUTF());
        assertEquals(TEST_UUID.toString(), response.readUTF());
        assertFalse(response.readBoolean());
    }

    // ==========================================
    // TESTS DE REGISTER REQUEST
    // ==========================================

    @Test
    void testHandleRegisterRequest_Success() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("RegisterRequest");
        out.writeUTF(TEST_UUID.toString());
        out.writeUTF(TEST_EMAIL);
        out.writeUTF(TEST_PASSWORD);
        out.writeUTF(TEST_BIRTHDATE);
        out.writeUTF(TEST_USERNAME);

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());
        when(mockAuthService.register(TEST_UUID, TEST_EMAIL, TEST_PASSWORD, TEST_BIRTHDATE, TEST_USERNAME))
                .thenReturn(true);

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockAuthService, times(1))
                .register(TEST_UUID, TEST_EMAIL, TEST_PASSWORD, TEST_BIRTHDATE, TEST_USERNAME);

        // Verificar respuesta
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, times(1)).sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        ByteArrayDataInput response = ByteStreams.newDataInput(messageCaptor.getValue());
        assertEquals("RegisterResponse", response.readUTF());
        assertEquals(TEST_UUID.toString(), response.readUTF());
        assertTrue(response.readBoolean());
    }

    @Test
    void testHandleRegisterRequest_Failure() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("RegisterRequest");
        out.writeUTF(TEST_UUID.toString());
        out.writeUTF(TEST_EMAIL);
        out.writeUTF(TEST_PASSWORD);
        out.writeUTF(TEST_BIRTHDATE);
        out.writeUTF(TEST_USERNAME);

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());
        when(mockAuthService.register(TEST_UUID, TEST_EMAIL, TEST_PASSWORD, TEST_BIRTHDATE, TEST_USERNAME))
                .thenReturn(false);

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockAuthService, times(1))
                .register(TEST_UUID, TEST_EMAIL, TEST_PASSWORD, TEST_BIRTHDATE, TEST_USERNAME);

        // Verificar respuesta negativa
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, times(1)).sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        ByteArrayDataInput response = ByteStreams.newDataInput(messageCaptor.getValue());
        assertEquals("RegisterResponse", response.readUTF());
        assertEquals(TEST_UUID.toString(), response.readUTF());
        assertFalse(response.readBoolean());
    }

    // ==========================================
    // TESTS DE LOGOUT SESSION
    // ==========================================

    @Test
    void testHandleLogoutSession_Success() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("LogoutSession");
        out.writeUTF(TEST_UUID.toString());

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());
        when(mockTokenManager.sessionExists(TEST_UUID)).thenReturn(true);
        when(mockTokenManager.getToken(TEST_UUID)).thenReturn(TEST_TOKEN);
        when(mockAuthService.logout(TEST_TOKEN)).thenReturn(true);

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockTokenManager, times(1)).sessionExists(TEST_UUID);
        verify(mockTokenManager, times(1)).getToken(TEST_UUID);
        verify(mockAuthService, times(1)).logout(TEST_TOKEN);
        verify(mockTokenManager, times(1)).removeSession(TEST_UUID);

        // Verificar respuesta
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, times(1)).sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        ByteArrayDataInput response = ByteStreams.newDataInput(messageCaptor.getValue());
        assertEquals("LogoutResponse", response.readUTF());
        assertEquals(TEST_UUID.toString(), response.readUTF());
        assertTrue(response.readBoolean());
    }

    @Test
    void testHandleLogoutSession_NoSession() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("LogoutSession");
        out.writeUTF(TEST_UUID.toString());

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());
        when(mockTokenManager.sessionExists(TEST_UUID)).thenReturn(false);

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockAuthService, never()).logout(any());
        verify(mockTokenManager, never()).removeSession(TEST_UUID);

        // Verificar respuesta (considera exitoso si no hay sesión)
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, times(1)).sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        ByteArrayDataInput response = ByteStreams.newDataInput(messageCaptor.getValue());
        assertEquals("LogoutResponse", response.readUTF());
        assertEquals(TEST_UUID.toString(), response.readUTF());
        assertTrue(response.readBoolean());
    }

    @Test
    void testHandleLogoutSession_BackendFailure() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("LogoutSession");
        out.writeUTF(TEST_UUID.toString());

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());
        when(mockTokenManager.sessionExists(TEST_UUID)).thenReturn(true);
        when(mockTokenManager.getToken(TEST_UUID)).thenReturn(TEST_TOKEN);
        when(mockAuthService.logout(TEST_TOKEN)).thenReturn(false);

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockAuthService, times(1)).logout(TEST_TOKEN);
        verify(mockTokenManager, never()).removeSession(TEST_UUID); // No limpia si el backend falla

        // Verificar respuesta negativa
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockServerConnection, times(1)).sendPluginMessage(eq(PRAX_CHANNEL), messageCaptor.capture());

        ByteArrayDataInput response = ByteStreams.newDataInput(messageCaptor.getValue());
        assertEquals("LogoutResponse", response.readUTF());
        assertEquals(TEST_UUID.toString(), response.readUTF());
        assertFalse(response.readBoolean());
    }

    // ==========================================
    // TESTS DE MANEJO DE ERRORES
    // ==========================================

    @Test
    void testHandleUnknownSubChannel() {
        // Arrange
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("UnknownChannel");

        when(mockEvent.getIdentifier()).thenReturn(PRAX_CHANNEL);
        when(mockEvent.getSource()).thenReturn(mockServerConnection);
        when(mockEvent.getData()).thenReturn(out.toByteArray());

        // Act
        handler.onPluginMessage(mockEvent);

        // Assert
        verify(mockLogger, times(1)).warn(contains("Subcanal desconocido"), eq("UnknownChannel"));
        verify(mockAuthService, never()).login(any(), any(), any());
        verify(mockAuthService, never()).validate(any());
    }
}