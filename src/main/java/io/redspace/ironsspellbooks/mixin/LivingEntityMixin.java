package io.redspace.ironsspellbooks.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.attribute.IMagicAttribute;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.effect.IMobEffectEndCallback;
import io.redspace.ironsspellbooks.item.weapons.IMultihandWeapon;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "onEffectRemoved", at = @At(value = "HEAD"))
    public void onEffectRemoved(MobEffectInstance pEffectInstance, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level.isClientSide) {
            if (pEffectInstance.getEffect().value() instanceof IMobEffectEndCallback mobEffect) {
                mobEffect.onEffectRemoved(self, pEffectInstance.getAmplifier());
            }
        }
    }

    @Inject(method = "updateInvisibilityStatus", at = @At(value = "TAIL"))
    public void updateInvisibilityStatus(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(MobEffectRegistry.TRUE_INVISIBILITY))
            self.setInvisible(true);
    }

    @Inject(method = "isCurrentlyGlowing", at = @At(value = "HEAD"), cancellable = true)
    public void isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level.isClientSide() && self.hasEffect(MobEffectRegistry.GUIDING_BOLT)) {
            cir.setReturnValue(true);
        }
    }

    @Shadow
    abstract ItemStack getLastHandItem(EquipmentSlot pSlot);

    @Unique
    private static final List<EquipmentSlot> handSlots = List.of(EquipmentSlot.OFFHAND, EquipmentSlot.MAINHAND);

    // The equipment change event fires 5 lines too early for this to have been able to be done via events
    @Inject(method = "collectEquipmentChanges", at = @At(value = "RETURN"))
    public void handleEquipmentChanges(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        //TODO: mixin to ItemStack#getAttributeModifiers and just: if(multihand && slot == offhand) -> slot = offhand...?

        // Last hand items are accurate at this point
        // Mainhand assigning/removing is handled by minecraft. All we are doing is fudging offhand handling
        // The return of this function is a map of equipmentslots to itemstacks, of itemstacks who have been changed
        var changedEquipment = cir.getReturnValue();
        if (changedEquipment == null) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        for (EquipmentSlot slot : handSlots) {
            ItemStack currentStack = changedEquipment.get(slot);
            if (currentStack == null) {
                // If this stack was not changed, continue
                continue;
            }
            ItemStack oldStack = getLastHandItem(slot);
            //IronsSpellbooks.LOGGER.debug("LivingMixin.handleEquipmentChanges - Hands: {}| {} -> {}", slot, oldStack, currentStack);
            boolean selected = currentStack.getItem() instanceof IMultihandWeapon;
            boolean deselected = oldStack.getItem() instanceof IMultihandWeapon;
            if (selected || deselected) {
                if (slot == EquipmentSlot.MAINHAND) {
                    ItemStack offhandStack = self.getOffhandItem();
                    if (offhandStack.getItem() instanceof IMultihandWeapon && !ItemStack.isSameItem(offhandStack, currentStack)) {
                        // If we select a mainhand item, revoke offhand attributes
                        // If we deselect a mainhand item, reinstate offhand attributes
                        if (selected) {
                            self.getAttributes().removeAttributeModifiers(filterApplicableAttributes(offhandStack.getAttributeModifiers()));
                        }
                        if (deselected) {
                            self.getAttributes().addTransientAttributeModifiers(filterApplicableAttributes(offhandStack.getAttributeModifiers()));
                        }
                    }
                } else if (slot == EquipmentSlot.OFFHAND) {
                    // If we select an offhand item, grant attributes, unless we already hold a mainhand item
                    // If we deselect an offhand item, revoke attributes
                    ItemStack mainhandStack = self.getMainHandItem();
                    if (selected) {
                        if (!(mainhandStack.getItem() instanceof IMultihandWeapon)) {
                            self.getAttributes().addTransientAttributeModifiers(filterApplicableAttributes(currentStack.getAttributeModifiers()));
                        }
                    }
                    if (deselected && !ItemStack.isSameItem(mainhandStack, oldStack)) {
                        self.getAttributes().removeAttributeModifiers(filterApplicableAttributes(oldStack.getAttributeModifiers()));
                    }
                }
            }
        }
    }

    @Unique
    private static Multimap<Holder<Attribute>, AttributeModifier> filterApplicableAttributes(ItemAttributeModifiers modifiers) {
        var list = modifiers.modifiers().stream().filter(entry -> entry.slot() == EquipmentSlotGroup.MAINHAND).toList();
        Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();
        for (ItemAttributeModifiers.Entry entry : list) {
            var predicate = ServerConfigs.APPLY_ALL_MULTIHAND_ATTRIBUTES.get() ? allNonBaseAttackAttributes : onlyIronAttributes;
            if (predicate.test(entry.attribute())) {
                map.put(entry.attribute(), entry.modifier());
            }
        }
        return map;
    }

    @Unique
    private static final Predicate<Holder<Attribute>> allNonBaseAttackAttributes = (attribute) -> !(attribute == Attributes.ENTITY_INTERACTION_RANGE || attribute == Attributes.ATTACK_DAMAGE || attribute == Attributes.ATTACK_SPEED || attribute == Attributes.ATTACK_KNOCKBACK);
    @Unique
    private static final Predicate<Holder<Attribute>> onlyIronAttributes = (attribute) -> attribute instanceof IMagicAttribute;

}