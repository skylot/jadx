package jadx.plugins.input.aab;

import jadx.api.ResourcesLoader;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.plugins.input.aab.factories.ProtoBundleConfigResContainerFactory;
import jadx.plugins.input.aab.factories.ProtoTableResContainerFactory;
import jadx.plugins.input.aab.factories.ProtoXmlResContainerFactory;

public class AabInputPlugin implements JadxPlugin {
	public static final String PLUGIN_ID = "aab-input";

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo(
				PLUGIN_ID,
				".AAB Input",
				"Loads .AAB files.");
	}

	@Override
	public synchronized void init(JadxPluginContext context) {
		ResTableProtoParserProvider tableParserProvider = new ResTableProtoParserProvider();
		ResourcesLoader.addResTableParserProvider(tableParserProvider);

		ResourcesLoader.addResContainerFactory(new ProtoTableResContainerFactory(tableParserProvider));
		ResourcesLoader.addResContainerFactory(new ProtoXmlResContainerFactory());
		ResourcesLoader.addResContainerFactory(new ProtoBundleConfigResContainerFactory());
	}
}
