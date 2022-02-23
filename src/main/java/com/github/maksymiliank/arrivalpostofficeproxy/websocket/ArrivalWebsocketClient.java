package com.github.maksymiliank.arrivalpostofficeproxy.websocket;

import com.github.maksymiliank.arrivalpostofficeproxy.ApiPostman;
import com.github.maksymiliank.arrivalpostofficeproxy.websocket.message.Message;
import com.github.maksymiliank.arrivalpostofficeproxy.websocket.message.RawMessageDeserializer;
import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;

import java.net.URI;

public class ArrivalWebsocketClient extends WebSocketClient {

    private static final String SERVER_ADDRESS_TEMPLATE = "ws://%s:%d/%s";

    private final Gson gson;
    private final RawMessageDeserializer messageDeserializer;
    private final Logger logger;

    private ApiPostman postman;

    public ArrivalWebsocketClient(WebSocketAddress serverAddress, Gson gson, RawMessageDeserializer messageDeserializer,
                                  Logger logger) {
        super(getServerURI(serverAddress));

        this.gson = gson;
        this.messageDeserializer = messageDeserializer;
        this.logger = logger;
    }

    public void registerMessageType(int messageType, Class<? extends Message> messageClass) {
        messageDeserializer.registerMessageType(messageType, messageClass);
    }

    public void setPostman(ApiPostman postman) {
        this.postman = postman;
    }

    public void send(Message message) {
        send(gson.toJson(message));
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        logger.info("Opened WebSocket connection to the server");
    }

    @Override
    public void onMessage(String rawMessage) {
        postman.onMessage(gson.fromJson(rawMessage, Message.class));
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info("Connection from the server has been closed");
    }

    @Override
    public void onError(Exception e) {
        logger.error(e.getMessage());
    }

    private static URI getServerURI(WebSocketAddress serverAddress) {
        return URI.create(
                String.format(
                        SERVER_ADDRESS_TEMPLATE,
                        serverAddress.host(),
                        serverAddress.port(),
                        serverAddress.path()
                )
        );
    }
}
