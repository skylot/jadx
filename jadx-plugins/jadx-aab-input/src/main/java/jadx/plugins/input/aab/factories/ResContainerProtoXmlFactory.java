package jadx.plugins.input.aab.factories;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeInfo;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.plugins.resources.IResContainerFactory;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.ResContainer;
import jadx.plugins.input.aab.parsers.ResXmlProtoParser;

public class ResContainerProtoXmlFactory implements IResContainerFactory {
	private ResXmlProtoParser xmlParser;

	@Override
	@Nullable
	public ResContainer create(RootNode root, ResourceFile resFile, InputStream inputStream) throws IOException {
		if (!root.isProto() || resFile.getType() != ResourceType.XML) {
			return null;
		}
		ICodeInfo content = getInstance(root).parse(inputStream);
		return ResContainer.textResource(resFile.getDeobfName(), content);
	}

	private synchronized ResXmlProtoParser getInstance(RootNode root) {
		if (xmlParser == null) {
			xmlParser = new ResXmlProtoParser(root);
		}
		return xmlParser;
	}
}
