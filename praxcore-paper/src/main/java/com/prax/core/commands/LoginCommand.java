package com.prax.core.commands;

import com.prax.core.PraxCorePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LoginCommand implements CommandExecutor {

    private final PraxCorePlugin plugin;

    public LoginCommand(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por un jugador.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (!plugin.getDataManager().isPlayerRegistered(player.getUniqueId())) {
            player.sendMessage("§cNo estás registrado. Usa /register <email> <contraseña> <contraseña> <fecha_nacimiento> para crear una cuenta.");
            return true;
        }

        // Validar formato de uso
        if (args.length != 2) {
            player.sendMessage("§cUso correcto: /login <email> <contraseña>");
            return true;
        }

        String email = args[0];
        String password = args[1];

        if (plugin.isAuthenticated(uuid)) {
            player.sendMessage("§eYa estás autenticado.");
            return true;
        }

        player.sendMessage("§7Validando tus credenciales con PraxSuite...");
        plugin.sendCreateSessionAuth(player, email, password);  // 🔹 Nuevo método

        return true;
    }
}
