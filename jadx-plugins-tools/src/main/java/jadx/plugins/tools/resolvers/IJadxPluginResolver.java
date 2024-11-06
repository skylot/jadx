package jadx.plugins.tools.resolvers;

import java.util.List;
import java.util.Optional;

import jadx.plugins.tools.data.JadxPluginMetadata;

public interface IJadxPluginResolver {

	/**
	 * Unique resolver identifier, should be same as locationId prefix
	 */
	String id();

	/**
	 * This resolver support updates and can fetch the latest version.
	 */
	boolean isUpdateSupported();

	/**
	 * Fetch the latest version plugin metadata by location
	 */
	Optional<JadxPluginMetadata> resolve(String locationId);

	/**
	 * Fetch several latest versions (pageable) of plugin by locationId.
	 *
	 * @param page    page number, starts with 1
	 * @param perPage result's count limit
	 */
	List<JadxPluginMetadata> resolveVersions(String locationId, int page, int perPage);

	/**
	 * Check if locationId has a specified version number
	 */
	boolean hasVersion(String locationId);
}
