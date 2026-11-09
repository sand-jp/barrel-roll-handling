package sand.barrelrollhandling;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Without Do a Barrel Roll only the landing works: the pulls turn its camera, and only while it rolls, and the thrust is its own. */
public final class HandlingClient implements ClientModInitializer {
	public static final String MOD_ID = "barrel_roll_handling";
	public static final String BARREL_ROLL = "do_a_barrel_roll";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static boolean barrelRollBroken;

	/**
	 * Do a Barrel Roll's API is not the one this was made for (a later version changed it): what uses it stays off
	 * from now on, said once in the log, rather than crashing the game in its tick or frame.
	 */
	public static void barrelRollBroken(LinkageError e) {
		if (!barrelRollBroken) {
			barrelRollBroken = true;
			LOGGER.warn("Do a Barrel Roll's API is not the one this was made for, so what uses it is off from now on", e);
		}
	}

	public static boolean isBarrelRollBroken() {
		return barrelRollBroken;
	}

	@Override
	public void onInitializeClient() {
		HandlingConfig.load();
		if (!FabricLoader.getInstance().isModLoaded(BARREL_ROLL)) {
			LOGGER.info("Do a Barrel Roll is not here, so the flight pulls stay off");
			return;
		}
		try {
			BarrelRollPulls.register();
		} catch (LinkageError e) {
			LOGGER.warn("Do a Barrel Roll's API is not the one this was made for, so the flight pulls stay off", e);
		}
		try {
			BarrelRollThrust.register();
		} catch (LinkageError e) {
			LOGGER.warn("Do a Barrel Roll's API is not the one this was made for, so its thrust is left as it is", e);
		}
	}
}
