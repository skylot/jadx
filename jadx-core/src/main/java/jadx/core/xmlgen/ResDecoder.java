package jadx.core.xmlgen;

import java.io.IOException;
import java.io.InputStream;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.ResourcesLoader;
import jadx.api.plugins.resources.IResTableParserProvider;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class ResDecoder {

	public static IResTableParser decode(RootNode root, ResourceFile resFile, InputStream is) throws IOException {
		if (resFile.getType() != ResourceType.ARSC) {
			throw new IllegalArgumentException("Unexpected resource type for decode: " + resFile.getType() + ", expect ARSC");
		}

		IResTableParser parser = null;
		for (IResTableParserProvider provider : ResourcesLoader.getResTableParserProviders()) {
			parser = provider.getInstance(root, resFile);
			if (parser != null) {
				break;
			}
		}

		if (parser == null) {
			throw new JadxRuntimeException("Unknown type of resource file: " + resFile.getOriginalName());
		}
		parser.decode(is);
		return parser;
	}
}
