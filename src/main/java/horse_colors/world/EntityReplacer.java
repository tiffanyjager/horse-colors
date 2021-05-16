package sekelsta.horse_colors.world;

import java.util.*;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.passive.horse.DonkeyEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import sekelsta.horse_colors.config.HorseConfig;
import sekelsta.horse_colors.entity.*;

public class EntityReplacer {
    private static Random rand = new Random();

    // Makes any modifications needed to the entity, and recursively to its
    // passengers
    public static void recursiveModifyEntityCompound(CompoundNBT compound) {
        // Check if the entity is a horse
        String horseId = ModEntities.HORSE_GENETIC.getRegistryName().toString();
        if (horseId.equals(compound.getString("id"))) {
            geneticToVanillaHorseTag(compound);
        }
        // Call recursively on passengers
        ListNBT passengers = compound.getList("Passengers", 10);
        for (int i = 0; i < passengers.size(); ++i) {
            recursiveModifyEntityCompound(passengers.getCompound(i));
        }
    }

    @SubscribeEvent
    public static void modifyChunkSave(ChunkDataEvent.Save event) {
        CompoundNBT chunkCompound = event.getData();
        CompoundNBT level = chunkCompound.getCompound("Level");
        ListNBT entities = level.getList("Entities", 10);
        for(int i = 0; i < entities.size(); ++i) {
            recursiveModifyEntityCompound(entities.getCompound(i));
        }
    }

    @SubscribeEvent
	public static void replaceHorses(EntityJoinWorldEvent event)
    {
        // We don't want to replace subclasses of horses
        if (event.getEntity().getClass() == HorseEntity.class
            && !event.getWorld().isClientSide
            && HorseConfig.SPAWN.convertVanillaHorses.get())
        {
            HorseEntity horse = (HorseEntity)event.getEntity();
            HorseGeneticEntity newHorse = ModEntities.HORSE_GENETIC.create(event.getWorld());
            CompoundNBT data = horse.saveWithoutId(new CompoundNBT());
            vanillaToGeneticHorseTag(data);
            newHorse.load(data);
            // Spawn the new horse
            // Normally this is done by calling world.addEntity, which
            // makes sure to load the chunk first. However calling that
            // from chunk loading creates a deadlock. Minecraft's chunk 
            // loading code calls ServerWorld.loadFromChunk
            // instead, which assumes the chunk is already loaded.
            World world = event.getWorld();
            if (world instanceof ServerWorld) {
                ((ServerWorld)world).loadFromChunk(newHorse);
            }
            // Cancel the event regardless
            event.setCanceled(true);
            // If the horse should be a passenger, make it so
            if (horse.isPassenger()) {
                // TODO horse.getVehicle()
            }
        }
	}

    // When an entity writes to NBT, the "ForgeData" compound in the result
    // points to the same object as that entity's persistent data.
    // This copies the data into a new object to avoid anything unexpected
    // happening if the original entity modifies its persistent data.
    private static CompoundNBT deepCopyForgeData(CompoundNBT compound) {
        CompoundNBT forge = new CompoundNBT();
        forge.merge(compound.getCompound("ForgeData"));
        compound.put("ForgeData", forge);
        return forge;
    }

    private static void geneticToVanillaHorseTag(CompoundNBT compound) {
        // Change entity ID
        compound.putString("id", EntityType.HORSE.getRegistryName().toString());
        // Move vanilla data into Forge data (persistent data)
        CompoundNBT forge = null;
        if (compound.contains("ForgeData")) {
            // Deep copy the forge data to avoid modifications by the entity
            forge = deepCopyForgeData(compound);
        }
        else {
            // We are saving a genetic horse that has never been a vanilla horse
            forge = new CompoundNBT();
            compound.put("ForgeData", forge);
        }
        // Set vanilla variant
        String variant = "Variant";
        if (forge.contains(variant)) {
            compound.putInt(variant, forge.getInt(variant));
            forge.remove(variant);
        }
        else {
            compound.putInt(variant, rand.nextInt());
        }
        // Set vanilla attributes
        boolean saveJump = forge.contains("JumpStrength");
        boolean saveHealth = forge.contains("Health");
        boolean saveSpeed = forge.contains("Speed");
        ListNBT attributes = compound.getList("Attributes", 10);
        for (int i = 0; i < attributes.size(); ++i) {
            CompoundNBT c = attributes.getCompound(i);
            String name = c.getString("Name");
            if (saveJump && "minecraft:horse.jump_strength".equals(name)) {
                c.putDouble("Base", forge.getDouble("JumpStrength"));
                forge.remove("JumpStrength");
            }
            else if (saveHealth && "minecraft:generic.max_health".equals(name)) {
                c.putDouble("Base", forge.getDouble("Health"));
                forge.remove("Health");
            }
            else if (saveSpeed && "minecraft:generic.movement_speed".equals(name)) {
                c.putDouble("Base", forge.getDouble("Speed"));
                forge.remove("Speed");
            }
        }
        // Move genetic data
        forge.put("HorseGeneticsData", compound.getCompound("HorseGeneticsData"));
        compound.remove("HorseGeneticsData");
        // TODO: check equipment
    }

    public static void vanillaToGeneticHorseTag(CompoundNBT compound) {
        // Deep copy the forge data to avoid modifications by the entity
        CompoundNBT forge = deepCopyForgeData(compound);
        // Save the vanilla horse's variant and stats
        if (compound.contains("Variant")) {
            forge.putInt("Variant", compound.getInt("Variant"));
            // Make sure to remove it so it won't be read as legacy data
            compound.remove("Variant");
        }
        ListNBT attributes = compound.getList("Attributes", 10);
        for (int i = 0; i < attributes.size(); ++i) {
            CompoundNBT c = attributes.getCompound(i);
            String name = c.getString("Name");
            double base = c.getDouble("Base");
            if ("minecraft:horse.jump_strength".equals(name)) {
                forge.putDouble("JumpStrength", base);
            }
            else if ("minecraft:generic.max_health".equals(name)) {
                forge.putDouble("Health", base);
            }
            else if ("minecraft:generic.movement_speed".equals(name)) {
                forge.putDouble("Speed", base);
            }
        }
        // Read in any saved genetic data
        if (forge.contains("HorseGeneticsData")) {
            compound.put("HorseGeneticsData", forge.getCompound("HorseGeneticsData"));
            forge.remove("HorseGeneticsData");
        }
        // TODO: equipment
    }
}
