package com.diffusehyperion.serverRedirector;

import com.diffusehyperion.serverRedirector.commands.RegisterServerCommand;
import com.diffusehyperion.serverRedirector.commands.UnregisterServerCommand;
import com.diffusehyperion.serverRedirector.database.ServersTable;
import com.diffusehyperion.serverRedirector.events.PlayerJoinEvent;
import com.google.inject.Inject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

@Plugin(id = "serverredirector", name = "ServerRedirector", version = BuildConstants.VERSION)
public class ServerRedirector {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer proxy;

    @Inject @DataDirectory
    private Path dataDirectory;

    private Connection connection;
    private ServersTable serversTable;

    private Connection getDatabaseConnection() {
        try {
            dataDirectory.toFile().mkdir();
            File databaseFile = dataDirectory.resolve("servers.sqlite").toFile();
            databaseFile.createNewFile();

            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        } catch (SQLException | ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int registerServers() {
        List<ServersTable.Server> serverList = this.serversTable.readServers();

        for (ServersTable.Server server : serverList) {
            this.proxy.registerServer(new ServerInfo(server.getPrefixedName(), new InetSocketAddress(server.ip(), server.port())));
        }
        return serverList.size();
    }

    private void registerCommands() {
        CommandManager commandManager = this.proxy.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("serverredirector")
                .plugin(this)
                .build();

        LiteralArgumentBuilder<CommandSource> baseCommandNode = BrigadierCommand.literalArgumentBuilder("serverredirector");
        baseCommandNode.then(RegisterServerCommand.createRegisterServerNode(this.serversTable, this.proxy, this.logger));
        baseCommandNode.then(UnregisterServerCommand.createUnregisterServerNode(this.serversTable, this.proxy, this.logger));

        this.proxy.getCommandManager().register(commandMeta, new BrigadierCommand(baseCommandNode.build()));
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.logger.info("Initializing ServerRedirector");

        this.logger.info("Initializing database");
        this.connection = getDatabaseConnection();

        this.logger.info("Initializing database tables");
        this.serversTable = new ServersTable(connection);
        this.serversTable.initializeTable();

        this.logger.info("Registering servers found in database");
        int count = registerServers();
        this.logger.info("Registered {} servers", count);

        this.logger.info("Registering commands");
        registerCommands();

        this.logger.info("Registering events");
        this.proxy.getEventManager().register(this, new PlayerJoinEvent(this.proxy, this.serversTable));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
