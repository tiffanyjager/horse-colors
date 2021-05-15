package sekelsta.horse_colors.entity.genetics;

import java.util.*;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import sekelsta.horse_colors.breed.Breed;
import sekelsta.horse_colors.client.renderer.CustomLayeredTexture;
import sekelsta.horse_colors.client.renderer.TextureLayerGroup;
import sekelsta.horse_colors.config.HorseConfig;
import sekelsta.horse_colors.util.RandomSupplier;
import sekelsta.horse_colors.util.Util;

public abstract class Genome {
    public final Species species;
    public abstract List<Enum> listGenes();
    public List<Linkage> listLinkages() {
        ArrayList linkages = new ArrayList<Genome.Linkage>();
        for (Enum gene : listGenes()) {
            linkages.add(new Genome.Linkage(gene));
        }
        return linkages;
    }

    protected IGeneticEntity entity;

    protected String textureCacheName;
    protected TextureLayerGroup textureLayers;

    protected final RandomSupplier randSource;

    public static java.util.Random rand = new java.util.Random();

    public Genome(Species species, RandomSupplier rand) {
        this(species, new FakeGeneticEntity(), rand);
    }

    public Genome(Species species, IGeneticEntity entityIn, RandomSupplier rand) {
        this.species = species;
        this.entity = entityIn;
        this.randSource = rand;
    }

    public void resetTexture() {
        this.textureCacheName = null;
    }

    public abstract List<List<String>> getBookContents();
    public abstract void setTexturePaths();
    public abstract String genesToString();
    public abstract void genesFromString(String s);
    public abstract boolean isValidGeneString(String s);

    @OnlyIn(Dist.CLIENT)
    public String getTexture()
    {
        if (this.textureCacheName == null)
        {
            this.setTexturePaths();
        }
        return this.textureCacheName;
    }

    @OnlyIn(Dist.CLIENT)
    public TextureLayerGroup getTexturePaths()
    {
        if (this.textureCacheName == null)
        {
            this.setTexturePaths();
        }

        return this.textureLayers;
    }

    @Deprecated
    public abstract int getGeneSize(String gene);

    @Deprecated
    public void setNamedGene(String name, int val, Map<String, Integer> map)
    {
        String chr = getGeneChromosome(name);
        if (map.get(chr) == null) {
            map.put(chr, 0);
        }
        map.put(chr, (map.get(chr) & (~getGeneLoci(name))) 
            | (val << (getGenePos(name) % 32)));
    }

    @Deprecated
    public int getNamedGene(String name, Map<String, Integer> map)
    {
        String chr = getGeneChromosome(name);
        if (map.get(chr) == null) {
            map.put(chr, 0);
        }
        // Use unsigned right shift to avoid returning negative numbers
        return (map.get(chr) & getGeneLoci(name)) >>> getGenePos(name);
    }

    @Deprecated
    public int getGenePos(String name)
    {
        return getPos(name, listGenes());
    }

    @Deprecated
    private int getPos(String name, List<Enum> genes)
    {
        int i = 0;
        for (Enum gene : genes)
        {
            int next = (i + (2 * getGeneSize(gene.toString())));
            // Special case to keep each gene completely on the same int
            if (next / 32 != i / 32 && next % 32 != 0)
            {
                i = (i / 32 + 1) * 32;
            }

            if (gene.toString().equals(name))
            {
                return i;
            }
            i += (2 * getGeneSize(gene.toString()));
        }

        // Return statement needed to compile
        System.out.println("Gene not recognized: " + name);
        return -1;
    }

    @Deprecated
    public int getGeneIndex(String name) {
        int n = 0;
        for (Enum gene : listGenes()) {
            if (gene.toString().equals(name)) {
                return n;
            }
            ++n;
        }
        throw new RuntimeException("Unrecognized name");
    }

    @Deprecated
    public int getGeneLoci(String gene)
    {
        return getLoci(gene, getGenePos(gene));
    }

    /* This returns a bitmask which is 1 where the gene is stored and 0 everywhere else. */
    @Deprecated
    private int getLoci(String gene, int pos)
    {
        return ((1 << (2 * getGeneSize(gene))) - 1) << (pos % 32);
    }

    @Deprecated
    public String getGeneChromosome(String gene)
    {
        // Which of the ints full of genes ours is on
        return Integer.toString(getGenePos(gene) / 32);
    }

    @Deprecated
    public int getAlleleOld(String name, int n, Map<String, Integer> map)
    {
        int gene = getNamedGene(name, map);
        gene >>= n * getGeneSize(name);
        gene %= 1 << getGeneSize(name);
        return gene;
    }

    @Deprecated
    public void setAlleleOld(String name, int n, int v, Map<String, Integer> map)
    {
        int other = getAlleleOld(name, 1 - n, map);
        int size = getGeneSize(name);
        setNamedGene(name, (other << ((1 - n) * size)) | (v << (n * size)), map);
    }

    public int getAllele(Enum gene, int n) {
        // Each gene has two alleles, so double the index
        int index = 2 * gene.ordinal() + n;
        if (index >= entity.getGeneData().length()) {
            return 0;
        }
        return (int)entity.getGeneData().charAt(index);
    }

    public void setAllele(Enum gene, int n, int v) {
        int index = 2 * gene.ordinal() + n;
        StringBuffer buffer = new StringBuffer(entity.getGeneData());
        // Append null characters until it is long enough
        if (buffer.length() <= index) {
            buffer.setLength(index + 1);
        }
        buffer.setCharAt(index, (char)v);
        entity.setGeneData(new String(buffer));
    }

    // Replace the given allele with a random one.
    // It may be the same as before.
    public void mutateAllele(Enum gene, int n) {
        Breed breed = entity.getDefaultBreed();
        if (!breed.contains(gene.toString())) {
            return;
        }
        List<Float> frequencies = breed.get(gene.toString());
        List<Integer> allowedAlleles = new ArrayList<>();
        float val = 0;
        for (int i = 0; i < frequencies.size(); ++i) {
            if (val >= 1f) {
                break;
            }
            if (val < frequencies.get(i)) {
                allowedAlleles.add(i);
                val = frequencies.get(i);
            }
        }
        int size = allowedAlleles.size();
        int v = allowedAlleles.get(this.rand.nextInt(size));
        setAllele(gene, n, v);
    }

    // Will mutate with p probability
    public void mutateAlleleChance(Enum gene, int n, double p) {
        if (this.rand.nextDouble() < p) {
            mutateAllele(gene, n);
        }
    }

    public void mutate() {
        double p = HorseConfig.GENETICS.mutationChance.get();
        for (Enum gene : listGenes()) {
            int a = getAllele(gene, 0);
            int b = getAllele(gene, 1);
            mutateAlleleChance(gene, 0, p);
            mutateAlleleChance(gene, 1, p);
        }
    }

    // Add together allele values for a set of genes named according to
    // name + n, where min <= n < max
    public int sumGenes(Class enumType, String name, int min, int max) {
        int sum = 0;
        for (int i = min; i < max; ++i) {
            Enum e = Enum.valueOf(enumType, name + i);
            sum += getAllele(e, 0);
            sum += getAllele(e, 1);
        }
        return sum;
    }

    public boolean hasAllele(Enum gene, int allele)
    {
        return getAllele(gene, 0) == allele || getAllele(gene, 1) == allele;
    }

    public int getMaxAllele(Enum gene)
    {
        return Math.max(getAllele(gene, 0), getAllele(gene, 1));
    }

    public boolean isHomozygous(Enum gene, int allele)
    {
        return  getAllele(gene, 0) == allele && getAllele(gene, 1) == allele;
    }

    public int countAlleles(Enum gene, int allele) {
        int count = 0;
        if (getAllele(gene, 0) == allele) {
            count++;
        }
        if (getAllele(gene, 1) == allele) {
            count++;
        }
        return count;
    }

    public void inheritGenes(Genome parent1, Genome parent2) {
        int rand1 = this.rand.nextInt(2);
        int rand2 = this.rand.nextInt(2);
        for (Linkage link : this.listLinkages()) {
            int allele1 = parent1.getAllele(link.gene, rand1);
            int allele2 = parent2.getAllele(link.gene, rand2);
            this.setAllele(link.gene, 0, allele1);
            this.setAllele(link.gene, 1, allele2);
            if (this.rand.nextFloat() < link.p) {
                rand1 = 1 - rand1;
            }
            if (this.rand.nextFloat() < link.p) {
                rand2 = 1 - rand2;
            }
        }
        mutate();
    }

    public int getRandom(String key) {
        return randSource.getVal(key, this.entity.getSeed());
    }

    // Returns the gene data as a base 64 string of printable characters
    public String getBase64() {
        String genes = entity.getGeneData();
        StringBuilder builder = new StringBuilder(genes.length());
        for (int i = 0; i < genes.length(); ++i) {
            int v = (int)genes.charAt(i);
            builder.append(Util.toBase64(v));
        }
        return builder.toString();
    }

    // Reads the gene data from a base 64 string
    public void setFromBase64(String base64Genes) {
        StringBuilder builder = new StringBuilder(base64Genes.length());
        for (int i = 0; i < base64Genes.length(); ++i) {
            char c = base64Genes.charAt(i);
            int v = Util.fromBase64(c);
            builder.append((char)v);
        }
        entity.setGeneData(builder.toString());
    }

    // Chromosomal linkage for storing in a list
    // p is the probability there are an odd number of crossovers between this gene and the next
    public static class Linkage {
        public Enum gene;
        public float p;
        public Linkage(Enum gene, float p) {
            this.gene = gene;
            this.p = p;
        }
        public Linkage(Enum gene) {
            this.gene = gene;
            this.p = 0.5f;
        }
    }
}
