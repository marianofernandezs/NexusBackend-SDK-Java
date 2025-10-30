package com.prax.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mi primer test en PraxCore")
class PrimerTest {

    @Test
    @DisplayName("Verificar que el testing funciona")
    void testBasico() {
        // Arrange (Preparar)
        int numero1 = 5;
        int numero2 = 3;

        // Act (Actuar)
        int resultado = numero1 + numero2;

        // Assert (Verificar)
        assertEquals(8, resultado, "5 + 3 debería ser 8");
    }

    @Test
    @DisplayName("Verificar que los Strings funcionan")
    void testStrings() {
        String saludo = "Hola PraxCore";

        assertNotNull(saludo, "El saludo no debería ser null");
        assertTrue(saludo.contains("PraxCore"), "Debería contener 'PraxCore'");
        assertEquals(13, saludo.length(), "Debería tener 13 caracteres");
    }

    @Test
    @DisplayName("Verificar que podemos usar UUIDs")
    void testUUID() {
        java.util.UUID uuid = java.util.UUID.randomUUID();

        assertNotNull(uuid);
        assertEquals(36, uuid.toString().length());
    }
}