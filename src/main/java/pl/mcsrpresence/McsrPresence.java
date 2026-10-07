package pl.mcsrpresence;

import net.fabricmc.api.ClientModInitializer;

public class McsrPresence implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DiscordPresence.init();
    }
}
