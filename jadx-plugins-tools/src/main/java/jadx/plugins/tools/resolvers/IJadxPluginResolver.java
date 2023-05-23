package jadx.plugins.tools.resolvers;

import java.util.Optional;

import jadx.plugins.tools.data.JadxPluginMetadata;

public interface IJadxPluginResolver {

	String id();

	boolean isUpdateSupported();

	Optional<JadxPluginMetadata> resolve(String locationId);
}
