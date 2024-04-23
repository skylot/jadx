package jadx.plugins.input.aab;

import jadx.api.ResourcesLoader;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.plugins.input.aab.factories.ResContainerProtoBundleConfigFactory;
import jadx.plugins.input.aab.factories.ResContainerProtoTableFactory;
import jadx.plugins.input.aab.factories.ResContainerProtoXmlFactory;

public class ResProtoPlugin implements JadxPlugin {
	public static final String PLUGIN_ID = "aab-input";

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo(
				PLUGIN_ID,
				"Protobuf-encoded Resources Input",
				"Loads and protobuf-encoded XML and resource table files (used in AAB).");
	}

	@Override
	public synchronized void init(JadxPluginContext context) {
		ResTableProtoParserProvider tableParserProvider = new ResTableProtoParserProvider();
		ResourcesLoader.addResTableParserProvider(tableParserProvider);

		ResourcesLoader.addResContainerFactory(new ResContainerProtoTableFactory(tableParserProvider));
		ResourcesLoader.addResContainerFactory(new ResContainerProtoXmlFactory());
		ResourcesLoader.addResContainerFactory(new ResContainerProtoBundleConfigFactory());
	}
}
