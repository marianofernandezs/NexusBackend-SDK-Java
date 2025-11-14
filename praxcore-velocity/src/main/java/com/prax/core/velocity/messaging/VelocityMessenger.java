package com.prax.core.velocity.messaging;

import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

public class VelocityMessenger {

    private final ProxyServer server;
    private final MinecraftChannelIdentifier channel;

    public VelocityMessenger(ProxyServer server, MinecraftChannelIdentifier channel) {
        this.server = server;
        this.channel = channel;
    }

    public void sendToAll(byte[] data) {
        for (RegisteredServer srv : server.getAllServers()) {
            srv.sendPluginMessage(channel, data);
        }
    }
}
