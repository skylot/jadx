package jadx.plugins.input.res.proto;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.plugins.resources.IResContainerFactory;
import jadx.api.plugins.resources.IResTableParserProvider;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.IResTableParser;
import jadx.core.xmlgen.ResContainer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

public class ResContainerProtoTableFactory implements IResContainerFactory {
	private final IResTableParserProvider provider;

	public ResContainerProtoTableFactory(IResTableParserProvider provider) {
		this.provider = provider;
	}

	@Override
	@Nullable
	public synchronized ResContainer create(RootNode root, ResourceFile resFile, InputStream inputStream) throws IOException {
		if (!root.isProto() || resFile.getType() != ResourceType.ARSC) {
			return null;
		}
		IResTableParser parser = provider.getInstance(root, resFile);
		if (parser == null) {
			return null;
		}
		return parser.decodeFiles(inputStream);
	}
}
