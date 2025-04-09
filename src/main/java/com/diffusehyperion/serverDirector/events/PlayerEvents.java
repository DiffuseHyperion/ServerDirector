package com.diffusehyperion.serverDirector.events;

import com.diffusehyperion.serverDirector.database.ServersTable;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.Objects;

public class PlayerEvents {
    private final ServersTable serversTable;
    private final ProxyServer proxy;

    public PlayerEvents(ProxyServer proxy, ServersTable serversTable) {
        this.proxy = proxy;
        this.serversTable = serversTable;
    }

    @Subscribe
    public void onPlayerChat(ServerPostConnectEvent event) {
        String serverName = event.getPlayer().getCurrentServer().orElseThrow().getServer().getServerInfo().getName();
        ServersTable.Server server = this.serversTable.readServer(serverName);
        Player player = event.getPlayer();

        if (Objects.isNull(server)) {
            // player joined server outside of ServerDirector's control
            RegisteredServer previousServer = event.getPreviousServer();
            if (Objects.isNull(previousServer)) {
                // player just joined the network directly
                player.sendMessage(getDirectWelcomeMessage());
            } else {
                player.sendMessage(getIndirectWelcomeMessage());
            }
            player.sendMessage(getServerSelectionMessage());
        }

    }

    @Subscribe
    public void onPlayerLogin(PlayerChooseInitialServerEvent event) {
        List<ServersTable.Server> servers = this.serversTable.readServers();
        if (servers.size() == 1) {
            // there is only 1 registered server, immediately redirect to there
            RegisteredServer registeredServer = this.proxy.getServer(servers.get(0).name()).orElse(null);
            event.setInitialServer(registeredServer);
        }
    }

    private Component getDirectWelcomeMessage() {
        return Component.text("Welcome! ").color(NamedTextColor.GREEN)
                .append(Component.text("Click on a server to join:").color(NamedTextColor.GOLD));
    }

    private Component getIndirectWelcomeMessage() {
        return Component.text("You have been redirected to server selection. ").color(NamedTextColor.RED)
                .append(Component.text("Click on a server to join:").color(NamedTextColor.GOLD));
    }

    private Component getServerSelectionMessage() {
        List<ServersTable.Server> servers = this.serversTable.readServers();

        Component result = Component.empty();
        for (ServersTable.Server server : servers) {
            String display = (Objects.isNull(server.description()) ? server.name() : server.name() + " - " + server.description());
            result = result.append(Component.text(display)
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.UNDERLINED)
                    .hoverEvent(HoverEvent.showText(
                            Component.text("Click to join \"")
                                    .append(Component.text(server.name()))
                                    .append(Component.text("\""))
                    ))
                    .clickEvent(ClickEvent.runCommand("/server " + server.name())));
            if (servers.indexOf(server) != servers.size() - 1) {
                result = result.append(Component.newline());

            }
        }
        return result;
    }
}
