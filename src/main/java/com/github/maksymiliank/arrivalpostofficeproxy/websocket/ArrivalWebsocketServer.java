package com.github.maksymiliank.arrivalpostofficeproxy.websocket;

import com.github.maksymiliank.arrivalpostofficeproxy.McPostman;
import com.github.maksymiliank.arrivalpostofficeproxy.websocket.message.*;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ArrivalWebsocketServer extends WebSocketServer {

    public static final int CODE_UNAUTHORIZED = 4000;
    public static final int CODE_ALREADY_CONNECTED = 4001;

    private static final int CLIENT_REGISTRATION_TIME = 5000;
    private static final String HANDSHAKE_PROTOCOL_KEY = "Sec-WebSocket-Protocol";

    private final ReadWriteLock clientsLock = new ReentrantReadWriteLock(true);

    private final Gson gson;
    private final RawMessageDeserializer messageDeserializer;
    private final Logger logger;
    private final Set<String> allowedMcHosts;
    private final Map<String, WebSocket> mcServersConnections = new HashMap<>();
    private final Map<WebSocket, String> mcServersNames = new HashMap<>();

    private McPostman postman;

    public ArrivalWebsocketServer(int port,  Gson gson, RawMessageDeserializer messageDeserializer, Logger logger,
                                  Collection<String> allowedMcHosts) {
        super(new InetSocketAddress("localhost", port));

        this.gson = gson;
        this.messageDeserializer = messageDeserializer;
        this.logger = logger;
        this.allowedMcHosts = Set.copyOf(allowedMcHosts);
    }

    public void registerMessageType(int messageType, Class<? extends Message> messageClass) {
        messageDeserializer.registerMessageType(messageType, messageClass);
    }

    public void setPostman(McPostman postman) {
        this.postman = postman;
    }

    public boolean send(String mcServer, Message message) {
        clientsLock.readLock().lock();
        try {
            if (!mcServersConnections.containsKey(mcServer)) {
                return false;
            }

            mcServersConnections.get(mcServer).send(gson.toJson(message));
            return true;
        } finally {
            clientsLock.readLock().unlock();
        }
    }

    @Override
    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket connection, Draft draft,
                                                                       ClientHandshake request) throws InvalidDataException {
        ServerHandshakeBuilder builder = super.onWebsocketHandshakeReceivedAsServer(connection, draft, request);

        String client = getClientHost(connection);
        if (!allowedMcHosts.contains(client)) {
            throw new InvalidDataException(
                    CODE_UNAUTHORIZED,
                    "Connection from the host '%s' is not allowed".formatted(client)
            );
        }

        if (request.getFieldValue(HANDSHAKE_PROTOCOL_KEY).isEmpty()) {
            throw new InvalidDataException(
                    CODE_UNAUTHORIZED,
                    "Connection from the host '%s' does not contain server name in protocol field".formatted(client)
            );
        }

        return builder;
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        clientsLock.writeLock().lock();
        try {
            mcServersConnections.put(connection.getProtocol().toString(), connection);
            mcServersNames.put(connection, connection.getProtocol().toString());
        } finally {
            clientsLock.writeLock().unlock();
        }

        logger.info("Opened connection from client {}", connection.getProtocol().toString());
    }

    @Override
    public void onClose(WebSocket connection, int code, String reason, boolean remote) {
        clientsLock.writeLock().lock();
        try {
            mcServersConnections.remove(connection.getProtocol().toString());
            mcServersNames.remove(connection);
        } finally {
            clientsLock.writeLock().unlock();
        }

        logger.info("Closed connection from client {}", connection.getProtocol().toString());
    }

    @Override
    public void onMessage(WebSocket connection, String rawMessage) {
        postman.onMessage(gson.fromJson(rawMessage, Message.class));
    }

    @Override
    public void onError(WebSocket connection, Exception e) {
        logger.error(e.getMessage());
    }

    @Override
    public void onStart() {
        logger.info("WebSocket server is running");
    }

    private static String getClientHost(WebSocket connection) {
        return connection.getRemoteSocketAddress().getHostString();
    }
}
