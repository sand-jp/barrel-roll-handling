package sand.barrelrollhandling.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.screens.Screen;

/**
 * What the game's screens do differently from one version to the next, for Minecraft 26.1. The build takes this folder
 * or the other by the game it builds for, and all else is shared.
 */
public final class GameScreens {
	public static void open(Minecraft minecraft, Screen screen) {
		minecraft.setScreen(screen);
	}

	public static Screen current(Minecraft minecraft) {
		return minecraft.screen;
	}

	static TabNavigationBar tabBar(TabManager manager, int width, Tab[] tabs) {
		return TabNavigationBar.builder(manager, width).addTabs(tabs).build();
	}

	static void arrange(TabNavigationBar bar, int width) {
		bar.updateWidth(width);
		bar.arrangeElements();
	}

	private GameScreens() {
	}
}
