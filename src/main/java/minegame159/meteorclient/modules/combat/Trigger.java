/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

public class Trigger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> onlyWhenHoldingAttack = sgGeneral.add(new BoolSetting.Builder()
            .name("only-when-holding-attack")
            .description("Attacks only when you are holding left click.")
            .defaultValue(false)
            .build()
    );

    public Trigger() {
        super(Category.Combat, "trigger", "Automatically swings when you look at entities.");
    }

    private Entity target;

    @EventHandler
    private final Listener<TickEvent.Post> onTick = new Listener<>(event -> {
        target = null;

        if (mc.player.getHealth() <= 0 || mc.player.getAttackCooldownProgress(0.5f) < 1) return;
        if (!(mc.targetedEntity instanceof LivingEntity)) return;
        if (((LivingEntity) mc.targetedEntity).getHealth() <= 0) return;

        target = mc.targetedEntity;

        if (onlyWhenHoldingAttack.get()) {
            if (mc.options.keyAttack.isPressed()) attack(target);
        } else {
            attack(target);
        }
    });

    private void attack(Entity entity) {
        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Override
    public String getInfoString() {
        if (target != null && target instanceof PlayerEntity) return target.getEntityName();
        if (target != null) return target.getType().getName().getString();
        return null;
    }
}
