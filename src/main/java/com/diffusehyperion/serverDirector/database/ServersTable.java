package com.diffusehyperion.serverDirector.database;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServersTable {
    private final Connection connection;

    public ServersTable(Connection connection) {
        this.connection = connection;
    }

    public void initializeTable() {
        try {
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS servers (name TEXT PRIMARY KEY NOT NULL, ip TEXT NOT NULL, port INTEGER NOT NULL, description TEXT)").executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Server createServer(String name, String ip, int port, @Nullable String description) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO servers (name, ip, port, description) VALUES (?, ?, ?, ?)");
            statement.setString(1, name);
            statement.setString(2, ip);
            statement.setInt(3, port);
            statement.setString(4, description);
            statement.executeUpdate();
            return new Server(name, ip, port, description);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public @Nullable Server readServer(String name) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT name, ip, port, description FROM servers WHERE name = ?");
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return new Server(
                        resultSet.getString("name"),
                        resultSet.getString("ip"),
                        resultSet.getInt("port"),
                        resultSet.getString("description")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Server> readServers() {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT name, ip, port, description FROM servers");
            ResultSet resultSet = statement.executeQuery();

            List<Server> servers = new ArrayList<>();
            while (resultSet.next()) {
                servers.add(new Server(
                        resultSet.getString("name"),
                        resultSet.getString("ip"),
                        resultSet.getInt("port"),
                        resultSet.getString("description")
                ));
            }

            return servers;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteServer(String name) {
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM servers WHERE name = ?");
            statement.setString(1, name);
            return statement.executeUpdate() < 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public record Server(String name, String ip, int port, @Nullable String description) {
    }
}
