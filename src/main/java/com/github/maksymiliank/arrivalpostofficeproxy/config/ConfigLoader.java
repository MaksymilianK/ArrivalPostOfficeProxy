package com.github.maksymiliank.arrivalpostofficeproxy.config;

import com.github.maksymiliank.arrivalwebsocketutils.WebSocketAddress;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigLoader {

    private static final String CONFIG_FILE_NAME = "config.yml";

    private static final String WS_PORT_PATH = "port";
    private static final String API_WS_HOST_PATH = "api.host";
    private static final String API_WS_PORT_PATH = "api.port";
    private static final String API_RECONNECT_PERIOD_PATH = "api.reconnect-period";
    private static final String ALLOWED_MC_SERVERS_PATH = "allowed-mc-servers";
    private static final String MC_SERVER_ID = "id";
    private static final String MC_SERVER_HOST = "host";
    private static final String MC_SERVER_PORT = "port";

    private static final int DEFAULT_WS_PORT = 4300;
    private static final String DEFAULT_API_WS_HOST = "localhost";
    private static final int DEFAULT_API_WS_PORT = 4200;
    private static final int DEFAULT_API_RECONNECT_PERIOD = 15;

    public static Config load(File dataFolder) {
        var configFile = new File(dataFolder, CONFIG_FILE_NAME);

        Configuration config;
        if (ensureFileCreated(dataFolder, configFile)) {
            config = saveDefault(configFile);
        } else {
            config = readFromFile(configFile);
        }

        return new Config(
                config.getInt(WS_PORT_PATH),
                config.getString(API_WS_HOST_PATH),
                config.getInt(API_WS_PORT_PATH),
                config.getInt(API_RECONNECT_PERIOD_PATH),
                readAllowedMcServers(config)
        );
    }

    private static boolean ensureFileCreated(File dataFolder, File configFile) {
        dataFolder.mkdir();

        boolean configCreated;
        try {
            configCreated = configFile.createNewFile();
        } catch (IOException e) {
            throw new ConfigException(e);
        }

        return configCreated;
    }

    private static Configuration readFromFile(File configFile) {
        Configuration config;
        try {
            config = ConfigurationProvider
                    .getProvider(YamlConfiguration.class)
                    .load(configFile);
        } catch (IOException e) {
            throw new ConfigException(e);
        }

        return config;
    }

    private static Configuration saveDefault(File configFile) {
        Configuration config = new Configuration();
        config.set(WS_PORT_PATH, DEFAULT_WS_PORT);
        config.set(API_WS_HOST_PATH, DEFAULT_API_WS_HOST);
        config.set(API_WS_PORT_PATH, DEFAULT_API_WS_PORT);
        config.set(API_RECONNECT_PERIOD_PATH, DEFAULT_API_RECONNECT_PERIOD);
        config.set(ALLOWED_MC_SERVERS_PATH, new Configuration());
        config.getSection(ALLOWED_MC_SERVERS_PATH).set("server1", new Configuration());
        config.getSection(ALLOWED_MC_SERVERS_PATH).set("server2", new Configuration());
        config.getSection(ALLOWED_MC_SERVERS_PATH).getSection("server1").set("id", 1);
        config.getSection(ALLOWED_MC_SERVERS_PATH).getSection("server1").set("host", "localhost");
        config.getSection(ALLOWED_MC_SERVERS_PATH).getSection("server1").set("port", 4201);
        config.getSection(ALLOWED_MC_SERVERS_PATH).getSection("server2").set("id", 2);
        config.getSection(ALLOWED_MC_SERVERS_PATH).getSection("server2").set("host", "localhost");
        config.getSection(ALLOWED_MC_SERVERS_PATH).getSection("server2").set("port", 4202);

        try {
            ConfigurationProvider
                    .getProvider(YamlConfiguration.class)
                    .save(config, configFile);
        } catch (IOException e) {
            throw new ConfigException(e);
        }

        return config;
    }

    private static List<McServer> readAllowedMcServers(Configuration config) {
        return config.getSection(ALLOWED_MC_SERVERS_PATH).getKeys().stream()
                .map(k -> readMcServer(config, k))
                .collect(Collectors.toList());
    }

    private static McServer readMcServer(Configuration config, String name) {
        var section = config.getSection(ALLOWED_MC_SERVERS_PATH).getSection(name);
        return new McServer(
                section.getInt(MC_SERVER_ID),
                new WebSocketAddress(section.getString(MC_SERVER_HOST), section.getInt(MC_SERVER_PORT))
        );
    }
}
