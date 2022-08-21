package jadx.api.plugins.options;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public interface OptionDescription {

	String name();

	String description();

	/**
	 * Possible values.
	 * Empty if not a limited set
	 */
	List<String> values();

	/**
	 * Default value.
	 * Null if required
	 */
	@Nullable
	String defaultValue();

	enum OptionType {
		STRING, NUMBER, BOOLEAN
	}

	default OptionType getType() {
		return OptionType.STRING;
	}
}
