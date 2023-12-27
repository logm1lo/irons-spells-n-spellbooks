package io.redspace.ironsspellbooks.spells.ender;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.PortalManager;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalData;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalEntity;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalPos;
import io.redspace.ironsspellbooks.util.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class PortalSpell extends AbstractSpell {
    public static final int PORTAL_RECAST_COUNT = 2;
    private final ResourceLocation spellId = new ResourceLocation(IronsSpellbooks.MODID, "portal");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(100)
            .build();

    public PortalSpell() {
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 10;
        this.baseManaCost = 50;
        this.manaCostPerLevel = 10;
        this.castTime = 0;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.PORTAL_TRIGGER);
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new PortalData();
    }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return PORTAL_RECAST_COUNT;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (Log.SPELL_DEBUG) {
            IronsSpellbooks.LOGGER.debug("PortalSpell.onCast isClient:{}, entity:{}, pmd:{}", level.isClientSide, entity, playerMagicData);
        }

        if (entity instanceof Player player && level instanceof ServerLevel serverLevel) {
            Vec3 portalLocation = TeleportSpell.findTeleportLocation(level, entity, getCastDistance(spellLevel, entity));
            float portalRotation = 90 + Utils.getAngle(portalLocation.x, portalLocation.z, entity.getX(), entity.getZ()) * Mth.RAD_TO_DEG;
            if (playerMagicData.getPlayerRecasts().hasRecastForSpell(getSpellId())) {
                var portalData = (PortalData) playerMagicData.getPlayerRecasts().getRecastInstance(getSpellId()).getCastData();

                if (portalData.globalPos1 != null & portalData.portalEntityId1 != null) {
                    portalData.globalPos2 = PortalPos.of(player.level.dimension(), portalLocation, portalRotation);
                    portalData.setPortalDuration(getPortalDuration(spellLevel, player));
                    PortalEntity secondPortalEntity = setupPortalEntity(level, portalData, player, portalLocation, portalRotation);
                    portalData.portalEntityId2 = secondPortalEntity.getUUID();
                    PortalManager.INSTANCE.addPortalData(portalData.portalEntityId1, portalData);
                    PortalManager.INSTANCE.addPortalData(portalData.portalEntityId2, portalData);
                }
            } else {
                var portalData = new PortalData();
                portalData.setPortalDuration(getRecastDuration(spellLevel, player) + 10);
                PortalEntity portalEntity = setupPortalEntity(level, portalData, player, portalLocation, portalRotation);
                portalData.globalPos1 = PortalPos.of(player.level.dimension(), portalLocation, portalRotation);
                portalData.portalEntityId1 = portalEntity.getUUID();
                playerMagicData.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(), spellLevel, 1, getRecastDuration(spellLevel, player), portalData));
            }
        }

        super.onCast(level, spellLevel, entity, playerMagicData);
    }

    private PortalEntity setupPortalEntity(Level level, PortalData portalData, Player owner, Vec3 spawnPos, float rotation) {
        var portalEntity = new PortalEntity(level, portalData);
        portalEntity.setOwnerUUID(owner.getUUID());
        portalEntity.moveTo(spawnPos);
        portalEntity.setYRot(rotation);
        level.addFreshEntity(portalEntity);
        return portalEntity;
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, int spellLevel, int recastsRemainingAtCancel, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        if (recastResult != RecastResult.USED_ALL_RECASTS) {
            if (castDataSerializable instanceof PortalData portalData && portalData.portalEntityId1 != null) {
                if (portalData.globalPos1 != null) {
                    var server = serverPlayer.getServer();
                    if (server != null) {
                        var level = server.getLevel(portalData.globalPos1.dimension());
                        if (level != null) {
                            var portal1 = level.getEntity(portalData.portalEntityId1);
                            if (portal1 != null) {
                                portal1.discard();
                            } else {
                                PortalManager.INSTANCE.removePortalData(portalData.portalEntityId1);
                            }
                        }
                    }
                }
            }
        }
    }

    public int getRecastDuration(int spellLevel, LivingEntity caster) {
        //TODO: revisit this
        return 20 * 120;
    }

    public int getPortalDuration(int spellLevel, LivingEntity caster) {
        //TODO: revisit this
        return (int) (getSpellPower(spellLevel, caster) * 200);
    }

    private float getPortalMaxDistanceApart(int spellLevel, LivingEntity sourceEntity) {
        //TODO: revisit this
        return 200;
    }

    private float getCastDistance(int spellLevel, LivingEntity sourceEntity) {
        //TODO: revisit this
        return 48;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getCastDistance(spellLevel, caster), 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

}
