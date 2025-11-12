// Ubicación: praxcore-velocity/src/main/java/com/prax/core/velocity/TokenManager.java
package com.prax.core.velocity;

// CORRECCIÓN: Usamos los paquetes originales de la librería.
// El plugin 'maven-shade-plugin' se encargará de reubicarlos a 'com.prax.core.libs.jwt' durante la compilación.
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TokenManager {

    private final Map<UUID, String> activeSessions = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenManager.class);

    private static final String SECRET_KEY_STRING = "k2_s!d5P@9fG-h3J&t8L$p1N*c7B?e6V+y0W#z%C&v)b1n3m5k7j9H4g6F8d";
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());

    public void createAndStoreToken(UUID playerUuid) {
        // Verificar si ya existe una sesión activa válida
        if (sessionExists(playerUuid) && validateToken(playerUuid)) {
            LOGGER.info("[TokenManager] Sesión ya existe y es válida para " + playerUuid);
            return;
        }

        String token = generateJwtToken(playerUuid);
        activeSessions.put(playerUuid, token);
        LOGGER.info("[TokenManager] Sesión creada y token almacenado para " + playerUuid);
    }

    public boolean validateToken(UUID playerUuid) {
        if (!activeSessions.containsKey(playerUuid)) {
            LOGGER.info("[TokenManager] Validación fallida: No existe sesión para " + playerUuid);
            return false;
        }

        String token = activeSessions.get(playerUuid);
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);

            if (claims.getBody().getExpiration().before(new Date())) {
                LOGGER.warn("[TokenManager] Validación fallida: Token expirado para " + playerUuid +
                        " (expiró: " + claims.getBody().getExpiration() + ")");
                removeSession(playerUuid);
                return false;
            }

            LOGGER.info("[TokenManager] Validación exitosa para " + playerUuid +
                    " (expira: " + claims.getBody().getExpiration() + ")");
            return true;

        } catch (Exception e) {
            LOGGER.error("[TokenManager] Error al validar token para " + playerUuid + ": " + e.getMessage());
            removeSession(playerUuid);
            return false;
        }
    }

    public void removeSession(UUID playerUuid) {
        activeSessions.remove(playerUuid);
        LOGGER.info("[TokenManager] Sesión eliminada para " + playerUuid);
    }

    private String generateJwtToken(UUID playerUuid) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        long expMillis = nowMillis + 86400000; // 24 horas
        Date exp = new Date(expMillis);

        return Jwts.builder()
                .setSubject(playerUuid.toString())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean sessionExists(UUID playerUuid) {
        return activeSessions.containsKey(playerUuid);
    }

    public void cleanupExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> {
            try {
                UUID uuid = entry.getKey();
                String token = entry.getValue();

                Jws<Claims> claims = Jwts.parserBuilder()
                        .setSigningKey(SECRET_KEY)
                        .build()
                        .parseClaimsJws(token);

                if (claims.getBody().getExpiration().before(new Date())) {
                    LOGGER.info("[TokenManager] Sesión expirada eliminada para " + uuid);
                    return true; // Remover entrada
                }
                return false; // Mantener entrada
            } catch (Exception e) {
                LOGGER.warn("[TokenManager] Error al verificar expiración, eliminando sesión: " + entry.getKey());
                return true; // Remover entrada problemática
            }
        });
    }
    public void storeToken(UUID uuid, String token) {
        activeSessions.put(uuid, token);
    }

    public String getToken(UUID uuid) {
        return activeSessions.get(uuid);
    }
}