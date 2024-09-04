package jadx.core.xmlgen;

import org.jetbrains.annotations.Nullable;

import jadx.api.ResourceFile;
import jadx.api.plugins.resources.IResTableParserProvider;
import jadx.core.dex.nodes.RootNode;

public class ResTableBinaryParserProvider implements IResTableParserProvider {
	private RootNode root;

	@Override
	public void init(RootNode root) {
		this.root = root;
	}

	@Override
	public @Nullable IResTableParser getParser(ResourceFile resFile) {
		String fileName = resFile.getOriginalName();
		if (!fileName.endsWith(".arsc")) {
			return null;
		}
		return new ResTableBinaryParser(root);
	}
}
