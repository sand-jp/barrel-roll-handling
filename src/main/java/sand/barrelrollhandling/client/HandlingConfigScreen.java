package sand.barrelrollhandling.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import sand.barrelrollhandling.BarrelRollSettings;
import sand.barrelrollhandling.HandlingClient;
import sand.barrelrollhandling.HandlingConfig;
import sand.barrelrollhandling.HandlingPreset;

/**
 * The settings in tabs, two to a row as on the game's own option screens: each pull under its heading, its switch on
 * the left, and what hangs on the switch greyed while it is off. A tab is its own screen, built again on a switch.
 */
public class HandlingConfigScreen extends OptionsSubScreen {
	private static final int ROW_WIDTH = 150;
	private static final int ROW_HEIGHT = 20;
	private static final int FOOTER_GAP = 8;
	private static final String KEY = "barrel_roll_handling.config.";
	private static final String[] TABS = {"preset", "turn", "nose", "thrust", "barrel_roll"};
	private static final int PRESET = 0;
	private static final int TURN = 1;
	private static final int NOSE = 2;
	private static final int THRUST = 3;
	private static final boolean BARREL_ROLL = FabricLoader.getInstance().isModLoaded(HandlingClient.BARREL_ROLL);

	private final int tab;
	private boolean barrelRollChanged;
	private final List<Runnable> greying = new ArrayList<>();
	/** Each widget read again from the settings, after a preset or the reset changed them under it. */
	private final List<Runnable> rereading = new ArrayList<>();
	/** What the settings are now, a preset or custom. */
	private Button presetNow;
	/** The preset picked, put in only by its apply button. Kept across the tabs. */
	private HandlingPreset chosen;
	private final List<Tab> tabs = new ArrayList<>();
	private TabManager tabManager;
	private MenuTabBar tabBar;

	public HandlingConfigScreen(Screen parent) {
		this(parent, PRESET, false, null);
	}

	private HandlingConfigScreen(Screen parent, int tab, boolean barrelRollChanged, HandlingPreset chosen) {
		super(parent, Minecraft.getInstance().options, Component.translatable(KEY + "title"));
		this.tab = tab;
		this.barrelRollChanged = barrelRollChanged;
		HandlingPreset now = HandlingPreset.of(HandlingConfig.current);
		this.chosen = chosen != null ? chosen : now != null ? now : HandlingPreset.NORMAL;
	}

	/** For the tests. */
	public int tab() {
		return tab;
	}

	/** The tabs where the title would be. */
	@Override
	protected void addTitle() {
		tabManager = new TabManager(widget -> { }, widget -> { });
		tabs.clear();
		for (String name : TABS) {
			tabs.add(new GridLayoutTab(Component.translatable(KEY + "tab." + name)));
		}
		tabBar = MenuTabBar.builder(tabManager, width).addTabs(tabs.toArray(Tab[]::new)).build();
		addRenderableWidget(tabBar);
		tabBar.selectTab(tab, false);
		layout.setHeaderHeight(tabBar.getRectangle().height());
	}

	@Override
	protected void repositionElements() {
		if (tabBar != null) {
			tabBar.arrangeElements(width);
			layout.setHeaderHeight(tabBar.getRectangle().height());
		}
		super.repositionElements();
	}

	@Override
	public void tick() {
		super.tick();
		int selected = tabs.indexOf(tabManager.getCurrentTab());
		if (selected >= 0 && selected != tab) {
			reopen(selected);
		}
	}

	/** A new screen: rebuilding would keep the old list in the layout, under the new one and taking its clicks. */
	private void reopen(int toTab) {
		minecraft.gui.setScreen(new HandlingConfigScreen(lastScreen, toTab, barrelRollChanged, chosen));
	}

	@Override
	protected void addOptions() {
		HandlingConfig c = HandlingConfig.current;
		switch (tab) {
			case PRESET -> presets();
			case TURN -> {
				list.addHeader(Component.translatable(KEY + "bank_lift"));
				pull("bank_lift", () -> c.bankLift, on -> c.bankLift = on, () -> c.bankLiftDegrees, value -> c.bankLiftDegrees = (int) value,
						percent("bank_yaw", 0, 100, () -> c.bankYawPercent, value -> c.bankYawPercent = (int) value), null);
				list.addHeader(Component.translatable(KEY + "bank_sink"));
				pull("bank_sink", () -> c.bankSink, on -> c.bankSink = on, () -> c.bankSinkDegrees, value -> c.bankSinkDegrees = (int) value, null, null);
				list.addHeader(Component.translatable(KEY + "roll_limit"));
				AbstractWidget limit = toggle(KEY + "roll_limit", () -> c.rollLimit, on -> c.rollLimit = on);
				AbstractWidget degrees = slider(HandlingConfig.MIN_ROLL_LIMIT, HandlingConfig.MAX_ROLL_LIMIT, 1, () -> c.rollLimitDegrees,
						value -> c.rollLimitDegrees = (int) value, value -> Component.translatable(KEY + "roll_limit_degrees", (int) value));
				AbstractWidget soft = percent("roll_limit_soft", 0, HandlingConfig.MAX_ROLL_LIMIT_SOFT_PERCENT, () -> c.rollLimitSoftPercent,
						value -> c.rollLimitSoftPercent = (int) value);
				AbstractWidget turn = percent("roll_limit_turn", 0, 100, () -> c.rollLimitTurnPercent, value -> c.rollLimitTurnPercent = (int) value);
				list.addSmall(limit, degrees);
				list.addSmall(soft, turn);
				greyWith(() -> c.rollLimit, degrees, soft, turn);
			}
			case NOSE -> {
				list.addHeader(Component.translatable(KEY + "climb_drop"));
				AbstractWidget curve = tooltip(slider(HandlingConfig.MIN_CURVE, HandlingConfig.MAX_CURVE, 0.5, () -> c.climbCurve, value -> c.climbCurve = value,
						value -> Component.translatable(KEY + "climb_curve", String.format(Locale.ROOT, "%.1f", value))), "climb_curve");
				AbstractWidget belly = percent("climb_belly", 0, 100, () -> c.climbBellyPercent, value -> c.climbBellyPercent = (int) value);
				pull("climb_drop", () -> c.climbDrop, on -> c.climbDrop = on, () -> c.climbDropDegrees, value -> c.climbDropDegrees = (int) value, curve, belly);
				list.addHeader(Component.translatable(KEY + "stall_drop"));
				AbstractWidget speed = tooltip(slider(HandlingConfig.MIN_STALL_SPEED, HandlingConfig.MAX_STALL_SPEED, 1, () -> c.stallSpeed,
						value -> c.stallSpeed = (int) value, value -> Component.translatable(KEY + "stall_speed", (int) value)), "stall_speed");
				pull("stall_drop", () -> c.stallDrop, on -> c.stallDrop = on, () -> c.stallDropDegrees, value -> c.stallDropDegrees = (int) value, speed, null);
				list.addHeader(Component.translatable(KEY + "speed_lift"));
				AbstractWidget full = tooltip(slider(HandlingConfig.MIN_STALL_SPEED, HandlingConfig.MAX_FULL_SPEED, 1, () -> c.speedLiftFullSpeed,
						value -> c.speedLiftFullSpeed = (int) value, value -> Component.translatable(KEY + "speed_lift_full", (int) value)), "speed_lift_full");
				pull("speed_lift", () -> c.speedLift, on -> c.speedLift = on, () -> c.speedLiftDegrees, value -> c.speedLiftDegrees = (int) value, full, null);
			}
			case THRUST -> {
				if (BARREL_ROLL) {
					thrust(c);
				} else {
					needsBarrelRoll();
				}
				list.addHeader(Component.translatable(KEY + "landing"));
				Button landing = Button.builder(landingText(), button -> {
					HandlingConfig.current.landingRelease = HandlingConfig.current.landingRelease.next();
					button.setMessage(landingText());
				}).bounds(0, 0, ROW_WIDTH, ROW_HEIGHT).build();
				rereading.add(() -> landing.setMessage(landingText()));
				landing.setTooltip(Tooltip.create(Component.translatable(KEY + "landing_release.tooltip")));
				list.addSmall(landing, null);
			}
			default -> {
				if (BARREL_ROLL) {
					barrelRoll();
				} else {
					needsBarrelRoll();
				}
			}
		}
		changed();
	}

	private static Component landingText() {
		String value = HandlingConfig.current.landingRelease.name().toLowerCase(Locale.ROOT);
		return Component.translatable(KEY + "landing_release", Component.translatable(KEY + "landing_release." + value));
	}

	private void needsBarrelRoll() {
		AbstractWidget missing = Button.builder(Component.translatable(KEY + "needs_barrel_roll"), button -> { }).bounds(0, 0, ROW_WIDTH, ROW_HEIGHT).build();
		missing.active = false;
		list.addSmall(missing, null);
	}

	/** The thrust is Do a Barrel Roll's, so these show only with it. */
	private void thrust(HandlingConfig c) {
		list.addHeader(Component.translatable(KEY + "thrust"));
		AbstractWidget thrust = toggle(KEY + "thrust", () -> c.thrust, on -> c.thrust = on);
		AbstractWidget brake = tooltip(slider(0, HandlingConfig.MAX_BRAKE_PERCENT, 1, () -> c.brakePercent,
				value -> c.brakePercent = (int) value, value -> Component.translatable(KEY + "brake", (int) value)), "brake");
		AbstractWidget rise = seconds(() -> c.thrustRiseSeconds, value -> c.thrustRiseSeconds = value, "thrust_rise");
		AbstractWidget fall = seconds(() -> c.thrustFallSeconds, value -> c.thrustFallSeconds = value, "thrust_fall");
		AbstractWidget fade = tooltip(slider(0, HandlingConfig.MAX_BRAKE_FADE_SPEED, 1, () -> c.brakeFadeSpeed,
				value -> c.brakeFadeSpeed = (int) value, value -> Component.translatable(KEY + "brake_fade", (int) value)), "brake_fade");
		list.addSmall(thrust, brake);
		list.addSmall(rise, fall);
		list.addSmall(fade, null);
		greyWith(() -> c.thrust, brake, rise, fall, fade);
	}

	/** Do a Barrel Roll's own settings that go with these, saved to its file when the screen closes. */
	private void barrelRoll() {
		list.addHeader(Component.translatable(KEY + "barrel_roll"));
		AbstractWidget banking = toggleText(BarrelRollSettings::banking, on -> {
			BarrelRollSettings.setBanking(on);
			barrelRollChanged = true;
		}, value -> Component.translatable(KEY + "banking", onOff(value)));
		banking.setTooltip(Tooltip.create(Component.translatable(KEY + "banking.tooltip")));
		AbstractWidget righting = toggleText(BarrelRollSettings::righting, on -> {
			BarrelRollSettings.setRighting(on);
			barrelRollChanged = true;
		}, value -> Component.translatable(KEY + "righting", onOff(value)));
		righting.setTooltip(Tooltip.create(Component.translatable(KEY + "righting.tooltip")));
		AbstractWidget strength = tooltip(slider(0, BarrelRollSettings.MAX_RIGHTING_STRENGTH, 1, BarrelRollSettings::rightingStrength, value -> {
			BarrelRollSettings.setRightingStrength(value);
			barrelRollChanged = true;
		}, value -> Component.translatable(KEY + "righting_strength", (int) value)), "righting_strength");
		list.addSmall(banking, null);
		list.addSmall(righting, strength);
		greyWith(BarrelRollSettings::righting, strength);
	}

	private AbstractWidget seconds(DoubleSupplier get, DoubleConsumer set, String name) {
		return tooltip(slider(0, HandlingConfig.MAX_THRUST_SECONDS, 0.05, get, set,
				value -> Component.translatable(KEY + name, String.format(Locale.ROOT, "%.2f", value))), name);
	}

	private AbstractWidget percent(String name, int min, int max, DoubleSupplier get, DoubleConsumer set) {
		return tooltip(slider(min, max, 1, get, set, value -> Component.translatable(KEY + name, (int) value)), name);
	}

	private static AbstractWidget tooltip(AbstractWidget widget, String name) {
		widget.setTooltip(Tooltip.create(Component.translatable(KEY + name + ".tooltip")));
		return widget;
	}

	/** Its switch and strength on one row, and up to two more of its own on the next. */
	private void pull(String name, BooleanSupplier on, Consumer<Boolean> setOn, DoubleSupplier degrees, DoubleConsumer setDegrees,
			AbstractWidget left, AbstractWidget right) {
		AbstractWidget toggle = toggle(KEY + name, on, setOn);
		AbstractWidget strength = tooltip(slider(0, HandlingConfig.MAX_DEGREES, 1, degrees, setDegrees,
				value -> Component.translatable(KEY + "degrees", (int) value)), "degrees");
		list.addSmall(toggle, strength);
		if (left != null) {
			list.addSmall(left, right);
		}
		greyWith(on, strength, left, right);
	}

	private void greyWith(BooleanSupplier on, AbstractWidget... widgets) {
		greying.add(() -> {
			for (AbstractWidget widget : widgets) {
				if (widget != null) {
					widget.active = on.getAsBoolean();
				}
			}
		});
	}

	private void grey() {
		greying.forEach(Runnable::run);
	}

	/** The switch of a heading: its tooltip is the heading's. */
	private AbstractWidget toggle(String name, BooleanSupplier get, Consumer<Boolean> set) {
		AbstractWidget widget = toggleText(get, set, value -> Component.translatable(KEY + "enabled", onOff(value)));
		widget.setTooltip(Tooltip.create(Component.translatable(name + ".tooltip")));
		return widget;
	}

	private AbstractWidget toggleText(BooleanSupplier get, Consumer<Boolean> set, Function<Boolean, Component> text) {
		Button button = Button.builder(text.apply(get.getAsBoolean()), pressed -> {
			set.accept(!get.getAsBoolean());
			pressed.setMessage(text.apply(get.getAsBoolean()));
			changed();
		}).bounds(0, 0, ROW_WIDTH, ROW_HEIGHT).build();
		rereading.add(() -> button.setMessage(text.apply(get.getAsBoolean())));
		return button;
	}

	/** After a change by hand: what greys follows, and the preset may now be custom. */
	private void changed() {
		grey();
		if (presetNow != null) {
			presetNow.setMessage(Component.translatable(KEY + "preset.now", presetName(HandlingPreset.of(HandlingConfig.current))));
		}
	}

	/** After a preset or the reset: everything shown is read again, in place, so the button stays under the cursor. */
	private void reread() {
		rereading.forEach(Runnable::run);
		changed();
	}

	private static Component onOff(boolean on) {
		return on ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
	}

	private static Component presetName(HandlingPreset preset) {
		String name = preset == null ? "custom" : preset.name().toLowerCase(Locale.ROOT);
		return Component.translatable(KEY + "preset." + name);
	}

	/** The choice on the left, put in by the button on the right; below, what the settings are now. */
	private void presets() {
		list.addHeader(Component.translatable(KEY + "tab.preset"));
		Button choice = Button.builder(Component.translatable(KEY + "preset", presetName(chosen)), button -> {
			chosen = chosen.next();
			button.setMessage(Component.translatable(KEY + "preset", presetName(chosen)));
		}).bounds(0, 0, ROW_WIDTH, ROW_HEIGHT).build();
		choice.setTooltip(Tooltip.create(Component.translatable(KEY + "preset.tooltip")));
		Button apply = Button.builder(Component.translatable(KEY + "preset.apply"), button -> {
			chosen.apply(HandlingConfig.current);
			reread();
		}).bounds(0, 0, ROW_WIDTH, ROW_HEIGHT).build();
		apply.setTooltip(Tooltip.create(Component.translatable(KEY + "preset.apply.tooltip")));
		list.addSmall(choice, apply);
		presetNow = Button.builder(Component.empty(), button -> { }).bounds(0, 0, ROW_WIDTH, ROW_HEIGHT).build();
		presetNow.active = false;
		list.addSmall(presetNow, null);
	}

	@Override
	protected void addFooter() {
		LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(FOOTER_GAP));
		footer.addChild(Button.builder(Component.translatable(KEY + "reset"), button -> {
			HandlingConfig.resetToDefaults();
			reread();
		}).width(ROW_WIDTH).build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(ROW_WIDTH).build());
	}

	@Override
	public void onClose() {
		HandlingConfig.save();
		if (barrelRollChanged) {
			BarrelRollSettings.save();
		}
		super.onClose();
	}

	private AbstractWidget slider(double min, double max, double step, DoubleSupplier get, DoubleConsumer set, DoubleFunction<Component> text) {
		Slider slider = new Slider(min, max, step, get, set, text);
		rereading.add(slider::reread);
		return slider;
	}

	private final class Slider extends AbstractSliderButton {
		private final DoubleSupplier get;
		private final double min;
		private final double max;
		private final double step;
		private final DoubleConsumer set;
		private final DoubleFunction<Component> text;

		Slider(double min, double max, double step, DoubleSupplier get, DoubleConsumer set, DoubleFunction<Component> text) {
			super(0, 0, ROW_WIDTH, ROW_HEIGHT, text.apply(get.getAsDouble()), Mth.clamp((get.getAsDouble() - min) / (max - min), 0.0, 1.0));
			this.get = get;
			this.min = min;
			this.max = max;
			this.step = step;
			this.set = set;
			this.text = text;
		}

		private double current() {
			return Mth.clamp(min + Math.round(value * (max - min) / step) * step, min, max);
		}

		@Override
		protected void updateMessage() {
			setMessage(text.apply(current()));
		}

		@Override
		protected void applyValue() {
			set.accept(current());
			changed();
		}

		void reread() {
			value = Mth.clamp((get.getAsDouble() - min) / (max - min), 0.0, 1.0);
			updateMessage();
		}
	}
}
