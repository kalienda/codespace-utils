package me.vladosik.csu.utils;

import me.vladosik.csu.CodespaceUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class PacketManager {
    private static ClientTickEvents.EndTick task;

    private final List<Packet<?>> packets = new ArrayList<>();
    private int packetsPerTick;

    public PacketManager(int packetsPerSec) {
        packetsPerTick = packetsPerSec / 20;

        if (task == null) {
            task = client -> {
                if (client.getConnection() == null) return;
                int i = 0;
                while (!packets.isEmpty() && i <= packetsPerTick) {
                    client.getConnection().send(packets.removeFirst());
                    ++i;
                }
            };
            ClientTickEvents.END_CLIENT_TICK.register(task);
        }
    }

    public void setPacketsRate(int packetsPerSecond) {
        this.packetsPerTick = packetsPerSecond / 20;
    }

    public <P extends Packet<?>> void upsert(@NonNull P packet) {
        if (CodespaceUtils.config.debugDisablePacketQueue) {
            if (Minecraft.getInstance().getConnection() != null) Minecraft.getInstance().getConnection().send(packet);
        } else {
            this.packets.add(packet);
        }
    }

    public <P extends Packet<?>> void upsertAll(@NonNull P @NonNull [] packets) {
        for (P packet : packets) { upsert(packet); }
    }

    public <P extends Packet<?>> void upsertAll(@NonNull Iterable<P> packets) {
        packets.forEach(this::upsert);
    }

    public <P extends Packet<?>> void upsertAll(@NonNull Iterator<P> packets) {
        packets.forEachRemaining(this::upsert);
    }

    public <P extends Packet<?>> void upsertAll(@NonNull Collection<P> packets) {
        packets.forEach(this::upsert);
    }
}
