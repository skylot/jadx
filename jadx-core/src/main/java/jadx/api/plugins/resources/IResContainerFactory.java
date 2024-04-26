package jadx.api.plugins.resources;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;

import jadx.api.ResourceFile;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.ResContainer;

/**
 * Factory for {@link ResContainer}. Can be used in plugins via
 * {@code context.getResourcesLoader().addResContainerFactory()} to implement content parsing in
 * files with
 * different formats.
 */
public interface IResContainerFactory {

	/**
	 * Optional init method
	 */
	default void init(RootNode root) {
	}

	/**
	 * Checks if resource file is of expected format and tries to parse its content.
	 *
	 * @return {@link ResContainer} if file is of expected format, {@code null} otherwise.
	 */
	@Nullable
	ResContainer create(ResourceFile resFile, InputStream inputStream) throws IOException;
}
