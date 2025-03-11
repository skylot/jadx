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
import jadx.zip.IZipEntry;

public class ProtoXmlResContainerFactory implements IResContainerFactory {
	private ResXmlProtoParser xmlParser;

	@Override
	public void init(RootNode root) {
		xmlParser = new ResXmlProtoParser(root);
	}

	@Override
	public @Nullable ResContainer create(ResourceFile resFile, InputStream inputStream) throws IOException {
		ResourceType type = resFile.getType();
		if (type != ResourceType.XML && type != ResourceType.MANIFEST) {
			return null;
		}
		IZipEntry zipEntry = resFile.getZipEntry();
		if (zipEntry == null) {
			return null;
		}
		boolean isFromAab = zipEntry.getZipFile().getPath().contains(".aab");
		if (!isFromAab) {
			return null;
		}
		ICodeInfo content = xmlParser.parse(inputStream);
		return ResContainer.textResource(resFile.getDeobfName(), content);
	}
}
