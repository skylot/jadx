package jadx.plugins.tools.resolvers.file;

import java.io.File;
import java.util.List;
import java.util.Optional;

import jadx.plugins.tools.data.JadxPluginMetadata;
import jadx.plugins.tools.resolvers.IJadxPluginResolver;

import static jadx.plugins.tools.utils.PluginUtils.removePrefix;

public class LocalFileResolver implements IJadxPluginResolver {
	@Override
	public String id() {
		return "file";
	}

	@Override
	public boolean isUpdateSupported() {
		return false;
	}

	private static boolean isValidFileLocation(String locationId) {
		return locationId.startsWith("file:")
				&& (locationId.endsWith(".jar") || locationId.endsWith(".zip"));
	}

	@Override
	public Optional<JadxPluginMetadata> resolve(String locationId) {
		if (!isValidFileLocation(locationId)) {
			return Optional.empty();
		}
		File pluginFile = new File(removePrefix(locationId, "file:"));
		if (!pluginFile.isFile()) {
			throw new RuntimeException("File not found: " + pluginFile.getAbsolutePath());
		}
		JadxPluginMetadata metadata = new JadxPluginMetadata();
		metadata.setLocationId(locationId);
		metadata.setPath(pluginFile.getAbsolutePath());
		return Optional.of(metadata);
	}

	@Override
	public List<JadxPluginMetadata> resolveVersions(String locationId, int page, int perPage) {
		if (page > 1) {
			// no other versions
			return List.of();
		}
		// return only the current file
		return resolve(locationId).map(List::of).orElseGet(List::of);
	}

	@Override
	public boolean hasVersion(String locationId) {
		// no supported
		return false;
	}
}
