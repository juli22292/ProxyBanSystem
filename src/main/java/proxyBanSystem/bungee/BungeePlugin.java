package proxyBanSystem.bungee;

import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import proxyBanSystem.ProxyBanSystem;

public class BungeePlugin extends Plugin implements Listener {

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
        ProxyBanSystem.initialize(new BungeeAdapter(this));
    }

    @Override
    public void onDisable() {
        ProxyBanSystem.shutdown();
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        String denyMessage = ProxyBanSystem.handleLogin(
                event.getConnection().getUniqueId(),
                event.getConnection().getName()
        );

        if (denyMessage != null) {
            event.setCancelled(true);
            event.setCancelReason(BungeeCommandSource.deserialize(denyMessage));
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        ProxyBanSystem.handleDisconnect(event.getPlayer().getUniqueId());
    }
}
