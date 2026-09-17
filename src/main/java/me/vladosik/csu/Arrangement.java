package me.vladosik.csu;

import com.google.gson.*;
import me.vladosik.csu.config.Config;
import me.vladosik.csu.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class Arrangement {
    private static final byte INVENTORY_SIZE = 36;
    private static final byte CONTAINER_MENU_SIZE = 45;

    private final List<Item> arrangement;

    public static Arrangement primary() throws IllegalStateException {
        try {
            return new Arrangement(JsonParser.parseString(
                Files.readString(CodespaceUtils.config.primaryArrangement.toPath())
            ).getAsJsonArray());
        } catch (IOException e) {
            throw new IllegalStateException("failed to parse primary loadout file!", e);
        }
    }

    public Arrangement(Inventory inventory) {
        arrangement = new ArrayList<>();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            arrangement.add(inventory.getItem(i).getItem());
        }
    }

    public Arrangement(JsonArray array) {
        if (array.size() != 36) throw new IllegalStateException("The size of JsonArray with an arrangement should be size of exactly 36, not "+array.size()+'!');
        arrangement = array.asList().stream().map(e -> Utils.parseItemOrThrow(e.getAsString())).toList();
    }

    public void rearrange(boolean wait) {
        Config config = CodespaceUtils.config;
        Thread.startVirtualThread(() -> {
            if (wait) try {
                Thread.sleep(config.delayBeforeAutoApply);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player == null) return;
                var menu = client.player.containerMenu;
                boolean isWithCreativeMenu = client.screen instanceof CreativeModeInventoryScreen;
                List<ItemStack> values = findCreativeValues(menu);
                var editorEquipment = collectDecoratedItems(menu);
                var packets = new ArrayList<ServerboundSetCreativeModeSlotPacket>();

                for (int i = 0; i < INVENTORY_SIZE; ++i) {
                    Item current = arrangement.get(i);
                    int slotId = (i < 9) ? (isWithCreativeMenu ? i+45 : i+36) : i;

                    ItemStack toApply = ItemStack.EMPTY;
                    if (!current.equals(Items.AIR)) {
                        toApply = editorEquipment.getOrDefault(current, new ItemStack(current));
                    }
                    menu.getSlot(slotId).set(toApply);
                    packets.add(new ServerboundSetCreativeModeSlotPacket(slotId, toApply));
                }
                if (config.movementMultiplier.enable()) {
                    var item = config.movementMultiplier.assemble();
                    menu.getSlot(8 /* ботинки */).set(item);
                    packets.add(new ServerboundSetCreativeModeSlotPacket(8, item));
                }

                // Действия с пустыми слотами
                var emptySlots = findEmptySlots(menu);
                if (values.isEmpty()) {
                    for (int slot : findEmptySlots(menu)) {
                        packets.add(new ServerboundSetCreativeModeSlotPacket(slot, ItemStack.EMPTY));
                    }
                } else {
                    for (int slot : emptySlots) {
                        if (values.isEmpty()) break;
                        var value = values.removeFirst();
                        menu.getSlot(slot).set(value);
                        packets.add(new ServerboundSetCreativeModeSlotPacket(slot, value));
                    }
                    for (int slot : findEmptySlots(menu)) {
                        packets.add(new ServerboundSetCreativeModeSlotPacket(slot, ItemStack.EMPTY));
                    }
                }
                menu.broadcastChanges();
                Utils.sendDebugMessage("upserting "+packets.size()+" packets");
                CodespaceUtils.getPacketManager().upsertAll(packets);
            });
        });
    }

    private ArrayList<Integer> findEmptySlots(AbstractContainerMenu menu) {
        ArrayList<Integer> slots = new ArrayList<>();
        for (int i = 9 /* первый слот хотбара */; i < CONTAINER_MENU_SIZE; i++) {
            if (menu.getSlot(i).getItem().isEmpty()) slots.add(i);
        }
        return slots;
    }

    private HashMap<Item, ItemStack> collectDecoratedItems(AbstractContainerMenu menu) {
        var map = new HashMap<Item, ItemStack>();
        menu.slots.forEach(s -> {
            if (s.getItem().isEmpty()) return;
            if (!(Utils.extractCustomData(s.getItem()) instanceof JsonObject obj)) return;
            if (obj.has("PublicBukkitValues") && obj.getAsJsonObject("PublicBukkitValues").has("creative_plus:editor_equipment"))
                map.put(s.getItem().getItem(), s.getItem());
        });
        return map;
    }

    private List<ItemStack> findCreativeValues(AbstractContainerMenu menu) {
        ArrayList<ItemStack> list = new ArrayList<>();
        menu.slots.forEach(s -> {
            if (s.getItem().isEmpty()) return;
            if (!(Utils.extractCustomData(s.getItem()) instanceof JsonObject object)) return;
            if (!object.has("creative_plus")) return;
            list.add(s.getItem());
        });
        return list;
    }

    public JsonArray serialize() {
        JsonArray array = new JsonArray();
        arrangement.stream()
            .map(Item::toString)
            .map(s -> s.replace("minecraft:air", ""))
            .map(s -> {
                String[] identifier = s.split(":");
                if (identifier.length == 2) return identifier[1];
                else return s;
            })
            .forEach(s -> array.add(new JsonPrimitive(s)));
        return array;
    }

    @Override
    public String toString() {
        return serialize().toString();
    }
}
