package sand.barrelrollhandling.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sand.barrelrollhandling.LandingRelease;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
	/** Once the walking input is read, keyboard and controller alike, before the player moves. */
	@Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = At.Shift.AFTER))
	private void barrelRollHandling$landing(CallbackInfo info) {
		LandingRelease.afterInput((LocalPlayer) (Object) this);
	}
}
