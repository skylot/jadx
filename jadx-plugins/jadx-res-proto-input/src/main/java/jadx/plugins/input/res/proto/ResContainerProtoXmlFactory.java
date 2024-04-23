package jadx.plugins.input.res.proto;

import com.android.bundle.Config;
import jadx.api.ICodeInfo;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.impl.SimpleCodeInfo;
import jadx.api.plugins.resources.IResContainerFactory;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.ResContainer;
import jadx.plugins.input.res.proto.parsers.ResXmlProtoParser;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

public class ResContainerProtoXmlFactory implements IResContainerFactory {
	private ResXmlProtoParser xmlParser;

	@Override
	@Nullable
	public synchronized ResContainer create(RootNode root, ResourceFile resFile, InputStream inputStream) throws IOException {
		if (resFile.getOriginalName().endsWith("BundleConfig.pb")) {
			var bundleConfig = Config.BundleConfig.parseFrom(inputStream);
			ICodeInfo content = new SimpleCodeInfo(bundleConfig.toString());
			return ResContainer.textResource(resFile.getDeobfName(), content);
		}

		if (!root.isProto() || resFile.getType() != ResourceType.XML) {
			return null;
		}
		ICodeInfo content = getInstance(root).parse(inputStream);
		return ResContainer.textResource(resFile.getDeobfName(), content);
	}

	synchronized private ResXmlProtoParser getInstance(RootNode root) {
		if (xmlParser == null) {
			xmlParser = new ResXmlProtoParser(root);
		}
		return xmlParser;
	}
}
