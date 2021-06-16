/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.speed;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.speed.modes.Strafe;
import meteordevelopment.meteorclient.systems.modules.movement.speed.modes.Tunnel;
import meteordevelopment.meteorclient.systems.modules.movement.speed.modes.Vanilla;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;

public class Speed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<SpeedModes> speedMode = sgGeneral.add(new EnumSetting.Builder<SpeedModes>()
            .name("mode")
            .description("The method of applying speed.")
            .defaultValue(SpeedModes.Vanilla)
            .onModuleActivated(speedModesSetting -> onSpeedModeChanged(speedModesSetting.get()))
            .onChanged(this::onSpeedModeChanged)
            .build()
    );

    public final Setting<Double> vanillaSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("vanilla-speed")
            .description("The speed in blocks per second.")
            .defaultValue(5.6)
            .min(0)
            .sliderMax(20)
            .visible(() -> speedMode.get() == SpeedModes.Vanilla)
            .build()
    );

    public final Setting<Double> ncpSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("strafe-speed")
            .description("The speed.")
            .visible(() -> speedMode.get() == SpeedModes.Strafe)
            .defaultValue(1.6)
            .min(0)
            .sliderMax(3)
            .build()
    );

    public final Setting<Double> tunnelSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("tunnel-speed")
        .description("The speed.")
        .visible(() -> speedMode.get() == SpeedModes.Tunnel)
        .defaultValue(0.15)
        .min(0.01)
        .sliderMax(1)
        .build()
    );

    public final Setting<Boolean> ncpSpeedLimit = sgGeneral.add(new BoolSetting.Builder()
            .name("speed-limit")
            .description("Limits your speed on servers with very strict anticheats.")
            .visible(() -> speedMode.get() == SpeedModes.Strafe)
            .defaultValue(false)
            .build()
    );

    public final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder()
            .name("timer")
            .description("Timer override.")
            .defaultValue(1)
            .min(0.01)
            .sliderMin(0.01)
            .sliderMax(10)
            .build()
    );

    public final Setting<Boolean> inLiquids = sgGeneral.add(new BoolSetting.Builder()
            .name("in-liquids")
            .description("Uses speed when in lava or water.")
            .visible(() -> speedMode.get() != SpeedModes.Tunnel)
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> whenSneaking = sgGeneral.add(new BoolSetting.Builder()
            .name("when-sneaking")
            .description("Uses speed when sneaking.")
            .visible(() -> speedMode.get() != SpeedModes.Tunnel)
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> swapToTunnel = sgGeneral.add(new BoolSetting.Builder()
            .name("swap-to-tunnel")
            .description("Automatically switches your speed mode to 'tunnel' if you are in a 1x2 tunnel.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> vanillaOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Uses speed only when standing on a block.")
            .visible(() -> speedMode.get() == SpeedModes.Vanilla)
            .defaultValue(false)
            .build()
    );

    private SpeedMode currentMode;
    SpeedModes preSpeedMode;

    public Speed() {
        super(Categories.Movement, "speed", "Modifies your movement speed when moving on the ground.");

        onSpeedModeChanged(speedMode.get());
    }

    @Override
    public void onActivate() {
        preSpeedMode = speedMode.get();
        currentMode.onActivate();
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        currentMode.onDeactivate();
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (event.type != MovementType.SELF || mc.player.isFallFlying() || mc.player.isClimbing() || mc.player.getVehicle() != null) return;
        if (!whenSneaking.get() && mc.player.isSneaking()) return;
        if (vanillaOnGround.get() && !mc.player.isOnGround() && speedMode.get() == SpeedModes.Vanilla) return;
        if (!inLiquids.get() && (mc.player.isTouchingWater() || mc.player.isInLava())) return;

        Modules.get().get(Timer.class).setOverride(PlayerUtils.isMoving() ? timer.get() : Timer.OFF);

        currentMode.onMove(event);
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player.isFallFlying() || mc.player.isClimbing() || mc.player.getVehicle() != null) return;
        if (!whenSneaking.get() && mc.player.isSneaking()) return;
        if (vanillaOnGround.get() && !mc.player.isOnGround() && speedMode.get() == SpeedModes.Vanilla) return;
        if (!inLiquids.get() && (mc.player.isTouchingWater() || mc.player.isInLava())) return;


        Block blockAbove = mc.world.getBlockState(new BlockPos(mc.player.getX(), mc.player.getY() + 2, mc.player.getZ())).getBlock();
        Block blockBelow = mc.world.getBlockState(new BlockPos(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ())).getBlock();

        if (swapToTunnel.get() && (isAboveValid(blockAbove) && isBelowValid(blockBelow))) {
            speedMode.set(SpeedModes.Tunnel);
        } else if (swapToTunnel.get() && !isAboveValid(blockAbove) || !isBelowValid(blockBelow)) {
            speedMode.set(preSpeedMode);
        }

        currentMode.onTick();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) currentMode.onRubberband();
    }

    private void onSpeedModeChanged(SpeedModes mode) {
        switch (mode) {
            case Vanilla:   currentMode = new Vanilla(); break;
            case Strafe:    currentMode = new Strafe(); break;
            case Tunnel:    currentMode = new Tunnel(); break;
        }
        if (mode != SpeedModes.Tunnel) preSpeedMode = mode;
    }

    @Override
    public String getInfoString() {
        return currentMode.getHudString();
    }

    public static boolean isAboveValid(Block block) {
        return block != Blocks.AIR && block != Blocks.NETHER_PORTAL && block != Blocks.END_PORTAL && !(block instanceof FluidBlock);
    }

    public static boolean isBelowValid(Block block) {
        return block != Blocks.AIR && block != Blocks.ICE && block != Blocks.BLUE_ICE && block != Blocks.FROSTED_ICE && block != Blocks.PACKED_ICE;
    }
}
