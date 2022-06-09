package jadx.plugins.input.javaconvert;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jadx.api.plugins.options.OptionDescription;
import jadx.api.plugins.options.impl.BaseOptionsParser;
import jadx.api.plugins.options.impl.JadxOptionDescription;

public class JavaConvertOptions extends BaseOptionsParser {

	private static final String MODE_OPT = JavaConvertPlugin.PLUGIN_ID + ".mode";
	private static final String D8_DESUGAR_OPT = JavaConvertPlugin.PLUGIN_ID + ".d8-desugar";

	public enum Mode {
		DX, D8, BOTH
	}

	private Mode mode = Mode.BOTH;
	private boolean d8Desugar = false;

	public void apply(Map<String, String> options) {
		mode = getOption(options, MODE_OPT, name -> Mode.valueOf(name.toUpperCase(Locale.ROOT)), Mode.BOTH);
		d8Desugar = getBooleanOption(options, D8_DESUGAR_OPT, false);
	}

	public List<OptionDescription> buildOptionsDescriptions() {
		return Arrays.asList(
				new JadxOptionDescription(
						MODE_OPT,
						"convert mode",
						"both",
						Arrays.asList("dx", "d8", "both")),
				new JadxOptionDescription(
						D8_DESUGAR_OPT,
						"use desugar in d8",
						"no",
						Arrays.asList("yes", "no")));
	}

	public Mode getMode() {
		return mode;
	}

	public boolean isD8Desugar() {
		return d8Desugar;
	}
}
