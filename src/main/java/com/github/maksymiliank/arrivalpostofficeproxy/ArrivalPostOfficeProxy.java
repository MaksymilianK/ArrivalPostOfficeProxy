package com.github.maksymiliank.arrivalpostofficeproxy;

import com.github.maksymiliank.arrivalpostofficeproxy.config.Config;
import com.github.maksymiliank.arrivalpostofficeproxy.config.ConfigLoader;
import com.github.maksymiliank.arrivalpostofficeproxy.websocket.ArrivalWebsocketClient;
import com.github.maksymiliank.arrivalpostofficeproxy.websocket.ArrivalWebsocketServer;
import com.github.maksymiliank.arrivalpostofficeproxy.websocket.WebSocketAddress;
import com.github.maksymiliank.arrivalpostofficeproxy.websocket.message.Message;
import com.github.maksymiliank.arrivalpostofficeproxy.websocket.message.RawMessage;
import com.github.maksymiliank.arrivalpostofficeproxy.websocket.message.RawMessageDeserializer;
import com.google.gson.GsonBuilder;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ArrivalPostOfficeProxy extends Plugin {

    private ArrivalWebsocketClient client;
    private ArrivalWebsocketServer server;

    private ApiPostman apiPostman;
    private McPostman mcPostman;

    @Override
    public void onEnable() {
        var config = ConfigLoader.load(getDataFolder());

        startClient(config);
        startServer(config);
    }

    @Override
    public void onDisable() {
        try {
            server.stop();
        } catch (InterruptedException e) {
            getSLF4JLogger().warn("Interrupted stopping server");
        }

        client.close();
    }

    public void addApiListener(int messageType, Class<? extends Message> messageClass, Consumer<Message> onMessage) {
        apiPostman.addListener(messageType, messageClass, onMessage);
    }

    public void sendToApi(Message message) {
        apiPostman.send(message);
    }

    public Optional<Message> sendToApiBlocking(Message message, int responseType,
                                               Class<? extends Message> responseClass) {
        return apiPostman.sendBlocking(message, responseType, responseClass);
    }

    public void addMcListener(int messageType, Class<? extends Message> messageClass, Consumer<Message> onMessage) {
        mcPostman.addListener(messageType, messageClass, onMessage);
    }

    public void sendToMc(String mcServer, Message message) {
        mcPostman.send(mcServer, message);
    }

    public Optional<Message> sendToMcBlocking(String mcServer, Message message, int responseType,
                                              Class<? extends Message> responseClass) {
        return mcPostman.sendBlocking(mcServer, message, responseType, responseClass);
    }

    private void startClient(Config config) {
        var messageDeserializer = new RawMessageDeserializer();
        client = new ArrivalWebsocketClient(
                new WebSocketAddress(config.apiHost(), config.apiPort(), config.apiPath()),
                new GsonBuilder().registerTypeAdapter(RawMessage.class, messageDeserializer).create(),
                messageDeserializer,
                getSLF4JLogger()
        );

        apiPostman = new ApiPostman(client);
        client.setPostman(apiPostman);

        client.connect();

        getProxy().getScheduler().schedule(
                this,
                this::tryReconnectClient,
                config.apiReconnectPeriod(),
                config.apiReconnectPeriod(),
                TimeUnit.SECONDS
        );
    }

    private void startServer(Config config) {
        var messageDeserializer = new RawMessageDeserializer();
        server = new ArrivalWebsocketServer(
                config.port(),
                new GsonBuilder().registerTypeAdapter(RawMessage.class, messageDeserializer).create(),
                messageDeserializer,
                getSLF4JLogger(),
                config.allowedMcHosts()
        );

        mcPostman = new McPostman(server);
        server.setPostman(mcPostman);

        server.start();
    }

    private void tryReconnectClient() {
        if (!client.isOpen()) {
            client.reconnect();
            getSLF4JLogger().info("Trying to reconnect to API");
        }
    }
}
