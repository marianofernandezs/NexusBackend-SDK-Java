package com.tesseractsoftwares.nexusbackend.sdkjava;

import com.tesseractsoftwares.nexusbackend.sdkjava.auth.AuthService;
import com.tesseractsoftwares.nexusbackend.sdkjava.config.NexusConfig;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.NexusHttpClient;
import com.tesseractsoftwares.nexusbackend.sdkjava.players.PlayerService;

public class NexusSDK {

    private static NexusSDK instance;

    private final NexusConfig config;
    private final NexusHttpClient httpClient;
    private final AuthService authService;
    private final PlayerService playerService;



    private NexusSDK(String baseUrl) {
        this.config = new NexusConfig(baseUrl, 5000); // timeout por defecto
        this.httpClient = new NexusHttpClient(config);
        this.authService = new AuthService(httpClient, config);
        this.playerService = new PlayerService(httpClient);

    }

    // Método de inicialización principal
    public static NexusSDK init(String baseUrl) {
        if (instance == null) {
            instance = new NexusSDK(baseUrl);
        }
        return instance;
    }

    // Obtener la instancia (optional)
    public static NexusSDK getInstance() {
        return instance;
    }

    // Exponer AuthService
    public AuthService auth() {
        return authService;
    }

    public PlayerService players() {
        return playerService;
    }
}
