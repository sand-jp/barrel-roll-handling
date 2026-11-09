package sand.barrelrollhandling;

import java.util.function.Supplier;
import nl.enjarai.doabarrelroll.config.ModConfig;

/**
 * Do a Barrel Roll's own settings that sit with these pulls, changed from this mod's screen by the player and saved to
 * its own file. Apart, so its classes load only when it is there.
 */
public final class BarrelRollSettings {
	public static final int MAX_RIGHTING_STRENGTH = 200;

	/** Its defaults stand in when its API is not the one this was made for. */
	public static boolean banking() {
		return read(() -> ModConfig.INSTANCE.getEnableBanking(), true);
	}

	public static void setBanking(boolean on) {
		write(() -> ModConfig.INSTANCE.setEnableBanking(on));
	}

	public static boolean righting() {
		return read(() -> ModConfig.INSTANCE.getAutomaticRighting(), false);
	}

	public static void setRighting(boolean on) {
		write(() -> ModConfig.INSTANCE.setAutomaticRighting(on));
	}

	public static double rightingStrength() {
		return read(() -> ModConfig.INSTANCE.getRightingStrength(), 50.0);
	}

	public static void setRightingStrength(double strength) {
		write(() -> ModConfig.INSTANCE.setRightingStrength(strength));
	}

	public static void save() {
		write(() -> ModConfig.INSTANCE.save());
	}

	private static <T> T read(Supplier<T> get, T fallback) {
		if (HandlingClient.isBarrelRollBroken()) {
			return fallback;
		}
		try {
			return get.get();
		} catch (LinkageError e) {
			HandlingClient.barrelRollBroken(e);
			return fallback;
		}
	}

	private static void write(Runnable set) {
		if (HandlingClient.isBarrelRollBroken()) {
			return;
		}
		try {
			set.run();
		} catch (LinkageError e) {
			HandlingClient.barrelRollBroken(e);
		}
	}

	private BarrelRollSettings() {
	}
}
