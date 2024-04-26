package jadx.plugins.input.aab;

import org.jetbrains.annotations.Nullable;

import jadx.api.ResourceFile;
import jadx.api.plugins.resources.IResTableParserProvider;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.IResTableParser;
import jadx.plugins.input.aab.parsers.ResTableProtoParser;

public class ResTableProtoParserProvider implements IResTableParserProvider {
	private ResTableProtoParser parser;

	@Override
	public void init(RootNode root) {
		parser = new ResTableProtoParser(root);
	}

	@Override
	public synchronized @Nullable IResTableParser getParser(ResourceFile resFile) {
		String fileName = resFile.getOriginalName();
		if (!fileName.endsWith("resources.pb")) {
			return null;
		}
		return parser;
	}
}
