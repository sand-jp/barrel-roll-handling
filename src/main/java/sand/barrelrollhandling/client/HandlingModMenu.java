package sand.barrelrollhandling.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class HandlingModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		// YetAnotherConfigLib's screen when it is there, else the game's own kind
		if (FabricLoader.getInstance().isModLoaded(HandlingYaclScreen.YACL)) {
			return HandlingYaclScreen::create;
		}
		return HandlingConfigScreen::new;
	}
}
