package jadx.plugins.tools.resolvers;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import jadx.plugins.tools.data.JadxPluginMetadata;
import jadx.plugins.tools.resolvers.file.LocalFileResolver;
import jadx.plugins.tools.resolvers.github.GithubReleaseResolver;

public class ResolversRegistry {

	private static final Map<String, IJadxPluginResolver> RESOLVERS_MAP = new TreeMap<>();

	static {
		register(new LocalFileResolver());
		register(new GithubReleaseResolver());
	}

	private static void register(IJadxPluginResolver resolver) {
		RESOLVERS_MAP.put(resolver.id(), resolver);
	}

	public static Optional<JadxPluginMetadata> resolve(String locationId) {
		for (IJadxPluginResolver resolver : RESOLVERS_MAP.values()) {
			Optional<JadxPluginMetadata> result = resolver.resolve(locationId);
			if (result.isPresent()) {
				return result;
			}
		}
		return Optional.empty();
	}

	public static IJadxPluginResolver getById(String resolverId) {
		IJadxPluginResolver resolver = RESOLVERS_MAP.get(resolverId);
		if (resolver == null) {
			throw new IllegalArgumentException("Unknown resolverId: " + resolverId);
		}
		return resolver;
	}
}
