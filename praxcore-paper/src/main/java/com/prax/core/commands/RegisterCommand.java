package com.prax.core.commands;

import com.prax.core.PraxCorePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.InetSocketAddress;
import  java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.Period;

public class RegisterCommand implements CommandExecutor {

    private final PraxCorePlugin plugin;
    private static final DateTimeFormatter BIRTHDATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public RegisterCommand(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por un jugador.");
            return true;
        }

        Player player = (Player) sender;

        if (plugin.getDataManager().isPlayerRegistered(player.getUniqueId())) {
            player.sendMessage("§c¡Ya estás registrado! Usa /login <contraseña> para entrar.");
            return true;
        }

        if (args.length != 4) {
            player.sendMessage("cError: El uso correcto es:");
            player.sendMessage("/register <email> <contraseña> <repetir_contraseña> <fecha_nacimiento>");
            player.sendMessage("Formato de fecha: DD-MM-YYYY (ejemplo: 15-03-2005)");
            return false;
        }

        String email = args[0];
        String password = args[1];
        String confirmPassword = args[2];
        String birthdateStr = args[3]; // Fecha de nacimiento

        if (!email.contains("@") || !email.contains(".")) {
            player.sendMessage("§cPor favor, introduce un correo electrónico válido.");
            return true;
        }

        if (!password.equals(confirmPassword)) {
            player.sendMessage("§cLas contraseñas no coinciden. Inténtalo de nuevo.");
            return true;
        }

        // Logica para validar formato de fecha de nacimiento
        LocalDate birthdate;
        try {
            birthdate = LocalDate.parse(birthdateStr, BIRTHDATE_FORMATTER);
        } catch (DateTimeParseException e) {
            player.sendMessage("§cFormato de fecha inválido.");
            player.sendMessage("§eUsa el formato: DD-MM-YYYY (ejemplo: 15-03-2005)");
            return true;
        }

        // NUEVO: Validar que la fecha no sea futura
        if (birthdate.isAfter(LocalDate.now())) {
            player.sendMessage("§cLa fecha de nacimiento no puede ser en el futuro.");
            return true;
        }

        // NUEVO: Calcular edad
        int age = Period.between(birthdate, LocalDate.now()).getYears();

        // NUEVO: Validar edad mínima (ejemplo: 13 años)
        if (age < 13) {
            player.sendMessage("§cDebes tener al menos 13 años para registrarte.");
            player.sendMessage("§7Si crees que esto es un error, contacta a un administrador.");
            return true;
        }

        // NUEVO: Validar edad máxima razonable (ejemplo: 100 años)
        if (age > 100) {
            player.sendMessage("Por favor, verifica tu fecha de nacimiento.");
            player.sendMessage("Si es correcta, contacta a un administrador.");
            return true;
        }



        // --- LÓGICA DE DETECCIÓN DE CLIENTE BASADA EN TU IDEA ---
        InetSocketAddress address = player.getAddress();
        String ipAddress = address != null ? address.getAddress().getHostAddress() : "IP Desconocida";
        String version = String.valueOf(player.getProtocolVersion());

        // Convertimos el UUID a String para poder analizarlo.
        String playerUuidString = player.getUniqueId().toString();
        String clientType;

        // Verificamos si el UUID comienza con el prefijo estándar de Floodgate para Bedrock.
        if (playerUuidString.startsWith("00000000-0000-0000-")) {
            clientType = "BEDROCK";
        } else {
            clientType = "JAVA";
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        plugin.getDataManager().registerPlayer(player.getUniqueId(), hashedPassword, email, ipAddress, clientType, version, birthdateStr);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String formattedDate = dtf.format(now);
        plugin.getDataManager().setFirstLoginDate(player.getUniqueId(), formattedDate);
        plugin.getDataManager().incrementLoginCount(player.getUniqueId());
        plugin.getDataManager().setLastLoginDate(player.getUniqueId(), formattedDate);

        player.sendMessage("§a¡Te has registrado exitosamente con el email " + email + "! Ahora, por favor, inicia sesión.");

        return true;
    }
}