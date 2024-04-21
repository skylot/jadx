package jadx.plugins.input.res.proto;

import jadx.api.ResourcesLoader;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;

public class ResProtoPlugin implements JadxPlugin {
	public static final String PLUGIN_ID = "res-proto-input";

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo(
				PLUGIN_ID,
				"Protobuf-encoded Resources Input",
				"Loads .pb and .xml (protobuf-encoded) files."
		);
	}

	@Override
	public synchronized void init(JadxPluginContext context) {
		ResTableProtoParserProvider tableParserProvider = new ResTableProtoParserProvider();
		ResourcesLoader.addResTableParserProvider(tableParserProvider);

		ResContainerProtoTableFactory tableFactory = new ResContainerProtoTableFactory(tableParserProvider);
		ResourcesLoader.addResContainerFactory(tableFactory);

		ResContainerProtoXmlFactory xmlFactory = new ResContainerProtoXmlFactory();
		ResourcesLoader.addResContainerFactory(xmlFactory);
	}
}
