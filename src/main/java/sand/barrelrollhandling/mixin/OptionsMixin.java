package sand.barrelrollhandling.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sand.barrelrollhandling.HandlingKeys;

@Mixin(Options.class)
abstract class OptionsMixin {
	@Shadow
	@Final
	@Mutable
	public KeyMapping[] keyMappings;

	/** Before the saved keys are read, so this mod's are read with them. */
	@Inject(method = "load", at = @At("HEAD"))
	private void barrelRollHandling$keys(CallbackInfo info) {
		keyMappings = HandlingKeys.with(keyMappings);
	}
}
