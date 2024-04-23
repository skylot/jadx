package jadx.api.plugins.resources;

import org.jetbrains.annotations.Nullable;

import jadx.api.ResourceFile;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.IResTableParser;

public interface IResTableParserProvider {
	@Nullable
	IResTableParser getInstance(RootNode root, ResourceFile resFile);
}
