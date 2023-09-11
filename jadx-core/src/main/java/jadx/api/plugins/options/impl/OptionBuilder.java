package jadx.api.plugins.options.impl;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import jadx.api.plugins.options.OptionFlag;
import jadx.api.plugins.options.OptionType;

public interface OptionBuilder<T> {

	/**
	 * Option description (required)
	 */
	OptionBuilder<T> description(String desc);

	OptionBuilder<T> defaultValue(T defValue);

	/**
	 * Function to parse input string into option value (required)
	 */
	OptionBuilder<T> parser(Function<String, T> parser);

	/**
	 * Function to format option value into string for build help (required)
	 */
	OptionBuilder<T> formatter(Function<T, String> formatter);

	/**
	 * Function to save/apply parsed option value (required)
	 */
	OptionBuilder<T> setter(Consumer<T> setter);

	/**
	 * Possible option values
	 */
	OptionBuilder<T> values(List<T> values);

	OptionBuilder<T> type(OptionType optionType);

	OptionBuilder<T> flags(OptionFlag... flags);
}
