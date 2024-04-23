package jadx.plugins.input.smali;

import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class SmaliInputOptions extends BasePluginOptionsBuilder {

	private int apiLevel;
	private int threads; // use jadx global threads count option

	@Override
	public void registerOptions() {
		intOption(SmaliInputPlugin.PLUGIN_ID + ".api-level")
				.description("Android API level")
				.defaultValue(27)
				.setter(v -> apiLevel = v);
	}

	public int getApiLevel() {
		return apiLevel;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}
}
