package com.github.maksymiliank.arrivalpostofficeproxy.websocket.message;

public abstract class Message {

    private int type;

    public Message() {}

    public Message(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
