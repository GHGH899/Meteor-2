/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.entity.EntityAttributeManager;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.StatusEffectManager;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DamageUtils {
    private static final DamageSource explosion = new DamageSource(mc.getNetworkHandler().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DamageTypes.EXPLOSION));

    // Explosion damage

    public static float explosionDamage(LivingEntity target, Vec3d origin, float power, boolean predictMovement, BlockPos override, BlockState overrideState) {
        if (target == null) return 0f;
        if (target instanceof PlayerEntity player && EntityUtils.getGameMode(player) == GameMode.CREATIVE && !(player instanceof FakePlayerEntity)) return 0f;

        Vec3d position = predictMovement ? target.getPos().add(target.getVelocity()) : target.getPos();

        double modDistance = position.distanceTo(origin);
        if (modDistance > power) return 0f;

        Box box = target.getBoundingBox();
        if (predictMovement) box = box.offset(target.getVelocity());

        double exposure = getExposure(origin, box, override, overrideState);
        double impact = (1 - (modDistance / power)) * exposure;
        float damage = (int) ((impact * impact + impact) / 2 * 7 * 12 + 1);

        return calculateReductions(damage, target, explosion);
    }

    public static float crystalDamage(LivingEntity target, Vec3d crystal, boolean predictMovement, BlockPos obsidianPos) {
        return explosionDamage(target, crystal, 12f, predictMovement, obsidianPos, Blocks.OBSIDIAN.getDefaultState());
    }

    public static float crystalDamage(LivingEntity target, Vec3d crystal) {
        return explosionDamage(target, crystal, 12f, false, null, null);
    }

    public static float bedDamage(LivingEntity target, Vec3d bed) {
        return explosionDamage(target, bed, 10f, false, null, null);
    }

    public static float anchorDamage(LivingEntity target, Vec3d anchor) {
        return explosionDamage(target, anchor, 10f, false, BlockPos.ofFloored(anchor), Blocks.AIR.getDefaultState());
    }

    // Sword damage

    /**
     * @see PlayerEntity#attack(Entity)
     */
    public static float getAttackDamage(LivingEntity attacker, LivingEntity target) {
        float itemDamage = (float) EntityAttributeManager.getAttributeValue(attacker, EntityAttributes.GENERIC_ATTACK_DAMAGE);

        // Get enchant damage
        ItemStack stack = attacker.getStackInHand(attacker.getActiveHand());
        float enchantDamage = EnchantmentHelper.getAttackDamage(stack, target.getGroup());

        // Factor charge
        if (attacker instanceof PlayerEntity playerEntity) {
            float charge = playerEntity.getAttackCooldownProgress(0.5f);
            itemDamage *= 0.2f + charge * charge * 0.8f;
            enchantDamage *= charge;

            // Factor critical hit
            if (charge > 0.9f && attacker.fallDistance > 0f && !attacker.isOnGround() && !attacker.isClimbing() && !attacker.isTouchingWater() && !attacker.hasStatusEffect(StatusEffects.BLINDNESS) && !attacker.hasVehicle()) {
                itemDamage *= 1.5f;
            }
        }

        float damage = itemDamage + enchantDamage;

        damage = calculateReductions(damage, target, attacker instanceof PlayerEntity player ? mc.world.getDamageSources().playerAttack(player) : mc.world.getDamageSources().mobAttack(attacker));

        // Factor Fire Aspect
        if (EnchantmentHelper.getFireAspect(attacker) > 0 && !StatusEffectManager.hasStatusEffect(target, StatusEffects.FIRE_RESISTANCE)) {
            damage++;
        }

        return damage;
    }

    // Fall Damage

    /**
     * @see LivingEntity#computeFallDamage(float, float) (float, float, DamageSource)
     */
    @SuppressWarnings("JavadocReference")
    public static float fallDamage(LivingEntity entity) {
        if (entity instanceof PlayerEntity player && player.getAbilities().flying) return 0f;
        if (StatusEffectManager.hasStatusEffect(entity, StatusEffects.SLOW_FALLING) || StatusEffectManager.hasStatusEffect(entity, StatusEffects.LEVITATION)) return 0f;

        StatusEffectInstance jumpBoostInstance = StatusEffectManager.getStatusEffect(entity, StatusEffects.JUMP_BOOST);

        int surface = mc.world.getWorldChunk(entity.getBlockPos()).getHeightmap(Heightmap.Type.MOTION_BLOCKING).get(entity.getBlockX(), entity.getBlockZ());
        if (entity.getBlockY() >= surface) {
            int fallHeight = (int) (entity.getPos().y - surface + entity.fallDistance - 3d);
            if (jumpBoostInstance != null) fallHeight -= jumpBoostInstance.getAmplifier() + 1;
            
            return calculateReductions(fallHeight, entity, mc.world.getDamageSources().fall());
        }

        float totalHealth = entity.getHealth() + entity.getAbsorptionAmount();
        float survivableHeight = totalHealth;

        // Reverse protection reduction
        int protection = Math.min(EnchantmentHelper.getProtectionAmount(entity.getArmorItems(), mc.world.getDamageSources().fall()), 20);
        survivableHeight /= 1f - protection * 0.04f;

        // Reverse resistance reduction
        StatusEffectInstance resistance = StatusEffectManager.getStatusEffect(entity, StatusEffects.RESISTANCE);
        if (resistance != null) {
            int lvl = resistance.getAmplifier() + 1;
            survivableHeight /= 1f - lvl * 0.2f;
        }

        // Simple reverse armor reduction
        float armor = getArmor(entity);
        survivableHeight /= 1f - armor * 0.008f;

        survivableHeight += (float) EntityAttributeManager.getAttributeValue(entity, EntityAttributes.GENERIC_ARMOR_TOUGHNESS);

        if (jumpBoostInstance != null) survivableHeight += jumpBoostInstance.getAmplifier() + 1;

        survivableHeight += 3f;

        int raycastLength = MathHelper.ceil(survivableHeight - entity.fallDistance);

        BlockHitResult raycastResult = mc.world.raycast(new RaycastContext(entity.getPos(), entity.getPos().subtract(0, raycastLength, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.WATER, entity));
        if (raycastResult.getType() == HitResult.Type.MISS) return totalHealth;

        float fallHeight = (float) (entity.getPos().y - raycastResult.getBlockPos().getY() + entity.fallDistance - 3d);
        if (jumpBoostInstance != null) fallHeight -= jumpBoostInstance.getAmplifier() + 1;

        return calculateReductions(fallHeight, entity, mc.world.getDamageSources().fall());
    }

    // Utils

    private static float calculateReductions(float damage, LivingEntity entity, DamageSource damageSource) {
        if (damageSource.isScaledWithDifficulty()) {
            switch (mc.world.getDifficulty()) {
                case PEACEFUL -> {
                    return 0;
                }
                case EASY     -> damage = Math.min(damage / 2 + 1, damage);
                case HARD     -> damage *= 1.5f;
            }
        }

        // Armor reduction
        damage = DamageUtil.getDamageLeft(damage, getArmor(entity), (float) EntityAttributeManager.getAttributeValue(entity, EntityAttributes.GENERIC_ARMOR_TOUGHNESS));

        // Resistance reduction
        damage = resistanceReduction(entity, damage);

        // Protection reduction
        damage = protectionReduction(entity, damage, damageSource);

        return Math.max(damage, 0);
    }

    private static float getArmor(LivingEntity entity) {
        return (float) Math.floor(EntityAttributeManager.getAttributeValue(entity, EntityAttributes.GENERIC_ARMOR));
    }

    /**
     * @see LivingEntity#modifyAppliedDamage(DamageSource, float)
     */
    @SuppressWarnings("JavadocReference")
    private static float protectionReduction(Entity player, float damage, DamageSource source) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), source);
        return DamageUtil.getInflictedDamage(damage, protLevel);
    }

    /**
     * @see LivingEntity#modifyAppliedDamage(DamageSource, float)
     */
    @SuppressWarnings("JavadocReference")
    private static float resistanceReduction(LivingEntity player, float damage) {
        StatusEffectInstance resistance = StatusEffectManager.getStatusEffect(player, StatusEffects.RESISTANCE);
        if (resistance != null) {
            int lvl = resistance.getAmplifier() + 1;
            damage *= (1 - (lvl * 0.2f));
        }

        return Math.max(damage, 0);
    }

    /**
     * @see Explosion#getExposure(Vec3d, Entity)
     */
    private static float getExposure(Vec3d source, Box box, @Nullable BlockPos override, @Nullable BlockState overrideState) {
        double xStep = 1 / ((box.maxX - box.minX) * 2 + 1);
        double yStep = 1 / ((box.maxY - box.minY) * 2 + 1);
        double zStep = 1 / ((box.maxZ - box.minZ) * 2 + 1);

        if (xStep > 0 && yStep > 0 && zStep > 0) {
            int misses = 0;
            int hits = 0;

            xStep = xStep * (box.maxX - box.minX);
            yStep = yStep * (box.maxY - box.minY);
            zStep = zStep * (box.maxZ - box.minZ);

            double xOffset = (1 - Math.floor(1 / xStep) * xStep) / 2;
            double zOffset = (1 - Math.floor(1 / zStep) * zStep) / 2;

            double startX = box.minX + xOffset;
            double startY = box.minY;
            double startZ = box.minZ + zOffset;
            double endX = box.maxX + xOffset;
            double endY = box.maxY;
            double endZ = box.maxZ + zOffset;

            Vec3d position = new Vec3d(0, 0, 0);

            for (double x = startX; x <= endX; x += xStep) {
                for (double y = startY; y <= endY; y += yStep) {
                    for (double z = startZ; z <= endZ; z += zStep) {
                        ((IVec3d) position).set(x, y, z);

                        if ((override != null ? raycast(position, source, override, overrideState) : raycast(position, source)) == HitResult.Type.MISS) misses++;

                        hits++;
                    }
                }
            }

            return (float) misses / hits;
        }

        return 0f;
    }

    /**
     * @see BlockView#raycast(RaycastContext)
     */
    private static HitResult.Type raycast(Vec3d start, Vec3d end) {
        return BlockView.raycast(start, end, null, (_null, blockPos) -> {
            BlockState blockState = mc.world.getBlockState(blockPos);
            if (blockState.getBlock().getBlastResistance() < 600) return null;

            BlockHitResult hitResult = blockState.getCollisionShape(mc.world, blockPos).raycast(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
        }, (_null) -> HitResult.Type.MISS);
    }


    private static HitResult.Type raycast(Vec3d start, Vec3d end, BlockPos override, BlockState overrideState) {
        return BlockView.raycast(start, end, null, (_null, blockPos) -> {
            BlockState blockState;
            if (blockPos.equals(override)) blockState = overrideState;
            else {
                blockState = mc.world.getBlockState(blockPos);
                if (blockState.getBlock().getBlastResistance() < 600) return null;
            }

            BlockHitResult hitResult = blockState.getCollisionShape(mc.world, blockPos).raycast(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
        }, (_null) -> HitResult.Type.MISS);
    }
}
