package jadx.plugins.input.aab.factories;

import java.io.IOException;
import java.io.InputStream;

import jadx.api.JadxDecompiler;
import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeInfo;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.plugins.resources.IResContainerFactory;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.ResContainer;
import jadx.plugins.input.aab.parsers.ResXmlProtoParser;

public class ProtoXmlResContainerFactory implements IResContainerFactory {
	private ResXmlProtoParser xmlParser;
	private RootNode root;

	@Override
	@Nullable
	public ResContainer create(JadxDecompiler jadxRef, ResourceFile resFile, InputStream inputStream) throws IOException {
		ResourceType type = resFile.getType();
		if (type != ResourceType.XML && type != ResourceType.MANIFEST) {
			return null;
		}
		ResourceFile.ZipRef ref = resFile.getZipRef();
		if (ref == null) {
			return null;
		}
		boolean isFromAab = ref.getZipFile().getPath().contains(".aab");
		if (!isFromAab) {
			return null;
		}
		ICodeInfo content = getInstance(jadxRef.getRoot()).parse(inputStream);
		return ResContainer.textResource(resFile.getDeobfName(), content);
	}

	private synchronized ResXmlProtoParser getInstance(RootNode root) {
		if (xmlParser == null || this.root != root) { // Recompilation creates new RootNode.
			xmlParser = new ResXmlProtoParser(root);
			this.root = root;
		}
		return xmlParser;
	}
}
