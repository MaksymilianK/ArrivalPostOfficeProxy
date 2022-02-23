package com.github.maksymiliank.arrivalpostofficeproxy.websocket.message;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RawMessageDeserializer implements JsonDeserializer<Message> {

    private final Map<Integer, Class<? extends Message>> messageTypes = new HashMap<>();

    private final ReadWriteLock messageTypesLock = new ReentrantReadWriteLock(true);

    public void registerMessageType(int messageType, Class<? extends Message> messageClass) {
        messageTypesLock.writeLock().lock();
        try {
            messageTypes.put(messageType, messageClass);
        } finally {
            messageTypesLock.writeLock().unlock();
        }
    }

    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        var body = json.getAsJsonObject();

        var typeJson = body.get("type");
        if (typeJson == null) {
            throw new JsonParseException("Message type is not present");
        }

        int type;
        try {
            type = typeJson.getAsInt();
        } catch (ClassCastException | IllegalStateException e) {
            throw new JsonParseException("Message type is not an integer");
        }

        messageTypesLock.readLock().lock();
        try {
            if (!messageTypes.containsKey(type)) {
                throw new JsonParseException("Message type %d is not registered".formatted(type));
            }

            return context.deserialize(json, messageTypes.get(type));
        } finally {
            messageTypesLock.readLock().unlock();
        }
    }
}
