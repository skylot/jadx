package jadx.plugins.input.aab;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.resources.IResourcesLoader;
import jadx.plugins.input.aab.factories.ProtoAppDependenciesResContainerFactory;
import jadx.plugins.input.aab.factories.ProtoAssetsConfigResContainerFactory;
import jadx.plugins.input.aab.factories.ProtoBundleConfigResContainerFactory;
import jadx.plugins.input.aab.factories.ProtoNativeConfigResContainerFactory;
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
		IResourcesLoader resourcesLoader = context.getResourcesLoader();
		ResTableProtoParserProvider tableParserProvider = new ResTableProtoParserProvider();
		resourcesLoader.addResTableParserProvider(tableParserProvider);

		resourcesLoader.addResContainerFactory(new ProtoTableResContainerFactory(tableParserProvider));
		resourcesLoader.addResContainerFactory(new ProtoXmlResContainerFactory());
		resourcesLoader.addResContainerFactory(new ProtoBundleConfigResContainerFactory());
		resourcesLoader.addResContainerFactory(new ProtoAssetsConfigResContainerFactory());
		resourcesLoader.addResContainerFactory(new ProtoNativeConfigResContainerFactory());
		resourcesLoader.addResContainerFactory(new ProtoAppDependenciesResContainerFactory());
	}
}
