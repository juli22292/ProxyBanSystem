package proxyBanSystem.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import proxyBanSystem.ProxyBanSystem;

public class BukkitPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        ProxyBanSystem.initialize(new BukkitAdapter(this));
    }

    @Override
    public void onDisable() {
        ProxyBanSystem.shutdown();
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String denyMessage = ProxyBanSystem.handleLogin(
                event.getUniqueId(),
                event.getName()
        );

        if (denyMessage != null) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    BukkitCommandSource.deserialize(denyMessage)
            );
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ProxyBanSystem.handleDisconnect(event.getPlayer().getUniqueId());
    }
}
