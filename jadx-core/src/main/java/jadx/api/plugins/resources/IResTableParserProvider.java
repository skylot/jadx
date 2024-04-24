package jadx.api.plugins.resources;

import org.jetbrains.annotations.Nullable;

import jadx.api.ResourceFile;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.IResTableParser;

/**
 * Provides the resource table parser instance for specific resource table file format. Can be used
 * in plugins via {@code ResourcesLoader.addResTableParserProvider()} to parse resources from tables
 * in different formats.
 */
public interface IResTableParserProvider {

	/**
	 * Checks file format and provides the instance if the format is expected.
	 *
	 * @return {@link IResTableParser} if resource table is of expected format, {@code null} otherwise.
	 */
	@Nullable
	IResTableParser getInstance(RootNode root, ResourceFile resFile);
}
