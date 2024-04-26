package jadx.core.xmlgen;

import org.jetbrains.annotations.Nullable;

import jadx.api.ResourceFile;
import jadx.api.plugins.resources.IResTableParserProvider;
import jadx.core.dex.nodes.RootNode;

public class ResTableBinaryParserProvider implements IResTableParserProvider {
	private IResTableParser parser;

	@Override
	public void init(RootNode root) {
		parser = new ResTableBinaryParser(root);
	}

	@Override
	public synchronized @Nullable IResTableParser getParser(ResourceFile resFile) {
		String fileName = resFile.getOriginalName();
		if (!fileName.endsWith(".arsc")) {
			return null;
		}
		return parser;
	}
}
