/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.Dimension;
import minegame159.meteorclient.utils.world.Dir;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.List;

public class VoidESP extends Module {
    private static final Direction[] SIDES = { Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST };

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Boolean> airOnly = sgGeneral.add(new BoolSetting.Builder()
            .name("air-only")
            .description("Checks bedrock only for air blocks.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-radius")
            .description("Horizontal radius in which to search for holes.")
            .defaultValue(64)
            .min(0)
            .sliderMax(256)
            .build()
    );

    private final Setting<Integer> holeHeight = sgGeneral.add(new IntSetting.Builder()
            .name("hole-height")
            .description("The minimum hole height to be rendered.")
            .defaultValue(1)
            .min(1)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> netherRoof = sgGeneral.add(new BoolSetting.Builder()
            .name("nether-roof")
            .description("Check for holes in nether roof.")
            .defaultValue(true)
            .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("fill-color")
            .description("The color that fills holes in the void.")
            .defaultValue(new SettingColor(225, 25, 25, 50))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color to draw lines of holes to the void.")
            .defaultValue(new SettingColor(225, 25, 255))
            .build()
    );

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private final Pool<Void> voidHolePool = new Pool<>(Void::new);
    private final List<Void> voidHoles = new ArrayList<>();

    public VoidESP() {
        super(Categories.Render, "VoidESP", "Renders holes in bedrock layers that lead to the void.");
    }

    private boolean isBlockWrong(BlockPos blockPos) {
        Chunk chunk = mc.world.getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4, ChunkStatus.FULL, false);
        if (chunk == null) return true;

        Block block = chunk.getBlockState(blockPos).getBlock();

        if (airOnly.get()) return block != Blocks.AIR;
        return block == Blocks.BEDROCK;
    }

    private boolean isHole(BlockPos.Mutable blockPos, boolean nether) {
        for (int i = 0; i < holeHeight.get(); i++) {
            blockPos.setY(nether ? 127 - i : 0);
            if (isBlockWrong(blockPos)) return false;
        }

        return true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        voidHoles.clear();
        if (PlayerUtils.getDimension() == Dimension.End) return;

        int px = mc.player.getBlockPos().getX();
        int pz = mc.player.getBlockPos().getZ();
        int radius = horizontalRadius.get();

        for (int x = px - radius; x <= px + radius; x++) {
            for (int z = pz - radius; z <= pz + radius; z++) {
                blockPos.set(x, 0, z);
                if (isHole(blockPos, false)) voidHoles.add(voidHolePool.get().set(blockPos.set(x, 0, z), false));

                // Check for nether roof
                if (netherRoof.get() && PlayerUtils.getDimension() == Dimension.Nether) {
                    blockPos.set(x, 127, z);
                    if (isHole(blockPos, true)) voidHoles.add(voidHolePool.get().set(blockPos.set(x, 127, z), true));
                }
            }
        }
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        for (Void voidHole : voidHoles) voidHole.render();
    }

    private class Void {
        private int x, y, z;
        private int excludeDir;

        public Void set(BlockPos.Mutable blockPos, boolean nether) {
            x = blockPos.getX();
            y = blockPos.getY();
            z = blockPos.getZ();

            excludeDir = 0;

            for (Direction side : SIDES) {
                blockPos.set(x + side.getOffsetX(), y, z + side.getOffsetZ());
                if (isHole(blockPos, nether)) excludeDir |= Dir.get(side);
            }

            return this;
        }

        public void render() {
            Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x, y, z, 1, sideColor.get(), lineColor.get(), shapeMode.get(), excludeDir);
        }
    }
}
