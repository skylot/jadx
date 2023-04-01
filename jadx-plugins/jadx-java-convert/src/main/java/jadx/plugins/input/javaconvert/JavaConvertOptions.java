package jadx.plugins.input.javaconvert;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jadx.api.plugins.options.OptionDescription;
import jadx.api.plugins.options.impl.BaseOptionsParser;
import jadx.api.plugins.options.impl.JadxOptionDescription;
import jadx.core.utils.files.FileUtils;

public class JavaConvertOptions extends BaseOptionsParser {

	private static final String MODE_OPT = JavaConvertPlugin.PLUGIN_ID + ".mode";
	private static final String D8_DESUGAR_OPT = JavaConvertPlugin.PLUGIN_ID + ".d8-desugar";

	public enum Mode {
		DX, D8, BOTH
	}

	private Mode mode = Mode.BOTH;
	private boolean d8Desugar = false;

	@Override
	public void parseOptions() {
		mode = getOption(MODE_OPT, name -> Mode.valueOf(name.toUpperCase(Locale.ROOT)), Mode.BOTH);
		d8Desugar = getBooleanOption(D8_DESUGAR_OPT, false);
	}

	@Override
	public List<OptionDescription> getOptionsDescriptions() {
		return Arrays.asList(
				new JadxOptionDescription(
						MODE_OPT,
						"convert mode",
						"both",
						Arrays.asList("dx", "d8", "both")),
				JadxOptionDescription.booleanOption(
						D8_DESUGAR_OPT,
						"use desugar in d8",
						false));
	}

	public Mode getMode() {
		return mode;
	}

	public boolean isD8Desugar() {
		return d8Desugar;
	}

	public String getOptionsHash() {
		return FileUtils.md5Sum(mode + ":" + d8Desugar);
	}
}
