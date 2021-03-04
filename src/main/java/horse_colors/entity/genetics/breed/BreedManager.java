package sekelsta.horse_colors.entity.genetics.breed;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AddReloadListenerEvent;

import sekelsta.horse_colors.HorseColors;

public class BreedManager extends JsonReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();

    private static Map<ResourceLocation, Breed> breeds;

    private static BreedManager instance = new BreedManager();

    public BreedManager() {
        super(GSON, "breeds");
    }

    protected void apply(Map<ResourceLocation, JsonElement> mapIn, IResourceManager resourceManagerIn, IProfiler profilerIn) {
        breeds = new HashMap();
        for(ResourceLocation key : mapIn.keySet()) {
            // Forge uses names starting with _ for metadata
            if (key.getPath().startsWith("_")) {
                continue;
            }
            try {
                // Possible IllegalStateException will be caught
                JsonObject json = mapIn.get(key).getAsJsonObject();
                Breed b = deserializeBreed(json);
                if (b != null) {
                    breeds.put(key, b);
                }
            }
            catch (IllegalStateException e) {
                HorseColors.logger.error("Could not parse json: " + key);
            }
            catch (ClassCastException e) {
                HorseColors.logger.error("Unexpected data type in json: " + key);
            }
            HorseColors.logger.debug("Loaded " + breeds.size() 
                + " breed data files");
        }
    }

    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(instance);
    }

    private static Breed deserializeBreed(JsonObject json) 
        throws ClassCastException, IllegalStateException
    {
        Breed breed = new Breed();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            JsonArray jarray = entry.getValue().getAsJsonArray();
            ArrayList<Float> frequencies = new ArrayList<>();
            for (int i = 0; i < jarray.size(); ++i) {
                frequencies.add(jarray.get(i).getAsFloat());
            }
            breed.genes.put(entry.getKey(), frequencies);
        }
        return breed;
    }

    public static Breed getBreed(ResourceLocation name) {
        return breeds.get(name);
    }
}
