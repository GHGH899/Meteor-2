package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ClientBrandRetrieverEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import net.minecraft.client.ClientBrandRetriever;

@Mixin(ClientBrandRetriever.class)
public class ClientBrandRetrieverMixin {
	@Inject(at = @At("HEAD"), method = "getClientModName", cancellable = true, remap = false)
	private static void getConfiguredClientBrand(CallbackInfoReturnable<String> info) {
		System.out.println("info: " + info.getReturnValue());
		ClientBrandRetrieverEvent event = MeteorClient.EVENT_BUS.post(ClientBrandRetrieverEvent.get(info));

		System.out.println(event.info.getReturnValue());
		
		if(event.isCancelled()) {
			return;
		}
		
		info.setReturnValue(event.info.getReturnValue());
		
	}
}
