package com.github.maksymiliank.arrivalpostofficeproxy;

import com.github.maksymiliank.arrivalpostofficeproxy.config.ConfigLoader;
import net.md_5.bungee.api.plugin.Plugin;

public class ArrivalPostOfficeProxy extends Plugin {

    @Override
    public void onEnable() {
        var config = ConfigLoader.load(getDataFolder());
    }
}
