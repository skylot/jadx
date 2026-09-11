package jadx.api.plugins.options.impl;

import java.util.List;
import java.util.Map;

import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.OptionDescription;

public class NoPluginOptions implements JadxPluginOptions {

	public static final JadxPluginOptions INSTANCE = new NoPluginOptions();

	private NoPluginOptions() {
		// singleton
	}

	@Override
	public void setOptions(Map<String, String> options) {
	}

	@Override
	public List<OptionDescription> getOptionsDescriptions() {
		return List.of();
	}
}
