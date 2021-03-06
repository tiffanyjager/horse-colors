package sekelsta.horse_colors;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;


public class CreativeTab extends ItemGroup {
    // Make creative mode tab
    public static final CreativeTab instance = new CreativeTab(ItemGroup.TABS.length, "tabHorseColors");

    public CreativeTab(int index, String label) {
        super(index, label);
    }

    @Override
    public ItemStack makeIcon() {
        return new ItemStack(Items.HAY_BLOCK);
    }
}
