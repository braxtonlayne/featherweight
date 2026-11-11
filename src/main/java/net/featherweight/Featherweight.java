package net.featherweight;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Featherweight.MOD_ID)
public final class Featherweight {
    public static final String MOD_ID = "featherweight";
    private static final int CLEANUP_INTERVAL_TICKS = 20 * 30;
    private int ticks;

    public Featherweight(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
    }

    // Guava only evicts expired entries while the cache is in use; sweep now and then so templates are
    // released even after the player stops exploring.
    private void onServerTick(ServerTickEvent.Post event) {
        if (++ticks >= CLEANUP_INTERVAL_TICKS) {
            ticks = 0;
            ExpiringTemplateMap.cleanUpAll();
        }
    }
}
