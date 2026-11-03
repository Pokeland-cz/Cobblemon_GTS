package org.pokesplash.gts.util;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.ItemStack;
import org.pokesplash.gts.Gts;

/**
 * Methods to encode and decode ItemStack to and from json objects.
 */
public abstract class CodecUtils {

    /**
     * Encodes an ItemStack to a JsonElement so that it can be stored or saved to file.
     * @param stack The ItemStack to encode.
     * @return The encoded ItemStack, as a JsonElement.
     */
    public static JsonElement encodeItem(ItemStack stack) {
        return ItemStack.CODEC.encodeStart(Gts.server.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                stack).getOrThrow();
    }

    /**
     * Decodes a JsonElement to convert to back to an ItemStack.
     * @param json The JsonElement to decode.
     * @return The decoded JsonElement as an ItemStack.
     */
    public static ItemStack decodeItem(JsonElement json) {
        try {
            return ItemStack.CODEC.decode(Gts.server.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                    json).getOrThrow().getFirst();
        } catch (Exception e) {
            Gts.LOGGER.error("Could not decode item: " + getJsonData(json));
            return null;
        }
    }

    private static String getJsonData(JsonElement json) {
        if (json.isJsonArray()) return json.getAsJsonArray().toString();

        if (json.isJsonObject()) return json.getAsJsonObject().toString();

        if (json.isJsonPrimitive()) return json.getAsJsonPrimitive().toString();

        return null;
    }
}
