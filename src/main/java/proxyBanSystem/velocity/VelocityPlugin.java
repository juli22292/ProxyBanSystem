package proxyBanSystem.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import proxyBanSystem.ProxyBanSystem;

import java.nio.file.Path;

@Plugin(
        id = "proxybansystem",
        name = "ProxyBanSystem",
        version = "4.1",
        authors = {"Julian1657"}
)
public class VelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ProxyBanSystem.initialize(new VelocityAdapter(server, logger, dataDirectory));
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        String denyMessage = ProxyBanSystem.handleLogin(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getUsername()
        );

        if (denyMessage != null) {
            event.setResult(LoginEvent.ComponentResult.denied(VelocityCommandSource.deserialize(denyMessage)));
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        ProxyBanSystem.handleDisconnect(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        ProxyBanSystem.shutdown();
    }
}
