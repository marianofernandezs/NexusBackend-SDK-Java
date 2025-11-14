package com.prax.core.commands;

import com.prax.core.PraxCorePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LogoutCommand implements CommandExecutor {

    private final PraxCorePlugin plugin;

    public LogoutCommand(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por un jugador.");
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.isAuthenticated(player.getUniqueId())) {
            player.sendMessage("§eNo tienes una sesión activa.");
            return true;
        }

        player.sendMessage("§7Cerrando tu sesión...");
        plugin.sendLogoutRequest(player); // 🔹 método que agregamos en PraxCorePlugin

        return true;
    }
}
