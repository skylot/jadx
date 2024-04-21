package jadx.plugins.input.res.proto;

import jadx.api.ResourceFile;
import jadx.api.plugins.resources.IResTableParserProvider;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.IResTableParser;
import jadx.core.xmlgen.ResTableBinaryParser;
import org.jetbrains.annotations.Nullable;

public class ResTableProtoParserProvider implements IResTableParserProvider {
	private static IResTableParser parser = null;

	@Nullable
	public synchronized IResTableParser getInstance(RootNode root, ResourceFile resFile) {
		String fileName = resFile.getOriginalName();
		if (!fileName.endsWith(".pb")) {
			return null;
		}
		if (parser == null) {
			parser = new ResTableProtoParser(root);
		}
		return parser;
	}
}
