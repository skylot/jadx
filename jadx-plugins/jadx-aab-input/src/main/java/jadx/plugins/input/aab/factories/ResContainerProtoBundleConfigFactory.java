package jadx.plugins.input.aab.factories;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;

import com.android.bundle.Config;

import jadx.api.ICodeInfo;
import jadx.api.ResourceFile;
import jadx.api.impl.SimpleCodeInfo;
import jadx.api.plugins.resources.IResContainerFactory;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.ResContainer;

public class ResContainerProtoBundleConfigFactory implements IResContainerFactory {

	@Override
	@Nullable
	public ResContainer create(RootNode root, ResourceFile resFile, InputStream inputStream) throws IOException {
		if (!resFile.getOriginalName().endsWith("BundleConfig.pb")) {
			return null;
		}

		var bundleConfig = Config.BundleConfig.parseFrom(inputStream);
		ICodeInfo content = new SimpleCodeInfo(bundleConfig.toString());
		return ResContainer.textResource(resFile.getDeobfName(), content);
	}
}
