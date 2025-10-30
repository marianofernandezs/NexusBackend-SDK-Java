package com.prax.core.utils;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class BirthdateValidator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final int MIN_AGE = 13;
    private static final int MAX_AGE = 100;

    /**
     * Valida si una fecha de nacimiento en formato DD-MM-YYYY es válida
     * @param birthdateStr Fecha en formato DD-MM-YYYY
     * @return ValidationResult con el resultado de la validación
     */
    public static ValidationResult validate(String birthdateStr) {
        // Validar formato
        LocalDate birthdate;
        try {
            birthdate = LocalDate.parse(birthdateStr, FORMATTER);
        } catch (DateTimeParseException e) {
            return ValidationResult.error("Formato de fecha inválido. Usa DD-MM-YYYY");
        }

        // Validar fecha no futura
        if (birthdate.isAfter(LocalDate.now())) {
            return ValidationResult.error("La fecha de nacimiento no puede ser en el futuro");
        }

        // Calcular edad
        int age = Period.between(birthdate, LocalDate.now()).getYears();

        // Validar edad mínima
        if (age < MIN_AGE) {
            return ValidationResult.error("Debes tener al menos " + MIN_AGE + " años");
        }

        // Validar edad máxima
        if (age > MAX_AGE) {
            return ValidationResult.error("Por favor, verifica tu fecha de nacimiento");
        }

        return ValidationResult.success(birthdate, age);
    }

    /**
     * Calcula la edad actual desde una fecha de nacimiento
     */
    public static int calculateAge(String birthdateStr) {
        try {
            LocalDate birthdate = LocalDate.parse(birthdateStr, FORMATTER);
            return Period.between(birthdate, LocalDate.now()).getYears();
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    /**
     * Clase para retornar el resultado de la validación
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final LocalDate birthdate;
        private final int age;

        private ValidationResult(boolean valid, String errorMessage, LocalDate birthdate, int age) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.birthdate = birthdate;
            this.age = age;
        }

        public static ValidationResult success(LocalDate birthdate, int age) {
            return new ValidationResult(true, null, birthdate, age);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message, null, 0);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public LocalDate getBirthdate() {
            return birthdate;
        }

        public int getAge() {
            return age;
        }
    }
}
