package sekelsta.horse_colors.entity;
import net.minecraft.entity.passive.horse.*;

import javax.annotation.Nullable;

import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;

import sekelsta.horse_colors.breed.Breed;
import sekelsta.horse_colors.breed.donkey.*;
import sekelsta.horse_colors.breed.horse.*;
import sekelsta.horse_colors.config.HorseConfig;
import sekelsta.horse_colors.entity.genetics.EquineGenome;
import sekelsta.horse_colors.entity.genetics.Species;
import sekelsta.horse_colors.HorseColors;

public class MuleGeneticEntity extends AbstractHorseGenetic {
    protected static final DataParameter<Integer> SPECIES = EntityDataManager.<Integer>defineId(MuleGeneticEntity.class, DataSerializers.INT);

    private static final ResourceLocation LOOT_TABLE = new ResourceLocation("minecraft", "entities/mule");

    public MuleGeneticEntity(EntityType<? extends MuleGeneticEntity> entity, World world) {
        super(entity, world);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(SPECIES, Species.MULE.ordinal());
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("Species", this.getSpecies().toString());
    }

   /**
    * Helper method to read subclass entity data from NBT.
    */
    public void readAdditionalSaveData(CompoundNBT compound) {
        throw new RuntimeException("Not yet implemented");
        //super.readAdditionalSaveData(compound);
        //this.setSpecies(Species.valueOf(compound.getString("Species")));
    }

    @Override
    public ResourceLocation getDefaultLootTable() {
        return this.LOOT_TABLE;
    }

    @Override
    public boolean fluffyTail() {
        return true;
    }

    @Override
    public boolean longEars() {
        return true;
    }

    @Override
    public boolean thinMane() {
        return false;
    }

    public void setSpecies(Species species) {
        this.entityData.set(SPECIES, species.ordinal());
    }

    @Override
    public Species getSpecies() {
        return Species.values()[this.entityData.get(SPECIES).intValue()];
    }

    @Override
    protected boolean canParent() {
        return false;
    }

    @Override
    // Helper function for createChild that creates and spawns an entity of the 
    // correct species
    public AbstractHorseEntity getChild(ServerWorld world, AgeableEntity ageable) {
        MuleGeneticEntity child = ModEntities.MULE_GENETIC.create(this.level);
        return child;
    }

    protected SoundEvent getAmbientSound() {
        super.getAmbientSound();
        return SoundEvents.MULE_AMBIENT;
    }

    protected SoundEvent getDeathSound() {
        super.getDeathSound();
        return SoundEvents.MULE_DEATH;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        super.getHurtSound(damageSourceIn);
        return SoundEvents.MULE_HURT;
    }

    protected void playChestEquipSound() {
        this.playSound(SoundEvents.MULE_CHEST, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    @Override
    protected ITextComponent getTypeName() {
        if (!this.isBaby() && !HorseConfig.BREEDING.enableGenders.get()
            && this.getSpecies() == Species.HINNY) {
            String s = "entity." + HorseColors.MODID + ".hinny";
            return new TranslationTextComponent(s);
        }
        return super.getTypeName();
    }

    /**
     * Called only once on an entity when first time spawned, via egg, mob spawner, natural spawning etc, but not called
     * when entity is reloaded from nbt. Mainly used for initializing attributes and inventory
     */
    @Nullable
    @Override
    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag)
    {
        spawnDataIn = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
        EquineGenome horse = new EquineGenome(Species.HORSE);
        horse.randomize(DefaultHorse.breed);
        EquineGenome donkey = new EquineGenome(Species.DONKEY);
        donkey.randomize(DefaultDonkey.breed);
        this.genes.inheritGenes(horse, donkey);
        this.useGeneticAttributes();
        return spawnDataIn;
    }
}
