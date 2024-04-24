package jadx.core.xmlgen;

import org.jetbrains.annotations.Nullable;

import jadx.api.ResourceFile;
import jadx.api.plugins.resources.IResTableParserProvider;
import jadx.core.dex.nodes.RootNode;

public class ResTableBinaryParserProvider implements IResTableParserProvider {
	private static IResTableParser parser = null;
	private RootNode root;

	@Nullable
	public synchronized IResTableParser getInstance(RootNode root, ResourceFile resFile) {
		String fileName = resFile.getOriginalName();
		if (!fileName.endsWith(".arsc")) {
			return null;
		}
		if (parser == null || this.root != root) { // Recompilation creates new RootNode.
			parser = new ResTableBinaryParser(root);
			this.root = root;
		}
		return parser;
	}
}
