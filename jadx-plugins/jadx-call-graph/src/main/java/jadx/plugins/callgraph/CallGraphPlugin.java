package jadx.plugins.callgraph;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;

public class CallGraphPlugin implements JadxPlugin {
	public static final String PLUGIN_ID = "call-graph";

	private final CallGraphOptions options = new CallGraphOptions();

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId(PLUGIN_ID)
				.name("Call graph")
				.description("Export application call graph to JSON or DOT")
				.build();
	}

	@Override
	public void init(JadxPluginContext context) {
		context.registerOptions(options);
		if (options.getFormat() == CallGraphOptions.Format.NONE) {
			return;
		}
		context.addPass(new CallGraphPass(options));
	}
}
