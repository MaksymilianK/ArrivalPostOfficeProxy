package com.github.maksymiliank.arrivalpostofficeproxy.websocket.message;

import com.google.gson.JsonObject;

public record RawMessage (int type, JsonObject body) {}
