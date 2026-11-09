package sand.barrelrollhandling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import nl.enjarai.doabarrelroll.api.event.RollContext;
import nl.enjarai.doabarrelroll.api.event.RollEvents;
import nl.enjarai.doabarrelroll.api.rotation.RotationInstant;

/**
 * The pulls, as one of Do a Barrel Roll's camera modifiers, after its own sensitivity so they keep their strength. Apart
 * from {@link HandlingClient}, so its classes load only when it is there. It calls these only while it rolls, every frame.
 *
 * <p>A bank's turn is toward the camera's own up, as if the mouse went up, and a level turn toward the low side. The
 * fall is toward the ground: in the frame without the roll the ground is always straight down, so it is a turn down
 * there, which the roll then carries to the camera, upside down included.
 */
public final class BarrelRollPulls {
	/**
	 * After Do a Barrel Roll's own, which are at 1000 and run in rising order: its smoothing would otherwise carry these
	 * turns on after they stop, past level for the speed lift.
	 */
	private static final int PRIORITY = 5000;

	static void register() {
		RollEvents.LATE_CAMERA_MODIFIERS.register(context -> context.useModifier(BarrelRollPulls::safely), PRIORITY, () -> true);
	}

	private static RotationInstant safely(RotationInstant rotation, RollContext context) {
		if (HandlingClient.isBarrelRollBroken()) {
			return rotation;
		}
		try {
			return apply(rotation, context);
		} catch (LinkageError e) {
			HandlingClient.barrelRollBroken(e);
			return rotation;
		}
	}

	private static RotationInstant apply(RotationInstant rotation, RollContext context) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !player.isFallFlying()) {
			return rotation;
		}
		double lookY = player.getLookAngle().y;
		double[] turn = HandlingMath.turn(context.getCurrentRotation().roll(), lookY,
				player.getDeltaMovement().length() * HandlingMath.TICKS_PER_SECOND, context.getRenderDelta());
		RotationInstant pulled = rotation;
		HandlingConfig config = HandlingConfig.current;
		double right = turn[0];
		if (config.rollLimit) {
			// Here the rotation is still the player's own, after Do a Barrel Roll's sensitivity
			double limit = Mth.clamp(config.rollLimitDegrees, HandlingConfig.MIN_ROLL_LIMIT, HandlingConfig.MAX_ROLL_LIMIT);
			double soft = limit * Mth.clamp(config.rollLimitSoftPercent, 0, HandlingConfig.MAX_ROLL_LIMIT_SOFT_PERCENT) / 100.0;
			double[] limited = HandlingMath.limitAndTurn(pulled.roll(), context.getCurrentRotation().roll(), limit, soft,
					Mth.clamp(config.rollLimitTurnPercent, 0, 100) / 100.0);
			pulled = RotationInstant.of(pulled.pitch(), pulled.yaw(), limited[0]);
			right += HandlingMath.easeAtPole(limited[1], 0, lookY)[0];
		}
		if (turn[2] != 0) {
			// Down on the camera's own axes: a higher pitch
			pulled = pulled.add(turn[2], 0, 0);
		}
		if (right != 0 || turn[1] != 0) {
			pulled = pulled.addAbsolute(right, turn[1], Math.toRadians(context.getCurrentRotation().roll()));
		}
		return pulled;
	}

	private BarrelRollPulls() {
	}
}
