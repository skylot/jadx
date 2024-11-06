package jadx.plugins.tools.resolvers;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jadx.plugins.tools.resolvers.file.LocalFileResolver;
import jadx.plugins.tools.resolvers.github.GithubReleaseResolver;

public class ResolversRegistry {

	private static final Map<String, IJadxPluginResolver> RESOLVERS_MAP = new HashMap<>();

	static {
		register(new LocalFileResolver());
		register(new GithubReleaseResolver());
	}

	private static void register(IJadxPluginResolver resolver) {
		RESOLVERS_MAP.put(resolver.id(), resolver);
	}

	public static IJadxPluginResolver getResolver(String locationId) {
		Objects.requireNonNull(locationId);
		int sep = locationId.indexOf(':');
		if (sep <= 0) {
			throw new IllegalArgumentException("Malformed locationId: " + locationId);
		}
		return getById(locationId.substring(0, sep));
	}

	public static IJadxPluginResolver getById(String resolverId) {
		IJadxPluginResolver resolver = RESOLVERS_MAP.get(resolverId);
		if (resolver == null) {
			throw new IllegalArgumentException("Unknown resolverId: " + resolverId);
		}
		return resolver;
	}
}
