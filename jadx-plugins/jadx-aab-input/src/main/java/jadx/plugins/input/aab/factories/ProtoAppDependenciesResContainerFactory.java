package jadx.plugins.input.aab.factories;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;

import com.android.bundle.AppDependenciesOuterClass;

import jadx.api.ICodeInfo;
import jadx.api.ResourceFile;
import jadx.api.impl.SimpleCodeInfo;
import jadx.api.plugins.resources.IResContainerFactory;
import jadx.core.xmlgen.ResContainer;

public class ProtoAppDependenciesResContainerFactory implements IResContainerFactory {

	@Override
	public @Nullable ResContainer create(ResourceFile resFile, InputStream inputStream) throws IOException {
		if (!resFile.getOriginalName().endsWith("BUNDLE-METADATA/com.android.tools.build.libraries/dependencies.pb")) {
			return null;
		}

		AppDependenciesOuterClass.AppDependencies appDependencies = AppDependenciesOuterClass.AppDependencies.parseFrom(inputStream);
		ICodeInfo content = new SimpleCodeInfo(appDependencies.toString());
		return ResContainer.textResource(resFile.getDeobfName(), content);
	}
}
