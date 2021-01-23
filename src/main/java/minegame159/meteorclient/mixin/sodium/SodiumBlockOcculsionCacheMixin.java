/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin.sodium;

import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.render.DrawSideEvent;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockOcclusionCache.class, remap = false)
public class SodiumBlockOcculsionCacheMixin {
    @Inject(at = @At("HEAD"), method = "shouldDrawSide", cancellable = true)
    private void shouldDrawSide(BlockState selfState, BlockView view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> info) {
        DrawSideEvent event = MeteorClient.postEvent(DrawSideEvent.get(selfState));
        if (event.isSet()) info.setReturnValue(event.getDraw());
    }
}
