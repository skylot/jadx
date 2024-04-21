package jadx.api.plugins.resources;

import jadx.api.ResourceFile;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.IResTableParser;
import org.jetbrains.annotations.Nullable;

public interface IResTableParserProvider {
	@Nullable
	IResTableParser getInstance(RootNode root, ResourceFile resFile);
}
