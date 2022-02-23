package com.github.maksymiliank.arrivalpostofficeproxy;

import com.github.maksymiliank.arrivalpostofficeproxy.websocket.ArrivalWebsocketServer;
import com.github.maksymiliank.arrivalpostofficeproxy.websocket.message.Message;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class McPostman {

    private static final int WAIT_FOR_MESSAGE_TIME_MS = 5000;

    private final ReadWriteLock listenersLock = new ReentrantReadWriteLock(true);
    private final ReadWriteLock receiversLock = new ReentrantReadWriteLock(true);

    private final ArrivalWebsocketServer server;
    private final Map<Integer, List<Consumer<Message>>> listeners = new HashMap<>();
    private final Map<Integer, List<Consumer<Message>>> receivers = new HashMap<>();

    public McPostman(ArrivalWebsocketServer server) {
        this.server = server;
    }

    public <T extends Message> void addListener(int messageType, Class<T> messageClass, Consumer<Message> onMessage) {
        server.registerMessageType(messageType, messageClass);

        listenersLock.writeLock().lock();
        try {
            if (!listeners.containsKey(messageType)) {
                listeners.put(messageType, new ArrayList<>());
            }

            listeners.get(messageType).add(onMessage);
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    public boolean send(String mcServer, Message message) {
        return server.send(mcServer, message);
    }

    public <T extends Message> Optional<Message> sendBlocking(String mcServer, Message request, int responseType,
                                                                   Class<T> responseClass) {
        var future = new CompletableFuture<Message>();

        Consumer<Message> onMessage = future::complete;
        addReceiver(responseType, responseClass, onMessage);
        server.send(mcServer, request);

        try {
            return Optional.of(future.get(WAIT_FOR_MESSAGE_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException | ExecutionException e) {
            throw new PostmanException(e);
        } catch (TimeoutException e) {
            return Optional.empty();
        } finally {
            removeReceiver(responseType, onMessage);
        }
    }

    public void onMessage(Message message) {
        passMessageToReceivers(message);
        passMessageToListeners(message);
    }

    private <T extends Message> void addReceiver(int messageType, Class<T> messageClass, Consumer<Message> onMessage) {
        server.registerMessageType(messageType, messageClass);

        receiversLock.writeLock().lock();
        try {
            if (!receivers.containsKey(messageType)) {
                receivers.put(messageType, new ArrayList<>());
            }

            receivers.get(messageType).add(onMessage);
        } finally {
            receiversLock.writeLock().unlock();
        }
    }

    private void removeReceiver(int messageType, Consumer<? extends Message> onMessage) {
        receiversLock.writeLock().lock();
        try {
            receivers.get(messageType).remove(onMessage);
        } finally {
            receiversLock.writeLock().unlock();
        }
    }

    private void passMessageToReceivers(Message message) {
        receiversLock.readLock().lock();
        try {
            if (!receivers.containsKey(message.getType())) {
                return;
            }

            receivers.get(message.getType()).forEach(r -> r.accept(message));
        } finally {
            receiversLock.readLock().unlock();
        }
    }

    private void passMessageToListeners(Message message) {
        listenersLock.readLock().lock();
        try {
            if (!listeners.containsKey(message.getType())) {
                return;
            }

            listeners.get(message.getType()).forEach(r -> r.accept(message));
        } finally {
            listenersLock.readLock().unlock();
        }
    }
}
