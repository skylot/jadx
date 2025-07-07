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

public class ProtoNativeConfigResContainerFactory implements IResContainerFactory {

	@Override
	public @Nullable ResContainer create(ResourceFile resFile, InputStream inputStream) throws IOException {
		if (!resFile.getOriginalName().endsWith("native.pb")) {
			return null;
		}

		Files.NativeLibraries nativeConfig = Files.NativeLibraries.parseFrom(inputStream);
		ICodeInfo content = new SimpleCodeInfo(nativeConfig.toString());
		return ResContainer.textResource(resFile.getDeobfName(), content);
	}
}
