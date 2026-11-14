package sand.barrelrollhandling;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * This mod's keys, put among the game's own as its options load, so they show in the controls and are saved with them.
 * Without Fabric API, which this mod does not need.
 */
public final class HandlingKeys {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(HandlingClient.MOD_ID, "keys"));
	/** The left mouse button at first: it also stays the game's attack. */
	public static final KeyMapping YAW_ROLL_SWITCH = new KeyMapping("key.barrel_roll_handling.yaw_roll_switch", InputConstants.Type.MOUSE, 0, CATEGORY);
	private static final List<KeyMapping> ADDED = new ArrayList<>();

	/** Before the game's options are made: only what is added shows in the controls. */
	static void add(KeyMapping key) {
		ADDED.add(key);
	}

	/** The game's keys with these after them, each once. */
	public static KeyMapping[] with(KeyMapping[] keys) {
		List<KeyMapping> all = new ArrayList<>(Arrays.asList(keys));
		for (KeyMapping key : ADDED) {
			if (!all.contains(key)) {
				all.add(key);
			}
		}
		return all.toArray(KeyMapping[]::new);
	}

	private HandlingKeys() {
	}
}
