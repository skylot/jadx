package jadx.api.plugins;

import java.io.Closeable;
import java.io.File;
import java.util.List;

import jadx.api.ResourceFile;
import jadx.api.ResourcesLoader;

public interface CustomResourcesLoader extends Closeable {
	/**
	 * Load resources from file to list of ResourceFile
	 *
	 * @param list list to add loaded resources
	 * @param file file to load
	 * @return true if file was loaded
	 */
	boolean load(ResourcesLoader loader, List<ResourceFile> list, File file);
}
