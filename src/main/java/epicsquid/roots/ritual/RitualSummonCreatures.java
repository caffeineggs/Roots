package epicsquid.roots.ritual;

import epicsquid.mysticallib.util.ItemUtil;
import epicsquid.roots.entity.ritual.EntityRitualBase;
import epicsquid.roots.entity.ritual.EntityRitualSummonCreatures;
import epicsquid.roots.init.ModItems;
import epicsquid.roots.init.ModRecipes;
import epicsquid.roots.recipe.SummonCreatureRecipe;
import epicsquid.roots.ritual.conditions.ConditionRunedPillars;
import epicsquid.roots.ritual.conditions.ConditionValidSummon;
import epicsquid.roots.tileentity.TileEntityOfferingPlate;
import epicsquid.roots.util.RitualUtil;
import epicsquid.roots.util.types.Property;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreIngredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RitualSummonCreatures extends RitualBase {
  public static Property.PropertyDuration PROP_DURATION = new Property.PropertyDuration(200);
  public static Property<Integer> PROP_RADIUS_X = new Property<>("radius_x", 3).setDescription("Radius on the X Axis of the square area in which the ritual takes place");
  public static Property<Integer> PROP_RADIUS_Z = new Property<>("radius_z", 3).setDescription("Radius on the Z Axis of the square area in which the ritual takes place");
  public static Property.PropertyInterval PROP_INTERVAL = new Property.PropertyInterval(150).setDescription("interval in ticks between each summoned creature");
  public static Property<Integer> PROP_TRIES = new Property<>("tries", 10).setDescription("number of attempts at finding a random good (satisfying the conditions) position to spawn the creature");

  public int radius_x, radius_z, tries, interval;

  public RitualSummonCreatures(String name, boolean disabled) {
    super(name, disabled);
    properties.addProperties(PROP_DURATION, PROP_RADIUS_X, PROP_RADIUS_Z, PROP_INTERVAL, PROP_TRIES);
    setEntityClass(EntityRitualSummonCreatures.class);
  }

  @Override
  public void init() {
    recipe = new RitualRecipe(this,
        new ItemStack(Items.WHEAT_SEEDS),
        new OreIngredient("cropWheat"),
        new OreIngredient("egg"),
        new ItemStack(Items.ROTTEN_FLESH),
        new ItemStack(Items.WHEAT_SEEDS)
    );
    addCondition(new ConditionValidSummon());
    addCondition(new ConditionRunedPillars(RitualUtil.RunedWoodType.BIRCH, 3, 1));
    setIcon(ModItems.ritual_summon_creatures);
    setColor(TextFormatting.DARK_PURPLE);
    setBold(true);
  }

  @Override
  public void doFinalise() {
    duration = properties.get(PROP_DURATION);
    radius_x = properties.get(PROP_RADIUS_X);
    radius_z = properties.get(PROP_RADIUS_Z);
    interval = properties.get(PROP_INTERVAL);
    tries = properties.get(PROP_TRIES);
  }

  @Override
  public EntityRitualBase doEffect(World world, BlockPos pos, @Nullable EntityPlayer player) {
    EntityRitualSummonCreatures entity = (EntityRitualSummonCreatures) this.spawnEntity(world, pos, EntityRitualSummonCreatures.class, player);
    if (!world.isRemote) {
      List<TileEntityOfferingPlate> plates = RitualUtil.getNearbyOfferingPlates(world, pos);
      List<ItemStack> plateItems = RitualUtil.getItemsFromNearbyPlates(plates);

      SummonCreatureRecipe recipe = ModRecipes.findSummonCreatureEntry(plateItems);
      List<ItemStack> ingredients = new ArrayList<>();
      if (recipe != null) {
        for (TileEntityOfferingPlate plate : plates) {
          ingredients.add(plate.removeItem());
        }
      }
      ItemStack essence = ItemStack.EMPTY;
      if (recipe == null) {
        for (TileEntityOfferingPlate plate : plates) {
          ItemStack stack = plate.getHeldItem();
          if (stack.getItem() == ModItems.life_essence) {
            essence = stack;
            plate.removeItem();
            break;
          }
        }
      }
      if (!ingredients.isEmpty()) {
        for (ItemStack stack : recipe.transformIngredients(ingredients, null)) {
          ItemUtil.spawnItem(world, pos.add(random.nextBoolean() ? -1 : 1, 1, random.nextBoolean() ? -1 : 1), stack);
        }
      }

      if (entity != null) {
        entity.setEssence(essence);
        entity.setSummonRecipe(recipe);
      }
    }

    return entity;
  }
}
