package jadx.core.xmlgen;

import org.jetbrains.annotations.Nullable;

import jadx.api.ResourceFile;
import jadx.api.plugins.resources.IResTableParserProvider;
import jadx.core.dex.nodes.RootNode;

public class ResTableBinaryParserProvider implements IResTableParserProvider {
	private static IResTableParser parser = null;

	@Nullable
	public synchronized IResTableParser getInstance(RootNode root, ResourceFile resFile) {
		String fileName = resFile.getOriginalName();
		if (!fileName.endsWith(".arsc")) {
			return null;
		}
		if (parser == null) {
			parser = new ResTableBinaryParser(root);
		}
		return parser;
	}
}
