package io.redspace.ironsspellbooks.entity;

import io.redspace.ironsspellbooks.registries.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class VisualFallingBlockEntity extends FallingBlockEntity {
    public VisualFallingBlockEntity(EntityType<? extends VisualFallingBlockEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    private double originalX;
    private double originalY;
    private double originalZ;
    private double ticks;

    @Override
    public void setOnGround(boolean pOnGround) {
    }

    @Override
    public boolean isOnGround() {
        return tickCount > 1 && (this.position().y <= originalY || this.getDeltaMovement().lengthSqr() < .001f || tickCount > 200);
    }

    public VisualFallingBlockEntity(Level pLevel, double pX, double pY, double pZ, BlockState pState) {
        this(EntityRegistry.FALLING_BLOCK.get(), pLevel);

        originalX = pX;
        originalY = pY;
        originalZ = pZ;
        ticks = 0;

        this.blocksBuilding = false;
        this.blockState = pState;
        this.setPos(pX + .5, pY, pZ + .5);
        this.xo = pX;
        this.yo = pY;
        this.zo = pZ;
        this.setStartPos(this.blockPosition());

        this.dropItem = false;
        this.cancelDrop = true;
    }

    @Override
    public void tick() {
//        super.tick();
        if (this.blockState.isAir() || this.isOnGround()) {
            this.discard();
        } else {
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.08D, 0.0D));
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.dropItem = false;
        this.cancelDrop = true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void callOnBrokenAfterFall(Block pBlock, BlockPos pPos) {
        return;
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

}