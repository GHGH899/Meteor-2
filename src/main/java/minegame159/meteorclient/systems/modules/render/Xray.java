/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderBlockEntityEvent;
import minegame159.meteorclient.events.world.AmbientOcclusionEvent;
import minegame159.meteorclient.events.world.ChunkOcclusionEvent;
import minegame159.meteorclient.settings.BlockListSetting;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.util.Arrays;
import java.util.List;

public class Xray extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Blocks.")
            .defaultValue(Arrays.asList(Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.LAPIS_ORE,
                    Blocks.REDSTONE_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE,
                    Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS))
            .onChanged(blocks1 -> {
                if (isActive()) mc.worldRenderer.reload();
            })
            .build()
    );

    private final Setting<Boolean> antixraybypass = sgGeneral.add(new BoolSetting.Builder()
            .name("Anti-Xray Bypass")
            .description("Only shows blocks that have at least one face exposed to air, This will only show the ores that are in caves!")
            .defaultValue(false)
            .onChanged(blocks2 -> {
                if (isActive()) mc.worldRenderer.reload();
            })
            .build()
    );

    public Xray() {
        super(Categories.Render, "xray", "Only renders specified blocks. Good for mining.");
    }

    @Override
    public void onActivate() {
        Fullbright.enable();

        mc.worldRenderer.reload();
    }

    @Override
    public void onDeactivate() {
        Fullbright.disable();

        mc.worldRenderer.reload();
    }

    @EventHandler
    private void onRenderBlockEntity(RenderBlockEntityEvent event) {
        if (isBlocked(event.blockEntity.getCachedState().getBlock(), event.blockEntity.getPos())) event.cancel();
    }

    @EventHandler
    private void onChunkOcclusion(ChunkOcclusionEvent event) {
        event.cancel();
    }

    @EventHandler
    private void onAmbientOcclusion(AmbientOcclusionEvent event) {
        event.lightLevel = 1;
    }

    public boolean modifyDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing, boolean returns) {
        if (returns) {
            if (isBlocked(state.getBlock(), pos)) return false;
        }
        else {
            if (!isBlocked(state.getBlock(), pos)) {
                BlockPos adjPos = pos.offset(facing);
                BlockState adjState = view.getBlockState(adjPos);

                return adjState.getCullingFace(view, adjPos, facing.getOpposite()) != VoxelShapes.fullCube() || adjState.getBlock() != state.getBlock();
            }
        }

        return returns;
    }

    // Checks if the given BlockPos has at least one face exposed to air
    private boolean isExposedToAir(BlockPos pos) {
        BlockPos.Mutable posStart = new BlockPos.Mutable(pos.getX(), pos.getY(), pos.getZ());
        return mc.world.getBlockState(posStart.add(1,0,0)).getBlock().is(Blocks.AIR) ||
                mc.world.getBlockState(posStart.add(-1,0,0)).getBlock().is(Blocks.AIR) ||
                mc.world.getBlockState(posStart.add(0,0,1)).getBlock().is(Blocks.AIR) ||
                mc.world.getBlockState(posStart.add(0,1,0)).getBlock().is(Blocks.AIR) ||
                mc.world.getBlockState(posStart.add(0,-1,0)).getBlock().is(Blocks.AIR) ||
                mc.world.getBlockState(posStart.add(0,0,-1)).getBlock().is(Blocks.AIR);

    }

    public boolean isBlocked(Block block, BlockPos pos) {
        if (antixraybypass.get()) {
            if (isExposedToAir(pos)) {
                return !blocks.get().contains(block);
            } else {
                return true;
            }
        } else { return !blocks.get().contains(block); }
    }
}
