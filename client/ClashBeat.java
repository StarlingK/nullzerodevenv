package nullmod.client;

import java.io.InputStreamReader;
import java.io.Reader;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nullmod.NullMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class ClashBeat {
    private static final ResourceLocation FILE =
            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "clash_envelope.json");
    private static final float OFFSET = 0.08f;
    private static final float FALLBACK_BPM = 93.75f;

    private static float[] beat;
    private static float[] level;
    private static float rate = 50f;
    private static boolean loaded;

    private ClashBeat() {}

    private static void load() {
        loaded = true;
        try (Reader reader = new InputStreamReader(
                Minecraft.getInstance().getResourceManager().open(FILE))) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            rate = json.get("rate").getAsFloat();
            beat = read(json.getAsJsonArray("beat"));
            level = read(json.getAsJsonArray("level"));
        } catch (Exception e) {
            NullMod.LOGGER.error("Could not load the clash envelope; using a fixed pulse", e);
            beat = null;
            level = null;
        }
    }

    private static float[] read(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsInt() / 99.0f;
        }
        return out;
    }

    private static float sample(float[] data, float seconds) {
        float p = Math.max(0f, seconds) * rate;
        int i = (int) p;
        if (i >= data.length - 1) return 0f;
        float t = p - i;
        return data[i] * (1f - t) + data[i + 1] * t;
    }

    public static float beat(float seconds) {
        if (!loaded) load();
        if (beat == null) {
            float phase = (Math.max(0f, seconds) * FALLBACK_BPM / 60f) % 1f;
            return (float) Math.exp(-phase * 6.0);
        }
        return sample(beat, seconds + OFFSET);
    }

    public static float level(float seconds) {
        if (!loaded) load();
        if (level == null) return 0.5f;
        return sample(level, seconds + OFFSET);
    }
}
