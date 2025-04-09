package com.diffusehyperion.serverRedirector.commands;

import com.diffusehyperion.serverRedirector.database.ServersTable;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

public class UnregisterServerCommand {
    public static LiteralArgumentBuilder<CommandSource> createUnregisterServerNode(ServersTable serversTable, ProxyServer proxy, Logger logger) {
        return BrigadierCommand.literalArgumentBuilder("unregister")
                .then(BrigadierCommand.requiredArgumentBuilder("name", StringArgumentType.word())
                        .suggests((commandContext, builder) -> {
                            List<ServersTable.Server> servers = serversTable.readServers();
                            servers.forEach(server -> builder.suggest(server.name()));
                            return builder.buildFuture();
                        })
                        .executes(commandContext -> {
                            String name = StringArgumentType.getString(commandContext, "name");

                            ServersTable.Server server = serversTable.readServer(name);
                            if (Objects.isNull(server)) {
                                commandContext.getSource().sendMessage(getNotFoundMessage(name));
                                return Command.SINGLE_SUCCESS;
                            }

                            serversTable.deleteServer(name);
                            proxy.unregisterServer(new ServerInfo(server.getPrefixedName(), new InetSocketAddress(server.ip(), server.port())));

                            commandContext.getSource().sendMessage(getSuccessMessage(name));
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private static Component getSuccessMessage(String name) {
        return
                Component.text("Successfully unregistered server \"").color(NamedTextColor.GREEN)
                        .append(Component.text(name, NamedTextColor.GOLD))
                        .append(Component.text("\"").color(NamedTextColor.GREEN));

    }

    private static Component getNotFoundMessage(String name) {
        return
                Component.text("There was no server called \"").color(NamedTextColor.RED)
                        .append(Component.text(name, NamedTextColor.GOLD))
                        .append(Component.text("\"").color(NamedTextColor.RED));

    }
}
