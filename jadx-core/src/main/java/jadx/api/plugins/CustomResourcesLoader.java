package jadx.api.plugins;

import java.io.File;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import jadx.api.ResourceFile;
import jadx.api.ResourcesLoader;

public interface CustomResourcesLoader {
	/**
	 * Load resources from file to list of ResourceFile
	 *
	 * @param list list to add loaded resources
	 * @param file file to load
	 * @return true if file was loaded
	 */
	boolean load(@NotNull ResourcesLoader loader, @NotNull List<ResourceFile> list, @NotNull File file);
}
