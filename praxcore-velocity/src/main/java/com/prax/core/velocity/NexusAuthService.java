package com.prax.core.velocity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tesseractsoftwares.nexusbackend.sdkjava.auth.AuthService;
import com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto.LoginDto;
import com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto.RegisterDto;
import com.tesseractsoftwares.nexusbackend.sdkjava.config.NexusConfig;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.NexusHttpClient;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.exceptions.NexusHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Wrapper del SDK de Nexus Backend adaptado para PraxCore
 * Mantiene la misma interfaz que BackendClient pero usa el SDK internamente
 */
public class NexusAuthService {

    private static final Logger logger = LoggerFactory.getLogger(NexusAuthService.class);
    private final AuthService authService;
    private final NexusConfig config;
    private final Gson gson = new Gson();

    /**
     * Constructor - Inicializa el SDK
     * @param baseUrl URL del backend (ej: "http://localhost:3000/api/")
     * @param timeoutMs Timeout en milisegundos
     */


    public NexusAuthService(String baseUrl, int timeoutMs) {
        logger.info("[NexusAuthService] Inicializando SDK con baseUrl: {}", baseUrl);

        // Inicializar configuración del SDK
        this.config = new NexusConfig(baseUrl, timeoutMs);

        // Crear cliente HTTP
        NexusHttpClient httpClient = new NexusHttpClient(config);

        // Crear servicio de autenticación
        this.authService = new AuthService(httpClient, config);

        logger.info("[NexusAuthService] SDK inicializado correctamente");
    }

    // ==============================
    // 🔹 REGISTER
    // ==============================
    public boolean register(UUID uuid, String email, String password, String birthdate, String playerName) {
        try {
            // Crear DTO del SDK
            RegisterDto dto = new RegisterDto(
                    uuid.toString(),
                    email,
                    password,
                    birthdate,
                    playerName
            );

            logger.info("[NexusAuthService] Registrando usuario: {} ({})", playerName, maskEmail(email));

            // Llamar al SDK
            boolean success = authService.register(dto);

            if (success) {
                logger.info("[NexusAuthService] Registro exitoso para {}", playerName);
            } else {
                logger.warn("[NexusAuthService] Registro fallido para {}", playerName);
            }

            return success;

        } catch (NexusHttpException e) {
            logger.error("[NexusAuthService] Error HTTP en registro: [{}] {}",
                    e.getStatusCode(), e.getMessage());
            return false;

        } catch (Exception e) {
            logger.error("[NexusAuthService] Error inesperado en registro: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==============================
    // 🔹 LOGIN
    // ==============================
    public Optional<String> login(UUID uuid, String email, String password) {
        try {
            // NOTA: El SDK actual de LoginDto no incluye UUID
            // Necesitarás actualizar el SDK o enviar el UUID de otra forma
            LoginDto dto = new LoginDto(email, password);

            logger.info("[NexusAuthService] Intentando login para: {}", maskEmail(email));

            // Llamar al SDK - retorna el token directamente
            String token = authService.login(dto);

            if (token != null && !token.isEmpty()) {
                logger.info("[NexusAuthService] Login exitoso, token recibido");
                return Optional.of(token);
            } else {
                logger.warn("[NexusAuthService] Login fallido, token vacío");
                return Optional.empty();
            }

        } catch (NexusHttpException e) {
            logger.error("[NexusAuthService] Error HTTP en login: [{}] {}",
                    e.getStatusCode(), e.getMessage());
            return Optional.empty();

        } catch (Exception e) {
            logger.error("[NexusAuthService] Error inesperado en login: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    // ==============================
    // 🔹 VALIDATE
    // ==============================
    public boolean validate(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("[NexusAuthService] Token nulo o vacío en validación");
            return false;
        }

        try {
            // Configurar el token antes de validar
            config.setToken(token);

            logger.info("[NexusAuthService] Validando token...");

            // Llamar al SDK
            boolean isValid = authService.validate();

            if (isValid) {
                logger.info("[NexusAuthService] Token válido");
            } else {
                logger.warn("[NexusAuthService] Token inválido");
            }

            return isValid;

        } catch (NexusHttpException e) {
            logger.error("[NexusAuthService] Error HTTP en validación: [{}] {}",
                    e.getStatusCode(), e.getMessage());
            return false;

        } catch (Exception e) {
            logger.error("[NexusAuthService] Error inesperado en validación: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==============================
    // 🔹 LOGOUT
    // ==============================
    public boolean logout(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("[NexusAuthService] Token nulo o vacío en logout");
            return false;
        }

        try {
            // Configurar el token antes de hacer logout
            config.setToken(token);

            logger.info("[NexusAuthService] Cerrando sesión...");

            // Llamar al SDK
            boolean success = authService.logout();

            if (success) {
                logger.info("[NexusAuthService] Logout exitoso");
            } else {
                logger.warn("[NexusAuthService] Logout fallido");
            }

            return success;

        } catch (NexusHttpException e) {
            logger.error("[NexusAuthService] Error HTTP en logout: [{}] {}",
                    e.getStatusCode(), e.getMessage());
            return false;

        } catch (Exception e) {
            logger.error("[NexusAuthService] Error inesperado en logout: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==============================
    // 🔹 REFRESH TOKEN
    // ==============================
    public Optional<String> refresh(String oldToken) {
        if (oldToken == null || oldToken.isEmpty()) {
            logger.warn("[NexusAuthService] Token nulo o vacío en refresh");
            return Optional.empty();
        }

        try {
            // Configurar el token viejo
            config.setToken(oldToken);

            logger.info("[NexusAuthService] Refrescando token...");

            // Llamar al SDK
            String newToken = authService.refresh();

            if (newToken != null && !newToken.isEmpty()) {
                logger.info("[NexusAuthService] Token refrescado exitosamente");
                return Optional.of(newToken);
            } else {
                logger.warn("[NexusAuthService] Refresh falló, token vacío");
                return Optional.empty();
            }

        } catch (NexusHttpException e) {
            logger.error("[NexusAuthService] Error HTTP en refresh: [{}] {}",
                    e.getStatusCode(), e.getMessage());
            return Optional.empty();

        } catch (Exception e) {
            logger.error("[NexusAuthService] Error inesperado en refresh: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    // ==============================
    // 🔹 UTILIDADES
    // ==============================

    /**
     * Obtiene el token actual del config
     */
    public String getCurrentToken() {
        return config.getToken();
    }

    /**
     * Establece un token manualmente en el config
     */
    public void setToken(String token) {
        config.setToken(token);
    }

    /**
     * Limpia el token del config
     */
    public void clearToken() {
        config.clearToken();
    }

    /**
     * Enmascara un email para logs (muestra solo primeros 3 caracteres)
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, Math.min(3, atIndex)) + "***" + email.substring(atIndex);
        }
        return email.substring(0, 3) + "***";
    }
}