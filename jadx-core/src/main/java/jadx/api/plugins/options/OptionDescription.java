package jadx.api.plugins.options;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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

	enum OptionFlag {
		PER_PROJECT, // store in project settings instead global (for jadx-gui)
		HIDE_IN_GUI, // do not show this option in jadx-gui (useful if option is configured with custom ui)
	}

	default Set<OptionFlag> getFlags() {
		return Collections.emptySet();
	}
}
