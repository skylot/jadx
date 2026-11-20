package jadx.plugins.callgraph;

import jadx.api.plugins.options.OptionFlag;
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

import static jadx.plugins.callgraph.CallGraphPlugin.PLUGIN_ID;

public class CallGraphOptions extends BasePluginOptionsBuilder {
	public static final String FORMAT_OPT = PLUGIN_ID + ".format";
	public static final String UNRESOLVED_OPT = PLUGIN_ID + ".unresolved";
	public static final String INCLUDE_PACKAGES_OPT = PLUGIN_ID + ".includePackages";
	public static final String EXCLUDE_PACKAGES_OPT = PLUGIN_ID + ".excludePackages";
	public static final String USE_ALIAS_OPT = PLUGIN_ID + ".useAlias";

	public enum Format {
		NONE,
		JSON,
		DOT
	}

	private Format format = Format.NONE;
	private boolean unresolved = false;
	private String includePackages = "";
	private String excludePackages = "";
	private boolean useAlias = false;

	@Override
	public void registerOptions() {
		enumOption(FORMAT_OPT, Format.values(), Format::valueOf)
				.description("save call graph in specified format")
				.defaultValue(Format.NONE)
				.flags(OptionFlag.HIDE_IN_GUI, OptionFlag.NOT_CHANGING_CODE)
				.setter(v -> format = v);

		boolOption(UNRESOLVED_OPT)
				.description("include unresolved method calls in call graph")
				.defaultValue(false)
				.flags(OptionFlag.HIDE_IN_GUI, OptionFlag.NOT_CHANGING_CODE)
				.setter(v -> unresolved = v);

		strOption(INCLUDE_PACKAGES_OPT)
				.description("comma-separated package prefixes to include in call graph")
				.defaultValue("")
				.flags(OptionFlag.HIDE_IN_GUI, OptionFlag.NOT_CHANGING_CODE)
				.setter(v -> includePackages = v);

		strOption(EXCLUDE_PACKAGES_OPT)
				.description("comma-separated package prefixes to exclude from call graph")
				.defaultValue("")
				.flags(OptionFlag.HIDE_IN_GUI, OptionFlag.NOT_CHANGING_CODE)
				.setter(v -> excludePackages = v);

		boolOption(USE_ALIAS_OPT)
				.description("use deobfuscated/aliased names in call graph export")
				.defaultValue(false)
				.flags(OptionFlag.HIDE_IN_GUI, OptionFlag.NOT_CHANGING_CODE)
				.setter(v -> useAlias = v);
	}

	public Format getFormat() {
		return format;
	}

	public boolean isUnresolved() {
		return unresolved;
	}

	public String getIncludePackages() {
		return includePackages;
	}

	public String getExcludePackages() {
		return excludePackages;
	}

	public boolean isUseAlias() {
		return useAlias;
	}
}
