package jadx.plugins.mappings;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.format.MappingFormat;

import jadx.api.plugins.options.OptionFlag;
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;
import jadx.core.utils.ListUtils;

import static jadx.plugins.mappings.RenameMappingsPlugin.PLUGIN_ID;

public class RenameMappingsOptions extends BasePluginOptionsBuilder {

	public static final String INVERT_OPT = PLUGIN_ID + ".invert";
	public static final String FORMAT_OPT = PLUGIN_ID + ".format";

	private boolean invert = false;

	/**
	 * null value - used for 'auto' option
	 */
	private @Nullable MappingFormat format = null;

	@Override
	public void registerOptions() {
		option(FORMAT_OPT, MappingFormat.class)
				.description("mapping format")
				.parser(RenameMappingsOptions::parseMappingFormat)
				.formatter(v -> v == null ? "AUTO" : v.name())
				.values(ListUtils.concat(null, MappingFormat.values()))
				.defaultValue(null)
				.flags(OptionFlag.PER_PROJECT, OptionFlag.DISABLE_IN_GUI)
				.setter(v -> format = v);

		boolOption(INVERT_OPT)
				.description("invert mapping on load")
				.defaultValue(false)
				.flags(OptionFlag.PER_PROJECT)
				.setter(v -> invert = v);
	}

	private static MappingFormat parseMappingFormat(String name) {
		String upName = name.toUpperCase(Locale.ROOT);
		if (upName.equals("AUTO")) {
			return null;
		}
		return MappingFormat.valueOf(upName);
	}

	public @Nullable MappingFormat getFormat() {
		return format;
	}

	public boolean isInvert() {
		return invert;
	}

	public String getOptionsHashString() {
		return format + ":" + invert;
	}
}
