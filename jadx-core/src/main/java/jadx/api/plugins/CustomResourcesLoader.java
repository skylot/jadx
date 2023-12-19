package jadx.api.plugins;

import jadx.api.ResourceFile;
import jadx.api.ResourcesLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public interface CustomResourcesLoader {
	/**
	 * Load resources from file to list of ResourceFile
	 *
	 * @param list list to add decoded resources
	 * @param file file to decode
	 * @return true if file was loaded
	 */
	boolean load(@NotNull ResourcesLoader loader, @NotNull List<ResourceFile> list, @NotNull File file);
}
