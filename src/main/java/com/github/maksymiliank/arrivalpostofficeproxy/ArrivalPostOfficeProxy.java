package com.github.maksymiliank.arrivalpostofficeproxy;

import com.github.maksymiliank.arrivalpostofficeproxy.config.Config;
import com.github.maksymiliank.arrivalpostofficeproxy.config.ConfigLoader;
import com.github.maksymiliank.arrivalwebsocketutils.*;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ArrivalPostOfficeProxy extends Plugin {

    private static ArrivalWebsocketServer server;
    private static ArrivalWebsocketClient client;

    @Override
    public void onEnable() {
        var config = ConfigLoader.load(getDataFolder());
        startClient(config);
        startServer(config);
    }

    public static void addApiListener(int messageType, Consumer<InboundMessage> onMessage) {
        client.addListener(messageType, onMessage);
    }

    public static void addMcServerListener(int messageType, BiConsumer<Integer, InboundMessage> onMessage) {
        server.addListener(messageType, onMessage);
    }

    public static void sendToApi(OutboundMessage message) {
        client.send(message);
    }

    public static void sendToMcServer(OutboundMessage message) {
        client.send(message);
    }

    private void startClient(Config config) {
        client = new ArrivalWebsocketClient(
                new WebSocketAddress(config.apiHost(), config.apiPort()),
                getSLF4JLogger()
        );

        getProxy().getScheduler().schedule(
                this,
                this::tryReconnectClient,
                config.apiReconnectPeriod(),
                TimeUnit.SECONDS
        );
    }

    private void startServer(Config config) {
        var allowedClients = new HashMap<WebSocketAddress, Integer>();
        config.allowedMcServers().forEach(s -> allowedClients.put(s.address(), s.id()));

        server = new ArrivalWebsocketServer(config.port(), getSLF4JLogger(), allowedClients);
    }

    private void tryReconnectClient() {
        if (!client.isOpen()) {
            client.reconnect();
        }
    }
}
