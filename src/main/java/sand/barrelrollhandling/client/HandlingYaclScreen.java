package sand.barrelrollhandling.client;

import com.google.gson.Gson;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.StateManager;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import sand.barrelrollhandling.BarrelRollSettings;
import sand.barrelrollhandling.HandlingClient;
import sand.barrelrollhandling.HandlingConfig;
import sand.barrelrollhandling.HandlingPreset;

/**
 * The same settings and tabs as {@link HandlingConfigScreen}, on YetAnotherConfigLib's screen when it is there: each
 * setting's text stays in the panel beside it, each has its own reset, and nothing is kept until saved. Only loaded
 * when YetAnotherConfigLib is.
 */
public final class HandlingYaclScreen {
	public static final String YACL = "yet_another_config_lib_v3";
	private static final String KEY = "barrel_roll_handling.config.";
	private static final String NAME = "barrel_roll_handling.yacl.";
	private static final Gson GSON = new Gson();
	private static final boolean BARREL_ROLL = FabricLoader.getInstance().isModLoaded(HandlingClient.BARREL_ROLL);

	/** This mod's settings shown, to read the pending ones together and to set them from a preset. */
	private final List<Field<?>> fields = new ArrayList<>();
	private final HandlingConfig defaults = new HandlingConfig();
	private HandlingPreset chosen;
	private boolean barrelRollChanged;

	private record Field<T>(Option<T> option, Function<HandlingConfig, T> get, BiConsumer<HandlingConfig, T> set) {
		void into(HandlingConfig config) {
			set.accept(config, option.pendingValue());
		}

		void from(HandlingConfig config) {
			option.requestSet(get.apply(config));
		}
	}

	public static Screen create(Screen parent) {
		return new HandlingYaclScreen().build().generateScreen(parent);
	}

	private YetAnotherConfigLib build() {
		HandlingPreset now = HandlingPreset.of(HandlingConfig.current);
		chosen = now == null ? HandlingPreset.NORMAL : now;
		ConfigCategory presets = ConfigCategory.createBuilder().name(Component.translatable(KEY + "tab.preset"))
				.option(preset()).option(apply())
				.build();
		ConfigCategory turn = ConfigCategory.createBuilder().name(Component.translatable(KEY + "tab.turn"))
				.group(pull("bank_lift", c -> c.bankLift, (c, v) -> c.bankLift = v, c -> c.bankLiftDegrees, (c, v) -> c.bankLiftDegrees = v,
						percent("bank_yaw", 0, 100, c -> c.bankYawPercent, (c, v) -> c.bankYawPercent = v)))
				.group(pull("bank_sink", c -> c.bankSink, (c, v) -> c.bankSink = v, c -> c.bankSinkDegrees, (c, v) -> c.bankSinkDegrees = v))
				.group(rollLimit())
				.build();
		ConfigCategory nose = ConfigCategory.createBuilder().name(Component.translatable(KEY + "tab.nose"))
				.group(pull("climb_drop", c -> c.climbDrop, (c, v) -> c.climbDrop = v, c -> c.climbDropDegrees, (c, v) -> c.climbDropDegrees = v,
						decimal("climb_curve", HandlingConfig.MIN_CURVE, HandlingConfig.MAX_CURVE, 0.5, "%.1f", null, c -> c.climbCurve, (c, v) -> c.climbCurve = v),
						percent("climb_belly", 0, 100, c -> c.climbBellyPercent, (c, v) -> c.climbBellyPercent = v)))
				.group(pull("stall_drop", c -> c.stallDrop, (c, v) -> c.stallDrop = v, c -> c.stallDropDegrees, (c, v) -> c.stallDropDegrees = v,
						integer("stall_speed", HandlingConfig.MIN_STALL_SPEED, HandlingConfig.MAX_STALL_SPEED, "speed", c -> c.stallSpeed, (c, v) -> c.stallSpeed = v)))
				.group(pull("speed_lift", c -> c.speedLift, (c, v) -> c.speedLift = v, c -> c.speedLiftDegrees, (c, v) -> c.speedLiftDegrees = v,
						integer("speed_lift_full", HandlingConfig.MIN_STALL_SPEED, HandlingConfig.MAX_FULL_SPEED, "speed", c -> c.speedLiftFullSpeed,
								(c, v) -> c.speedLiftFullSpeed = v)))
				.build();
		ConfigCategory.Builder thrust = ConfigCategory.createBuilder().name(Component.translatable(KEY + "tab.thrust"));
		if (BARREL_ROLL) {
			thrust.group(thrust());
		}
		thrust.group(OptionGroup.createBuilder().name(Component.translatable(KEY + "landing"))
				.option(field(Option.<HandlingConfig.LandingRelease>createBuilder().name(Component.translatable(NAME + "landing_release"))
						.description(OptionDescription.of(Component.translatable(KEY + "landing_release.tooltip")))
						.controller(option -> EnumControllerBuilder.create(option).enumClass(HandlingConfig.LandingRelease.class)
								.formatValue(value -> Component.translatable(KEY + "landing_release." + value.name().toLowerCase(Locale.ROOT)))),
						c -> c.landingRelease, (c, v) -> c.landingRelease = v))
				.build());
		YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder().title(Component.translatable(KEY + "title"))
				.category(presets).category(turn).category(nose).category(thrust.build());
		if (BARREL_ROLL) {
			builder.category(barrelRoll());
		}
		return builder.save(() -> {
			HandlingConfig.save();
			if (barrelRollChanged) {
				BarrelRollSettings.save();
				barrelRollChanged = false;
			}
		}).build();
	}

	/** The settings as they would be saved now. */
	private HandlingConfig pending() {
		HandlingConfig config = GSON.fromJson(GSON.toJson(HandlingConfig.current), HandlingConfig.class);
		for (Field<?> field : fields) {
			field.into(config);
		}
		return config;
	}

	/** The choice, apart from the settings: picking one changes nothing until it is put in. */
	private Option<HandlingPreset> preset() {
		return Option.<HandlingPreset>createBuilder().name(Component.translatable(NAME + "preset"))
				.description(OptionDescription.of(Component.translatable(KEY + "preset.tooltip")))
				.stateManager(StateManager.createInstant(HandlingPreset.NORMAL, () -> chosen, value -> chosen = value))
				.controller(option -> EnumControllerBuilder.create(option).enumClass(HandlingPreset.class)
						.formatValue(value -> Component.translatable(KEY + "preset." + value.name().toLowerCase(Locale.ROOT))))
				.build();
	}

	/** Puts the chosen preset into the settings shown, as pending like any change by hand. */
	private ButtonOption apply() {
		return ButtonOption.createBuilder().name(Component.translatable(NAME + "preset_apply"))
				.description(OptionDescription.of(Component.translatable(KEY + "preset.apply.tooltip")))
				.text(Component.empty())
				.action((screen, button) -> {
					HandlingConfig config = pending();
					chosen.apply(config);
					for (Field<?> field : fields) {
						field.from(config);
					}
				})
				.build();
	}

	/** Its switch, its strength, and what else hangs on the switch, greyed while it is off. */
	@SafeVarargs
	private OptionGroup pull(String name, Function<HandlingConfig, Boolean> on, BiConsumer<HandlingConfig, Boolean> setOn,
			Function<HandlingConfig, Integer> degrees, BiConsumer<HandlingConfig, Integer> setDegrees, Option<?>... more) {
		Option<Integer> strength = integer("degrees", 0, HandlingConfig.MAX_DEGREES, "degrees", degrees, setDegrees);
		List<Option<?>> hanging = new ArrayList<>(List.of(strength));
		hanging.addAll(List.of(more));
		return group(name, toggle(name, on, setOn, hanging), hanging);
	}

	private OptionGroup rollLimit() {
		List<Option<?>> hanging = List.of(
				integer("roll_limit_degrees", HandlingConfig.MIN_ROLL_LIMIT, HandlingConfig.MAX_ROLL_LIMIT, "angle", c -> c.rollLimitDegrees, (c, v) -> c.rollLimitDegrees = v),
				percent("roll_limit_soft", 0, HandlingConfig.MAX_ROLL_LIMIT_SOFT_PERCENT, c -> c.rollLimitSoftPercent, (c, v) -> c.rollLimitSoftPercent = v),
				percent("roll_limit_turn", 0, 100, c -> c.rollLimitTurnPercent, (c, v) -> c.rollLimitTurnPercent = v));
		return group("roll_limit", toggle("roll_limit", c -> c.rollLimit, (c, v) -> c.rollLimit = v, hanging), hanging);
	}

	private OptionGroup thrust() {
		List<Option<?>> hanging = List.of(
				percent("brake", 0, HandlingConfig.MAX_BRAKE_PERCENT, c -> c.brakePercent, (c, v) -> c.brakePercent = v),
				decimal("thrust_rise", 0, HandlingConfig.MAX_THRUST_SECONDS, 0.05, "%.2f", "seconds", c -> c.thrustRiseSeconds, (c, v) -> c.thrustRiseSeconds = v),
				decimal("thrust_fall", 0, HandlingConfig.MAX_THRUST_SECONDS, 0.05, "%.2f", "seconds", c -> c.thrustFallSeconds, (c, v) -> c.thrustFallSeconds = v),
				integer("brake_fade", 0, HandlingConfig.MAX_BRAKE_FADE_SPEED, "speed", c -> c.brakeFadeSpeed, (c, v) -> c.brakeFadeSpeed = v));
		return group("thrust", toggle("thrust", c -> c.thrust, (c, v) -> c.thrust = v, hanging), hanging);
	}

	private static OptionGroup group(String name, Option<Boolean> toggle, List<Option<?>> hanging) {
		return OptionGroup.createBuilder().name(Component.translatable(KEY + name))
				.description(OptionDescription.of(Component.translatable(KEY + name + ".tooltip")))
				.option(toggle).options(hanging).build();
	}

	private Option<Boolean> toggle(String name, Function<HandlingConfig, Boolean> get, BiConsumer<HandlingConfig, Boolean> set, List<Option<?>> hanging) {
		for (Option<?> other : hanging) {
			other.setAvailable(get.apply(HandlingConfig.current));
		}
		return field(Option.<Boolean>createBuilder().name(Component.translatable(NAME + "enabled"))
				.description(OptionDescription.of(Component.translatable(KEY + name + ".tooltip")))
				.controller(option -> BooleanControllerBuilder.create(option).coloured(true))
				.addListener((option, event) -> {
					for (Option<?> other : hanging) {
						other.setAvailable(option.pendingValue());
					}
				}), get, set);
	}

	private Option<Integer> percent(String name, int min, int max, Function<HandlingConfig, Integer> get, BiConsumer<HandlingConfig, Integer> set) {
		return integer(name, min, max, "percent", get, set);
	}

	private Option<Integer> integer(String name, int min, int max, String unit, Function<HandlingConfig, Integer> get, BiConsumer<HandlingConfig, Integer> set) {
		return field(Option.<Integer>createBuilder().name(Component.translatable(NAME + name))
				.description(OptionDescription.of(Component.translatable(KEY + name + ".tooltip")))
				.controller(option -> IntegerSliderControllerBuilder.create(option).range(min, max).step(1)
						.formatValue(value -> Component.translatable(NAME + "unit." + unit, value))), get, set);
	}

	private Option<Double> decimal(String name, double min, double max, double step, String format, String unit,
			Function<HandlingConfig, Double> get, BiConsumer<HandlingConfig, Double> set) {
		return field(Option.<Double>createBuilder().name(Component.translatable(NAME + name))
				.description(OptionDescription.of(Component.translatable(KEY + name + ".tooltip")))
				.controller(option -> DoubleSliderControllerBuilder.create(option).range(min, max).step(step).formatValue(value -> {
					String text = String.format(Locale.ROOT, format, value);
					return unit == null ? Component.literal(text) : Component.translatable(NAME + "unit." + unit, text);
				})), get, set);
	}

	/** Bound to the settings in memory, its default from a fresh one, and kept for the presets. */
	private <T> Option<T> field(Option.Builder<T> builder, Function<HandlingConfig, T> get, BiConsumer<HandlingConfig, T> set) {
		Option<T> option = builder.binding(get.apply(defaults), () -> get.apply(HandlingConfig.current), value -> set.accept(HandlingConfig.current, value)).build();
		fields.add(new Field<>(option, get, set));
		return option;
	}

	/**
	 * Do a Barrel Roll's own settings that go with these, written to its file only when changed here. Their default is
	 * what they were on opening, so the reset, which is for this mod's own, leaves them as they were.
	 */
	private ConfigCategory barrelRoll() {
		Option<Double> strength = Option.<Double>createBuilder().name(Component.translatable(NAME + "righting_strength"))
				.description(OptionDescription.of(Component.translatable(KEY + "righting_strength.tooltip")))
				.binding(BarrelRollSettings.rightingStrength(), BarrelRollSettings::rightingStrength, value -> {
					BarrelRollSettings.setRightingStrength(value);
					barrelRollChanged = true;
				})
				.controller(option -> DoubleSliderControllerBuilder.create(option).range(0.0, (double) BarrelRollSettings.MAX_RIGHTING_STRENGTH).step(1.0)
						.formatValue(value -> Component.literal(String.valueOf(Math.round(value)))))
				.build();
		Option<Boolean> righting = Option.<Boolean>createBuilder().name(Component.translatable(NAME + "righting"))
				.description(OptionDescription.of(Component.translatable(KEY + "righting.tooltip")))
				.binding(BarrelRollSettings.righting(), BarrelRollSettings::righting, value -> {
					BarrelRollSettings.setRighting(value);
					barrelRollChanged = true;
				})
				.controller(option -> BooleanControllerBuilder.create(option).coloured(true))
				.addListener((option, event) -> strength.setAvailable(option.pendingValue()))
				.build();
		strength.setAvailable(BarrelRollSettings.righting());
		Option<Boolean> banking = Option.<Boolean>createBuilder().name(Component.translatable(NAME + "banking"))
				.description(OptionDescription.of(Component.translatable(KEY + "banking.tooltip")))
				.binding(BarrelRollSettings.banking(), BarrelRollSettings::banking, value -> {
					BarrelRollSettings.setBanking(value);
					barrelRollChanged = true;
				})
				.controller(option -> BooleanControllerBuilder.create(option).coloured(true))
				.build();
		return ConfigCategory.createBuilder().name(Component.translatable(KEY + "tab.barrel_roll"))
				.group(OptionGroup.createBuilder().name(Component.translatable(KEY + "barrel_roll"))
						.option(banking).option(righting).option(strength).build())
				.build();
	}

	private HandlingYaclScreen() {
	}
}
