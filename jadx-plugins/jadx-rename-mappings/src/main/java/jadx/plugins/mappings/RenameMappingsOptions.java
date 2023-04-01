package jadx.plugins.mappings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.format.MappingFormat;

import jadx.api.plugins.options.OptionDescription;
import jadx.api.plugins.options.OptionDescription.OptionFlag;
import jadx.api.plugins.options.impl.BaseOptionsParser;
import jadx.api.plugins.options.impl.JadxOptionDescription;

import static jadx.plugins.mappings.RenameMappingsPlugin.PLUGIN_ID;

public class RenameMappingsOptions extends BaseOptionsParser {

	public static final String INVERT_OPT = PLUGIN_ID + ".invert";
	public static final String FORMAT_OPT = PLUGIN_ID + ".format";

	private boolean invert = false;

	/**
	 * null value - used for 'auto' option
	 */
	private @Nullable MappingFormat format = null;

	@Override
	public void parseOptions() {
		format = getOption(FORMAT_OPT, RenameMappingsOptions::parseMappingFormat, null);
		invert = getBooleanOption(INVERT_OPT, false);
	}

	@Override
	public List<OptionDescription> getOptionsDescriptions() {
		return Arrays.asList(
				new JadxOptionDescription(FORMAT_OPT, "mapping format", "auto", getMappingFormats())
						.withFlag(OptionFlag.PER_PROJECT),
				JadxOptionDescription.booleanOption(INVERT_OPT, "invert mapping", false)
						.withFlag(OptionFlag.PER_PROJECT));
	}

	private static MappingFormat parseMappingFormat(String name) {
		String upName = name.toUpperCase(Locale.ROOT);
		if (upName.equals("AUTO")) {
			return null;
		}
		return MappingFormat.valueOf(upName);
	}

	private static List<String> getMappingFormats() {
		List<String> list = new ArrayList<>();
		list.add("auto");
		for (MappingFormat value : MappingFormat.values()) {
			list.add(value.name());
		}
		return list;
	}

	public MappingFormat getFormat() {
		return format;
	}

	public boolean isInvert() {
		return invert;
	}

	public String getOptionsHashString() {
		return format + ":" + invert;
	}
}
