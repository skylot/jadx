package jadx.api.plugins.resources;

import jadx.api.ResourceFile;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.ResContainer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

public interface IResContainerFactory {
	@Nullable
	ResContainer create(RootNode root, ResourceFile resFile, InputStream inputStream) throws IOException;
}
