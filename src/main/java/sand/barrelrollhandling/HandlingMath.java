package sand.barrelrollhandling;

import net.minecraft.util.Mth;

/**
 * How hard each pull is, from 0 to 1, and the turn it makes in a frame. Do a Barrel Roll counts a turn in the mouse's
 * units, of which one is 0.15 degrees, as the game's own mouse does.
 */
public final class HandlingMath {
	public static final double DEGREES_PER_UNIT = 0.15;
	/** A frame longer than this, after a pause, turns no further than this would. */
	public static final double MAX_FRAME_SECONDS = 0.1;
	public static final double TICKS_PER_SECOND = 20;
	/**
	 * Within this many degrees of straight down or straight up, a turn toward it, or around it, grows weaker as it nears.
	 * At the pole the frame without the roll turns round on itself, so a turn that keeps its strength there overshoots,
	 * comes back, and shakes.
	 */
	public static final double POLE_DEGREES = 20;

	/** Most when leaning on a side, none when level or upside down. */
	public static double bank(double rollDegrees) {
		return Math.abs(Math.sin(Math.toRadians(rollDegrees)));
	}

	/**
	 * Most when straight up, none when level or going down: the climb's share of a right angle to the power of the
	 * curve, so a higher curve leaves a shallow climb nearly alone and rises sharply near the top. The look's height is
	 * the sine of the climb.
	 */
	public static double climb(double lookY, double curve) {
		double angle = Math.toDegrees(Math.asin(Mth.clamp(lookY, 0, 1)));
		return Math.pow(angle / 90, Math.max(1, curve));
	}

	/** Most at a standstill, none from the stall speed up. Both in blocks a second. */
	public static double stall(double speed, double stallSpeed) {
		return stallSpeed <= 0 ? 0 : Mth.clamp(1 - speed / stallSpeed, 0, 1);
	}

	/**
	 * Rising evenly from none at the stall speed to all at the full speed, times how far the nose points down (the sine
	 * of the dive). None level, so it brings the nose to level and never past it. A full speed not above the stall
	 * speed counts as one above it, so the rise stays even and starts where the stall drop ends.
	 */
	public static double speedLift(double speed, double lookY, double stallSpeed, double fullSpeed) {
		double full = Math.max(fullSpeed, stallSpeed + 1);
		return Mth.clamp((speed - stallSpeed) / (full - stallSpeed), 0, 1) * Mth.clamp(-lookY, 0, 1);
	}

	/** None level, one on a side, two upside down: how much of the lift that held the nose up a bank takes away. */
	public static double sink(double rollDegrees) {
		return 1 - Math.cos(Math.toRadians(rollDegrees));
	}

	/**
	 * The bank turn's way in the frame without the roll, as right and up, of length one: the camera's own up (right and
	 * up by the roll) and a level turn to the low side, mixed by an angle, the level turn's share of a right angle. On a
	 * side both are the level turn. Level, none.
	 */
	public static double[] bankWay(double rollDegrees, double levelShare) {
		double roll = Math.toRadians(rollDegrees);
		double side = Math.signum(Math.sin(roll));
		if (side == 0) {
			return new double[] {0, 0};
		}
		double share = Math.toRadians(90 * Mth.clamp(levelShare, 0, 1));
		double right = Math.cos(share) * Math.sin(roll) + Math.sin(share) * side;
		double up = Math.cos(share) * Math.cos(roll);
		double length = Math.hypot(right, up);
		return length == 0 ? new double[] {0, 0} : new double[] {right / length, up / length};
	}

	/** The mouse's units to turn in a frame at so many degrees a second. */
	public static double units(double degreesPerSecond, double seconds) {
		return degreesPerSecond / DEGREES_PER_UNIT * Mth.clamp(seconds, 0, MAX_FRAME_SECONDS);
	}

	/**
	 * The roll the player gives, held back near the limit: within the soft range before it, what would lean further is
	 * weakened as the room left shrinks, so the roll slows into the limit rather than striking it; it never goes past,
	 * and nothing leans further once there. Rolling back up is left alone. In the mouse's units, the roll now in degrees.
	 * A soft range of none stops it hard at the limit.
	 */
	public static double limitRoll(double rollInput, double roll, double limitDegrees, double softDegrees) {
		if (rollInput == 0 || Math.signum(rollInput) != Math.signum(roll) && roll != 0) {
			return rollInput;
		}
		double left = Math.max(0, limitDegrees - Math.abs(roll));
		double weaken = softDegrees <= 0 ? 1 : Math.min(1, left / softDegrees);
		return Math.signum(rollInput) * Math.min(Math.abs(rollInput) * weaken, left / DEGREES_PER_UNIT);
	}

	/** From 1 outside the pole's range to 0 at straight down or up. */
	public static double nearPole(double lookY) {
		double fromPole = 90 - Math.toDegrees(Math.asin(Mth.clamp(Math.abs(lookY), 0, 1)));
		return Mth.clamp(fromPole / POLE_DEGREES, 0, 1);
	}

	/**
	 * A turn in the frame without the roll, as right and up, eased near the pole the look is near: a level turn there
	 * would spin the view round the look, and a turn toward the pole would pass it. A turn away from it keeps its
	 * strength.
	 */
	public static double[] easeAtPole(double right, double up, double lookY) {
		double near = nearPole(lookY);
		boolean towardPole = up < 0 && lookY < 0 || up > 0 && lookY > 0;
		return new double[] {right * near, towardPole ? up * near : up};
	}

	/**
	 * The player's roll near the limit, and the level turn made from what it holds back: the roll kept, and the turn to
	 * the right in the mouse's units, the same way the roll went.
	 */
	public static double[] limitAndTurn(double rollInput, double roll, double limitDegrees, double softDegrees, double turnShare) {
		double kept = limitRoll(rollInput, roll, limitDegrees, softDegrees);
		return new double[] {kept, (rollInput - kept) * Mth.clamp(turnShare, 0, 1)};
	}

	/**
	 * How much of the climb drop goes to the camera's own down, the belly, rather than the ground: the share, more the
	 * steeper the climb. Near straight up the way to the ground can be any way on the screen and reads as a yaw, while
	 * the belly is always down on it; on a shallow climb the two are nearly one. Upside down the belly is the sky, so a
	 * shallow climb upside down keeps the ground, and only near straight up, where upside down hardly means anything,
	 * does the belly come back.
	 */
	public static double bellyWeight(double roll, double lookY, double share) {
		double steep = Math.toDegrees(Math.asin(Mth.clamp(lookY, 0, 1))) / 90;
		double upright = Math.cos(Math.toRadians(roll)) >= 0 ? 1 : steep;
		return Mth.clamp(share, 0, 1) * steep * upright;
	}

	/**
	 * The turn for a frame, in the mouse's units, in the frame without the roll: to the right, and up (the ground is
	 * below); then the pitch on the camera's own axes, down counting up. A positive roll leans to the right. The frame
	 * without the roll is eased at the poles; the camera's own axes need no easing.
	 */
	public static double[] turn(double roll, double lookY, double speed, double seconds) {
		HandlingConfig config = HandlingConfig.current;
		double right = 0;
		double up = 0;
		double belly = 0;
		if (config.bankLift) {
			double amount = degrees(config.bankLiftDegrees) * HandlingMath.bank(roll);
			double[] way = bankWay(roll, Mth.clamp(config.bankYawPercent, 0, 100) / 100.0);
			right += amount * way[0];
			up += amount * way[1];
		}
		if (config.bankSink) {
			up -= degrees(config.bankSinkDegrees) * HandlingMath.sink(roll);
		}
		if (config.climbDrop) {
			double drop = degrees(config.climbDropDegrees) * HandlingMath.climb(lookY, Mth.clamp(config.climbCurve, HandlingConfig.MIN_CURVE, HandlingConfig.MAX_CURVE));
			double weight = bellyWeight(roll, lookY, Mth.clamp(config.climbBellyPercent, 0, 100) / 100.0);
			belly += drop * weight;
			up -= drop * (1 - weight);
		}
		if (config.stallDrop) {
			up -= degrees(config.stallDropDegrees) * HandlingMath.stall(speed, config.stallSpeed);
		}
		if (config.speedLift) {
			up += degrees(config.speedLiftDegrees) * HandlingMath.speedLift(speed, lookY, config.stallSpeed, config.speedLiftFullSpeed);
		}
		double[] eased = easeAtPole(HandlingMath.units(right, seconds), HandlingMath.units(up, seconds), lookY);
		return new double[] {eased[0], eased[1], HandlingMath.units(belly, seconds)};
	}

	/**
	 * One tick of the throttle following the input, held within one: building toward a larger push with the rise's time
	 * constant, dying away toward a smaller one or the other way with the fall's. A time of none follows at once. Close
	 * enough, it takes the input itself, so nothing is left once let go.
	 */
	public static double throttle(double now, double input, double riseSeconds, double fallSeconds) {
		double target = Mth.clamp(input, -1, 1);
		boolean building = now * target >= 0 && Math.abs(target) > Math.abs(now);
		double seconds = Math.max(0, building ? riseSeconds : fallSeconds);
		double next = seconds == 0 ? target : now + (target - now) * (1 - Math.exp(-1 / TICKS_PER_SECOND / seconds));
		return Math.abs(next - target) < 1e-3 ? target : next;
	}

	/**
	 * The input for a throttle back, as a brake: Do a Barrel Roll pushes back (top + speed) / top times harder while
	 * moving forward, so it is divided out, and the brake slows by its share of the push from rest whatever the speed.
	 * Weaker below the fade speed, none once not moving forward, and never more in a tick than the forward speed left, so
	 * it never goes backward even with a large acceleration. A throttle forward is left alone. Speeds in blocks a second;
	 * forward is along the look, as the push is. The acceleration is Do a Barrel Roll's, in blocks a tick, each tick.
	 */
	public static double brake(double throttle, double forward, double speed, double top, double share, double fadeSpeed, double acceleration) {
		if (throttle >= 0) {
			return throttle;
		}
		if (forward <= 0 || top <= 0) {
			return 0;
		}
		double fade = fadeSpeed <= 0 ? 1 : Math.min(1, forward / fadeSpeed);
		double slowing = -throttle * Mth.clamp(share, 0, 1) * fade;
		if (acceleration > 0) {
			// The slowing in a tick is the acceleration times this
			slowing = Math.min(slowing, forward / TICKS_PER_SECOND / acceleration);
		}
		return Math.max(-1, -slowing * top / (top + speed));
	}

	private static double degrees(int value) {
		return Math.max(0, Math.min(value, HandlingConfig.MAX_DEGREES));
	}

	private HandlingMath() {
	}
}
