package io.redspace.ironsspellbooks.item.armor;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public abstract class ImbuableChestplateArmorItem extends ExtendedArmorItem implements IPresetSpellContainer {

    public ImbuableChestplateArmorItem(Holder<ArmorMaterial> pMaterial, Type pType, Properties pProperties, AttributeContainer... attributes) {
        super(pMaterial, pType, pProperties, attributes);
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null) {
            return;
        }

        if (itemStack.getItem() instanceof ArmorItem armorItem && armorItem.getType() == Type.CHESTPLATE) {
            if (!ISpellContainer.isSpellContainer(itemStack)) {
                var spellContainer = ISpellContainer.create(1, true, true);
                itemStack.set(ComponentRegistry.SPELL_CONTAINER, spellContainer);
            }
        }
    }
}
