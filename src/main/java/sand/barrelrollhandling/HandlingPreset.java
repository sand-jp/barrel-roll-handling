package sand.barrelrollhandling;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * The feel of the pulls and the roll limit in five steps: Gentle holds the nose and the roll steadiest, Very sharp is
 * closest to Do a Barrel Roll as it is, with every pull still there but weaker and no roll limit. Normal is the
 * defaults. The thrust, the landing and Do a Barrel Roll's own settings are left as they are.
 */
public enum HandlingPreset {
	GENTLE(45, 5, 55, 1.5, 50, 16, 35, 60, true, 60),
	MILD(38, 9, 48, 1.5, 45, 16, 30, 60, true, 70),
	NORMAL(30, 12, 40, 2.0, 40, 16, 25, 60, true, 80),
	SHARP(22, 15, 28, 2.0, 30, 15, 15, 70, true, 90),
	VERY_SHARP(16, 15, 12, 2.0, 25, 14, 6, 80, false, 180);

	private static final Gson GSON = new Gson();
	private final int bankLift;
	private final int bankSink;
	private final int climbDrop;
	private final double climbCurve;
	private final int stallDrop;
	private final int stallSpeed;
	private final int speedLift;
	private final int speedLiftFullSpeed;
	private final boolean rollLimit;
	private final int rollLimitDegrees;

	HandlingPreset(int bankLift, int bankSink, int climbDrop, double climbCurve, int stallDrop, int stallSpeed, int speedLift,
			int speedLiftFullSpeed, boolean rollLimit, int rollLimitDegrees) {
		this.bankLift = bankLift;
		this.bankSink = bankSink;
		this.climbDrop = climbDrop;
		this.climbCurve = climbCurve;
		this.stallDrop = stallDrop;
		this.stallSpeed = stallSpeed;
		this.speedLift = speedLift;
		this.speedLiftFullSpeed = speedLiftFullSpeed;
		this.rollLimit = rollLimit;
		this.rollLimitDegrees = rollLimitDegrees;
	}

	public void apply(HandlingConfig config) {
		HandlingConfig defaults = new HandlingConfig();
		config.bankLift = true;
		config.bankLiftDegrees = bankLift;
		config.bankYawPercent = defaults.bankYawPercent;
		config.bankSink = true;
		config.bankSinkDegrees = bankSink;
		config.climbDrop = true;
		config.climbDropDegrees = climbDrop;
		config.climbCurve = climbCurve;
		config.climbBellyPercent = defaults.climbBellyPercent;
		config.stallDrop = true;
		config.stallDropDegrees = stallDrop;
		config.stallSpeed = stallSpeed;
		config.speedLift = true;
		config.speedLiftDegrees = speedLift;
		config.speedLiftFullSpeed = speedLiftFullSpeed;
		config.rollLimit = rollLimit;
		// Off, its angle and the rest are greyed and left as they are: not part of the preset
		if (rollLimit) {
			config.rollLimitDegrees = rollLimitDegrees;
			config.rollLimitSoftPercent = defaults.rollLimitSoftPercent;
			config.rollLimitTurnPercent = defaults.rollLimitTurnPercent;
		}
	}

	/** The one these settings are, or null when changed by hand away from all of them. */
	public static HandlingPreset of(HandlingConfig config) {
		JsonObject now = GSON.toJsonTree(config).getAsJsonObject();
		for (HandlingPreset preset : values()) {
			HandlingConfig copy = GSON.fromJson(now, HandlingConfig.class);
			preset.apply(copy);
			if (GSON.toJsonTree(copy).equals(now)) {
				return preset;
			}
		}
		return null;
	}

	public HandlingPreset next() {
		return values()[(ordinal() + 1) % values().length];
	}
}
