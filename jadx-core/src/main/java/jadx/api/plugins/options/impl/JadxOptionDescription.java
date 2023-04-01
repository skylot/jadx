package jadx.api.plugins.options.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.options.OptionDescription;

public class JadxOptionDescription implements OptionDescription {

	public static JadxOptionDescription booleanOption(String name, String desc, boolean defaultValue) {
		return new JadxOptionDescription(name, desc,
				defaultValue ? "yes" : "no",
				Arrays.asList("yes", "no"),
				OptionType.BOOLEAN);
	}

	private final String name;
	private final String desc;
	private final String defaultValue;
	private final List<String> values;
	private final OptionType type;
	private final Set<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);

	public JadxOptionDescription(String name, String desc, @Nullable String defaultValue, List<String> values) {
		this(name, desc, defaultValue, values, OptionType.STRING);
	}

	public JadxOptionDescription(String name, String desc, @Nullable String defaultValue, List<String> values, OptionType type) {
		this.name = name;
		this.desc = desc;
		this.defaultValue = defaultValue;
		this.values = values;
		this.type = type;
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
	public @Nullable String defaultValue() {
		return defaultValue;
	}

	@Override
	public List<String> values() {
		return values;
	}

	@Override
	public OptionType getType() {
		return type;
	}

	@Override
	public Set<OptionFlag> getFlags() {
		return flags;
	}

	public JadxOptionDescription withFlag(OptionFlag flag) {
		this.flags.add(flag);
		return this;
	}

	public JadxOptionDescription withFlags(OptionFlag... flags) {
		Collections.addAll(this.flags, flags);
		return this;
	}

	@Override
	public String toString() {
		return "OptionDescription{" + desc + ", values=" + values + '}';
	}
}
