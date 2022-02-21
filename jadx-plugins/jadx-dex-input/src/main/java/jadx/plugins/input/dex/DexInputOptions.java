package jadx.plugins.input.dex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jadx.api.plugins.options.OptionDescription;
import jadx.api.plugins.options.impl.JadxOptionDescription;

public class DexInputOptions {

	private static final String VERIFY_CHECKSUM_OPT = DexInputPlugin.PLUGIN_ID + ".verify-checksum";

	private boolean verifyChecksum = true;

	public void apply(Map<String, String> options) {
		verifyChecksum = getBooleanOption(options, VERIFY_CHECKSUM_OPT, true);
	}

	public List<OptionDescription> buildOptionsDescriptions() {
		List<OptionDescription> list = new ArrayList<>(1);
		list.add(new JadxOptionDescription(
				VERIFY_CHECKSUM_OPT,
				"Verify dex file checksum before load",
				"yes",
				Arrays.asList("yes", "no")));
		return list;
	}

	private boolean getBooleanOption(Map<String, String> options, String key, boolean defValue) {
		String val = options.get(key);
		if (val == null) {
			return defValue;
		}
		String valLower = val.toLowerCase(Locale.ROOT);
		if (valLower.equals("yes") || valLower.equals("true")) {
			return true;
		}
		if (valLower.equals("no") || valLower.equals("false")) {
			return false;
		}
		throw new IllegalArgumentException("Unknown value '" + val + "' for option '" + key + "'"
				+ ", expect: 'yes' or 'no'");
	}

	public boolean isVerifyChecksum() {
		return verifyChecksum;
	}
}
