package dev.velolib.playfront.config.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import dev.velolib.playfront.PlayfrontClient;

import java.awt.Color;
import java.io.IOException;

public class ColorTypeAdapter extends TypeAdapter<Color> {
    @Override
    public void write(JsonWriter out, Color color) throws IOException {
        // Saves as #AARRGGBB
        out.value(String.format("#%08X", color.getRGB()));
    }

    @Override
    public Color read(JsonReader in) throws IOException {
        String hex = in.nextString();
        try {
            String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
            return new Color((int) Long.parseLong(cleanHex, 16), true);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            PlayfrontClient.LOGGER.error("Failed to parse color '{}'. Falling back to transparent black.", hex);
            return new Color(0, 0, 0, 0);
        }
    }
}