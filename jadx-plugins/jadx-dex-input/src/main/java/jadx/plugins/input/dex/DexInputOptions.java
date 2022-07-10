package jadx.plugins.input.dex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jadx.api.plugins.options.OptionDescription;
import jadx.api.plugins.options.impl.BaseOptionsParser;
import jadx.api.plugins.options.impl.JadxOptionDescription;

public class DexInputOptions extends BaseOptionsParser {

	private static final String VERIFY_CHECKSUM_OPT = DexInputPlugin.PLUGIN_ID + ".verify-checksum";

	private boolean verifyChecksum = true;

	public void apply(Map<String, String> options) {
		verifyChecksum = getBooleanOption(options, VERIFY_CHECKSUM_OPT, true);
	}

	public List<OptionDescription> buildOptionsDescriptions() {
		return Collections.singletonList(
				new JadxOptionDescription(
						VERIFY_CHECKSUM_OPT,
						"verify dex file checksum before load",
						"yes",
						Arrays.asList("yes", "no")));
	}

	public boolean isVerifyChecksum() {
		return verifyChecksum;
	}
}
