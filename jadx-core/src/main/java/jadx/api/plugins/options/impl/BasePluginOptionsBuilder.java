package jadx.api.plugins.options.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.OptionDescription;
import jadx.api.plugins.options.OptionFlag;
import jadx.api.plugins.options.OptionType;

/**
 * Base class for {@link JadxPluginOptions} implementation
 * <p>
 * Override {@link BasePluginOptionsBuilder#registerOptions()} method
 * and use *option methods to add option info.
 */
@SuppressWarnings("unused")
public abstract class BasePluginOptionsBuilder implements JadxPluginOptions {

	private final List<OptionData<?>> options = new ArrayList<>();

	public abstract void registerOptions();

	public BasePluginOptionsBuilder() {
		registerOptions();
		for (OptionData<?> option : options) {
			option.validate();
		}
	}

	public <T> OptionBuilder<T> option(String name) {
		return addOption(new OptionData<>(name));
	}

	public <T> OptionBuilder<T> option(String name, Class<T> optionType) {
		return addOption(new OptionData<>(name));
	}

	public OptionBuilder<Boolean> boolOption(String name) {
		return addOption(
				new OptionData<Boolean>(name)
						.type(OptionType.BOOLEAN)
						.values(Arrays.asList(Boolean.TRUE, Boolean.FALSE))
						.formatter(b -> b ? "yes" : "no")
						.parser(val -> parseBoolOption(name, val)));
	}

	public OptionBuilder<String> strOption(String name) {
		return addOption(
				new OptionData<String>(name)
						.type(OptionType.STRING)
						.formatter(v -> v)
						.parser(v -> v));
	}

	public OptionBuilder<Integer> intOption(String name) {
		return addOption(
				new OptionData<Integer>(name)
						.type(OptionType.NUMBER)
						.formatter(Object::toString)
						.parser(Integer::parseInt));
	}

	public <E extends Enum<?>> OptionBuilder<E> enumOption(String name, E[] values, Function<String, E> valueOf) {
		return addOption(
				new OptionData<E>(name)
						.type(OptionType.STRING)
						.values(Arrays.asList(values))
						.formatter(v -> v.name().toLowerCase(Locale.ROOT))
						.parser(v -> valueOf.apply(v.toUpperCase(Locale.ROOT))));
	}

	@Override
	public void setOptions(Map<String, String> map) {
		for (OptionData<?> option : options) {
			parseOption(option, map.get(option.name));
		}
	}

	@Override
	public List<OptionDescription> getOptionsDescriptions() {
		return Collections.unmodifiableList(options);
	}

	private static <T> void parseOption(OptionData<T> option, @Nullable String value) {
		T parsedValue;
		if (value == null) {
			parsedValue = option.defaultValue;
		} else {
			try {
				parsedValue = option.getParser().apply(value);
			} catch (Exception e) {
				throw new RuntimeException("Parse failed for option: " + option.name + ", value: " + value, e);
			}
		}
		try {
			option.getSetter().accept(parsedValue);
		} catch (Exception e) {
			throw new RuntimeException("Setter invoke failed for option: " + option.name + ", value: " + parsedValue, e);
		}
	}

	private static boolean parseBoolOption(String name, String val) {
		String valLower = val.trim().toLowerCase(Locale.ROOT);
		if (valLower.equals("yes") || valLower.equals("true")) {
			return true;
		}
		if (valLower.equals("no") || valLower.equals("false")) {
			return false;
		}
		throw new IllegalArgumentException("Unknown value '" + val + "' for option '" + name + "', expect: 'yes' or 'no'");
	}

	private <T> OptionBuilder<T> addOption(OptionBuilder<T> optionData) {
		this.options.add((OptionData<?>) optionData);
		return optionData;
	}

	protected static class OptionData<T> implements OptionDescription, OptionBuilder<T> {
		private final String name;
		private String desc;
		private List<T> values = Collections.emptyList();
		private OptionType type = OptionType.STRING;
		private Set<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);
		private Function<String, T> parser;
		private Function<T, String> formatter;
		private Consumer<T> setter;
		private T defaultValue;

		public OptionData(String name) {
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String description() {
			return desc;
		}

		@Override
		public List<String> values() {
			return values.stream().map(formatter).collect(Collectors.toList());
		}

		@Override
		public @Nullable String defaultValue() {
			return formatter.apply(defaultValue);
		}

		@Override
		public OptionType getType() {
			return type;
		}

		@Override
		public Set<OptionFlag> getFlags() {
			return flags;
		}

		@Override
		public OptionBuilder<T> description(String desc) {
			this.desc = desc;
			return this;
		}

		@Override
		public OptionBuilder<T> defaultValue(@Nullable T defValue) {
			this.defaultValue = defValue;
			return this;
		}

		@Override
		public OptionBuilder<T> parser(Function<String, T> parser) {
			this.parser = parser;
			return this;
		}

		@Override
		public OptionBuilder<T> formatter(Function<T, String> formatter) {
			this.formatter = formatter;
			return this;
		}

		@Override
		public OptionBuilder<T> setter(Consumer<T> setter) {
			this.setter = setter;
			return this;
		}

		@Override
		public OptionBuilder<T> type(OptionType optionType) {
			this.type = optionType;
			return this;
		}

		@Override
		public OptionBuilder<T> flags(OptionFlag... flags) {
			this.flags = EnumSet.copyOf(Arrays.asList(flags));
			return this;
		}

		@Override
		public OptionBuilder<T> values(List<T> values) {
			this.values = values;
			return this;
		}

		public Function<String, T> getParser() {
			return parser;
		}

		public Function<T, String> getFormatter() {
			return formatter;
		}

		public Consumer<T> getSetter() {
			return setter;
		}

		public void validate() {
			if (desc == null || desc.isEmpty()) {
				throw new IllegalArgumentException("Description should be set for option: " + name);
			}
			if (parser == null) {
				throw new IllegalArgumentException("Parser should be set for option: " + name);
			}
			if (formatter == null) {
				throw new IllegalArgumentException("Formatter should be set for option: " + name);
			}
			if (setter == null) {
				throw new IllegalArgumentException("Setter should be set for option: " + name);
			}
		}
	}
}
