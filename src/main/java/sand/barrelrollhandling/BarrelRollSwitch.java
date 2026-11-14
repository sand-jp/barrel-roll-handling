package sand.barrelrollhandling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import nl.enjarai.doabarrelroll.api.event.RollEvents;
import nl.enjarai.doabarrelroll.api.rotation.RotationInstant;

/**
 * Yaw and roll swapped while this mod's key is held, as one of Do a Barrel Roll's early camera modifiers: after its own, which set
 * what the mouse and its keys turn, and before its sensitivity and smoothing, which so stay with their own axis. Its
 * settings are not touched. Apart from {@link HandlingClient}, so its classes load only when it is there.
 */
public final class BarrelRollSwitch {
	/** After Do a Barrel Roll's own early ones, which are at 1000 and 2000 and run in rising order. */
	private static final int PRIORITY = 3000;

	static void register() {
		RollEvents.EARLY_CAMERA_MODIFIERS.register(context -> context.useModifier((rotation, roll) -> safely(rotation)), PRIORITY, () -> true);
	}

	private static RotationInstant safely(RotationInstant rotation) {
		if (HandlingClient.isBarrelRollBroken()) {
			return rotation;
		}
		try {
			LocalPlayer player = Minecraft.getInstance().player;
			boolean swapped = player != null && player.isFallFlying() && HandlingKeys.YAW_ROLL_SWITCH.isDown();
			return swapped ? RotationInstant.of(rotation.pitch(), rotation.roll(), rotation.yaw()) : rotation;
		} catch (LinkageError e) {
			HandlingClient.barrelRollBroken(e);
			return rotation;
		}
	}

	private BarrelRollSwitch() {
	}
}
