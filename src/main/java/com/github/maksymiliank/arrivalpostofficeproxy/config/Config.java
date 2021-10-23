package com.github.maksymiliank.arrivalpostofficeproxy.config;

import java.util.List;

public record Config(int port, String apiHost, int apiPort, int apiReconnectPeriod, List<McServer> allowedMcServers) {}
