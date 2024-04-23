package jadx.api.plugins.resources;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;

import jadx.api.ResourceFile;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.ResContainer;

public interface IResContainerFactory {
	@Nullable
	ResContainer create(RootNode root, ResourceFile resFile, InputStream inputStream) throws IOException;
}
