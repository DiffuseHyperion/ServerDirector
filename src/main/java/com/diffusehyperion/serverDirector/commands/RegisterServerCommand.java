package com.diffusehyperion.serverDirector.commands;

import com.diffusehyperion.serverDirector.database.ServersTable;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

public class RegisterServerCommand {
    public static LiteralArgumentBuilder<CommandSource> createRegisterServerNode(ServersTable serversTable, ProxyServer proxy, Logger logger) {
        return BrigadierCommand.literalArgumentBuilder("register")
                .then(BrigadierCommand.requiredArgumentBuilder("ip", StringArgumentType.word())
                        .then(BrigadierCommand.requiredArgumentBuilder("port", IntegerArgumentType.integer(0))
                                .then(BrigadierCommand.requiredArgumentBuilder("name", StringArgumentType.word())
                                        .executes(commandContext -> {
                                            String name = StringArgumentType.getString(commandContext, "name");
                                            String ip = StringArgumentType.getString(commandContext, "ip");
                                            int port = IntegerArgumentType.getInteger(commandContext, "port");

                                            for (ServersTable.Server server : serversTable.readServers()) {
                                                if (server.name().equals(name)) {
                                                    serversTable.updateServer(name, ip, port, null);
                                                    commandContext.getSource().sendMessage(getUpdatedMessage(name));
                                                    return Command.SINGLE_SUCCESS;
                                                }
                                            }

                                            ServersTable.Server server = serversTable.createServer(name, ip, port, null);
                                            proxy.registerServer(new ServerInfo(server.name(), new InetSocketAddress(server.ip(), server.port())));

                                            commandContext.getSource().sendMessage(getSuccessMessage(name, ip, port));
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(BrigadierCommand.requiredArgumentBuilder("description", StringArgumentType.greedyString())
                                                .executes(commandContext -> {
                                                    String name = StringArgumentType.getString(commandContext, "name");
                                                    String ip = StringArgumentType.getString(commandContext, "ip");
                                                    int port = IntegerArgumentType.getInteger(commandContext, "port");
                                                    String description = StringArgumentType.getString(commandContext, "description");

                                                    for (ServersTable.Server server : serversTable.readServers()) {
                                                        if (server.name().equals(name)) {
                                                            serversTable.updateServer(name, ip, port, description);
                                                            commandContext.getSource().sendMessage(getUpdatedMessage(name));
                                                            return Command.SINGLE_SUCCESS;
                                                        }
                                                    }

                                                    ServersTable.Server server = serversTable.createServer(name, ip, port, description);
                                                    proxy.registerServer(new ServerInfo(server.name(), new InetSocketAddress(server.ip(), server.port())));

                                                    commandContext.getSource().sendMessage(getSuccessMessage(name, ip, port));
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )));
    }

    private static Component getSuccessMessage(String name, String ip, int port) {
        return
                Component.text("Successfully registered server \"").color(NamedTextColor.GREEN)
                        .append(Component.text(name).color(NamedTextColor.GOLD))
                        .append(Component.text("\" at ").color(NamedTextColor.GREEN))
                        .append(Component.text(ip).color(NamedTextColor.GOLD))
                        .append(Component.text(":").color(NamedTextColor.GREEN))
                        .append(Component.text(port).color(NamedTextColor.GOLD));

    }

    private static Component getUpdatedMessage(String name) {
        return
                Component.text("Successfully updated server \"").color(NamedTextColor.GREEN)
                        .append(Component.text(name).color(NamedTextColor.GOLD))
                        .append(Component.text("\"").color(NamedTextColor.GREEN));

    }
}
