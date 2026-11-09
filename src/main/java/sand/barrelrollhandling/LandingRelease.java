package sand.barrelrollhandling;

import java.lang.ref.WeakReference;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import sand.barrelrollhandling.mixin.ClientInputAccessor;

/**
 * On landing from a glide, walking keys held through it count as let go until they are let go, so what was held to
 * steer or brake does not walk on: back only, or all four (left and right too, which Do a Barrel Roll's yaw may share). Read from the player's walking input once it is made, so the keyboard and
 * a controller (Controlify joins both into it) count alike. Needs nothing of Do a Barrel Roll.
 */
public final class LandingRelease {
	/** How far a stick must lean to count as held. */
	private static final float STICK = 0.1F;
	/** Weakly, so a player left behind in an old world is not kept. */
	private static WeakReference<LocalPlayer> lastPlayer = new WeakReference<>(null);
	private static boolean wasFlying;
	private static boolean holdForward;
	private static boolean holdBack;
	private static boolean holdLeft;
	private static boolean holdRight;

	/** Each tick, right after the walking input is read and before the player moves. */
	public static void afterInput(LocalPlayer player) {
		if (player != lastPlayer.get()) {
			// Another world, or back from death: nothing carries over
			reset();
			lastPlayer = new WeakReference<>(player);
		}
		ClientInput input = player.input;
		Input keys = input.keyPresses;
		Vec2 move = input.getMoveVector();
		boolean forward = keys.forward() || move.y > STICK;
		boolean back = keys.backward() || move.y < -STICK;
		// Left is positive, as the game reads it
		boolean left = keys.left() || move.x > STICK;
		boolean right = keys.right() || move.x < -STICK;
		boolean flying = player.isFallFlying();
		HandlingConfig.LandingRelease mode = HandlingConfig.current.landingRelease;
		if (wasFlying && !flying && mode != HandlingConfig.LandingRelease.NONE) {
			boolean all = mode == HandlingConfig.LandingRelease.ALL;
			holdBack = back;
			holdForward = forward && all;
			holdLeft = left && all;
			holdRight = right && all;
		}
		wasFlying = flying;
		// Let go, or flying again, it ends
		holdForward &= forward && !flying;
		holdBack &= back && !flying;
		holdLeft &= left && !flying;
		holdRight &= right && !flying;
		if (holdForward || holdBack || holdLeft || holdRight) {
			input.keyPresses = new Input(keys.forward() && !holdForward, keys.backward() && !holdBack, keys.left() && !holdLeft,
					keys.right() && !holdRight, keys.jump(), keys.shift(), keys.sprint());
			boolean dropY = holdForward && move.y > 0 || holdBack && move.y < 0;
			boolean dropX = holdLeft && move.x > 0 || holdRight && move.x < 0;
			((ClientInputAccessor) input).barrelRollHandling$setMoveVector(new Vec2(dropX ? 0 : move.x, dropY ? 0 : move.y));
		}
	}

	/** For the tests: as if never flown. */
	public static void reset() {
		lastPlayer.clear();
		wasFlying = false;
		holdForward = false;
		holdBack = false;
		holdLeft = false;
		holdRight = false;
	}

	private LandingRelease() {
	}
}
