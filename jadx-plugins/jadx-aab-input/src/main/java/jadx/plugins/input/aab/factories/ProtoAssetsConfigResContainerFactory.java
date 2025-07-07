package jadx.plugins.input.aab.factories;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;

import com.android.bundle.Files;

import jadx.api.ICodeInfo;
import jadx.api.ResourceFile;
import jadx.api.impl.SimpleCodeInfo;
import jadx.api.plugins.resources.IResContainerFactory;
import jadx.core.xmlgen.ResContainer;

public class ProtoAssetsConfigResContainerFactory implements IResContainerFactory {

	@Override
	public @Nullable ResContainer create(ResourceFile resFile, InputStream inputStream) throws IOException {
		if (!resFile.getOriginalName().endsWith("assets.pb")) {
			return null;
		}

		Files.Assets assetsConfig = Files.Assets.parseFrom(inputStream);
		ICodeInfo content = new SimpleCodeInfo(assetsConfig.toString());
		return ResContainer.textResource(resFile.getDeobfName(), content);
	}
}
