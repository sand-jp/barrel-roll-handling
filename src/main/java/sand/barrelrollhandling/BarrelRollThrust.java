package sand.barrelrollhandling;

import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.enjarai.doabarrelroll.api.event.ThrustEvents;
import nl.enjarai.doabarrelroll.config.ModConfig;

/**
 * Do a Barrel Roll's thrust, its input reshaped: the push eased in and out, and the back key a brake that stops at a
 * standstill rather than going backward. Its keys, whether the server allows it, its top speed and its acceleration
 * all stay its own, and the input given back is never larger than one, so it never pushes harder than it would. Apart
 * from {@link HandlingClient}, so its classes load only when it is there. It asks for the input once a tick while gliding,
 * and only when its thrust is on and allowed.
 */
public final class BarrelRollThrust {
	/** After the others, which run in rising order: Controlify's analog input is at 0, the default. */
	private static final int PRIORITY = 5000;
	private static double throttle;
	/** Weakly, so a player left behind in an old world is not kept. */
	private static WeakReference<LocalPlayer> lastPlayer = new WeakReference<>(null);
	private static int lastTick;

	static void register() {
		ThrustEvents.MODIFY_THRUST_INPUT.register(BarrelRollThrust::modify, PRIORITY);
	}

	/** Public for the tests, which give it the input as Do a Barrel Roll would. */
	public static double modify(double input) {
		if (HandlingClient.isBarrelRollBroken()) {
			return input;
		}
		try {
			return reshape(input);
		} catch (LinkageError e) {
			HandlingClient.barrelRollBroken(e);
			return input;
		}
	}

	private static double reshape(double input) {
		LocalPlayer player = Minecraft.getInstance().player;
		HandlingConfig config = HandlingConfig.current;
		if (!config.thrust || player == null) {
			throttle = 0;
			lastPlayer.clear();
			return input;
		}
		int tick = player.tickCount;
		boolean samePlayer = player == lastPlayer.get();
		if (!samePlayer || tick != lastTick) {
			// A tick missed, landing or the thrust switched off in between, starts again from nothing
			if (!samePlayer || tick != lastTick + 1) {
				throttle = 0;
			}
			// Against a wall or a ceiling the push dies away as if let go, and builds again once free
			boolean blocked = player.horizontalCollision && !player.minorHorizontalCollision || player.verticalCollision;
			throttle = HandlingMath.throttle(throttle, blocked ? 0 : input, config.thrustRiseSeconds, config.thrustFallSeconds);
			lastPlayer = new WeakReference<>(player);
			lastTick = tick;
		}
		Vec3 velocity = player.getDeltaMovement();
		double forward = velocity.dot(player.getLookAngle()) * HandlingMath.TICKS_PER_SECOND;
		return HandlingMath.brake(throttle, forward, velocity.length() * HandlingMath.TICKS_PER_SECOND,
				ModConfig.INSTANCE.getMaxThrust() * HandlingMath.TICKS_PER_SECOND,
				Mth.clamp(config.brakePercent, 0, HandlingConfig.MAX_BRAKE_PERCENT) / 100.0, config.brakeFadeSpeed,
				ModConfig.INSTANCE.getThrustAcceleration());
	}

	/** For the tests: as if just landed. */
	public static void reset() {
		throttle = 0;
		lastPlayer.clear();
	}

	private BarrelRollThrust() {
	}
}
