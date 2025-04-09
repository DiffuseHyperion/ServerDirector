package com.diffusehyperion.serverDirector.events;

import com.diffusehyperion.serverDirector.database.ServersTable;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.Objects;

public class PlayerJoinEvent {
    private final ServersTable serversTable;
    private final ProxyServer proxy;

    public PlayerJoinEvent(ProxyServer proxy, ServersTable serversTable) {
        this.proxy = proxy;
        this.serversTable = serversTable;
    }

    @Subscribe
    public void onPlayerChat(ServerPostConnectEvent event) {
        String serverName = event.getPlayer().getCurrentServer().get().getServer().getServerInfo().getName().substring("ServerDirector-".length());

        ServersTable.Server server = this.serversTable.readServer(serverName);
        if (Objects.isNull(server)) {
            // joined a server outside ServerDirector's control
            event.getPlayer().sendMessage(Component.text("testing!").clickEvent(ClickEvent.runCommand("/server lobby")));
        } else {
            event.getPlayer().sendMessage(Component.text("inside!"));
        }
    }
}
