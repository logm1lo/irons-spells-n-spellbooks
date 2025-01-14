package io.redspace.ironsspellbooks.effect;

import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;


@EventBusSubscriber()
public class BlightEffect extends MagicMobEffect {
    public static final float DAMAGE_PER_LEVEL = -.05f;
    public static final float HEALING_PER_LEVEL = -.10f;

    public BlightEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }

    @SubscribeEvent
    public static void reduceHealing(LivingHealEvent event) {
        var effect = event.getEntity().getEffect(MobEffectRegistry.BLIGHT);
        if (effect != null) {
            int lvl = effect.getAmplifier() + 1;
            float healingMult = 1 + HEALING_PER_LEVEL * lvl;
            float before = event.getAmount();
            event.setAmount(event.getAmount() * healingMult);
            //IronsSpellbooks.LOGGER.debug("BlightEffect.reduceHealing: {}->{}", before, event.getAmount());

        }
    }

    @SubscribeEvent
    public static void reduceDamageOutput(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity livingAttacker) {
            var effect = livingAttacker.getEffect(MobEffectRegistry.BLIGHT);
            if (effect != null) {
                int lvl = effect.getAmplifier() + 1;
                float before = event.getAmount();
                float multiplier = 1 + BlightEffect.DAMAGE_PER_LEVEL * lvl;
                event.setAmount(event.getAmount() * multiplier);
                //IronsSpellbooks.LOGGER.debug("BlightEffect.reduceDamageOutput: {}->{}", before, event.getAmount());
            }
        }
    }
}
