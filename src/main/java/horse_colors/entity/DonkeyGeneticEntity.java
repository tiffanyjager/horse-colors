package sekelsta.horse_colors.entity;
import net.minecraft.entity.passive.horse.*;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;

import sekelsta.horse_colors.breed.Breed;
import sekelsta.horse_colors.breed.donkey.*;
import sekelsta.horse_colors.config.HorseConfig;
import sekelsta.horse_colors.entity.genetics.Species;
import sekelsta.horse_colors.util.Util;

public class DonkeyGeneticEntity extends AbstractHorseGenetic {

    public static List<Breed> breeds = ImmutableList.of(MiniatureDonkey.breed,
        MammothDonkey.breed);

    private static final ResourceLocation LOOT_TABLE = new ResourceLocation("minecraft", "entities/donkey");

    public DonkeyGeneticEntity(EntityType<? extends DonkeyGeneticEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT compound) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public ResourceLocation getDefaultLootTable() {
        return this.LOOT_TABLE;
    }

    protected SoundEvent getAmbientSound() {
        super.getAmbientSound();
        return SoundEvents.DONKEY_AMBIENT;
    }

    protected SoundEvent getDeathSound() {
        super.getDeathSound();
        return SoundEvents.DONKEY_DEATH;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        super.getHurtSound(damageSourceIn);
        return SoundEvents.DONKEY_HURT;
    }

    @Override
    public boolean fluffyTail() {
        return false;
    }

    @Override
    public boolean longEars() {
        return true;
    }

    @Override
    public boolean thinMane() {
        return true;
    }

    @Override
    public Species getSpecies() {
        return Species.DONKEY;
    }

   /**
    * Returns true if the mob is currently able to mate with the specified mob.
    */
    @Override
    public boolean canMate(AnimalEntity otherAnimal) {
        if (otherAnimal == this)
        {
            return false;
        }
        if (otherAnimal instanceof AbstractHorseGenetic) {
            if (!this.isOppositeGender((AbstractHorseGenetic)otherAnimal)) {
                return false;
            }
        }
        if (otherAnimal instanceof DonkeyGeneticEntity 
                || otherAnimal instanceof HorseGeneticEntity
                || otherAnimal instanceof DonkeyEntity 
                || otherAnimal instanceof HorseEntity)
        {
            return this.canParent() && Util.horseCanMate((AbstractHorseEntity)otherAnimal);
        }
        else
        {
            return false;
        }
    }

    // Helper function for createChild that creates and spawns an entity of the 
    // correct species
    @Override
    public AbstractHorseEntity getChild(ServerWorld world, AgeableEntity ageable)
    {
        if (ageable instanceof AbstractHorseGenetic) {
            AbstractHorseGenetic child = null;
            AbstractHorseGenetic other = (AbstractHorseGenetic)ageable;
            if (ageable instanceof HorseGeneticEntity) {
                child = ModEntities.MULE_GENETIC.create(this.level);
                if (HorseConfig.BREEDING.enableGenders.get()
                        && !this.isMale() && ((HorseGeneticEntity)ageable).isMale()) {
                    ((MuleGeneticEntity)child).setSpecies(Species.HINNY);
                }
            }
            else if (ageable instanceof DonkeyGeneticEntity) {
                child = ModEntities.DONKEY_GENETIC.create(this.level);
            }
            return child;
        }
        else if (ageable instanceof HorseEntity) {
            return EntityType.MULE.create(this.level);
        }
        else if (ageable instanceof DonkeyEntity) {
            return EntityType.DONKEY.create(this.level);
        }
        return null;
    }

    @Override
    public Breed getDefaultBreed() {
        return DefaultDonkey.breed;
    }

    @Override
    public int getPopulation() {
        return 40000000;
    }
}
