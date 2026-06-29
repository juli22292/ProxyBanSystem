package proxyBanSystem.sponge;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.Server;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import proxyBanSystem.ProxyBanSystem;

import java.io.File;

@Plugin("proxybansystem")
public class SpongePlugin {

    private boolean initialized;

    @Listener
    public void onRegisterCommands(RegisterCommandEvent<Command.Raw> event) {
        if (initialized) {
            return;
        }

        PluginContainer container = Sponge.pluginManager()
                .fromInstance(this)
                .orElseThrow(() -> new IllegalStateException("PluginContainer not found"));
        File dataDirectory = Sponge.configManager()
                .pluginConfig(container)
                .directory()
                .toFile();

        ProxyBanSystem.initialize(new SpongeAdapter(container, event, dataDirectory));
        initialized = true;
    }

    @Listener
    public void onAuth(ServerSideConnectionEvent.Auth event) {
        String denyMessage = ProxyBanSystem.handleLogin(
                event.profile().uuid(),
                event.profile().name().orElse("")
        );

        if (denyMessage != null) {
            event.setMessage(SpongeCommandSource.deserialize(denyMessage));
            event.setCancelled(true);
        }
    }

    @Listener
    public void onDisconnect(ServerSideConnectionEvent.Disconnect event) {
        event.profile().ifPresent(profile -> ProxyBanSystem.handleDisconnect(profile.uuid()));
    }

    @Listener
    public void onStopping(StoppingEngineEvent<Server> event) {
        if (!initialized) {
            return;
        }

        ProxyBanSystem.shutdown();
        initialized = false;
    }
}
