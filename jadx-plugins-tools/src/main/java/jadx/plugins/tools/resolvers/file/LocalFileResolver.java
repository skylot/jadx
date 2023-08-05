package jadx.plugins.tools.resolvers.file;

import java.io.File;
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

	@Override
	public Optional<JadxPluginMetadata> resolve(String locationId) {
		if (!locationId.startsWith("file:") || !locationId.endsWith(".jar")) {
			return Optional.empty();
		}
		File jarFile = new File(removePrefix(locationId, "file:"));
		if (!jarFile.isFile()) {
			throw new RuntimeException("File not found: " + jarFile.getAbsolutePath());
		}
		JadxPluginMetadata metadata = new JadxPluginMetadata();
		metadata.setLocationId(locationId);
		metadata.setResolverId(id());
		metadata.setJar(jarFile.getAbsolutePath());
		return Optional.of(metadata);
	}
}
