package sand.barrelrollhandling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Each pull on or off with its own strength, in degrees a second at its fullest. */
public final class HandlingConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(HandlingClient.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("barrel_roll_handling.json");
	public static final int MAX_DEGREES = 90;
	public static final int MIN_STALL_SPEED = 2;
	public static final int MAX_STALL_SPEED = 60;
	public static final double MIN_CURVE = 1;
	public static final double MAX_CURVE = 4;
	public static final int MAX_FULL_SPEED = 100;
	public static final int MIN_ROLL_LIMIT = 10;
	public static final int MAX_ROLL_LIMIT = 180;

	public static HandlingConfig current = new HandlingConfig();

	/** A bank turns the nose, most at 90 degrees: up on the screen, toward the side it leans to, or between. */
	public boolean bankLift = true;
	public int bankLiftDegrees = 30;
	/**
	 * How much of the bank's turn is a level turn toward the low side rather than the camera's own up. On a side both
	 * are the level turn, so this sets how much a shallow bank climbs as it turns.
	 */
	public int bankYawPercent = 100;
	/** A bank lets the nose sink toward the ground: none level, most upside down, as the lift holding it up is lost. */
	public boolean bankSink = true;
	public int bankSinkDegrees = 12;
	/** A climb lets the nose fall toward the ground, most when straight up. */
	public boolean climbDrop = true;
	public int climbDropDegrees = 40;
	/** The power of the climb's share of a right angle: 1 grows evenly, higher stays weak when shallow and rises near the top. */
	public double climbCurve = 2;
	/** How much of the climb drop goes to the camera's own down near straight up rather than the ground. */
	public int climbBellyPercent = 100;
	/** Below the stall speed the nose falls toward the ground, most at a standstill. */
	public boolean stallDrop = true;
	public int stallDropDegrees = 40;
	/** In blocks a second, the speed in any direction. */
	public int stallSpeed = 16;
	/** Fast and nose down, the nose rises toward level: from the stall speed, all at the full speed, in blocks a second. */
	public boolean speedLift = true;
	public int speedLiftDegrees = 25;
	public int speedLiftFullSpeed = 60;
	/** Past this roll, the player's roll that would lean further is held back. */
	public boolean rollLimit = true;
	public int rollLimitDegrees = 80;
	/** Within this share of the limit before it, a roll further grows weaker; 0 stops it hard at the limit. */
	public int rollLimitSoftPercent = 90;
	public static final int MAX_ROLL_LIMIT_SOFT_PERCENT = 100;
	/** How much of the roll the limit holds back becomes a level turn the same way instead. */
	public int rollLimitTurnPercent = 10;
	/**
	 * Do a Barrel Roll's thrust: its push eased in and out, and the back key a brake that stops at a standstill rather
	 * than going backward. Its keys, top speed and acceleration stay its own.
	 */
	public boolean thrust = false;
	/** In seconds, how fast the push builds while held and dies away when let go: the time to about two thirds. 0 is at once. */
	public double thrustRiseSeconds = 2.0;
	public double thrustFallSeconds = 2.0;
	public static final double MAX_THRUST_SECONDS = 3;
	/** The brake's strength, as a share of Do a Barrel Roll's push from rest. */
	public int brakePercent = 50;
	public static final int MAX_BRAKE_PERCENT = 100;
	/** Below this forward speed, in blocks a second, the brake grows weaker, to none at a standstill. */
	public int brakeFadeSpeed = 16;
	public static final int MAX_BRAKE_FADE_SPEED = 60;
	/**
	 * On landing from a glide, the game's walking keys held through it count as let go until the key is let go and
	 * pressed again: none, back only, or all four (left and right too, as Do a Barrel Roll's yaw may be on them). With or
	 * without the thrust, and without Do a Barrel Roll.
	 */
	public LandingRelease landingRelease = LandingRelease.NONE;

	public enum LandingRelease {
		NONE, BACK, ALL;

		public LandingRelease next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	/** In place, so whatever holds the settings sees the defaults. */
	public static void resetToDefaults() {
		HandlingConfig defaults = new HandlingConfig();
		for (Field field : HandlingConfig.class.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers())) {
				try {
					field.set(current, field.get(defaults));
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			}
		}
	}

	public static void load() {
		if (Files.exists(PATH)) {
			try {
				HandlingConfig loaded = GSON.fromJson(Files.readString(PATH), HandlingConfig.class);
				if (loaded != null) {
					current = loaded;
					// An unknown value in the file reads as null
					if (current.landingRelease == null) {
						current.landingRelease = LandingRelease.NONE;
					}
					current.keepInRange();
				}
			} catch (IOException | JsonParseException e) {
				// Kept aside, not written over with the defaults: a slip while editing it by hand loses nothing
				Path kept = PATH.resolveSibling(PATH.getFileName() + ".broken");
				LOGGER.warn("Could not read {}; it is kept as {} and the defaults are used", PATH, kept, e);
				try {
					Files.copy(PATH, kept, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException copyFailed) {
					LOGGER.warn("Could not keep {}", PATH, copyFailed);
				}
				return;
			}
		}
		save();
	}

	/** As the screen allows: a hand-edited file's numbers out of range, or not numbers, come back within it. */
	public void keepInRange() {
		HandlingConfig defaults = new HandlingConfig();
		bankLiftDegrees = clamp(bankLiftDegrees, 0, MAX_DEGREES);
		bankYawPercent = clamp(bankYawPercent, 0, 100);
		bankSinkDegrees = clamp(bankSinkDegrees, 0, MAX_DEGREES);
		climbDropDegrees = clamp(climbDropDegrees, 0, MAX_DEGREES);
		climbCurve = clamp(climbCurve, MIN_CURVE, MAX_CURVE, defaults.climbCurve);
		climbBellyPercent = clamp(climbBellyPercent, 0, 100);
		stallDropDegrees = clamp(stallDropDegrees, 0, MAX_DEGREES);
		stallSpeed = clamp(stallSpeed, MIN_STALL_SPEED, MAX_STALL_SPEED);
		speedLiftDegrees = clamp(speedLiftDegrees, 0, MAX_DEGREES);
		speedLiftFullSpeed = clamp(speedLiftFullSpeed, MIN_STALL_SPEED, MAX_FULL_SPEED);
		rollLimitDegrees = clamp(rollLimitDegrees, MIN_ROLL_LIMIT, MAX_ROLL_LIMIT);
		rollLimitSoftPercent = clamp(rollLimitSoftPercent, 0, MAX_ROLL_LIMIT_SOFT_PERCENT);
		rollLimitTurnPercent = clamp(rollLimitTurnPercent, 0, 100);
		thrustRiseSeconds = clamp(thrustRiseSeconds, 0, MAX_THRUST_SECONDS, defaults.thrustRiseSeconds);
		thrustFallSeconds = clamp(thrustFallSeconds, 0, MAX_THRUST_SECONDS, defaults.thrustFallSeconds);
		brakePercent = clamp(brakePercent, 0, MAX_BRAKE_PERCENT);
		brakeFadeSpeed = clamp(brakeFadeSpeed, 0, MAX_BRAKE_FADE_SPEED);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double clamp(double value, double min, double max, double fallback) {
		return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
	}

	public static void save() {
		try {
			// Beside it first, then moved over it: a crash mid-write leaves the old file whole
			Path written = PATH.resolveSibling(PATH.getFileName() + ".tmp");
			Files.writeString(written, GSON.toJson(current));
			Files.move(written, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			LOGGER.warn("Could not write {}", PATH, e);
		}
	}
}
